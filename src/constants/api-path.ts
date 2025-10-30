export const API_PATH = {
  USERS: '/v1/users',
  USER_DETAIL: (userId: string) => `/v1/users/${userId}`,
  AUTH_LOGIN: '/v1/auth/login',
  AUTH_LOGOUT: '/v1/auth/logout',

  // 프로젝트 생성(POST), 프로젝트 목록 조회(GET)
  PROJECT: '/api/projects',

  // 프로젝트 상세 조회
  PROJECT_DETAIL: (projectId: string) => `/api/projects/${projectId}`,

  // 이름으로 멤버 검색, 뒤에 파라미터는 호출하는 곳에서
  AUTH_USERS: '/api/auth/users',

  // 멤버 초대
  INVITE_MEMBERS: (projectId: string) => `/api/projects/${projectId}/members`,

  // 아키텍처 흐름 조회
  ARCHITECTURE: (projectId: string) =>
    `/api/projects/${projectId}/architecture`,

  // 컴포넌트 목록 조회
  COMPONENTS: (projectId: string) => `/api/projects/${projectId}/components`,
} as const;
