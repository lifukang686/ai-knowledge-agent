import { request } from '@/utils/request';
import { LoginRequest, LoginResponse, UserInfo, RegisterRequest } from '@/types/auth';

const AUTH_PREFIX = '/auth';

interface AuthApiResp {
  token: string;
  userId: string | number;
  username: string;
}

const mapAuthResponse = (api: AuthApiResp): LoginResponse => ({
  token: api.token,
  user: {
    id: String(api.userId),
    username: api.username,
  },
});

export const authService = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await request.post<AuthApiResp>(`${AUTH_PREFIX}/login`, data);
    return mapAuthResponse(response);
  },

  register: async (data: RegisterRequest): Promise<LoginResponse> => {
    const response = await request.post<AuthApiResp>(`${AUTH_PREFIX}/register`, data);
    return mapAuthResponse(response);
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
