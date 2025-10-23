// src/core/types.ts

export type LogLevel = 'ERROR' | 'WARN' | 'INFO';

export type LogEntry = {
  // logs 테이블
  traceId: string | null;
  logLevel: LogLevel;
  sourceType: 'FRONT';
  timestamp: string;
  comment: string | null;

  // log_details 테이블
  methodName: string | null;
  className: string | null;
  stackTrace: string | null;
  requestData: object | null;
  responseData: object | null;
  executionTime: number | null;
  additionalInfo: object | null;
};

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
  maxLogs?: number; // 최대 보관 로그 수
  autoFlush?: {
    enabled: boolean; // 자동 전송 활성화
    interval: number; // 전송 주기 (ms)
    endpoint: string; // 백엔드 엔드포인트
  };
};
