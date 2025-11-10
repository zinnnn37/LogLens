// src/services/jiraService.ts
import { apiClient } from '@/services/apiClient';
import type {
  JiraConnectRequest,
  JiraConnectData,
  JiraIssueParams,
  JiraIssueData,
  JiraConnectionStatusData
} from '@/types/jira';
import { API_PATH } from '@/constants/api-path';

// Jira 연동 (Connect)
export const connectJiraIntegration = async (
  payload: JiraConnectRequest,
): Promise<JiraConnectData> => {
  return apiClient.post<JiraConnectData>(API_PATH.JIRA_INTEGRATION, payload);
};

// Jira 이슈 생성 (수동)
export const createJiraIssue = async (
  payload: JiraIssueParams,
): Promise<JiraIssueData> => {
  return apiClient.post<JiraIssueData>(API_PATH.JIRA_CREATE_ISSUE, payload);
};

// Jira 연결 상태 조회
export const getJiraConnectionStatus = async (
  projectUuid: string,
): Promise<JiraConnectionStatusData> => {
  return apiClient.get<JiraConnectionStatusData>(
    API_PATH.JIRA_CONNECTION_STATUS,
    { projectUuid }, 
  );
};