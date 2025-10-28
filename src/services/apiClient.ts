import axios, { type AxiosInstance, type AxiosResponse } from 'axios';

import type { ApiResponse, ApiSuccessResponse } from '@/types/api';
import { ApiError } from '@/types/api';
import { useAuthStore } from '@/stores/authStore';

/**
 * API 클라이언트 클래스
 *
 * 역할:
 * 1. 모든 HTTP 요청을 중앙에서 관리
 * 2. 요청마다 인증 토큰 자동 추가
 * 3. 응답을 일관된 형식으로 변환
 * 4. 에러를 표준화된 형태로 처리
 */
class ApiClient {
  private instance: AxiosInstance;

  constructor(baseURL = import.meta.env.VITE_API_BASE_URL) {
    this.instance = axios.create({
      baseURL: `${baseURL}`,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true, // 쿠키를 요청에 포함
    });

    this.setupInterceptors();
  }

  /**
   * 인터셉터 설정
   * Request: 요청 전 토큰 자동 추가
   * Response: 응답 후 에러 처리
   */
  private setupInterceptors() {
    // Request interceptor
    this.instance.interceptors.request.use(
      config => {
        const { accessToken } = useAuthStore.getState();
        if (accessToken) {
          config.headers.Authorization = `Bearer ${accessToken}`;
        }
        return config;
      },
      error => Promise.reject(error),
    );

    // Response interceptor
    this.instance.interceptors.response.use(
      (response: AxiosResponse<{ success: boolean }>) => {
        return response;
      },
      async error => {
        const originalRequest = error.config;

        // 로그인, 회원가입, 토큰 리프레시 요청은 401 재시도 제외
        const isAuthRequest =
          originalRequest.url?.includes('/api/auth/tokens') ||
          originalRequest.url?.includes('/api/auth/users');

        // 401 에러이고, 아직 재시도하지 않은 요청이며, 인증 요청이 아닌 경우
        if (
          error.response?.status === 401 &&
          !originalRequest._retry &&
          !isAuthRequest
        ) {
          originalRequest._retry = true;

          try {
            // 만료된 access token 가져오기
            const { accessToken: expiredToken } = useAuthStore.getState();

            // refresh token으로 새로운 access token 발급
            // Authorization 헤더에 만료된 토큰 포함 필요
            const response = await this.instance.post(
              '/api/auth/tokens/refresh',
              {},
              {
                headers: {
                  Authorization: `Bearer ${expiredToken}`,
                },
              },
            );
            const newAccessToken = response.data.data.accessToken;

            // 새로운 토큰 저장
            useAuthStore.getState().setAccessToken(newAccessToken);

            // 원래 요청에 새 토큰 적용
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

            // 원래 요청 재시도
            return this.instance(originalRequest);
          } catch (refreshError) {
            // refresh token도 만료된 경우 로그아웃 처리
            console.log('Refresh token 만료 - 로그아웃 필요');
            useAuthStore.getState().clearAuth();
            // 로그인 페이지로 리다이렉트는 ProtectedRoute에서 처리됨
            return Promise.reject(refreshError);
          }
        }

        if (error.isAxiosError) {
          throw new ApiError(error);
        }
        throw error;
      },
    );
  }

  async get<T>(url: string, params?: object): Promise<T> {
    const response = await this.instance.get<ApiResponse<T>>(url, { params });
    return (response.data as ApiSuccessResponse<T>).data;
  }

  async post<T>(url: string, data?: object | FormData): Promise<T> {
    let config = {};
    if (data instanceof FormData) {
      config = {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      };
    }

    const response = await this.instance.post<ApiResponse<T>>(
      url,
      data,
      config,
    );
    return (response.data as ApiSuccessResponse<T>).data;
  }

  async put<T>(url: string, data?: object): Promise<T> {
    const response = await this.instance.put<ApiResponse<T>>(url, data);
    return (response.data as ApiSuccessResponse<T>).data;
  }

  async patch<T>(url: string, data?: object): Promise<T> {
    const response = await this.instance.patch<ApiResponse<T>>(url, data);
    return (response.data as ApiSuccessResponse<T>).data;
  }

  async delete<T>(url: string, params?: object): Promise<T> {
    const response = await this.instance.delete<ApiResponse<T>>(url, {
      params,
    });
    return (response.data as ApiSuccessResponse<T>).data;
  }
}

export const apiClient = new ApiClient();
