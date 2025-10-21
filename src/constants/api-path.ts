export const API_PATH = {
  USERS: '/v1/users',
  USER_DETAIL: (userId: string) => `/v1/users/${userId}`,
  AUTH_LOGIN: '/v1/auth/login',
  AUTH_LOGOUT: '/v1/auth/logout',
} as const;
