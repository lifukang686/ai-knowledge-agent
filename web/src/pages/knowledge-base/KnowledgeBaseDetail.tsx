import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Upload, RefreshCw, Eye, FileText, Database, Clock } from 'lucide-react';
import { toast } from 'sonner';
import axios from 'axios';

import { KnowledgeBase, Document } from '@/types/knowledgeBase';
import { getKnowledgeBase, getKnowledgeBaseDocuments, uploadDocument } from '@/services/knowledgeBase';
import { DataTable } from '@/components/common/DataTable';
import { StatusTag } from '@/components/common/StatusTag';
import { Pagination } from '@/components/common/Pagination';
import { formatDateTime, formatFileSize, truncateText } from '@/utils/format';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';

const KnowledgeBaseDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [knowledgeBase, setKnowledgeBase] = useState<KnowledgeBase | null>(null);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [uploading, setUploading] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const fetchIdRef = useRef(0);

  const loadDocuments = async () => {
    if (!id) return;
    
    try {
      const result = await getKnowledgeBaseDocuments(id, { page, pageSize });
      setDocuments(result.items);
      setTotal(result.total);
    } catch (error) {
      console.error('加载文档列表失败:', error);
      toast.error('加载文档列表失败');
    }
  };

  useEffect(() => {
    const fetchId = ++fetchIdRef.current;
    const controller = new AbortController();

    async function doLoad() {
      setLoading(true);
      try {
        const data = await getKnowledgeBase(id!, controller.signal);
        if (fetchId !== fetchIdRef.current) return;
        setKnowledgeBase(data);
      } catch (error: any) {
        if (fetchId !== fetchIdRef.current) return;
        if (axios.isCancel(error) || error?.code === 'ERR_CANCELED' || controller.signal.aborted) return;
        console.error('加载知识库详情失败:', error);
        toast.error('加载知识库详情失败');
        navigate('/knowledge-bases');
      } finally {
        if (fetchId === fetchIdRef.current && !controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    if (id) doLoad();
    return () => controller.abort();
  }, [id, navigate, refreshKey]);

  // 文件上传处理
  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !id) return;

    setUploading(true);
    try {
      const result = await uploadDocument(id, file);
      toast.success(`文档上传成功，ID: ${result.documentId}`);
      loadDocuments();
    } catch (error) {
      console.error('文档上传失败:', error);
      toast.error('文档上传失败');
    } finally {
      setUploading(false);
      // 清空文件输入
      event.target.value = '';
    }
  };

  // 查看文档详情
  const handleViewDocument = (documentId: string) => {
    navigate(`/documents/${documentId}`);
  };

  const handleRefresh = () => {
    setRefreshKey(k => k + 1);
    loadDocuments();
  };

  // 文档表格列配置
  const documentColumns = [
    {
      key: 'name',
      title: '文档名称',
      render: (value: string, record: Document) => (
        <div className="flex items-center">
          <FileText className="h-4 w-4 text-gray-400 mr-2" />
          <span className="font-medium text-gray-900">{value}</span>
        </div>
      )
    },
    {
      key: 'file_size',
      title: '文件大小',
      width: '100px',
      render: (value: number) => (
        <span className="text-gray-600 text-sm">
          {value ? formatFileSize(value) : '-'}
        </span>
      )
    },
    {
      key: 'chunk_count',
      title: '分块数量',
      width: '100px',
      render: (value: number) => (
        <span className="text-gray-600">{value || 0}</span>
      )
    },
    {
      key: 'status',
      title: '处理状态',
      width: '100px',
      render: (value: string) => <StatusTag status={value} />
    },
    {
      key: 'uploaded_by',
      title: '上传人',
      width: '120px',
      render: (value: string) => (
        <span className="text-gray-600">{value || '-'}</span>
      )
    },
    {
      key: 'created_at',
      title: '上传时间',
      width: '150px',
      render: (value: string) => (
        <span className="text-gray-600 text-sm">
          {formatDateTime(value)}
        </span>
      )
    },
    {
      key: 'actions',
      title: '操作',
      width: '100px',
      render: (_: any, record: Document) => (
        <button
          onClick={() => handleViewDocument(record.id)}
          className="text-primary-600 hover:text-primary-800 text-sm font-medium"
        >
          <Eye className="h-4 w-4 inline mr-1" />
          查看
        </button>
      )
    }
  ];

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!knowledgeBase) {
    return (
      <div className="text-center py-12 text-gray-500">
        知识库不存在
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题和返回 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button
            onClick={() => navigate('/knowledge-bases')}
            className="text-gray-600 hover:text-gray-800 mr-4"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{knowledgeBase.name}</h1>
            <p className="text-gray-600 mt-1">
              {knowledgeBase.description || '暂无描述'}
            </p>
          </div>
        </div>
        
        <div className="flex items-center space-x-3">
          <button
            onClick={handleRefresh}
            className="btn-secondary"
          >
            <RefreshCw className="h-4 w-4 mr-2" />
            刷新状态
          </button>
          
          <label className="btn-primary cursor-pointer">
            <Upload className="h-4 w-4 mr-2" />
            上传文档
            <input
              type="file"
              className="hidden"
              accept=".pdf,.doc,.docx,.txt,.md"
              onChange={handleFileUpload}
              disabled={uploading}
            />
          </label>
        </div>
      </div>

      {/* 知识库信息卡片 */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div className="flex items-center">
            <Database className="h-8 w-8 text-primary-600 mr-3" />
            <div>
              <p className="text-sm text-gray-600">文档数量</p>
              <p className="text-2xl font-bold text-gray-900">{knowledgeBase.document_count || 0}</p>
            </div>
          </div>
          
          <div className="flex items-center">
            <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center mr-3">
              <div className="w-3 h-3 bg-green-500 rounded-full"></div>
            </div>
            <div>
              <p className="text-sm text-gray-600">状态</p>
              <StatusTag status={knowledgeBase.status || 'unknown'} />
            </div>
          </div>
          
          <div className="flex items-center">
            <Clock className="h-8 w-8 text-gray-400 mr-3" />
            <div>
              <p className="text-sm text-gray-600">创建时间</p>
              <p className="text-sm font-medium text-gray-900">
                {formatDateTime(knowledgeBase.created_at)}
              </p>
            </div>
          </div>
          
          <div className="flex items-center">
            <Clock className="h-8 w-8 text-gray-400 mr-3" />
            <div>
              <p className="text-sm text-gray-600">更新时间</p>
              <p className="text-sm font-medium text-gray-900">
                {formatDateTime(knowledgeBase.updated_at)}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 文档列表 */}
      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">文档列表</h2>
          <div className="text-sm text-gray-600">
            共 {total} 个文档
          </div>
        </div>
        
        <DataTable
          columns={documentColumns}
          data={documents}
          loading={loading}
          emptyText="暂无文档"
        />
        
        {total > 0 && (
          <div className="mt-6">
            <Pagination
              current={page}
              total={total}
              pageSize={pageSize}
              onChange={setPage}
              showSizeChanger
              onShowSizeChange={setPageSize}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default KnowledgeBaseDetail;