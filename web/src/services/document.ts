import { Document, DocumentChunk, DocumentCreateRequest, DocumentQueryParams, DocumentUploadResponse, PaginatedResponse } from '../types/document';

// Mock data for documents
const mockDocuments: Document[] = [
  {
    id: '1',
    name: '公司规章制度.pdf',
    file_path: '/uploads/documents/company_rules.pdf',
    file_size: 1024 * 1024, // 1MB
    file_type: 'application/pdf',
    status: 'completed',
    knowledge_base_id: '1',
    uploaded_by: 'admin',
    processing_progress: 100,
    chunks_count: 25,
    indexed_chunks_count: 25,
    created_at: '2024-01-15T10:30:00Z',
    updated_at: '2024-01-15T10:35:00Z'
  },
  {
    id: '2',
    name: '技术架构文档.docx',
    file_path: '/uploads/documents/tech_arch.docx',
    file_size: 512 * 1024, // 512KB
    file_type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    status: 'processing',
    knowledge_base_id: '1',
    uploaded_by: 'user1',
    processing_progress: 65,
    chunks_count: 15,
    indexed_chunks_count: 10,
    created_at: '2024-01-16T14:20:00Z',
    updated_at: '2024-01-16T14:25:00Z'
  },
  {
    id: '3',
    name: '产品需求文档.txt',
    file_path: '/uploads/docments/product_requirements.txt',
    file_size: 256 * 1024, // 256KB
    file_type: 'text/plain',
    status: 'failed',
    knowledge_base_id: '1',
    uploaded_by: 'user2',
    processing_progress: 30,
    error_message: '文件解析失败：不支持的编码格式',
    created_at: '2024-01-17T09:15:00Z',
    updated_at: '2024-01-17T09:20:00Z'
  }
];

// Mock data for document chunks
const mockChunks: DocumentChunk[] = [
  {
    id: 'chunk_1',
    document_id: '1',
    content: '第一章：公司概述\n\n本公司成立于2020年，致力于...',
    chunk_index: 1,
    token_count: 150,
    embedding_status: 'completed',
    indexed_at: '2024-01-15T10:31:00Z'
  },
  {
    id: 'chunk_2',
    document_id: '1',
    content: '第二章：员工行为规范\n\n所有员工应遵守以下行为准则...',
    chunk_index: 2,
    token_count: 200,
    embedding_status: 'completed',
    indexed_at: '2024-01-15T10:31:30Z'
  },
  {
    id: 'chunk_3',
    document_id: '1',
    content: '第三章：考勤制度\n\n工作时间：周一至周五 9:00-18:00...',
    chunk_index: 3,
    token_count: 180,
    embedding_status: 'completed',
    indexed_at: '2024-01-15T10:32:00Z'
  }
];

export const documentService = {
  // Get documents list with pagination and filtering
  async getDocuments(params: DocumentQueryParams): Promise<PaginatedResponse<Document>> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    let filteredDocuments = mockDocuments.filter(doc => 
      !params.knowledge_base_id || doc.knowledge_base_id === params.knowledge_base_id
    );

    if (params.status) {
      filteredDocuments = filteredDocuments.filter(doc => doc.status === params.status);
    }

    if (params.name) {
      filteredDocuments = filteredDocuments.filter(doc => 
        doc.name.toLowerCase().includes(params.name.toLowerCase())
      );
    }

    // Apply pagination
    const start = (params.page - 1) * params.pageSize;
    const end = start + params.pageSize;
    const paginatedDocuments = filteredDocuments.slice(start, end);

    return {
      data: paginatedDocuments,
      total: filteredDocuments.length,
      page: params.page,
      pageSize: params.pageSize
    };
  },

  // Get document by ID
  async getDocumentById(id: string): Promise<Document> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    const document = mockDocuments.find(doc => doc.id === id);
    if (!document) {
      throw new Error('Document not found');
    }
    
    return document;
  },

  // Upload document
  async uploadDocument(data: DocumentCreateRequest): Promise<DocumentUploadResponse> {
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // Simulate file upload
    const newDocument: Document = {
      id: String(Date.now()),
      name: data.file.name,
      file_path: `/uploads/documents/${data.file.name}`,
      file_size: data.file.size,
      file_type: data.file.type,
      status: 'pending',
      knowledge_base_id: data.knowledgeBaseId,
      uploaded_by: 'current_user', // 待确认：需要从auth获取当前用户
      processing_progress: 0,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
    
    mockDocuments.unshift(newDocument);
    
    // Simulate processing start after upload
    setTimeout(() => {
      const docIndex = mockDocuments.findIndex(doc => doc.id === newDocument.id);
      if (docIndex !== -1) {
        mockDocuments[docIndex].status = 'processing';
        mockDocuments[docIndex].processing_progress = 0;
      }
    }, 2000);

    return {
      documentId: newDocument.id,
      status: 'uploaded'
    };
  },

  // Get document chunks
  async getDocumentChunks(documentId: string): Promise<DocumentChunk[]> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    return mockChunks.filter(chunk => chunk.document_id === documentId);
  },

  // Refresh document status
  async refreshDocumentStatus(documentId: string): Promise<Document> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const document = mockDocuments.find(doc => doc.id === documentId);
    if (!document) {
      throw new Error('Document not found');
    }

    // Simulate status progression
    if (document.status === 'processing') {
      document.processing_progress = Math.min(100, (document.processing_progress || 0) + Math.random() * 20);
      
      if (document.processing_progress >= 100) {
        document.status = 'completed';
        document.chunks_count = 25;
        document.indexed_chunks_count = 25;
      }
    }

    document.updated_at = new Date().toISOString();
    return document;
  },

  // Delete document
  async deleteDocument(id: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const documentIndex = mockDocuments.findIndex(doc => doc.id === id);
    if (documentIndex === -1) {
      throw new Error('Document not found');
    }
    
    mockDocuments.splice(documentIndex, 1);
  }
};