// AI 분석 문서 관련 타입 정의

/**
 * 문서 형식
 */
export type DocumentFormat = 'HTML' | 'PDF' | 'MARKDOWN' | 'JSON';

/**
 * 문서 타입
 */
export type DocumentType = 'PROJECT_ANALYSIS' | 'ERROR_ANALYSIS';

/**
 * 프로젝트 분석 요청
 */
export interface ProjectAnalysisRequest {
  /**
   * 분석 시작 시간 (optional, ISO 8601 형식)
   */
  startTime?: string;

  /**
   * 분석 종료 시간 (optional, ISO 8601 형식)
   */
  endTime?: string;

  /**
   * 문서 출력 형식
   */
  format: DocumentFormat;

  /**
   * 분석 옵션
   */
  options?: {
    includeComponents?: boolean;
    includeAlerts?: boolean;
    includeDependencies?: boolean;
    includeCharts?: boolean;
    darkMode?: boolean;
  };
}

/**
 * 에러 분석 요청
 */
export interface ErrorAnalysisRequest {
  /**
   * 프로젝트 UUID
   */
  projectUuid: string;

  /**
   * 문서 출력 형식
   */
  format: DocumentFormat;

  /**
   * 분석 옵션
   */
  options?: {
    includeRelatedLogs?: boolean;
    includeSimilarErrors?: boolean;
    includeImpactAnalysis?: boolean;
    includeCodeExamples?: boolean;
    maxRelatedLogs?: number;
  };
}

/**
 * 문서 요약 정보
 */
export interface DocumentSummary {
  /**
   * 건강 점수 (프로젝트 분석용, 0-100)
   */
  healthScore?: number;

  /**
   * 전체 이슈 수
   */
  totalIssues?: number;

  /**
   * 심각한 이슈 수
   */
  criticalIssues?: number;

  /**
   * 권장사항 수
   */
  recommendations?: number;

  /**
   * 심각도 (에러 분석용)
   */
  severity?: string;

  /**
   * 근본 원인 (에러 분석용)
   */
  rootCause?: string;

  /**
   * 영향 받은 사용자 수
   */
  affectedUsers?: number;
}

/**
 * 문서 메타데이터
 */
export interface DocumentMetadata {
  /**
   * 문서 제목
   */
  title: string;

  /**
   * 생성 시간
   */
  generatedAt: string;

  /**
   * 단어 수
   */
  wordCount?: number;

  /**
   * 예상 읽기 시간
   */
  estimatedReadingTime?: string;

  /**
   * 요약 정보
   */
  summary?: DocumentSummary;
}

/**
 * 분석 문서 생성 응답
 */
export interface AnalysisDocumentResponse {
  /**
   * 문서 ID (DB 기본키)
   */
  documentId?: number;

  /**
   * 프로젝트별 문서 번호 (1부터 시작)
   */
  documentNumber?: number;

  /**
   * 프로젝트 UUID
   */
  projectUuid: string;

  /**
   * 로그 ID (에러 분석인 경우)
   */
  logId?: number;

  /**
   * 문서 형식
   */
  format: DocumentFormat;

  /**
   * 문서 내용 (HTML, Markdown, JSON 형식인 경우)
   */
  content?: string;

  /**
   * PDF 다운로드 URL (PDF 형식인 경우)
   */
  downloadUrl?: string;

  /**
   * 파일명 (PDF 형식인 경우)
   */
  fileName?: string;

  /**
   * 파일 크기 (bytes, PDF 형식인 경우)
   */
  fileSize?: number;

  /**
   * 만료 시간 (PDF 형식인 경우)
   */
  expiresAt?: string;

  /**
   * 문서 메타데이터
   */
  documentMetadata: DocumentMetadata;

  /**
   * HTML 검증 상태
   */
  validationStatus: string;

  /**
   * 캐시 TTL (초)
   */
  cacheTtl: number;
}

/**
 * 분석 문서 목록용 요약 정보
 */
export interface AnalysisDocumentSummary {
  /**
   * 문서 ID
   */
  id: number;

  /**
   * 프로젝트별 문서 번호 (1부터 시작)
   */
  documentNumber?: number;

  /**
   * 문서 제목
   */
  title: string;

  /**
   * 문서 타입
   */
  documentType: DocumentType;

  /**
   * 검증 상태
   */
  validationStatus: string;

  /**
   * 건강 점수
   */
  healthScore?: number;

  /**
   * 전체 이슈 수
   */
  totalIssues?: number;

  /**
   * 심각한 이슈 수
   */
  criticalIssues?: number;

  /**
   * 단어 수
   */
  wordCount?: number;

  /**
   * 예상 읽기 시간
   */
  estimatedReadingTime?: string;

  /**
   * 로그 ID (에러 분석인 경우)
   */
  logId?: number;

  /**
   * 생성 시간
   */
  createdAt: string;
}

/**
 * 분석 문서 상세 정보
 */
export interface AnalysisDocumentDetailResponse {
  /**
   * 문서 ID
   */
  id: number;

  /**
   * 프로젝트별 문서 번호 (1부터 시작)
   */
  documentNumber?: number;

  /**
   * 프로젝트 UUID
   */
  projectUuid: string;

  /**
   * 문서 타입
   */
  documentType: DocumentType;

  /**
   * 문서 제목
   */
  title: string;

  /**
   * 문서 내용 (HTML)
   */
  content: string;

  /**
   * 로그 ID (에러 분석인 경우)
   */
  logId?: number;

  /**
   * 검증 상태
   */
  validationStatus: string;

  /**
   * 건강 점수
   */
  healthScore?: number;

  /**
   * 전체 이슈 수
   */
  totalIssues?: number;

  /**
   * 심각한 이슈 수
   */
  criticalIssues?: number;

  /**
   * 단어 수
   */
  wordCount?: number;

  /**
   * 예상 읽기 시간
   */
  estimatedReadingTime?: string;

  /**
   * 생성 시간
   */
  createdAt: string;
}

/**
 * 페이지네이션 응답
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
