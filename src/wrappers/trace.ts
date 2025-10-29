// src/wrappers/trace.ts

import { LightZone } from '../core/lightZone';
import { LogCollector } from '../core/logCollector';
import type { LogLensOptions } from '../types/logTypes';

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
          'run',
          'Object',
          'Module',
          'eval',
          'anonymous',
          'Function',
          'mountMemo',
          'useMemo',
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
 *
 * ✅ 진짜 최종 해결책:
 * - async 함수 감지 후 runAsync 내부에서 실행
 * - Promise 반환 함수는 run + then 체인
 * - 동기 함수는 run으로 처리
 */
function withLogLens<T extends (...args: any[]) => any>(
  fn: T,
  options?: LogLensOptions,
): T {
  const functionName =
    options?.logger ||
    (fn.name && fn.name !== 'anonymous' ? fn.name : null) ||
    extractFunctionName();

  // ✅ async 함수인지 미리 확인
  const isAsyncFunction = fn.constructor.name === 'AsyncFunction';

  return ((...args: any[]) => {
    const traceId = crypto.randomUUID();
    const shouldLog = !options?.skipLog;
    const startTime = Date.now();

    // 시작 로그
    if (shouldLog) {
      logStart(traceId, functionName, args);
    }

    // ✅ async 함수: runAsync 내부에서 실행
    if (isAsyncFunction) {
      return LightZone.runAsync({ traceId }, async () => {
        try {
          const value = await fn(...args);
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
    }

    // ✅ 동기 또는 Promise 반환 함수: run 내부에서 실행
    let result: any;
    let syncError: any = null;

    LightZone.run({ traceId }, () => {
      try {
        result = fn(...args);
      } catch (error) {
        syncError = error;
      }
    });

    // 동기 에러 처리
    if (syncError) {
      const duration = Date.now() - startTime;
      logError(traceId, functionName, syncError, duration);
      throw syncError;
    }

    // Promise 체크
    if (result && typeof result.then === 'function') {
      // ✅ Promise 반환 함수: then 체인으로 로깅
      return result
        .then((value: any) => {
          const duration = Date.now() - startTime;

          if (shouldLog) {
            logSuccess(traceId, functionName, value, duration);
          }

          return value;
        })
        .catch((error: any) => {
          const duration = Date.now() - startTime;
          logError(traceId, functionName, error, duration);
          throw error;
        });
    }

    // ✅ 동기 함수: 즉시 로깅 후 반환
    const duration = Date.now() - startTime;

    if (shouldLog) {
      logSuccess(traceId, functionName, result, duration);
    }

    return result;
  }) as T;
}

export { withLogLens };
