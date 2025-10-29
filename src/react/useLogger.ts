// src/react/useLogger.ts

import { LightZone } from '../core/lightZone';
import { LogCollector } from '../core/logCollector';
import type { LogLevel } from '../types/logTypes';

type Logger = {
  info: (message: string, context?: any) => void;
  warn: (message: string, context?: any) => void;
  error: (message: string, error?: Error) => void;
};

export function useLogger(name?: string): Logger {
  const loggerName = name || extractComponentName();

  return {
    info: (message: string, context?: any) => {
      LogCollector.addLog({
        '@timestamp': new Date().toISOString(),
        traceId: LightZone.getTraceId() || null,
        level: 'INFO' as LogLevel,
        logger: loggerName,
        message,
        layer: 'FRONT',
        request: null,
        response: context || null,
        executionTimeMs: null,
      });
    },

    warn: (message: string, context?: any) => {
      LogCollector.addLog({
        '@timestamp': new Date().toISOString(),
        traceId: LightZone.getTraceId() || null,
        level: 'WARN' as LogLevel,
        logger: loggerName,
        message,
        layer: 'FRONT',
        request: null,
        response: context || null,
        executionTimeMs: null,
      });
    },

    error: (message: string, error?: any) => {
      LogCollector.addLog({
        '@timestamp': new Date().toISOString(),
        traceId: LightZone.getTraceId() || null,
        level: 'ERROR' as LogLevel,
        logger: loggerName,
        message: error?.stack || message,
        layer: 'FRONT',
        request: null,
        response: null,
        executionTimeMs: null,
      });
    },
  };
}

function extractComponentName(): string {
  try {
    const error = new Error();
    const stack = error.stack;

    if (!stack) return 'anonymous';

    const lines = stack.split('\n');

    for (let i = 3; i < Math.min(lines.length, 6); i++) {
      const line = lines[i];

      let match = line.match(/at\s+([A-Z]\w+)/);
      if (!match) {
        match = line.match(/^([A-Z]\w+)@/);
      }

      if (match && match[1]) {
        const name = match[1];

        const excludePatterns = [
          'useLogger',
          'extractComponentName',
          'renderWithHooks',
          'updateFunctionComponent',
          'beginWork',
          'performUnitOfWork',
          'workLoop',
          'Object',
          'Module',
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
