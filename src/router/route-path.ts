export const ROUTE_PATH = {
  INDEX: '/',
  MAIN: '/',
  LOGIN: '/login',
  SIGNUP: '/signup',
  // 프로젝트별 페이지 (동적 라우트)
  PROJECT: '/project/:projectUuid',
  LOGS: '/project/:projectUuid/logs',
  DASHBOARD: '/project/:projectUuid/dashboard',
  DEPENDENCY_GRAPH: '/project/:projectUuid/dependency-graph',
  REQUEST_FLOW: '/project/:projectUuid/request-flow',
  // AI Chat은 프로젝트별로 구분
  AI_CHAT: '/project/:projectUuid/chatbot',
  DOCS: '/docs',
  NOT_FOUND: '*',
} as const;

// 동적 경로 생성 헬퍼 함수
export const createProjectPath = (
  projectUuid: string,
  page: 'logs' | 'dashboard' | 'dependency-graph' | 'request-flow' | 'chatbot',
) => {
  return `/project/${projectUuid}/${page}`;
};
