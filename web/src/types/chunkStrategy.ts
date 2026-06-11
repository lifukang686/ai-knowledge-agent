import { ListResponse } from './common';

export type ChunkType = '按内容归属' | '按段落' | '按句子' | '按字符';

export interface ChunkStrategy {
  id: string;
  strategyName: string;
  chunkType: ChunkType;
  maxSegmentSize: number;
  overlapSize: number;
  isDefault: boolean;
  createTime: string;
  updateTime: string;
}

export interface ChunkStrategyApiResp {
  id: string | number;
  strategyName: string;
  chunkType: ChunkType;
  maxSegmentSize: number;
  overlapSize: number;
  isDefault: boolean;
  createTime: string;
  updateTime: string;
}

export interface CreateChunkStrategyReq {
  strategyName: string;
  chunkType: ChunkType;
  maxSegmentSize: number;
  overlapSize: number;
}

export interface UpdateChunkStrategyReq {
  strategyName?: string;
  chunkType?: ChunkType;
  maxSegmentSize?: number;
  overlapSize?: number;
}

export interface ChunkStrategyListResponse extends ListResponse<ChunkStrategy> {}
