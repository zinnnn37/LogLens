// src/wrappers/trace.ts

import TraceContext from '../core/traceContext';
import LogCollector from '../core/logCollector';
import type { LogLevel, LogLensOptions } from '../core/types';

/**
 * 함수를 TraceID 관리 + 로그 수집으로 감싸기
 *
 * @example
 * 기본 사용 - 사용하고자 하는 함수를 withLogLens로 감싸기
 * const fetchUser = withLogLens(async (userId) => {
 *   const response = await fetch(`/api/users/${userId}`);
 *   return response.json();
 * });
 *
 * @example
 * 옵션 사용 - 파라미터에 옵션 객체 전달
 * const updateUser = withLogLens(async (userId, data) => {
 *   const response = await fetch(`/api/users/${userId}`, {
 *     method: 'PUT',
 *     body: JSON.stringify(data)
 *   });
 *   return response.json();
 * }, {
 *   level: 'INFO',
 *   logger: 'UserAPI',
 *   includeArgs: true,
 *   includeResult: true
 * });
 *
 * @example
 * TraceID만 관리 (로그 안 남김)
 * const login = withLogLens(async () => {
 *   await validate();
 *   await fetchUser();
 * }, { skipLog: true });
 */

function withLogLens<T extends (...args: any[]) => Promise<any>>(
  fn: T,
  options?: LogLensOptions,
): T {
  // 함수 정의 시점에 이름 결정
  const functionName = options?.name || fn.name || 'anonymous';
  const logger = options?.logger || fn.name || 'function';
  const level: LogLevel = options?.level || 'INFO';

  return (async (...args: any[]) => {
    // TraceID 추출
    const explicitTraceId = args.find((arg) => arg?._traceId)?._traceId;
    const traceId = TraceContext.startTrace(explicitTraceId);

    const startTime = Date.now();
    const shouldLog = !options?.skipLog;

    // 함수 실행 전 로그
    if (shouldLog && options?.includeArgs) {
      LogCollector.addLog({
        '@timestamp': new Date().toISOString(),
        trace_id: traceId,
        level,
        logger,
        message: `${functionName} called`,
        context: { args },
        layer: 'function',
      });
    }

    try {
      const result = await fn(...args);
      const duration = Date.now() - startTime;

      // 성공 로그
      if (shouldLog) {
        LogCollector.addLog({
          '@timestamp': new Date().toISOString(),
          trace_id: traceId,
          level,
          logger,
          message: `${functionName} completed`,
          context: {
            ...(options?.includeResult ? { result } : {}),
            ...(options?.context || {}),
          },
          duration_ms: duration,
          layer: 'function',
        });
      }

      return result;
    } catch (error: any) {
      const duration = Date.now() - startTime;

      // 에러 로그
      LogCollector.addLog({
        '@timestamp': new Date().toISOString(),
        trace_id: traceId,
        level: 'ERROR',
        logger,
        message: `${functionName} failed`,
        exception: {
          type: error.constructor?.name || 'Error',
          message: error.message || String(error),
          stacktrace: error.stack || '',
        },
        context: options?.context,
        duration_ms: duration,
        layer: 'function',
      });

      throw error;
    } finally {
      TraceContext.endTrace();
    }
  }) as T;
}

export default withLogLens;
