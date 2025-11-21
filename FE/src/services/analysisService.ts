// AI 분석 문서 생성 API 서비스

import { apiClient } from './apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  ProjectAnalysisRequest,
  ErrorAnalysisRequest,
  AnalysisDocumentResponse,
  AnalysisDocumentSummary,
  AnalysisDocumentDetailResponse,
  PageResponse,
} from '@/types/analysis';

/**
 * 프로젝트 전체 분석 문서 생성
 *
 * @param projectUuid - 프로젝트 UUID
 * @param request - 프로젝트 분석 요청 데이터
 * @returns 생성된 분석 문서 응답
 *
 * @example
 * ```typescript
 * const response = await generateProjectAnalysis('my-project-uuid', {
 *   format: 'HTML',
 *   startTime: '2025-01-01T00:00:00',
 *   endTime: '2025-01-16T23:59:59',
 *   options: {
 *     includeCharts: true,
 *     darkMode: false
 *   }
 * });
 *
 * // HTML 표시
 * setHtmlContent(response.content);
 * ```
 */
export const generateProjectAnalysis = async (
  projectUuid: string,
  request: ProjectAnalysisRequest,
): Promise<AnalysisDocumentResponse> => {
  const response = await apiClient.post<AnalysisDocumentResponse>(
    `/api/analysis/projects/${projectUuid}/reports`,
    request,
  );
  return response;
};

/**
 * 에러 로그 상세 분석 문서 생성
 *
 * @param logId - 로그 ID
 * @param request - 에러 분석 요청 데이터
 * @returns 생성된 분석 문서 응답
 *
 * @example
 * ```typescript
 * const response = await generateErrorAnalysis(12345, {
 *   projectUuid: 'my-project-uuid',
 *   format: 'HTML',
 *   options: {
 *     includeRelatedLogs: true,
 *     includeImpactAnalysis: true,
 *     maxRelatedLogs: 10
 *   }
 * });
 *
 * // HTML 표시
 * setHtmlContent(response.content);
 * ```
 */
export const generateErrorAnalysis = async (
  logId: number,
  request: ErrorAnalysisRequest,
): Promise<AnalysisDocumentResponse> => {
  const response = await apiClient.post<AnalysisDocumentResponse>(
    `/api/analysis/errors/${logId}/reports`,
    request,
  );
  return response;
};

/**
 * PDF 파일 다운로드 (PDF 형식으로 생성한 경우)
 *
 * @param fileId - 파일 ID (downloadUrl에서 추출)
 * @returns Blob 데이터
 *
 * @example
 * ```typescript
 * // PDF로 생성한 경우
 * const response = await generateProjectAnalysis(projectUuid, {
 *   format: 'PDF',
 *   // ...
 * });
 *
 * // fileId 추출 (예: /api/analysis/downloads/abc123 -> abc123)
 * const fileId = response.downloadUrl?.split('/').pop();
 * if (fileId) {
 *   const blob = await downloadPdfFile(fileId);
 *   const url = URL.createObjectURL(blob);
 *   const a = document.createElement('a');
 *   a.href = url;
 *   a.download = response.fileName || 'analysis.pdf';
 *   a.click();
 *   URL.revokeObjectURL(url);
 * }
 * ```
 */
export const downloadPdfFile = async (fileId: string): Promise<Blob> => {
  const response = await apiClient.get<Blob>(
    `/api/analysis/downloads/${fileId}`,
    {
      responseType: 'blob',
    },
  );
  return response;
};

/**
 * 분석 문서 목록 조회
 *
 * @param projectUuid - 프로젝트 UUID
 * @param page - 페이지 번호 (0부터 시작)
 * @param size - 페이지 크기
 * @returns 페이지네이션된 문서 목록
 *
 * @example
 * ```typescript
 * const response = await getAnalysisDocuments('project-uuid', 0, 10);
 * console.log('Total documents:', response.totalElements);
 * response.content.forEach(doc => {
 *   console.log(doc.title, doc.documentType, doc.createdAt);
 * });
 * ```
 */
export const getAnalysisDocuments = async (
  projectUuid: string,
  page: number = 0,
  size: number = 10,
): Promise<PageResponse<AnalysisDocumentSummary>> => {
  const response = await apiClient.get<PageResponse<AnalysisDocumentSummary>>(
    API_PATH.ANALYSIS_DOCUMENTS(projectUuid),
    {
      params: { page, size },
    },
  );
  return response;
};

/**
 * 분석 문서 상세 조회
 *
 * @param projectUuid - 프로젝트 UUID
 * @param documentId - 문서 ID
 * @returns 문서 상세 정보 (HTML 내용 포함)
 *
 * @example
 * ```typescript
 * const document = await getAnalysisDocumentById('project-uuid', 123);
 * // HTML 내용 표시
 * setHtmlContent(document.content);
 * ```
 */
export const getAnalysisDocumentById = async (
  projectUuid: string,
  documentId: number,
): Promise<AnalysisDocumentDetailResponse> => {
  const response = await apiClient.get<AnalysisDocumentDetailResponse>(
    API_PATH.ANALYSIS_DOCUMENT_DETAIL(projectUuid, documentId),
  );
  return response;
};

/**
 * 분석 문서 삭제
 *
 * @param projectUuid - 프로젝트 UUID
 * @param documentId - 문서 ID
 *
 * @example
 * ```typescript
 * await deleteAnalysisDocument('project-uuid', 123);
 * // 삭제 성공
 * ```
 */
export const deleteAnalysisDocument = async (
  projectUuid: string,
  documentId: number,
): Promise<void> => {
  await apiClient.delete(
    API_PATH.ANALYSIS_DOCUMENT_DETAIL(projectUuid, documentId),
  );
};
