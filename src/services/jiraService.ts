// src/services/jiraService.ts
import { apiClient } from '@/services/apiClient';
import type {
  JiraConnectRequest,
  JiraConnectData,
  JiraIssueCreateRequest,
  JiraIssueCreateData,
  JiraConnectionParams,
  JiraConnectionResponse,
} from '@/types/jira';
import { API_PATH } from '@/constants/api-path';

// Jira 연동 API
export const connectJiraIntegration = async (
  payload: JiraConnectRequest,
): Promise<JiraConnectData> => {
  return apiClient.post<JiraConnectData>(API_PATH.JIRA_INTEGRATION, payload);
};

// Jira 이슈 생성 (수동)
export const createJiraIssue = async (
  payload: JiraIssueCreateRequest,
): Promise<JiraIssueCreateData> => {
  return apiClient.post<JiraIssueCreateData>(
    API_PATH.JIRA_CREATE_ISSUE,
    payload,
  );
};

// Jira 연결 상태 조회 (GET)
export const getJiraConnectionStatus = async (
  params: JiraConnectionParams,
): Promise<JiraConnectionResponse> => {
  return apiClient.get<JiraConnectionResponse>(
    API_PATH.JIRA_CONNECTION_STATUS,
    params,
  );
};
