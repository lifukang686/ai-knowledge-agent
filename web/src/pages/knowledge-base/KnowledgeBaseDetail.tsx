import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Upload, RefreshCw, Eye, Trash2, FileText, Database, Clock, AlertCircle, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { toast } from 'sonner';
import axios from 'axios';

import { KnowledgeBase, Document } from '@/types/knowledgeBase';
import { getKnowledgeBase, getKnowledgeBaseDocuments, uploadDocument, deleteDocument } from '@/services/knowledgeBase';
import { DataTable } from '@/components/common/DataTable';
import { StatusTag } from '@/components/common/StatusTag';
import { Pagination } from '@/components/common/Pagination';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { formatDateTime, formatFileSize } from '@/utils/format';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';

type SortField = 'name' | 'created_at';
type SortDir = 'asc' | 'desc';
type DocsLoadingState = 'idle' | 'loading' | 'error';

const MAX_RETRY_COUNT = 3;

const KnowledgeBaseDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(false);
  const [knowledgeBase, setKnowledgeBase] = useState<KnowledgeBase | null>(null);

  const [documents, setDocuments] = useState<Document[]>([]);
  const [docsTotal, setDocsTotal] = useState(0);
  const [docsPage, setDocsPage] = useState(1);
  const [docsPageSize, setDocsPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [docsLoadingState, setDocsLoadingState] = useState<DocsLoadingState>('idle');
  const [docsError, setDocsError] = useState<string | null>(null);
  const [retryCount, setRetryCount] = useState(0);

  const [uploading, setUploading] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const [sortField, setSortField] = useState<SortField | null>(null);
  const [sortDir, setSortDir] = useState<SortDir>('asc');

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Document | null>(null);
  const [deleting, setDeleting] = useState(false);

  const fetchIdRef = useRef(0);
  const kbLoadedRef = useRef(false);
  const retryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const retryCountRef = useRef(0);

  const loadDocuments = useCallback(async (isRetry = false) => {
    if (!id) return;

    if (!isRetry) {
      retryCountRef.current = 0;
      setRetryCount(0);
    }

    const fetchId = ++fetchIdRef.current;
    setDocsLoadingState('loading');
    setDocsError(null);

    try {
      const result = await getKnowledgeBaseDocuments(id, { page: docsPage, pageSize: docsPageSize });
      if (fetchId !== fetchIdRef.current) return;
      setDocuments(result.items);
      setDocsTotal(result.total);
      setDocsLoadingState('idle');
      retryCountRef.current = 0;
      setRetryCount(0);
    } catch (error: any) {
      if (fetchId !== fetchIdRef.current) return;

      const message = error?.message || '加载文档列表失败';
      console.error('加载文档列表失败:', error);
      setDocsError(message);
      setDocsLoadingState('error');

      const nextRetry = isRetry ? retryCountRef.current + 1 : 1;
      if (nextRetry <= MAX_RETRY_COUNT) {
        retryCountRef.current = nextRetry;
        setRetryCount(nextRetry);
        const delay = Math.min(1000 * Math.pow(2, nextRetry - 1), 10000);
        retryTimerRef.current = setTimeout(() => loadDocuments(true), delay);
      }
    }
  }, [id, docsPage, docsPageSize]);

  useEffect(() => {
    const fetchId = ++fetchIdRef.current;
    const controller = new AbortController();

    async function doLoad() {
      setLoading(true);
      try {
        const data = await getKnowledgeBase(id!, controller.signal);
        if (fetchId !== fetchIdRef.current) {
          setLoading(false);
          return;
        }
        setKnowledgeBase(data);
        kbLoadedRef.current = true;
        loadDocuments();
      } catch (error: any) {
        if (fetchId !== fetchIdRef.current) {
          setLoading(false);
          return;
        }
        if (axios.isCancel(error) || error?.code === 'ERR_CANCELED') {
          setLoading(false);
          return;
        }
        console.error('加载知识库详情失败:', error);
        toast.error('加载知识库详情失败');
        navigate('/knowledge-bases');
      } finally {
        if (fetchId === fetchIdRef.current) {
          setLoading(false);
        }
      }
    }

    if (id) doLoad();
    return () => controller.abort();
  }, [id, navigate, refreshKey]);

  useEffect(() => {
    if (kbLoadedRef.current) {
      loadDocuments();
    }
  }, [docsPage, docsPageSize]);

  useEffect(() => {
    if (kbLoadedRef.current) {
      setDocsPage(1);
      loadDocuments();
    }
  }, [refreshKey]);

  useEffect(() => {
    return () => {
      if (retryTimerRef.current) {
        clearTimeout(retryTimerRef.current);
      }
    };
  }, []);

  const handleDocsRetry = () => {
    if (retryTimerRef.current) {
      clearTimeout(retryTimerRef.current);
      retryTimerRef.current = null;
    }
    loadDocuments();
  };

  const handleDocsPageChange = (page: number) => {
    setDocsPage(page);
  };

  const handleDocsPageSizeChange = (size: number) => {
    setDocsPageSize(size);
    setDocsPage(1);
  };

  const handleSort = (field: SortField) => {
    let newDir: SortDir = 'asc';
    if (sortField === field) {
      newDir = sortDir === 'asc' ? 'desc' : 'asc';
    }
    setSortField(field);
    setSortDir(newDir);
  };

  const getSortedDocuments = (): Document[] => {
    if (!sortField) return documents;

    return [...documents].sort((a, b) => {
      let valA: string | number = '';
      let valB: string | number = '';

      if (sortField === 'name') {
        valA = a.name.toLowerCase();
        valB = b.name.toLowerCase();
      } else if (sortField === 'created_at') {
        valA = a.created_at;
        valB = b.created_at;
      }

      if (valA < valB) return sortDir === 'asc' ? -1 : 1;
      if (valA > valB) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });
  };

  const renderSortIcon = (field: SortField) => {
    if (sortField !== field) {
      return <ArrowUpDown className="h-4 w-4 text-gray-400" />;
    }
    return sortDir === 'asc'
      ? <ArrowUp className="h-4 w-4 text-primary-600" />
      : <ArrowDown className="h-4 w-4 text-primary-600" />;
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file || !id) return;

    setUploading(true);
    try {
      const result = await uploadDocument(id, file);
      toast.success(`文档上传成功`);
      setRefreshKey(k => k + 1);
    } catch (error) {
      console.error('文档上传失败:', error);
      toast.error('文档上传失败');
    } finally {
      setUploading(false);
      event.target.value = '';
    }
  };

  const handleViewDocument = (documentId: string) => {
    navigate(`/documents/${documentId}`);
  };

  const handleDeleteClick = (doc: Document) => {
    setDeleteTarget(doc);
    setConfirmOpen(true);
  };

  const handleDeleteCancel = () => {
    if (deleting) return;
    setConfirmOpen(false);
    setDeleteTarget(null);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;

    setDeleting(true);
    try {
      await deleteDocument(deleteTarget.id);
      toast.success(`文档「${deleteTarget.name}」已删除`);
      setConfirmOpen(false);
      setDeleteTarget(null);
      loadDocuments();
    } catch (error: any) {
      const message = error?.message || '删除文档失败';
      console.error('删除文档失败:', error);
      toast.error(message);
    } finally {
      setDeleting(false);
    }
  };

  const handleRefresh = () => {
    setRefreshKey(k => k + 1);
  };

  const documentColumns = [
    {
      key: 'name',
      title: (
        <button
          onClick={() => handleSort('name')}
          className="flex items-center hover:text-primary-600 transition-colors"
        >
          文档名称
          <span className="ml-1">{renderSortIcon('name')}</span>
        </button>
      ),
      render: (value: string, record: Document) => (
        <div className="flex items-center">
          <FileText className="h-4 w-4 text-gray-400 mr-2 flex-shrink-0" />
          <span className="font-medium text-gray-900 truncate" title={value}>
            {value}
          </span>
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
        <span className="text-gray-600">{value ?? 0}</span>
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
      title: (
        <button
          onClick={() => handleSort('created_at')}
          className="flex items-center hover:text-primary-600 transition-colors"
        >
          上传时间
          <span className="ml-1">{renderSortIcon('created_at')}</span>
        </button>
      ),
      width: '160px',
      render: (value: string) => (
        <span className="text-gray-600 text-sm">
          {formatDateTime(value)}
        </span>
      )
    },
    {
      key: 'actions',
      title: '操作',
      width: '130px',
      render: (_: any, record: Document) => (
        <div className="flex items-center space-x-2">
          <button
            onClick={() => handleViewDocument(record.id)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
          >
            <Eye className="h-4 w-4 inline mr-1" />
            查看
          </button>
          <button
            onClick={() => handleDeleteClick(record)}
            className="text-red-600 hover:text-red-800 text-sm font-medium"
          >
            <Trash2 className="h-4 w-4 inline mr-1" />
            删除
          </button>
        </div>
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
            disabled={docsLoadingState === 'loading'}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${docsLoadingState === 'loading' ? 'animate-spin' : ''}`} />
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

      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">文档列表</h2>
          <div className="text-sm text-gray-600">
            共 {docsTotal} 个文档
          </div>
        </div>

        {docsLoadingState === 'error' ? (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <AlertCircle className="h-12 w-12 text-red-400 mb-4" />
            <p className="text-gray-700 font-medium mb-1">文档加载失败</p>
            <p className="text-gray-500 text-sm mb-4 max-w-md">
              {docsError || '请检查网络连接后重试'}
              {retryCount > 0 && retryCount <= MAX_RETRY_COUNT && (
                <span className="ml-1">（自动重试 {retryCount}/{MAX_RETRY_COUNT}）</span>
              )}
            </p>
            <button
              onClick={handleDocsRetry}
              className="btn-primary"
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              重新加载
            </button>
          </div>
        ) : (
          <>
            <DataTable
              columns={documentColumns}
              data={getSortedDocuments()}
              loading={docsLoadingState === 'loading'}
              emptyText="暂无文档，请上传文档"
            />

            {docsTotal > 0 && (
              <div className="mt-6">
                <Pagination
                  current={docsPage}
                  total={docsTotal}
                  pageSize={docsPageSize}
                  onChange={handleDocsPageChange}
                  showSizeChanger
                  onShowSizeChange={handleDocsPageSizeChange}
                />
              </div>
            )}
          </>
        )}
      </div>

      <ConfirmModal
        isOpen={confirmOpen}
        title="确认删除文档"
        message={deleteTarget ? `确定要删除文档「${deleteTarget.name}」吗？删除后不可恢复。` : ''}
        confirmText="确认删除"
        cancelText="取消"
        danger={true}
        onConfirm={handleDeleteConfirm}
        onCancel={handleDeleteCancel}
        loading={deleting}
      />
    </div>
  );
};

export default KnowledgeBaseDetail;