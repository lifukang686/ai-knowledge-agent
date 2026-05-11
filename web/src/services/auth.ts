import { request } from '@/utils/request';
import { LoginRequest, LoginResponse, UserInfo, RegisterRequest } from '@/types/auth';

const AUTH_PREFIX = '/auth';

export const authService = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    return request.post<LoginResponse>(`${AUTH_PREFIX}/login`, data);
  },

  register: async (data: RegisterRequest): Promise<LoginResponse> => {
    return request.post<LoginResponse>(`${AUTH_PREFIX}/register`, data);
  },

  logout: async (): Promise<void> => {
    return request.post<void>(`${AUTH_PREFIX}/logout`);
  },

  getCurrentUser: async (): Promise<UserInfo> => {
    return request.get<UserInfo>(`${AUTH_PREFIX}/me`);
  },

  refreshToken: async (): Promise<{ token: string }> => {
    return request.post<{ token: string }>(`${AUTH_PREFIX}/refresh`);
  },
};
