export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserResponse {
  id: number;
  email: string;
  displayName: string;
  createdAt: string;
  title?: string | null;
  avatarUrl?: string | null;
  customStatusText?: string | null;
  customStatusEmoji?: string | null;
  customStatusExpiresAt?: string | null;
  presence?: 'ONLINE' | 'AWAY' | 'OFFLINE';
  lastSeenAt?: string | null;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors: Record<string, string> | null;
}
