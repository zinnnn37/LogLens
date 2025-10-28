// src/wrappers/trace.ts

import { LightZone } from '../core/lightZone';
import { LogCollector } from '../core/logCollector';
import type { LogLensOptions } from '../core/types';

/**
 * StackTrace에서 함수명 추출
 */
function extractFunctionName(): string {
  try {
    const error = new Error();
    const stack = error.stack;

    if (!stack) return 'anonymous';

    const lines = stack.split('\n');

    for (let i = 3; i < Math.min(lines.length, 8); i++) {
      const line = lines[i];

      let match =
        line.match(/at\s+(?:Object\.)?(\w+)\s*\(/) ||
        line.match(/at\s+(\w+)\s/) ||
        line.match(/^(\w+)@/);

      if (match && match[1]) {
        const name = match[1];

        const excludePatterns = [
          'withLogLens',
          'extractFunctionName',
          'runAsync',
          'Object',
          'Module',
          'eval',
          'anonymous',
          'Function',
        ];

        if (!excludePatterns.includes(name)) {
          return name;
        }
      }
    }

    return 'anonymous';
  } catch (err) {
    return 'anonymous';
  }
}

/**
 * 기본 로그 객체 생성
 */
function createBaseLog(
  traceId: string,
  functionName: string,
  level: 'INFO' | 'ERROR',
  message: string,
) {
  return {
    '@timestamp': new Date().toISOString(),
    traceId,
    level,
    logger: functionName,
    message,
    layer: 'FRONT' as const,
  };
}

/**
 * 시작 로그
 */
function logStart(traceId: string, functionName: string, args: any[]) {
  LogCollector.addLog({
    ...createBaseLog(traceId, functionName, 'INFO', `${functionName} called`),
    request: {
      method: functionName,
      parameters: args.length > 0 ? args : undefined,
    },
    response: null,
    executionTimeMs: null,
  });
}

/**
 * 성공 로그
 */
function logSuccess(
  traceId: string,
  functionName: string,
  response: any,
  duration: number,
) {
  LogCollector.addLog({
    ...createBaseLog(
      traceId,
      functionName,
      'INFO',
      `${functionName} completed`,
    ),
    request: {
      method: functionName,
    },
    response,
    executionTimeMs: duration,
  });
}

/**
 * 에러 로그
 */
function logError(
  traceId: string,
  functionName: string,
  error: any,
  duration: number,
) {
  LogCollector.addLog({
    ...createBaseLog(
      traceId,
      functionName,
      'ERROR',
      error?.stack || error?.message || String(error),
    ),
    request: {
      method: functionName,
    },
    response: null,
    executionTimeMs: duration,
  });
}

/**
 * 함수를 LogLens로 래핑 (자동 traceId 생성 및 전파)
 */
function withLogLens<T extends (...args: any[]) => any>(
  fn: T,
  options?: LogLensOptions,
): T {
  const functionName =
    options?.logger ||
    (fn.name && fn.name !== 'anonymous' ? fn.name : null) ||
    extractFunctionName();

  return ((...args: any[]) => {
    const traceId = crypto.randomUUID();
    const startTime = Date.now();
    const shouldLog = !options?.skipLog;

    // 시작 로그
    if (shouldLog) {
      logStart(traceId, functionName, args);
    }

    try {
      const result = fn(...args);

      if (result instanceof Promise) {
        // 비동기 처리
        return LightZone.runAsync({ traceId }, async () => {
          try {
            const value = await result;
            const duration = Date.now() - startTime;

            if (shouldLog) {
              logSuccess(traceId, functionName, value, duration);
            }

            return value;
          } catch (error: any) {
            const duration = Date.now() - startTime;
            logError(traceId, functionName, error, duration);
            throw error;
          }
        });
      } else {
        // 동기 처리
        return LightZone.run({ traceId }, () => {
          const duration = Date.now() - startTime;

          if (shouldLog) {
            logSuccess(traceId, functionName, result, duration);
          }

          return result;
        });
      }
    } catch (error: any) {
      const duration = Date.now() - startTime;
      logError(traceId, functionName, error, duration);
      throw error;
    }
  }) as T;
}

export { withLogLens };
