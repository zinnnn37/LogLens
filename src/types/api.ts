import type { AxiosError } from 'axios';

export interface ApiSuccessResponse<T> {
  success: true;
  status: number;
  data: T;
}

export interface ApiErrorResponse {
  success: false;
  status: number;
  code: string;
  message: string;
  timestamp: string;
  data?: unknown;
}

export type ApiResponse<T> = ApiSuccessResponse<T> | ApiErrorResponse;

export class ApiError extends Error {
  public readonly axiosError: AxiosError;
  public readonly response?: ApiErrorResponse;

  constructor(axiosError: AxiosError) {
    super(axiosError.message);
    this.axiosError = axiosError;

    if (axiosError.response?.data) {
      this.response = axiosError.response.data as ApiErrorResponse;
    }

    this.name = 'ApiError';
  }
}
