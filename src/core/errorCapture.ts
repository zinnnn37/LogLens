import { LightZone } from './lightZone';
import { LogCollector } from './logCollector';
import type { LogEntry } from '../types/logTypes';

// errorCapture.ts
class ErrorCapture {
  private static isInitialized = false;
  private static errorHandler: ((event: ErrorEvent) => void) | null = null;
  private static rejectionHandler:
    | ((event: PromiseRejectionEvent) => void)
    | null = null;

  static init(): void {
    if (this.isInitialized) {
      console.warn('[LogLens] ErrorCapture already initialized');
      return;
    }

    // 핸들러 저장
    this.errorHandler = (event: ErrorEvent) => {
      const error = event.error as MyError;
      const traceId = error?.__traceId || LightZone.getTraceId();

      const log: LogEntry = {
        '@timestamp': new Date().toISOString(),
        traceId,
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
    };

    this.rejectionHandler = (event: PromiseRejectionEvent) => {
      const error = event.reason as MyError;
      const traceId = error?.__traceId || LightZone.getTraceId();

      const log: LogEntry = {
        '@timestamp': new Date().toISOString(),
        traceId,
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
    };

    window.addEventListener('error', this.errorHandler);
    window.addEventListener('unhandledrejection', this.rejectionHandler);

    this.isInitialized = true;
    console.log('[LogLens] ErrorCapture initialized');
  }

  static isEnabled(): boolean {
    return this.isInitialized;
  }

  static reset(): void {
    if (this.errorHandler) {
      window.removeEventListener('error', this.errorHandler);
      this.errorHandler = null;
    }

    if (this.rejectionHandler) {
      window.removeEventListener('unhandledrejection', this.rejectionHandler);
      this.rejectionHandler = null;
    }

    this.isInitialized = false;
  }
}

export { ErrorCapture };
