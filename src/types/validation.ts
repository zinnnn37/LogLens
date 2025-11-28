/**
 * V2 RAG Validation Types
 *
 * AI 분석 결과의 출처, 유효성 검증, 메타데이터를 표현하는 타입 정의
 */

/**
 * 로그 출처 정보
 * AI 분석에 사용된 실제 로그 데이터
 */
export interface LogSource {
  /** 로그 ID */
  logId: string;
  /** 타임스탬프 (ISO 8601 형식) */
  timestamp: string;
  /** 로그 레벨 (ERROR, WARN, INFO 등) */
  level: string;
  /** 로그 메시지 (최대 500자) */
  message: string;
  /** 서비스 이름 */
  serviceName: string;
  /** 관련성 점수 (0.0 ~ 1.0, 높을수록 관련성 높음) */
  relevanceScore?: number;
  /** 클래스 이름 */
  className?: string;
  /** 메서드 이름 */
  methodName?: string;
}

/**
 * 유효성 검증 정보
 * AI 분석의 신뢰도 및 품질 메트릭
 */
export interface ValidationInfo {
  /** 신뢰도 (0-100, 높을수록 신뢰 가능) */
  confidence: number;
  /** 분석에 사용된 샘플 로그 수 */
  sampleCount: number;
  /** 샘플링 전략 */
  samplingStrategy:
    | 'single_log' // 단일 로그 분석
    | 'trace_id_filter' // Trace ID 기반 필터
    | 'proportional_vector_knn' // Vector 유사도 검색
    | 'direct_cache_hit' // 캐시 직접 히트
    | 'langgraph_agent_analysis' // LangGraph 에이전트 분석
    | 'aggregation_query' // 집계 쿼리
    | string;
  /** 샘플 커버리지 설명 */
  coverage: string;
  /** 데이터 품질 평가 */
  dataQuality: 'high' | 'medium' | 'low';
  /** 제한사항 (분석의 한계 설명) */
  limitation?: string;
  /** 추가 참고사항 */
  note?: string;
}

/**
 * 분석 메타데이터
 * 문서 생성 시 사용된 데이터 소스 및 통계 정보
 */
export interface AnalysisMetadata {
  /** 생성 시각 (ISO 8601 형식) */
  generatedAt: string;
  /** 데이터 분석 기간 (예: "2025-11-17 ~ 2025-11-18") */
  dataRange: string;
  /** 분석된 총 로그 수 */
  totalLogsAnalyzed: number;
  /** ERROR 레벨 로그 수 */
  errorLogs?: number;
  /** WARN 레벨 로그 수 */
  warnLogs?: number;
  /** INFO 레벨 로그 수 */
  infoLogs?: number;
  /** 로그 레벨별 샘플링 전략 */
  sampleStrategy: Record<string, string | number | boolean>;
  /** 분석의 제한사항 목록 */
  limitations: string[];
}

/**
 * 샘플링 전략 레이블 매핑
 */
export const SAMPLING_STRATEGY_LABELS: Record<string, string> = {
  single_log: '단일 로그 분석',
  trace_id_filter: 'Trace ID 기반 관련 로그',
  proportional_vector_knn: 'Vector 유사도 검색',
  direct_cache_hit: '캐시된 분석 결과',
  langgraph_agent_analysis: 'AI 에이전트 분석',
  aggregation_query: '집계 쿼리 기반 통계',
};

/**
 * 데이터 품질 레이블 및 색상 매핑
 */
export const DATA_QUALITY_CONFIG = {
  high: {
    label: '높음',
    color: 'green',
    description: '충분한 샘플 수와 높은 정확도',
  },
  medium: {
    label: '보통',
    color: 'yellow',
    description: '제한적인 샘플 수',
  },
  low: {
    label: '낮음',
    color: 'red',
    description: '단일 또는 극소수 샘플',
  },
} as const;

/**
 * 신뢰도 레벨 계산
 */
export const getConfidenceLevel = (
  confidence: number,
): {
  level: 'high' | 'medium' | 'low';
  label: string;
  color: string;
} => {
  if (confidence >= 85) {
    return { level: 'high', label: '매우 신뢰함', color: 'green' };
  } else if (confidence >= 70) {
    return { level: 'medium', label: '신뢰함', color: 'blue' };
  } else if (confidence >= 50) {
    return { level: 'medium', label: '보통', color: 'yellow' };
  } else {
    return { level: 'low', label: '낮음', color: 'red' };
  }
};
