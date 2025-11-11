// src/services/logService.ts

import { apiClient } from './apiClient';
import { API_PATH } from '@/constants/api-path';
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
