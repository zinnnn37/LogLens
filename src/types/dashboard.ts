// 대시보드 통계 요청 파라미터
export interface DashboardOverviewParams {
  projectUuid: string;
  startTime?: string;
  endTime?: string;
}

export interface DashboardSummary {
  totalLogs: number;
  errorCount: number;
  warnCount: number;
  infoCount: number;
  avgResponseTime: number;
}

export interface DashboardPeriod {
  startTime: string;
  endTime: string;
}

// 대시보드 통계 개요 조회 응답
export interface DashboardOverviewData {
  projectUuid: string;
  period: DashboardPeriod;
  summary: DashboardSummary;
}

// API 호출 통계 조회
// 요청 파라미터
export interface DashboardApiStatsParams {
  projectUuid: string;
  startTime?: string; 
  endTime?: string; 
  limit?: number; 
}

// API 엔드포인트별 통계 정보
export interface ApiEndpointStats {
  id: number;
  endpointPath: string;
  httpMethod: string; 
  totalRequests: number;
  errorCount: number;
  errorRate: number; 
  avgResponseTime: number; 
  anomalyCount: number; 
  lastAccessed: string; 
}

// API 통계 요약 정보
export interface ApiStatsSummary {
  totalEndpoints: number;
  totalRequests: number;
  totalErrors: number;
  overallErrorRate: number;
  avgResponseTime: number;
  criticalEndpoints: number; 
}

// API 호출 통계 전체 응답 데이터
export interface DashboardApiStatsData {
  projectUuid: string; 
  period: DashboardPeriod; 
  endpoints: ApiEndpointStats[];
  summary: ApiStatsSummary;
}
