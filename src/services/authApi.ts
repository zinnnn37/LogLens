import { apiClient } from './apiClient';
import type {
  SignupRequest,
  SignupResponse,
  LoginRequest,
  LoginResponse,
  EmailCheckResponse,
  RefreshTokenResponse,
} from '@/types/auth';

/**
 * 이메일 중복확인 API
 * GET /api/auth/emails?email=user@example.com
 */
export const checkEmailAvailability = async (
  email: string,
): Promise<EmailCheckResponse> => {
  return apiClient.get<EmailCheckResponse>('/api/auth/emails', { email });
};

/**
 * 회원가입 API
 * POST /v1/api/auth/users
 */
export const signup = async (data: SignupRequest): Promise<SignupResponse> => {
  return apiClient.post<SignupResponse>('/api/auth/users', data);
};

/**
 * 로그인 API
 * POST /api/auth/tokens
 */
export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  return apiClient.post<LoginResponse>('/api/auth/tokens', data);
};

/**
 * 로그아웃 API
 * DELETE /api/auth/tokens
 */
export const logout = async (): Promise<void> => {
  return apiClient.delete<void>('/api/auth/tokens');
};

/**
 * Access Token 재발급 API
 * POST /api/auth/tokens/refresh
 */
export const refreshAccessToken = async (): Promise<RefreshTokenResponse> => {
  return apiClient.post<RefreshTokenResponse>('/api/auth/tokens/refresh');
};
