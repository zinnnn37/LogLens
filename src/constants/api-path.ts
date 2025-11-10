export const API_PATH = {
  USERS: '/v1/users',
  USER_DETAIL: (userId: number) => `/v1/users/${userId}`,
  AUTH_LOGIN: '/v1/auth/login',
  AUTH_LOGOUT: '/v1/auth/logout',

  // 프로젝트 생성(POST), 프로젝트 목록 조회(GET)
  PROJECT: '/api/projects',

  // 프로젝트 상세 조회
  PROJECT_DETAIL: (projectUuid: string) => `/api/projects/${projectUuid}`,

  // 이름으로 유저 검색
  SEARCH_USERS: '/api/auth/users/search',

  // 멤버 초대
  INVITE_MEMBERS: (projectUuid: string) =>
    `/api/projects/${projectUuid}/members`,

  // 아키텍처 흐름 조회
  ARCHITECTURE: (projectId: string) =>
    `/api/projects/${projectId}/architecture`,

  // 컴포넌트 목록 조회
  COMPONENTS: (projectId: string) => `/api/projects/${projectId}/components`,

  // 대시보드 컴포넌트 목록 조회
  DASHBOARD_COMPONENTS: '/api/dashboards/components',

  // 컴포넌트 의존성 조회
  COMPONENT_DEPENDENCIES: (componentId: number) =>
    `/api/dashboards/components/${componentId}/dependencies`,

// 대시보드 통계 개요 조회
STATISTICS_OVERVIEW: '/api/statistics/overview',




  // 프로젝트 삭제
  DELETE_PROJECT: (projectUuid: string) => `/api/projects/${projectUuid}`,

  // 프로젝트 내 멤버 삭제
  DELETE_MEMBER: (projectUuid: string, memberId: number) =>
    `/api/projects/${projectUuid}/members/${memberId}`,

  // Jira 연동 설정
  JIRA_INTEGRATION: '/api/integrations/jira/connect',

  // Jira 이슈 생성
  JIRA_CREATE_ISSUE: '/api/integrations/jira/issues',

  // Jira 연동 상태 조회
  JIRA_CONNECTION_STATUS: '/api/integrations/jira/connection/status',

  // 프로젝트 연결 상태 조회
  PROJECT_CONNECTION: (projectUuid: string) =>
    `/api/projects/${projectUuid}/connection`,

  // 로그 검색
  LOGS_SEARCH: '/api/logs',

  // 로그 상세 조회(분석 포함)
  LOGS_DETAIL: (logId: number) => `/api/logs/${logId}`,
} as const;
