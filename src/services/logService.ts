import { apiClient } from './apiClient';
import { API_PATH } from '@/constants/api-path';
import type { LogSearchParams, LogSearchResponse } from '@/types/log';

/**
 * 로그 검색 API 호출
 */
export const searchLogs = async (
  params: LogSearchParams,
): Promise<LogSearchResponse> => {
  const response = await apiClient.get<LogSearchResponse>(
    API_PATH.LOGS_SEARCH,
    params,
  );
  return response;
};
