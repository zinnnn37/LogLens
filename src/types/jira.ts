// Jira Integration Types

export interface JiraIntegrationModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId?: number; // 추후 프로젝트 선택 기능 추가 시 사용
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
