// Jira Integration Types

export interface JiraIntegrationModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectUuid: string;
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
  projectUuid: string;
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
  projectUuid: string;
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

// Jira 연결 상태 조회 응답
export interface JiraConnectionStatusData {
  exists: boolean;
  projectUuid: string;
  connectionId: number;
  jiraProjectKey: string;
}


// 지라 이슈 생성

export type JiraIssueType = 'Bug' | 'Task' | 'Story' | 'Epic';
export type JiraIssuePriority =
  | 'Highest'
  | 'High'
  | 'Medium'
  | 'Low'
  | 'Lowest';

export interface JiraIssueParams {
  projectUuid: string;
  logId: number;
  summary: string;
  description: string;
  issueType: JiraIssueType;
  priority: JiraIssuePriority;
}

export interface JiraIssueCreator {
  userId: number;
  email: string;
  name: string;
}

export interface JiraIssueData {
  issueKey: string;
  jiraUrl: string;
  createdBy: JiraIssueCreator;
}

export interface JiraIssueResponse {
  code: string;
  message: string;
  status: number;
  timestamp: string;
  data: JiraIssueData;
}
