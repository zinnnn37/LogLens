// src/services/jiraService.ts
import { apiClient } from '@/services/apiClient';
import type {
  JiraConnectRequest,
  JiraConnectData,
  JiraIssueCreateRequest,
  JiraIssueCreateData,
} from '@/types/jira';

// Jira 연동 API
export const connectJiraIntegration = async (
  payload: JiraConnectRequest,
): Promise<JiraConnectData> => {
  return apiClient.post<JiraConnectData>(
    '/api/integrations/jira/connect',
    payload,
  );
};

// Jira 이슈 생성 (수동)
export const createJiraIssue = async (
  payload: JiraIssueCreateRequest,
): Promise<JiraIssueCreateData> => {
  return apiClient.post<JiraIssueCreateData>(
    '/api/integrations/jira/issues',
    payload,
  );
};
