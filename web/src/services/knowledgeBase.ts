import { request } from '@/utils/request';
import { KnowledgeBase, Document, CreateKnowledgeBaseRequest, UpdateKnowledgeBaseRequest } from '@/types/knowledgeBase';
import { ListResponse, SearchParams } from '@/types/common';
import { mockKnowledgeBases, delay, generateMockList } from './mockData';

// 知识库列表
export const getKnowledgeBases = async (params: SearchParams & { page?: number; pageSize?: number }): Promise<ListResponse<KnowledgeBase>> => {
  // 待确认：后端联调时替换为真实API
  console.log('getKnowledgeBases called with params:', params);
  await delay(500);
  
  const { keyword, page = 1, pageSize = 20 } = params;
  const result = generateMockList(mockKnowledgeBases, page, pageSize, keyword);
  console.log('getKnowledgeBases returning:', result);
  return result;
};

// 知识库详情
export const getKnowledgeBase = async (id: string): Promise<KnowledgeBase> => {
  // 待确认：后端联调时替换为真实API
  await delay(300);
  
  const knowledgeBase = mockKnowledgeBases.find(item => item.id === id);
  if (!knowledgeBase) {
    throw new Error('知识库不存在');
  }
  return knowledgeBase;
};

// 创建知识库
export const createKnowledgeBase = async (data: CreateKnowledgeBaseRequest): Promise<{ id: string }> => {
  // 待确认：后端联调时替换为真实API
  await delay(500);
  
  const newKnowledgeBase: KnowledgeBase = {
    id: Math.random().toString(36).substr(2, 9),
    name: data.name,
    description: data.description,
    document_count: 0,
    status: 'completed',
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  };
  
  mockKnowledgeBases.unshift(newKnowledgeBase);
  return { id: newKnowledgeBase.id };
};

// 更新知识库
export const updateKnowledgeBase = async (id: string, data: UpdateKnowledgeBaseRequest): Promise<void> => {
  // 待确认：后端联调时替换为真实API
  await delay(500);
  
  const index = mockKnowledgeBases.findIndex(item => item.id === id);
  if (index === -1) {
    throw new Error('知识库不存在');
  }
  
  mockKnowledgeBases[index] = {
    ...mockKnowledgeBases[index],
    ...data,
    updated_at: new Date().toISOString(),
  };
};

// 删除知识库
export const deleteKnowledgeBase = async (id: string): Promise<void> => {
  // 待确认：后端联调时替换为真实API
  await delay(500);
  
  const index = mockKnowledgeBases.findIndex(item => item.id === id);
  if (index === -1) {
    throw new Error('知识库不存在');
  }
  
  mockKnowledgeBases.splice(index, 1);
};

// 获取知识库下的文档列表
export const getKnowledgeBaseDocuments = async (knowledgeBaseId: string, params: { page?: number; pageSize?: number }): Promise<ListResponse<Document>> => {
  // 待确认：后端联调时替换为真实API
  await delay(400);
  
  // 模拟文档数据
  const mockDocuments: Document[] = [
    {
      id: Math.random().toString(36).substr(2, 9),
      name: 'API设计文档.pdf',
      file_path: '/documents/api-design.pdf',
      knowledge_base_id: knowledgeBaseId,
      status: 'completed',
      uploaded_by: '张三',
      chunk_count: 45,
      file_size: 1024 * 1024 * 2.5,
      created_at: new Date(Date.now() - 86400000).toISOString(),
      updated_at: new Date(Date.now() - 86400000).toISOString(),
    },
    {
      id: Math.random().toString(36).substr(2, 9),
      name: '用户手册.docx',
      file_path: '/documents/user-manual.docx',
      knowledge_base_id: knowledgeBaseId,
      status: 'processing',
      uploaded_by: '李四',
      chunk_count: 0,
      file_size: 1024 * 1024 * 1.8,
      created_at: new Date(Date.now() - 172800000).toISOString(),
      updated_at: new Date().toISOString(),
    },
  ];
  
  const { page = 1, pageSize = 20 } = params;
  return {
    items: mockDocuments,
    total: mockDocuments.length,
    page,
    pageSize,
  };
};

// 上传文档
export const uploadDocument = async (knowledgeBaseId: string, file: File): Promise<{ documentId: string; status: string }> => {
  // 待确认：后端联调时替换为真实API，需要支持文件上传
  await delay(1000);
  
  const documentId = Math.random().toString(36).substr(2, 9);
  return {
    documentId,
    status: 'processing',
  };
};

// 获取文档状态
export const getDocumentStatus = async (documentId: string): Promise<{ status: string }> => {
  // 待确认：后端联调时替换为真实API
  await delay(300);
  
  return {
    status: Math.random() > 0.5 ? 'completed' : 'processing',
  };
};

// 获取单个知识库详情
export const getKnowledgeBaseById = async (id: string): Promise<KnowledgeBase> => {
  // 待确认：后端联调时替换为真实API
  await delay(300);
  
  const knowledgeBase = mockKnowledgeBases.find(item => item.id === id);
  if (!knowledgeBase) {
    throw new Error('知识库不存在');
  }
  
  return knowledgeBase;
};

// 知识库服务对象
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