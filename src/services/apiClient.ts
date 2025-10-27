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
      error => {
        if (error.response?.status === 401) {
          console.log('401 에러 - 인증 만료');
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
