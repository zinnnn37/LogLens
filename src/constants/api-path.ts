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

  // API 호출 통계 조회
  STATISTICS_API_CALLS: '/api/statistics/api-calls',

  // 자주 발생하는 에러 TOP N 조회
  STATISTICS_ERROS_TOP: '/api/errors/top',

  // 히트맵
  STATISTICS_HEATMAP: '/api/statistics/logs/heatmap',

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

  // TraceId 기반 로그 조회
  TRACE_LOGS: (traceId: string) => `/api/traces/${traceId}/logs`,

  // TraceId 기반 요청 흐름 조회
  TRACE_FLOW: (traceId: string) => `/api/traces/${traceId}/flow`,

  // 실시간 로그 스트리밍(SSE)
  LOGS_STREAM: '/api/logs/stream',

  // 로그 발생 추이 조회
  LOGS_TREND: '/api/statistics/log-trend',

  // 트래픽 그래프
  TRAFFIC: '/api/statistics/traffic',

  // 챗봇 스트리밍 API
  CHATBOT_STREAM: '/api/v2/chatbot/ask/stream',

  // 아키텍처 의존성 조회 (DB 정보)
  ARCHITECTURE_DEPENDENCIES: '/api/dashboards/dependencies/architecture',

  // 알림 설정 조회,수정,생성
  ALERT_CONFIG: '/api/alerts/config',

  // 알림 읽음 처리
  ALERT_READ: (alertId: number) => `/api/alerts/${alertId}/read`,

  // 읽지 않은 알림 개수 조회
  ALERT_UNREAD_COUNT: '/api/alerts/unread-count',

  // 실시간 알림 스트리밍
  ALERT_STREAM: '/api/alerts/stream',

  // 알림 이력 조회
  ALERT_HISTORY: '/api/alerts/histories',

  // 분석 문서 API
  ANALYSIS_DOCUMENTS: (projectUuid: string) =>
    `/api/analysis/projects/${projectUuid}/documents`,

  ANALYSIS_DOCUMENT_DETAIL: (projectUuid: string, documentId: number) =>
    `/api/analysis/projects/${projectUuid}/documents/${documentId}`,

  ANALYSIS_PROJECT_REPORT: (projectUuid: string) =>
    `/api/analysis/projects/${projectUuid}/reports`,

  ANALYSIS_ERROR_REPORT: (logId: number) =>
    `/api/analysis/errors/${logId}/reports`,
} as const;
