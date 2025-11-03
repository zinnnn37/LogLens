export const API_PATH = {
  USERS: '/v1/users',
  USER_DETAIL: (userId: number) => `/v1/users/${userId}`,
  AUTH_LOGIN: '/v1/auth/login',
  AUTH_LOGOUT: '/v1/auth/logout',

  // 프로젝트 생성(POST), 프로젝트 목록 조회(GET)
  PROJECT: '/api/projects',

  // 프로젝트 상세 조회
  PROJECT_DETAIL: (projectId: number) => `/api/projects/${projectId}`,

  // 이름으로 멤버 검색, 뒤에 파라미터는 호출하는 곳에서
  SEARCH_USERS: '/api/auth/users',

  // 멤버 초대
  INVITE_MEMBERS: (projectId: number) => `/api/projects/${projectId}/members`,

  // 아키텍처 흐름 조회
  ARCHITECTURE: (projectId: string) =>
    `/api/projects/${projectId}/architecture`,

  // 컴포넌트 목록 조회
  COMPONENTS: (projectId: string) => `/api/projects/${projectId}/components`,

  // 프로젝트 삭제
  DELETE_PROJECT: (projectId: number) => `/api/projects/${projectId}`,

  // 프로젝트 내 멤버 삭제
  DELETE_MEMBER: (projectId: number, memberId: number) =>
    `/api/projects/${projectId}/members/${memberId}`,


  // Jira 연동 설정
  JIRA_INTEGRATION: '/api/integrations/jira/connect',

  
  // Jira 이슈 생성
  JIRA_CREATE_ISSUE: '/api/integrations/jira/issues',



} as const;
