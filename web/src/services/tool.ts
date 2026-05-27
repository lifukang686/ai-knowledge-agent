import { request } from '@/utils/request';
import type { CreateToolReq, ToolItem, UpdateToolReq } from '@/types/tool';
import type { ListResponse } from '@/types/common';

export interface ToolListParams {
  current: number;
  size: number;
  keyword?: string;
}

export const toolService = {
  create: (data: CreateToolReq): Promise<ToolItem> =>
    request.post('/tools', data),

  list: (): Promise<ToolItem[]> =>
    request.get('/tools'),

  listPage: (params: ToolListParams): Promise<ListResponse<ToolItem>> =>
    request.get('/tools/page', { params }),

  getById: (id: string): Promise<ToolItem> =>
    request.get(`/tools/${id}`),

  update: (id: string, data: UpdateToolReq): Promise<ToolItem> =>
    request.put(`/tools/${id}`, data),

  delete: (id: string): Promise<void> =>
    request.delete(`/tools/${id}`),
};