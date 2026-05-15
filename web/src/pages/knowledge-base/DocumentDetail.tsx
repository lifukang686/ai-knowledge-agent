import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, RefreshCw, FileText, Calendar, User, HardDrive, AlertCircle, Database } from 'lucide-react';
import { toast } from 'sonner';

import { DocumentDetail as DocumentDetailType } from '@/types/knowledgeBase';
import { getDocumentDetail } from '@/services/knowledgeBase';
import { StatusTag } from '@/components/common/StatusTag';
import { formatDateTime, formatFileSize } from '@/utils/format';

const DocumentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [document, setDocument] = useState<DocumentDetailType | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchIdRef = useRef(0);

  useEffect(() => {
    if (!id) return;

    const fetchId = ++fetchIdRef.current;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const data = await getDocumentDetail(id!);
        if (fetchId !== fetchIdRef.current) {
          setLoading(false);
          return;
        }
        setDocument(data);
      } catch (error: any) {
        if (fetchId !== fetchIdRef.current) {
          setLoading(false);
          return;
        }
        const message = error?.message || '加载文档详情失败';
        console.error('加载文档详情失败:', error);
        setError(message);
        toast.error(message);
      } finally {
        if (fetchId === fetchIdRef.current) {
          setLoading(false);
        }
      }
    }

    load();
    return () => { fetchIdRef.current++; };
  }, [id]);

  const handleRetry = () => {
    setError(null);
    setLoading(true);
    const fetchId = ++fetchIdRef.current;

    getDocumentDetail(id!)
      .then(data => {
        if (fetchId !== fetchIdRef.current) return;
        setDocument(data);
      })
      .catch(error => {
        if (fetchId !== fetchIdRef.current) return;
        const message = error?.message || '加载文档详情失败';
        setError(message);
        toast.error(message);
      })
      .finally(() => {
        if (fetchId === fetchIdRef.current) {
          setLoading(false);
        }
      });
  };

  const handleBack = () => {
    if (document?.knowledge_base_id) {
      navigate(`/knowledge-bases/${document.knowledge_base_id}`);
    } else {
      navigate('/knowledge-bases');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <AlertCircle className="h-16 w-16 text-red-400 mb-4" />
        <h2 className="text-xl font-semibold text-gray-900 mb-2">文档加载失败</h2>
        <p className="text-gray-500 mb-6 max-w-md">{error}</p>
        <div className="flex items-center space-x-3">
          <button onClick={handleBack} className="btn-secondary">
            <ArrowLeft className="h-4 w-4 mr-2" />
            返回知识库
          </button>
          <button onClick={handleRetry} className="btn-primary">
            <RefreshCw className="h-4 w-4 mr-2" />
            重试
          </button>
        </div>
      </div>
    );
  }

  if (!document) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <FileText className="h-16 w-16 text-gray-300 mb-4" />
        <h2 className="text-xl font-semibold text-gray-900 mb-2">文档不存在</h2>
        <p className="text-gray-500 mb-6">该文档可能已被删除或您没有访问权限</p>
        <button onClick={handleBack} className="btn-secondary">
          <ArrowLeft className="h-4 w-4 mr-2" />
          返回知识库
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button
            onClick={handleBack}
            className="text-gray-600 hover:text-gray-800 mr-4"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 flex items-center">
              <FileText className="h-6 w-6 text-primary-600 mr-2" />
              {document.title}
            </h1>
          </div>
        </div>
        <StatusTag status={document.status} />
      </div>

      <div className="card">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          <div className="flex items-center">
            <HardDrive className="h-5 w-5 text-gray-400 mr-2" />
            <div>
              <p className="text-xs text-gray-500">文件大小</p>
              <p className="text-sm font-medium text-gray-900">
                {document.file_size ? formatFileSize(document.file_size) : '-'}
              </p>
            </div>
          </div>
          <div className="flex items-center">
            <Database className="h-5 w-5 text-gray-400 mr-2" />
            <div>
              <p className="text-xs text-gray-500">分块数量</p>
              <p className="text-sm font-medium text-gray-900">{document.chunk_count}</p>
            </div>
          </div>
          <div className="flex items-center">
            <User className="h-5 w-5 text-gray-400 mr-2" />
            <div>
              <p className="text-xs text-gray-500">上传者</p>
              <p className="text-sm font-medium text-gray-900">{document.uploaded_by || '-'}</p>
            </div>
          </div>
          <div className="flex items-center">
            <Calendar className="h-5 w-5 text-gray-400 mr-2" />
            <div>
              <p className="text-xs text-gray-500">创建时间</p>
              <p className="text-sm font-medium text-gray-900">
                {formatDateTime(document.created_at)}
              </p>
            </div>
          </div>
          <div className="flex items-center">
            <Calendar className="h-5 w-5 text-gray-400 mr-2" />
            <div>
              <p className="text-xs text-gray-500">更新时间</p>
              <p className="text-sm font-medium text-gray-900">
                {formatDateTime(document.updated_at)}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">文档内容</h2>
        {document.content ? (
          <div className="bg-gray-50 rounded-lg border border-gray-200 overflow-auto">
            <pre className="p-6 text-sm text-gray-800 font-mono leading-relaxed whitespace-pre-wrap break-words max-h-[600px] overflow-y-auto">
              {document.content}
            </pre>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-12 text-center text-gray-500">
            <FileText className="h-12 w-12 text-gray-300 mb-3" />
            <p className="text-sm">暂无文档内容</p>
            <p className="text-xs text-gray-400 mt-1">文档可能尚未完成解析，或文件内容为空</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default DocumentDetailPage;