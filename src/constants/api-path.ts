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

  // 프로젝트 삭제
  DELETE_PROJECT: (projectId: number) => `/api/projects/${projectId}`,

  // 프로젝트 내 멤버 삭제
  DELETE_MEMBER: (projectId: number, memberId: number) => `/api/projects/${projectId}/members/${memberId}`,
} as const;


