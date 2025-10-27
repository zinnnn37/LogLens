import { apiClient } from './apiClient';
import type {
  SignupRequest,
  SignupResponse,
  LoginRequest,
  LoginResponse,
  EmailCheckResponse,
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
  return apiClient.post<SignupResponse>('/v1/api/auth/users', data);
};

/**
 * 로그인 API
 * POST /v1/api/auth/login
 */
export const login = async (data: LoginRequest): Promise<LoginResponse> => {
  return apiClient.post<LoginResponse>('/v1/api/auth/login', data);
};
