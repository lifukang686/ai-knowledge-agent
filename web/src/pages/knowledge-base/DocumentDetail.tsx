import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, RefreshCw, FileText, Calendar, User, HardDrive, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import { documentService } from '../../services/document';
import { knowledgeBaseService } from '../../services/knowledgeBase';
import { Document, DocumentChunk } from '../../types/document';
import { KnowledgeBase } from '../../types/knowledgeBase';
import { StatusTag, DataTable } from '../../components/common';

const DocumentDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [document, setDocument] = useState<Document | null>(null);
  const [knowledgeBase, setKnowledgeBase] = useState<KnowledgeBase | null>(null);
  const [chunks, setChunks] = useState<DocumentChunk[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(false);

  // Fetch document data
  const fetchDocument = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      const doc = await documentService.getDocumentById(id);
      setDocument(doc);
      
      // Fetch knowledge base info
      try {
        const kb = await knowledgeBaseService.getKnowledgeBaseById(doc.knowledge_base_id);
        setKnowledgeBase(kb);
      } catch (error) {
        console.error('Failed to fetch knowledge base:', error);
      }
      
      // Fetch document chunks
      try {
        const chunksData = await documentService.getDocumentChunks(id);
        setChunks(chunksData);
      } catch (error) {
        console.error('Failed to fetch chunks:', error);
      }
    } catch (error) {
      toast.error('获取文档详情失败');
      console.error('Failed to fetch document:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocument();
  }, [id]);

  // Auto refresh for processing documents
  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    if (autoRefresh && document?.status === 'processing') {
      interval = setInterval(() => {
        handleRefresh();
      }, 5000); // Refresh every 5 seconds
    }
    
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, [autoRefresh, document?.status]);

  // Handle refresh
  const handleRefresh = async () => {
    if (!id) return;
    
    try {
      setRefreshing(true);
      const updatedDoc = await documentService.refreshDocumentStatus(id);
      setDocument(updatedDoc);
      
      // Refresh chunks if document is completed
      if (updatedDoc.status === 'completed') {
        const chunksData = await documentService.getDocumentChunks(id);
        setChunks(chunksData);
      }
      
      // Stop auto refresh if completed or failed
      if (updatedDoc.status === 'completed' || updatedDoc.status === 'failed') {
        setAutoRefresh(false);
      }
    } catch (error) {
      toast.error('刷新文档状态失败');
      console.error('Failed to refresh document:', error);
    } finally {
      setRefreshing(false);
    }
  };

  // Format file size
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  // Format MIME type to readable format
  const formatFileType = (mimeType: string): string => {
    const typeMap: Record<string, string> = {
      'application/pdf': 'PDF',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'Word',
      'text/plain': 'Text',
      'text/markdown': 'Markdown'
    };
    return typeMap[mimeType] || mimeType;
  };

  // Get status color and text
  const getStatusInfo = (status: string) => {
    const statusMap = {
      pending: { text: '待处理', color: 'yellow' },
      processing: { text: '处理中', color: 'blue' },
      completed: { text: '已完成', color: 'green' },
      failed: { text: '失败', color: 'red' }
    };
    return statusMap[status as keyof typeof statusMap] || { text: status, color: 'gray' };
  };

  // Table columns for chunks
  const chunkColumns = [
    {
      title: '分块序号',
      dataIndex: 'chunk_index',
      key: 'chunk_index',
      width: 100,
      render: (index: number) => <span className="font-medium">#{index}</span>
    },
    {
      title: '内容预览',
      dataIndex: 'content',
      key: 'content',
      render: (content: string) => (
        <div className="max-w-md">
          <p className="text-sm text-gray-600 truncate">{content}</p>
        </div>
      )
    },
    {
      title: 'Token数',
      dataIndex: 'token_count',
      key: 'token_count',
      width: 100,
      render: (count: number) => <span className="text-sm text-gray-500">{count}</span>
    },
    {
      title: '嵌入状态',
      dataIndex: 'embedding_status',
      key: 'embedding_status',
      width: 120,
      render: (status: string) => (
        <StatusTag status={status} mapping={{
          pending: { text: '待处理', color: 'yellow' },
          processing: { text: '处理中', color: 'blue' },
          completed: { text: '已完成', color: 'green' },
          failed: { text: '失败', color: 'red' }
        }} />
      )
    },
    {
      title: '索引时间',
      dataIndex: 'indexed_at',
      key: 'indexed_at',
      width: 160,
      render: (text: string) => (
        <span className="text-sm text-gray-500">
          {text ? new Date(text).toLocaleString('zh-CN') : '-'}
        </span>
      )
    }
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!document) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500">文档不存在</p>
      </div>
    );
  }

  const statusInfo = getStatusInfo(document.status);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <button
            onClick={() => navigate(`/knowledge-bases/${document.knowledge_base_id}`)}
            className="flex items-center space-x-2 text-gray-600 hover:text-gray-800"
          >
            <ArrowLeft size={20} />
            <span>返回知识库</span>
          </button>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setAutoRefresh(!autoRefresh)}
            className={`px-3 py-2 rounded-lg text-sm font-medium ${
              autoRefresh 
                ? 'bg-green-100 text-green-800 border border-green-300' 
                : 'bg-gray-100 text-gray-700 border border-gray-300'
            }`}
          >
            {autoRefresh ? '自动刷新开启' : '自动刷新关闭'}
          </button>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} />
            <span>刷新状态</span>
          </button>
        </div>
      </div>

      {/* Document Info Card */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-100 rounded-lg">
              <FileText className="h-6 w-6 text-blue-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{document.name}</h1>
              <p className="text-gray-600">
                所属知识库: {knowledgeBase?.name || '未知'}
              </p>
            </div>
          </div>
          <StatusTag 
            status={document.status} 
            mapping={{
              pending: { text: '待处理', color: 'yellow' },
              processing: { text: '处理中', color: 'blue' },
              completed: { text: '已完成', color: 'green' },
              failed: { text: '失败', color: 'red' }
            }}
          />
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="flex items-center space-x-2">
            <HardDrive className="h-4 w-4 text-gray-400" />
            <div>
              <p className="text-sm text-gray-500">文件大小</p>
              <p className="font-medium">{formatFileSize(document.file_size)}</p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <FileText className="h-4 w-4 text-gray-400" />
            <div>
              <p className="text-sm text-gray-500">文件类型</p>
              <p className="font-medium">{formatFileType(document.file_type)}</p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <User className="h-4 w-4 text-gray-400" />
            <div>
              <p className="text-sm text-gray-500">上传者</p>
              <p className="font-medium">{document.uploaded_by}</p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Calendar className="h-4 w-4 text-gray-400" />
            <div>
              <p className="text-sm text-gray-500">上传时间</p>
              <p className="font-medium">{new Date(document.created_at).toLocaleString('zh-CN')}</p>
            </div>
          </div>
        </div>

        {/* Processing Progress */}
        {document.status === 'processing' && (
          <div className="mt-4">
            <div className="flex justify-between text-sm text-gray-600 mb-2">
              <span>处理进度</span>
              <span>{Math.round(document.processing_progress || 0)}%</span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div 
                className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${document.processing_progress || 0}%` }}
              ></div>
            </div>
          </div>
        )}

        {/* Error Message */}
        {document.status === 'failed' && document.error_message && (
          <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-center space-x-2">
              <AlertCircle className="h-5 w-5 text-red-500" />
              <span className="font-medium text-red-800">处理失败</span>
            </div>
            <p className="mt-2 text-sm text-red-700">{document.error_message}</p>
          </div>
        )}

        {/* Processing Stats */}
        {document.status === 'completed' && (
          <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="text-center p-3 bg-green-50 rounded-lg">
              <p className="text-2xl font-bold text-green-600">{document.chunks_count || 0}</p>
              <p className="text-sm text-gray-600">总分块数</p>
            </div>
            <div className="text-center p-3 bg-blue-50 rounded-lg">
              <p className="text-2xl font-bold text-blue-600">{document.indexed_chunks_count || 0}</p>
              <p className="text-sm text-gray-600">已索引分块</p>
            </div>
            <div className="text-center p-3 bg-purple-50 rounded-lg">
              <p className="text-2xl font-bold text-purple-600">
                {document.indexed_chunks_count && document.chunks_count 
                  ? Math.round((document.indexed_chunks_count / document.chunks_count) * 100) 
                  : 0}%
              </p>
              <p className="text-sm text-gray-600">索引完成率</p>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded-lg">
              <p className="text-2xl font-bold text-gray-600">
                {new Date(document.updated_at).toLocaleDateString('zh-CN')}
              </p>
              <p className="text-sm text-gray-600">最后更新</p>
            </div>
          </div>
        )}
      </div>

      {/* Document Chunks */}
      {chunks.length > 0 && (
        <div className="bg-white rounded-lg border border-gray-200">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">文档分块详情</h2>
            <p className="text-sm text-gray-600 mt-1">文档被分割成的文本块及其索引状态</p>
          </div>
          <div className="p-6">
            <DataTable
              columns={chunkColumns}
              dataSource={chunks}
              pagination={{
                current: 1,
                pageSize: 10,
                total: chunks.length,
                showSizeChanger: true,
                showQuickJumper: true
              }}
            />
          </div>
        </div>
      )}

      {/* Empty State */}
      {chunks.length === 0 && document.status === 'completed' && (
        <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
          <FileText className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无分块数据</h3>
          <p className="text-gray-500">文档尚未完成分块处理，请稍后刷新查看</p>
        </div>
      )}
    </div>
  );
};

export default DocumentDetail;