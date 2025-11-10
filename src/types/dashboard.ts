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
