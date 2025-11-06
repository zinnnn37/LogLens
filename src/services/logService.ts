// src/services/logService.ts

import { apiClient } from './apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  LogSearchParams,
  LogSearchResponse,
  TraceIdSearchResponse,
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
