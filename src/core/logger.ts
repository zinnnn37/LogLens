// src/core/logger.ts

import TraceContext from './traceContext';
import { LogCollector } from './logCollector';

const loglens = {
  info: (message: string, context?: any) => {
    const traceId = TraceContext.getCurrentTraceId();
    LogCollector.addLog({
      timestamp: new Date().toISOString(),
      traceId: traceId || null,
      logLevel: 'INFO',
      sourceType: 'FRONT',
      comment: message,
      methodName: null,
      className: null,
      stackTrace: null,
      requestData: null,
      responseData: null,
      executionTime: null,
      additionalInfo: context || null,
    });
  },

  warn: (message: string, context?: any) => {
    const traceId = TraceContext.getCurrentTraceId();
    LogCollector.addLog({
      timestamp: new Date().toISOString(),
      traceId: traceId || null,
      logLevel: 'WARN',
      sourceType: 'FRONT',
      comment: message,
      methodName: null,
      className: null,
      stackTrace: null,
      requestData: null,
      responseData: null,
      executionTime: null,
      additionalInfo: context || null,
    });
  },

  error: (message: string, error?: Error) => {
    const traceId = TraceContext.getCurrentTraceId();
    LogCollector.addLog({
      timestamp: new Date().toISOString(),
      traceId: traceId || null,
      logLevel: 'ERROR',
      sourceType: 'FRONT',
      comment: message,
      stackTrace: error?.stack || null,
      methodName: null,
      className: null,
      requestData: null,
      responseData: null,
      executionTime: null,
      additionalInfo: null,
    });
  },
};

export { loglens };
