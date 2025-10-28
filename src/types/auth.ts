export interface SignupRequest {
  name: string;
  email: string;
  password: string;
  passwordConfirm: string;
}

export interface SignupResponse {
  userId: number;
  userName: string;
  email: string;
  createdAt: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  userName: string;
  email: string;
}

export interface EmailCheckResponse {
  email: string;
  available: boolean;
}
