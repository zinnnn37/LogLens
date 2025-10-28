import { LightZone } from './lightZone';
import { LogCollector } from './logCollector';
import type { LogEntry } from './types';

class ErrorCapture {
  private static isInitialized = false;

  static init(): void {
    if (this.isInitialized) {
      console.warn('[LogLens] ErrorCapture already initialized');
      return;
    }

    // 전역 에러 캡처
    window.addEventListener('error', (event) => {
      const log: LogEntry = {
        '@timestamp': new Date().toISOString(),
        traceId: LightZone.getTraceId(),
        level: 'ERROR',
        logger: 'ErrorCapture',
        message: `Uncaught error: ${event.error?.message || event.message}\n${
          event.error?.stack || ''
        }`,
        layer: 'FRONT',
        request: null,
        response: {
          filename: event.filename,
          lineno: event.lineno,
          colno: event.colno,
        },
        executionTimeMs: null,
      };

      LogCollector.addLog(log);
    });

    // Promise rejection 에러
    window.addEventListener('unhandledrejection', (event) => {
      const log: LogEntry = {
        '@timestamp': new Date().toISOString(),
        traceId: LightZone.getTraceId(),
        level: 'ERROR',
        logger: 'ErrorCapture',
        message: `Unhandled promise rejection: ${
          event.reason?.message || event.reason
        }\n${event.reason?.stack || ''}`,
        layer: 'FRONT',
        request: null,
        response: {
          reason: event.reason,
        },
        executionTimeMs: null,
      };

      LogCollector.addLog(log);
    });

    this.isInitialized = true;
    console.log('[LogLens] ErrorCapture initialized');
  }

  static isEnabled(): boolean {
    return this.isInitialized;
  }

  static reset(): void {
    this.isInitialized = false;
  }
}

export { ErrorCapture };
