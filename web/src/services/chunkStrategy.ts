import { request } from '@/utils/request';
import type {
  ChunkStrategy,
  ChunkStrategyApiResp,
  ChunkStrategyListResponse,
  CreateChunkStrategyReq,
  UpdateChunkStrategyReq,
} from '@/types/chunkStrategy';

export interface ChunkStrategyListParams {
  page: number;
  pageSize: number;
  keyword?: string;
}

function mapChunkStrategy(api: ChunkStrategyApiResp): ChunkStrategy {
  return {
    id: api?.id != null ? String(api.id) : '',
    strategyName: api?.strategyName ?? '',
    chunkType: api?.chunkType,
    maxSegmentSize: api?.maxSegmentSize ?? 0,
    overlapSize: api?.overlapSize ?? 0,
    isDefault: Boolean(api?.isDefault),
    createTime: api?.createTime ?? '',
    updateTime: api?.updateTime ?? '',
  };
}

export const chunkStrategyService = {
  list: async (params: ChunkStrategyListParams): Promise<ChunkStrategyListResponse> => {
    const response = await request.get<{
      items: ChunkStrategyApiResp[];
      total: number;
      page: number;
      pageSize: number;
    }>('/chunk-strategies', { params });

    if (!response || !Array.isArray(response.items)) {
      return { items: [], total: 0, page: params.page, pageSize: params.pageSize };
    }

    return {
      items: response.items.map(mapChunkStrategy),
      total: response.total ?? 0,
      page: response.page ?? params.page,
      pageSize: response.pageSize ?? params.pageSize,
    };
  },

  create: (data: CreateChunkStrategyReq): Promise<string> =>
    request.post('/chunk-strategies', data),

  update: (id: string, data: UpdateChunkStrategyReq): Promise<void> =>
    request.put(`/chunk-strategies/${id}`, data),

  delete: (id: string): Promise<void> =>
    request.delete(`/chunk-strategies/${id}`),

  setDefault: (id: string): Promise<void> =>
    request.put(`/chunk-strategies/${id}/default`),
};
