import { request } from '@/utils/request';
import {
  KnowledgeBase,
  Document,
  CreateKnowledgeBaseRequest,
  UpdateKnowledgeBaseRequest,
  KnowledgeBaseApiResp,
  KnowledgeBaseListResponse,
} from '@/types/knowledgeBase';
import { ListResponse } from '@/types/common';
import { API_ENDPOINTS } from '@/utils/constants';

function mapKnowledgeBaseFromApi(api: KnowledgeBaseApiResp): KnowledgeBase {
  return {
    id: api?.id != null ? String(api.id) : '',
    name: api?.name ?? '',
    description: api?.description ?? undefined,
    document_count: api?.documentCount ?? 0,
    status: api?.status ?? 'unknown',
    created_at: api?.createTime ?? '',
    updated_at: api?.updateTime ?? '',
  };
}

function buildKnowledgeBaseParams(params: { page?: number; pageSize?: number; keyword?: string }) {
  const query: Record<string, string | number> = {};
  if (params.page) query.page = params.page;
  if (params.pageSize) query.pageSize = params.pageSize;
  if (params.keyword) query.keyword = params.keyword;
  return query;
}

export const getKnowledgeBases = async (params: {
  page?: number;
  pageSize?: number;
  keyword?: string;
}): Promise<KnowledgeBaseListResponse> => {
  const response = await request.get<{
    items: KnowledgeBaseApiResp[];
    total: number;
    page: number;
    pageSize: number;
  }>(API_ENDPOINTS.KNOWLEDGE_BASES, {
    params: buildKnowledgeBaseParams(params),
  });

  if (!response || !Array.isArray(response.items)) {
    return { items: [], total: 0, page: params.page || 1, pageSize: params.pageSize || 20 };
  }

  return {
    items: response.items.map(mapKnowledgeBaseFromApi),
    total: response.total ?? 0,
    page: response.page ?? 1,
    pageSize: response.pageSize ?? 20,
  };
};

export const getKnowledgeBase = async (id: string): Promise<KnowledgeBase> => {
  const response = await request.get<KnowledgeBaseApiResp>(
    API_ENDPOINTS.KNOWLEDGE_BASE_DETAIL(id),
  );
  if (!response) {
    throw new Error('知识库不存在');
  }
  return mapKnowledgeBaseFromApi(response);
};

export const createKnowledgeBase = async (
  data: CreateKnowledgeBaseRequest,
): Promise<string> => {
  const id = await request.post(API_ENDPOINTS.KNOWLEDGE_BASES, data);
  return id != null ? String(id) : '';
};

export const updateKnowledgeBase = async (
  id: string,
  data: UpdateKnowledgeBaseRequest,
): Promise<void> => {
  await request.put(API_ENDPOINTS.KNOWLEDGE_BASE_DETAIL(id), data);
};

export const deleteKnowledgeBase = async (id: string): Promise<void> => {
  await request.delete(API_ENDPOINTS.KNOWLEDGE_BASE_DETAIL(id));
};

export const getKnowledgeBaseDocuments = async (
  knowledgeBaseId: string,
  params: { page?: number; pageSize?: number },
): Promise<ListResponse<Document>> => {
  const response = await request.get<{
    items: Document[];
    total: number;
    page: number;
    pageSize: number;
  }>(API_ENDPOINTS.KNOWLEDGE_BASE_DOCUMENTS(knowledgeBaseId), {
    params: {
      page: params.page,
      pageSize: params.pageSize,
    },
  });
  return response;
};

export const uploadDocument = async (
  knowledgeBaseId: string,
  file: File,
): Promise<{ documentId: string; status: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('knowledgeBaseId', knowledgeBaseId);

  const response = await request.post<{ documentId: string; status: string }>(
    API_ENDPOINTS.UPLOAD_DOCUMENT,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,
    },
  );
  return response;
};

export const getDocumentStatus = async (documentId: string): Promise<{ status: string }> => {
  return request.get<{ status: string }>(API_ENDPOINTS.DOCUMENT_STATUS(documentId));
};

export const getKnowledgeBaseById = getKnowledgeBase;

export const knowledgeBaseService = {
  getKnowledgeBases,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase,
  getKnowledgeBaseDocuments,
  uploadDocument,
  getDocumentStatus,
  getKnowledgeBaseById,
};