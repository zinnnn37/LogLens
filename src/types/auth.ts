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
  userId: number;
  email: string;
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface EmailCheckResponse {
  email: string;
  available: boolean;
}

export interface RefreshTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}
