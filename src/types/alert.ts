export interface Alert {
  id: number;
  level: 'ERROR' | 'WARN' | 'INFO';
  message: string;
  traceId: string;
  timestamp: string;
}

// 알림 설정 조회 요청 파라미터
export interface GetAlertConfigParams {
  projectUuid: string;
}

// 알림 설정 조회 응답
export interface GetAlertConfigResponse {
  id: number;
  alertType: string;
  thresholdValue: number;
  activeYN: 'Y' | 'N';
  projectUuid: string;
  projectName: string;
}

// 알림 설정 수정 request body
export interface PutAlertConfigParams {
  alertConfigId: number;
  alertType?: string;
  thresholdValue?: number;
  activeYN?: string;
}

// 알림 설정 수정 응답
export interface PutAlertConfigResponse {
  id: number;
  alertType: string;
  thresholdValue: number;
  activeYN: 'Y' | 'N';
  projectUuid: string;
  projectName: string;
}

// 알림 설정 생성 request body
export interface PostAlertConfigParams {
  projectUuid: string;
  alertType: string;
  thresholdValue: number;
  activeYN?: string;
}

// 알림 설정 생성 응답
export interface PostAlertConfigResponse {
  id: number;
  alertType: string;
  thresholdValue: number;
  activeYN: 'Y' | 'N';
  projectUuid: string;
  projectName: string;
}

// 알림 읽음 처리 요청 파라미터
export interface ReadAlertParams {
  alertId: number;
}

// 알림 읽음 처리 응답
export type ReadAlertResponse = null;

// 읽지 않은 알림 개수 조회
export interface UnreadAlertParams {
  projectUuid: string;
}
// 읽지 않은 알림 개수 응답
export interface UnreadAlertCountResponse {
  unreadCount: number;
}

// 실시간 알림 스트리밍
export interface AlertSseParams {
  projectUuid: string;
}

// 실시간 알림 스트리밍 응답데이터
export interface AlertSseEventData {
  id: number;
  alertMessage: string;
  alertTime: string; 
  resolvedYN: 'Y' | 'N';
  logReference: string; 
  projectUuid: string;
}


// 알림 이력 조회
export interface AlertHistoryParams {
  projectUuid: string;
  resolvedYN?: 'Y' | 'N';
}

export interface AlertHistoryItem {
  id: number;
  alertMessage: string;
  alertTime: string; 
  resolvedYN: 'Y' | 'N';
  logReference: string; 
  projectUuid: string;
}

// 알림 이력 조회 응답
export type AlertHistoryResponse = AlertHistoryItem[];
