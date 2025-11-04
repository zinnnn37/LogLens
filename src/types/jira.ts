// Jira Integration Types

export interface JiraIntegrationModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: number;
}

export interface JiraFormData {
  jiraUrl: string;
  jiraEmail: string;
  jiraApiToken: string;
  jiraProjectKey: string;
}

export interface JiraFormErrors {
  jiraUrl?: string;
  jiraEmail?: string;
  jiraApiToken?: string;
  jiraProjectKey?: string;
}

// API 요청 타입
export interface JiraConnectRequest {
  projectId: number;
  jiraUrl: string;
  jiraEmail: string;
  jiraApiToken: string;
  jiraProjectKey: string;
}

// 지라 연결 응답값 타입
export interface JiraConnectionTest {
  status: 'SUCCESS' | 'FAILURE';
  message: string;
  testedAt: string;
}

export interface JiraConnectData {
  id: number;
  projectId: number;
  jiraUrl: string;
  jiraEmail: string;
  jiraProjectKey: string;
  connectionTest: JiraConnectionTest;
}

export interface JiraConnectResponse {
  code: string;
  message: string;
  status: number;
  timestamp: string;
  data: JiraConnectData;
}

// 지라 티켓 발행

export type JiraIssueType = 'Bug' | 'Task' | 'Story' | 'Epic';
export type JiraIssuePriority =
  | 'Highest'
  | 'High'
  | 'Medium'
  | 'Low'
  | 'Lowest';

// 요청
export interface JiraIssueCreateRequest {
  projectId: number;
  logId: number;
  summary: string;
  description?: string;
  issueType: JiraIssueType;
  priority: JiraIssuePriority;
}

// 응답
export interface JiraIssueCreatedBy {
  userId: number;
  email: string;
  name: string;
}

export interface JiraIssueCreateData {
  issueKey: string;
  jiraUrl: string;
  createdBy: JiraIssueCreatedBy;
}

export interface JiraIssueCreateResponse {
  code: string;
  message: string;
  status: number;
  timestamp: string;
  data: JiraIssueCreateData;
}
