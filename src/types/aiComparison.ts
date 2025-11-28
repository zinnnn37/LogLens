// AI vs DB 통계 비교 요청 파라미터
export interface AIComparisonParams {
  projectUuid: string;
  timeHours?: number;
  sampleSize?: number;
}

// DB 직접 조회 통계
export interface DBStatistics {
  total_logs: number;
  error_count: number;
  warn_count: number;
  info_count: number;
  error_rate: number;
  peak_hour: string;
  peak_count: number;
}

// AI(LLM) 추론 통계
export interface AIStatistics {
  estimated_total_logs: number;
  estimated_error_count: number;
  estimated_warn_count: number;
  estimated_info_count: number;
  estimated_error_rate: number;
  confidence_score: number;
  reasoning: string;
}

// 정확도 지표
export interface AccuracyMetrics {
  total_logs_accuracy: number;
  error_count_accuracy: number;
  warn_count_accuracy: number;
  info_count_accuracy: number;
  error_rate_accuracy: number;
  overall_accuracy: number;
  ai_confidence: number;
}

// 검증 결론
export interface ComparisonVerdict {
  grade: string; // 매우 우수/우수/양호/보통/미흡
  can_replace_db: boolean;
  explanation: string;
  recommendations: string[];
}

// AI vs DB 통계 비교 응답
export interface AIComparisonResponse {
  project_uuid: string;
  analysis_period_hours: number;
  sample_size: number;
  analyzed_at: string;
  db_statistics: DBStatistics | null;
  ai_statistics: AIStatistics | null;
  accuracy_metrics: AccuracyMetrics | null;
  verdict: ComparisonVerdict;
  technical_highlights: string[] | null;
}

// ERROR 비교 전용 타입
export interface ErrorComparisonParams {
  projectUuid: string;
  timeHours?: number;
  sampleSize?: number;
}

export interface DBErrorStats {
  total_errors: number;
  error_rate: number;
  peak_error_hour: string | null;
  peak_error_count: number | null;
}

export interface AIErrorStats {
  estimated_total_errors: number;
  estimated_error_rate: number;
  confidence_score: number;
  reasoning: string;
}

export interface ErrorAccuracyMetrics {
  error_count_accuracy: number;
  error_rate_accuracy: number;
  overall_accuracy: number;
}

export interface VectorGroupingInfo {
  vectorized_error_count: number;
  vectorization_rate: number;
  sampling_method: string;
  sample_distribution: string;
}

export interface ErrorComparisonResponse {
  project_uuid: string;
  analysis_period_hours: number;
  sample_size: number;
  analyzed_at: string;
  db_error_stats: DBErrorStats;
  ai_error_stats: AIErrorStats;
  accuracy_metrics: ErrorAccuracyMetrics;
  vector_analysis: VectorGroupingInfo | null;
}
