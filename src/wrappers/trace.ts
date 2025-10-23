// src/wrappers/trace.ts

import TraceContext from '../core/traceContext';
import { LogCollector } from '../core/logCollector';
import type { LogLevel, LogLensOptions } from '../core/types';

/**
 * 함수를 TraceID 관리 + 로그 수집으로 감싸기
 *
 * @example
 * 비동기 함수
 * const fetchUser = withLogLens(async (userId) => {
 *   const response = await fetch(`/api/users/${userId}`);
 *   return response.json();
 * }, { logger: 'API.fetchUser' });
 *
 * @example
 * 동기 함수
 * const calculateSum = withLogLens((a, b) => {
 *   return a + b;
 * }, { logger: 'Math.sum' });
 *
 * @example
 * 옵션 사용
 * const updateUser = withLogLens(async (userId, data) => {
 *   const response = await fetch(`/api/users/${userId}`, {
 *     method: 'PUT',
 *     body: JSON.stringify(data)
 *   });
 *   return response.json();
 * }, {
 *   logger: 'UserAPI',
 *   includeArgs: true,
 *   includeResult: true
 * });
 */

function withLogLens<T extends (...args: any[]) => any>(
  fn: T,
  options?: LogLensOptions,
): T {
  // 함수 정의 시점에 이름 결정
  const functionName =
    options?.name || options?.logger || fn.name || 'anonymous';
  const level: LogLevel = options?.level || 'INFO';

  return ((...args: any[]) => {
    // TraceID 추출
    const explicitTraceId = args.find((arg) => arg?._traceId)?._traceId;
    const traceId = TraceContext.startTrace(explicitTraceId);

    const startTime = Date.now();
    const shouldLog = !options?.skipLog;

    // 함수 실행 전 로그
    if (shouldLog) {
      LogCollector.addLog({
        timestamp: new Date().toISOString(),
        traceId: traceId,
        sourceType: 'FRONT',
        logLevel: level,
        comment: `${functionName} called`,
        methodName: functionName,
        className: null,
        stackTrace: null,
        requestData: null,
        responseData: null,
        executionTime: null,
        additionalInfo: options?.includeArgs ? { args } : null,
      });
    }

    try {
      const result = fn(...args);

      // Promise인지 확인 (비동기 함수)
      if (result instanceof Promise) {
        return result
          .then((value) => {
            const duration = Date.now() - startTime;

            // 성공 로그
            if (shouldLog) {
              LogCollector.addLog({
                timestamp: new Date().toISOString(),
                traceId: traceId,
                logLevel: level,
                sourceType: 'FRONT',
                comment: `${functionName} completed`,
                methodName: functionName,
                className: null,
                stackTrace: null,
                requestData: null,
                responseData: null,
                executionTime: duration,
                additionalInfo: {
                  ...(options?.includeResult ? { result: value } : {}),
                  ...(options?.context || {}),
                },
              });
            }

            return value;
          })
          .catch((error: any) => {
            const duration = Date.now() - startTime;

            // 에러 로그
            LogCollector.addLog({
              timestamp: new Date().toISOString(),
              traceId: traceId,
              logLevel: 'ERROR',
              sourceType: 'FRONT',
              comment: `${functionName} failed`,
              methodName: functionName,
              className: null,
              stackTrace: null,
              requestData: null,
              responseData: null,
              executionTime: duration,
              additionalInfo: {
                ...(options?.context || {}),
              },
            });

            throw error;
          })
          .finally(() => {
            TraceContext.endTrace();
          });
      } else {
        // 동기 함수
        const duration = Date.now() - startTime;

        // 성공 로그
        if (shouldLog) {
          LogCollector.addLog({
            timestamp: new Date().toISOString(),
            traceId,
            sourceType: 'FRONT',
            logLevel: level,
            comment: `${functionName} completed`,
            methodName: functionName,
            className: null,
            stackTrace: null,
            requestData: null,
            responseData: null,
            executionTime: duration,
            additionalInfo: {
              ...(options?.includeResult ? { result } : {}),
              ...(options?.context || {}),
            },
          });
        }

        TraceContext.endTrace();
        return result;
      }
    } catch (error: any) {
      // 동기 함수에서 에러 발생
      const duration = Date.now() - startTime;

      // 에러 로그
      LogCollector.addLog({
        timestamp: new Date().toISOString(),
        traceId: traceId,
        logLevel: 'ERROR',
        sourceType: 'FRONT',
        comment: `${functionName} failed`,
        methodName: functionName,
        className: null,
        stackTrace: null,
        requestData: null,
        responseData: null,
        executionTime: duration,
        additionalInfo: {
          type: error.constructor?.name || 'Error',
          message: error.message || String(error),
          stacktrace: error.stack || '',
        },
      });

      TraceContext.endTrace();
      throw error;
    }
  }) as T;
}

export { withLogLens };
