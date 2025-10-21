// src/core/types.ts

export type LogLevel = 'INFO' | 'WARN' | 'ERROR';

export type LogEntry = {
  '@timestamp': string;
  trace_id: string;
  level: LogLevel;
  logger: string; // 어디서 로그가 발생했는지
  message: string;
  exception?: {
    type: string;
    message: string;
    stacktrace: string;
  };
  layer: string;
  request?: {
    method?: string;
    uri?: string;
    user_id?: string;
  };
  context?: Record<string, any>;
  duration_ms?: number;
};

// 사용자 명시 가능한 옵션들
export type LogLensOptions = {
  name?: string;
  level?: LogLevel;
  logger?: string;
  includeArgs?: boolean;
  includeResult?: boolean;
  context?: Record<string, any>;
  skipLog?: boolean;
};

export type CollectorConfig = {
  maxLogs?: number;
  autoFlush?: {
    enabled: boolean;
    interval: number;
    endpoint: string;
  };
};
