// src/types/log.ts

// 로그 조회 API 타입 정의

export interface LogSearchParams {
  projectUuid: string;
  cursor?: string;
  size?: number;
  startTime?: string; // ISO 8601 date-time
  endTime?: string; // ISO 8601 date-time
  logLevel?: string[]; // ['WARN', 'ERROR']
  sourceType?: string[]; // ['FE', 'BE']
  keyword?: string;
  traceId?: string;
  sort?: string; // "필드,방향" 예: "timestamp,desc"
}

export interface LogData {
  logId: string;
  traceId: string;
  timestamp: string; // ISO 8601 date-time
  logLevel: 'WARN' | 'ERROR' | 'INFO';
  sourceType: 'FE' | 'BE' | 'INFRA';
  message: string;
  layer: string;
  logger: string;
  comment: string | null;
}

/**
 * 로그 검색 응답
 */
export interface LogSearchResponse {
  logs: LogData[];
  pagination: {
    nextCursor: string | null;
    hasNext: boolean;
    size: number;
  };
}

/**
 * TraceId 응답 summary 타입
 */
export interface LogSummary {
  totalLogs: number;
  durationMs: number;
  startTime: string; // ISO 8601 date-time
  endTime: string; // ISO 8601 date-time
  errorCount: number;
  warnCount: number;
  infoCount: number;
}

/**
 * TraceId 검색 응답
 */
export interface TraceIdSearchResponse {
  traceId: string;
  summary: LogSummary;
  logs: LogData[];
}
