// src/wrappers/trace.ts

import { LightZone } from '../core/lightZone';
import { LogCollector } from '../core/logCollector';
import type { LogLensOptions } from '../types/logTypes';

/**
 * StackTrace에서 함수명 추출
 */
// function extractFunctionName(): string {
//   try {
//     const error = new Error();
//     const stack = error.stack;

//     if (!stack) return 'anonymous';

//     const lines = stack.split('\n');

//     for (let i = 3; i < Math.min(lines.length, 8); i++) {
//       const line = lines[i];

//       let match =
//         line.match(/at\s+(?:Object\.)?(\w+)\s*\(/) ||
//         line.match(/at\s+(\w+)\s/) ||
//         line.match(/^(\w+)@/);

//       if (match && match[1]) {
//         const name = match[1];

//         const excludePatterns = [
//           'withLogLens',
//           'extractFunctionName',
//           'runAsync',
//           'run',
//           'Object',
//           'Module',
//           'eval',
//           'anonymous',
//           'Function',
//           'mountMemo',
//           'useMemo',
//         ];

//         if (!excludePatterns.includes(name)) {
//           return name;
//         }
//       }
//     }

//     return 'anonymous';
//   } catch (err) {
//     return 'anonymous';
//   }
// }

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
    timestamp: new Date().toISOString(),
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
 * - async 함수 감지 후 runAsync 내부에서 실행
 * - Promise 반환 함수는 run + then 체인
 * - 동기 함수는 run으로 처리
 */
// src/wrappers/trace.ts

// src/wrappers/trace.ts

function withLogLens<T extends (...args: any[]) => any>(
  fn: T,
  options?: LogLensOptions,
): T {
  const functionName = options?.logger || fn.name || 'anonymous';
  const isAsyncFunction = fn.constructor.name === 'AsyncFunction';

  return ((...args: any[]) => {
    const shouldLog = !options?.skipLog;
    const startTime = Date.now();

    // ✅ 기존 traceId 확인
    const existingTraceId = LightZone.getTraceId();
    const traceId = existingTraceId || crypto.randomUUID();
    const isNewTrace = !existingTraceId;

    // 시작 로그 (새 trace만)
    if (shouldLog && isNewTrace) {
      logStart(traceId, functionName, args);
    }

    // ✅ async 함수
    if (isAsyncFunction) {
      // 이미 컨텍스트 있으면 runAsync 스킵
      if (existingTraceId) {
        return (async () => {
          try {
            const value = await fn(...args);
            const duration = Date.now() - startTime;

            if (shouldLog && isNewTrace) {
              logSuccess(traceId, functionName, value, duration);
            }

            return value;
          } catch (error: any) {
            const duration = Date.now() - startTime;
            logError(traceId, functionName, error, duration);
            throw error;
          }
        })();
      } else {
        // 새 컨텍스트 생성
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
    }

    // ✅ 동기/Promise 반환 함수
    let result: any;
    let syncError: any = null;

    // 컨텍스트 있으면 run 스킵
    if (existingTraceId) {
      try {
        result = fn(...args);
      } catch (error) {
        syncError = error;
      }
    } else {
      LightZone.run({ traceId }, () => {
        try {
          result = fn(...args);
        } catch (error) {
          syncError = error;
        }
      });
    }

    if (syncError) {
      const duration = Date.now() - startTime;
      logError(traceId, functionName, syncError, duration);
      throw syncError;
    }

    // Promise 체크
    if (result && typeof result.then === 'function') {
      return result
        .then((value: any) => {
          const duration = Date.now() - startTime;

          if (shouldLog && isNewTrace) {
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

    // 동기 완료
    const duration = Date.now() - startTime;

    if (shouldLog && isNewTrace) {
      logSuccess(traceId, functionName, result, duration);
    }

    return result;
  }) as T;
}

export { withLogLens };
