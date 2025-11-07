// src/services/logService.ts

import { apiClient } from './apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  LogSearchParams,
  LogSearchResponse,
  TraceIdSearchResponse,
  LogAnalysisParams,
  LogAnalysisResponse,
} from '@/types/log';

/**
 * 로그 검색 API 호출
 */
export const searchLogs = async (
  params: LogSearchParams,
): Promise<LogSearchResponse | TraceIdSearchResponse> => {
  const response = await apiClient.get<
    LogSearchResponse | TraceIdSearchResponse
  >(API_PATH.LOGS_SEARCH, params);

  return response;
};

// 로그 상세조회
export const analyzeLogs = async (
  params: LogAnalysisParams,
): Promise<LogAnalysisResponse> => {
  const { logId, project_uuid } = params;

  const url = API_PATH.LOGS_DETAIL(logId);

  const response = await apiClient.get<LogAnalysisResponse>(url, {
    project_uuid: project_uuid,
  });

  return response;
};
