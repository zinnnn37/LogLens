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
  TraceLogsParams,
  TraceLogsResponse,
  TraceFlowParams,
  TraceFlowResponse,
  LogTrendParams,
  LogTrendResponse,
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
 * TraceId 기반 로그 조회 API 호출
 */
export const getTraceLogs = async (
  params: TraceLogsParams,
): Promise<TraceLogsResponse> => {
  const { traceId, projectUuid } = params;

  const url = API_PATH.TRACE_LOGS(traceId);

  const response = await apiClient.get<TraceLogsResponse>(url, {
    projectUuid: projectUuid,
  });

  return response;
};

/**
 * TraceId 기반 요청 흐름 조회 API 호출
 */
export const getTraceFlow = async (
  params: TraceFlowParams,
): Promise<TraceFlowResponse> => {
  const { traceId, projectUuid } = params;

  const url = API_PATH.TRACE_FLOW(traceId);

  const response = await apiClient.get<TraceFlowResponse>(url, {
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
  searchParams.append('size', '50'); // 디폴트로 50 추가

  // 전체 URL 생성
  const url = `${import.meta.env.VITE_API_BASE_URL}${API_PATH.LOGS_STREAM}?${searchParams.toString()}`;

  return new EventSourcePolyfill(url, {
    heartbeatTimeout: 3600000,
  });
};

// 로그 발생 추이 조회
export const getLogTrend = async (
  params: LogTrendParams,
): Promise<LogTrendResponse> => {
  const response = await apiClient.get<LogTrendResponse>(
    '/statistics/log-trend',
    {
      params,
    },
  );

  return response;
};
