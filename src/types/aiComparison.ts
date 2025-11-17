// AI vs DB 통계 비교 요청 파라미터
export interface AIComparisonParams {
  projectUuid: string;
  timeHours?: number;
  sampleSize?: number;
}

// DB 직접 조회 통계
export interface DBStatistics {
  totalLogs: number;
  errorCount: number;
  warnCount: number;
  infoCount: number;
  errorRate: number;
  peakHour: string;
  peakCount: number;
}

// AI(LLM) 추론 통계
export interface AIStatistics {
  estimatedTotalLogs: number;
  estimatedErrorCount: number;
  estimatedWarnCount: number;
  estimatedInfoCount: number;
  estimatedErrorRate: number;
  confidenceScore: number;
  reasoning: string;
}

// 정확도 지표
export interface AccuracyMetrics {
  totalLogsAccuracy: number;
  errorCountAccuracy: number;
  warnCountAccuracy: number;
  infoCountAccuracy: number;
  errorRateAccuracy: number;
  overallAccuracy: number;
  aiConfidence: number;
}

// 검증 결론
export interface ComparisonVerdict {
  grade: string; // 매우 우수/우수/양호/보통/미흡
  canReplaceDb: boolean;
  explanation: string;
  recommendations: string[];
}

// AI vs DB 통계 비교 응답
export interface AIComparisonResponse {
  projectUuid: string;
  analysisPeriodHours: number;
  sampleSize: number;
  analyzedAt: string;
  dbStatistics: DBStatistics;
  aiStatistics: AIStatistics;
  accuracyMetrics: AccuracyMetrics;
  verdict: ComparisonVerdict;
  technicalHighlights: string[];
}
