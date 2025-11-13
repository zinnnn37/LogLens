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
  traceId: string;
  timestamp: string; // ISO 8601 date-time
  logLevel: 'WARN' | 'ERROR' | 'INFO';
  sourceType: 'FE' | 'BE' | 'INFRA';
  message: string;
  layer: string;
  logger: string;
  requesterIp: string;
  serviceName: string;
  methodName: string | null;
  threadName: string;
  duration: number | null;
  componentName:string;
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

/**
 * TraceId 기반 요청 흐름 조회 파라미터
 */
export interface TraceFlowParams {
  traceId: string;
  projectUuid: string;
}

/**
 * 요청 흐름 타임라인 항목
 */
export interface TimelineItem {
  sequence: number;
  componentId: number;
  componentName: string;
  layer: string;
  startTime: string;
  endTime: string;
  duration: number;
  logs: LogData[];
}

/**
 * 요청 흐름 컴포넌트 정보
 */
export interface FlowComponent {
  id: number;
  name: string;
  layer: string;
}

/**
 * 요청 흐름 그래프 간선
 */
export interface FlowEdge {
  from: number;
  to: number;
}

/**
 * 요청 흐름 그래프
 */
export interface FlowGraph {
  edges: FlowEdge[];
}

/**
 * 요청 흐름 요약 정보
 */
export interface FlowSummary {
  totalDuration: number;
  status: string;
  startTime: string;
  endTime: string;
}

/**
 * TraceId 기반 요청 흐름 조회 응답
 */
export interface TraceFlowResponse {
  traceId: string;
  projectUuid: string;
  summary: FlowSummary;
  timeline: TimelineItem[];
  components: FlowComponent[];
  graph: FlowGraph;
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
