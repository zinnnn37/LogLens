// src/core/logger.ts

import { LightZone } from './lightZone';
import { LogCollector } from './logCollector';
import type { LogLevel } from '../types/logTypes';

const loglens = {
  info: (message: string, context?: any) => {
    const traceId = LightZone.getTraceId();
    LogCollector.addLog({
      timestamp: new Date().toISOString(),
      traceId: traceId || null,
      level: 'INFO' as LogLevel,
      logger: 'loglens',
      message,
      layer: 'FRONT',
      request: null,
      response: context || null,
      executionTimeMs: null,
    });
  },

  warn: (message: string, context?: any) => {
    const traceId = LightZone.getTraceId();
    LogCollector.addLog({
      timestamp: new Date().toISOString(),
      traceId: traceId || null,
      level: 'WARN' as LogLevel,
      logger: 'loglens',
      message,
      layer: 'FRONT',
      request: null,
      response: context || null,
      executionTimeMs: null,
    });
  },

  error: (message: string, error?: Error) => {
    const traceId = LightZone.getTraceId();
    LogCollector.addLog({
      timestamp: new Date().toISOString(),
      traceId: traceId || null,
      level: 'ERROR' as LogLevel,
      logger: 'loglens',
      message: error?.stack || message,
      layer: 'FRONT',
      request: null,
      response: null,
      executionTimeMs: null,
    });
  },

  // 추가
  send: async (): Promise<void> => {
    return LogCollector.send();
  },

  flush: async (): Promise<void> => {
    return LogCollector.flush();
  },

  getLogs: () => {
    return LogCollector.getLogs();
  },

  clear: () => {
    LogCollector.clear();
  },
};

export { loglens };
