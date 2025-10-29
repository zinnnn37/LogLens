// src/types/global.d.ts

declare global {
  var traceId: string | undefined;
  var logContext: Record<string, any> | undefined;

  interface MyError extends Error {
    traceId?: string;
    timestamp?: number;
    context?: Record<string, any>;
    stackInfo?: any;
  }
}

export {};
