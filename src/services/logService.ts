// src/services/logService.ts

import { apiClient } from './apiClient';
import { API_PATH } from '@/constants/api-path';
import { EventSourcePolyfill } from 'event-source-polyfill';
import type {
  LogSearchParams,
  LogSearchResponse,
  TraceIdSearchResponse,
  LogDetailParams,
  LogDetailResponse,
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
  params: LogDetailParams,
): Promise<LogDetailResponse> => {
  const { logId, projectUuid } = params;

  const url = API_PATH.LOGS_DETAIL(logId);

  const response = await apiClient.get<LogDetailResponse>(url, {
    projectUuid: projectUuid,
  });

  return response;
};

/**
 * 실시간 로그 스트리밍 연결 (SSE)
 */

export const connectLogStream = (
  params: LogSearchParams,
  accessToken: string,
): EventSourcePolyfill => {
  // 쿼리 스트링 생성
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      if (Array.isArray(value)) {
        value.forEach(v => searchParams.append(key, v));
      } else {
        searchParams.append(key, String(value));
      }
    }
  });

  searchParams.append('token', accessToken);

  // 전체 URL 생성
  const url = `${import.meta.env.VITE_API_BASE_URL}${API_PATH.LOGS_STREAM}?${searchParams.toString()}`;

  return new EventSourcePolyfill(url, {
    heartbeatTimeout: 3600000, 
  });
};
