import { apiClient } from './apiClient';
// API_PATH는 별도 파일로 관리하신다고 가정합니다.
// 예: export const API_PATH = { ALERT_CONFIG: '/api/alerts/config', ... }
import { API_PATH } from '@/constants/api-path';
import { EventSourcePolyfill } from 'event-source-polyfill';

import type {
  GetAlertConfigParams,
  GetAlertConfigResponse,
  PutAlertConfigParams,
  PutAlertConfigResponse,
  PostAlertConfigParams,
  PostAlertConfigResponse,
  ReadAlertParams,
  ReadAlertResponse,
  UnreadAlertParams,
  UnreadAlertCountResponse,
  AlertSseParams,
  AlertHistoryParams,
  AlertHistoryResponse,
} from '@/types/alert'; 

/**
 * 알림 설정 조회 (GET)
 */
export const getAlertConfig = (
  params: GetAlertConfigParams,
): Promise<GetAlertConfigResponse> => {
  return apiClient.get<GetAlertConfigResponse>(API_PATH.ALERT_CONFIG, params);
};

/**
 * 알림 설정 생성 (POST)
 */
export const createAlertConfig = (
  data: PostAlertConfigParams,
): Promise<PostAlertConfigResponse> => {
  return apiClient.post<PostAlertConfigResponse>(API_PATH.ALERT_CONFIG, data);
};

/**
 * 알림 설정 수정 (PUT)
 */
export const updateAlertConfig = (
  data: PutAlertConfigParams,
): Promise<PutAlertConfigResponse> => {
  return apiClient.put<PutAlertConfigResponse>(API_PATH.ALERT_CONFIG, data);
};

/**
 * 알림 읽음 처리 (PATCH)
 */
export const readAlert = (
  params: ReadAlertParams,
): Promise<ReadAlertResponse> => {
  return apiClient.patch<ReadAlertResponse>(API_PATH.ALERT_READ(params.alertId));
};

/**
 * 읽지 않은 알림 개수 조회 (GET)
 */
export const getUnreadAlertCount = (
  params: UnreadAlertParams,
): Promise<UnreadAlertCountResponse> => {
  return apiClient.get<UnreadAlertCountResponse>(
    API_PATH.ALERT_UNREAD_COUNT,
    params,
  );
};

/**
 * 알림 이력 조회 (GET)
 */
export const getAlertHistory = (
  params: AlertHistoryParams,
): Promise<AlertHistoryResponse> => {
  return apiClient.get<AlertHistoryResponse>(API_PATH.ALERT_HISTORY, params);
};

/**
 * 실시간 알림 스트리밍 (SSE)
 */
export const connectAlertStream = (
  params: AlertSseParams,
  accessToken: string, 
): EventSourcePolyfill => {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      searchParams.append(key, String(value));
    }
  });

  searchParams.append('token', accessToken);

  const url = `${import.meta.env.VITE_API_BASE_URL}${API_PATH.ALERT_STREAM}?${searchParams.toString()}`;

  return new EventSourcePolyfill(url, {
    heartbeatTimeout: 3600000, 
  });
};