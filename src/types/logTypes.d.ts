// src/core/logTypes.d.ts

export type LogLevel = 'ERROR' | 'WARN' | 'INFO';

export type LogEntry = {
  timestamp: string;
  traceId: string | null;
  level: LogLevel;
  logger: string;
  message: string;
  layer: 'FRONT';
  request: {
    http?: {
      method: string;
      endpoint: string;
    };
    method: string;
    parameters?: any;
  } | null;
  response: any;
  executionTimeMs: number | null;
};

export type LogLensOptions = {
  logger?: string;
  skipLog?: boolean;
};

export type CollectorConfig = {
  maxLogs?: number;
  autoFlush?: {
    enabled: boolean;
    interval: number;
    endpoint: string;
  };
  isProduction?: boolean;
};

export type MaskConfig = {
  sensitive?: string[];
  sensitivePatterns?: string[];
  exclude?: string[];
  excludePatterns?: string[];
};
