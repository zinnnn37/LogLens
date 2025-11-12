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
  logId: number;
  projectUuid: string;
  traceId: string;
  timestamp: string; // ISO 8601 date-time
  logLevel: 'WARN' | 'ERROR' | 'INFO';
  sourceType: 'FE' | 'BE' | 'INFRA';
  message: string;
  layer: string;
  logger: string;
  comment: string | null;
  requesterIp: string | null;
  serviceName: string | null;
  className: string | null;
  methodName: string | null;
  threadName: string | null;
  stackTrace: string | null;
  componentName: string | null;
  duration: number | null;
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

/**
 * TraceId 기반 로그 조회 파라미터
 */
export interface TraceLogsParams {
  traceId: string;
  projectUuid: string;
}

/**
 * TraceId 기반 로그 조회 응답
 */
export interface TraceLogsResponse {
  traceId: string;
  projectUuid: string;
  request: LogData;
  response: LogData;
  duration: number;
  status: string;
  logs: LogData[];
}

// 로그 상세 조회
export interface LogDetailParams {
  logId: number;
  projectUuid: string;
}

// AI 분석 결과 데이터
export interface LogAnalysisData {
  summary: string;
  error_cause: string;
  solution: string;
  tags: string[];
  analysisType: string;
  targetType: string;
  analyzedAt: string;
}

// 로그 상세 정보 응답 (전체 구조 반영)
export interface LogDetailResponse {
  logId: number;
  traceId: string;
  logLevel: 'WARN' | 'ERROR' | 'INFO';
  sourceType: 'FE' | 'BE' | 'INFRA';
  message: string;
  timestamp: string;
  logger: string;
  layer: string;
  comment: string | null;
  serviceName: string | null;
  className: string | null;
  methodName: string | null;
  threadName: string | null;
  requesterIp: string | null;
  duration: number | null;
  stackTrace: string | null;
  logDetails: Record<string, unknown> | null;
  analysis: LogAnalysisData | null;
  fromCache: boolean | null;
  similarLogId: number | null;
  similarityScore: number | null;
}

// 실시간 로그 스트리밍(SSE)
export type LogStreamParams = LogSearchParams;