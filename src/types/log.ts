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
  logLevel: 'WARN' | 'ERROR';
  sourceType: 'FE' | 'BE';
  message: string;
}

export interface LogSearchResponse {
  logs: LogData[];
  pagination: {
    nextCursor: string | null;
    hasNext: boolean;
    size: number;
  };
}
