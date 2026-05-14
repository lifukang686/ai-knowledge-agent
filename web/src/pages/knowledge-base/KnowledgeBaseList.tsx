import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, RefreshCw, Eye, Edit, Trash2, Upload } from 'lucide-react';
import { toast } from 'sonner';
import axios from 'axios';

import { KnowledgeBase } from '@/types/knowledgeBase';
import { getKnowledgeBases, createKnowledgeBase, updateKnowledgeBase, deleteKnowledgeBase } from '@/services/knowledgeBase';
import { DataTable } from '@/components/common/DataTable';
import { SearchBar } from '@/components/common/SearchBar';
import { FormModal } from '@/components/common/FormModal';
import { StatusTag } from '@/components/common/StatusTag';
import { Pagination } from '@/components/common/Pagination';
import { formatDateTime, truncateText } from '@/utils/format';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';

const KnowledgeBaseList: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<KnowledgeBase[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<KnowledgeBase | null>(null);

  const [formData, setFormData] = useState({
    name: '',
    description: ''
  });
  const [submitting, setSubmitting] = useState(false);

  const fetchIdRef = useRef(0);

  const loadData = useCallback(async () => {
    const fetchId = ++fetchIdRef.current;
    console.log('开始加载知识库数据...', { page, pageSize, keyword });
    setLoading(true);
    try {
      const result = await getKnowledgeBases({
        page,
        pageSize,
        keyword
      });
      if (fetchId !== fetchIdRef.current) return;
      console.log('知识库数据加载成功:', result);
      setData(result.items);
      setTotal(result.total);
    } catch (error: any) {
      if (fetchId !== fetchIdRef.current) return;
      if (axios.isCancel(error) || error?.code === 'ERR_CANCELED') return;
      console.error('加载知识库列表失败:', error);
      toast.error('加载知识库列表失败');
    } finally {
      if (fetchId === fetchIdRef.current) {
        setLoading(false);
      }
    }
  }, [page, pageSize, keyword]);

  useEffect(() => {
    const fetchId = ++fetchIdRef.current;
    const controller = new AbortController();

    async function doLoad() {
      setLoading(true);
      try {
        const result = await getKnowledgeBases({ page, pageSize, keyword }, controller.signal);
        if (fetchId !== fetchIdRef.current) return;
        setData(result.items);
        setTotal(result.total);
      } catch (error: any) {
        if (fetchId !== fetchIdRef.current) return;
        if (axios.isCancel(error) || error?.code === 'ERR_CANCELED' || controller.signal.aborted) return;
        console.error('加载知识库列表失败:', error);
        toast.error('加载知识库列表失败');
      } finally {
        if (fetchId === fetchIdRef.current && !controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    doLoad();
    return () => controller.abort();
  }, [page, pageSize, keyword]);

  // 搜索处理
  const handleSearch = () => {
    setPage(1);
    loadData();
  };

  // 重置搜索
  const handleReset = () => {
    setKeyword('');
    setPage(1);
  };

  // 新建知识库
  const handleCreate = () => {
    setEditingItem(null);
    setFormData({ name: '', description: '' });
    setModalOpen(true);
  };

  // 编辑知识库
  const handleEdit = (item: KnowledgeBase) => {
    setEditingItem(item);
    setFormData({
      name: item.name,
      description: item.description || ''
    });
    setModalOpen(true);
  };

  // 删除知识库
  const handleDelete = async (id: string) => {
    if (!window.confirm('确定要删除这个知识库吗？')) {
      return;
    }

    try {
      await deleteKnowledgeBase(id);
      toast.success('知识库删除成功');
      loadData();
    } catch (error) {
      console.error('删除知识库失败:', error);
      toast.error('删除知识库失败');
    }
  };

  // 表单提交
  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      toast.error('请输入知识库名称');
      return;
    }

    if (formData.name.length > 100) {
      toast.error('知识库名称不能超过100个字符');
      return;
    }

    if (formData.description && formData.description.length > 500) {
      toast.error('知识库描述不能超过500个字符');
      return;
    }

    setSubmitting(true);
    try {
      if (editingItem) {
        await updateKnowledgeBase(editingItem.id, {
          name: formData.name.trim(),
          description: formData.description.trim() || undefined,
        });
        toast.success('知识库更新成功');
      } else {
        await createKnowledgeBase({
          name: formData.name.trim(),
          description: formData.description.trim() || undefined,
        });
        toast.success('知识库创建成功');
      }

      setModalOpen(false);
      loadData();
    } catch (error) {
      console.error('保存知识库失败:', error);
      toast.error('保存知识库失败');
    } finally {
      setSubmitting(false);
    }
  };

  // 表格列配置
  const columns = [
    {
      key: 'id',
      title: 'ID',
      width: '80px',
      render: (value: string) => (
        <span className="text-xs text-gray-500 font-mono">
          {String(value).substring(0, 8)}
        </span>
      )
    },
    {
      key: 'name',
      title: '知识库名称',
      render: (value: string) => (
        <span className="font-medium text-gray-900">{value}</span>
      )
    },
    {
      key: 'description',
      title: '描述',
      render: (value: string) => (
        <span className="text-gray-600">
          {value ? truncateText(value, 50) : '-'}
        </span>
      )
    },
    {
      key: 'document_count',
      title: '文档数量',
      width: '100px',
      render: (value: number) => (
        <span className="text-gray-600">{value || 0}</span>
      )
    },
    {
      key: 'status',
      title: '状态',
      width: '80px',
      render: (value: string) => <StatusTag status={value || 'unknown'} />
    },
    {
      key: 'created_at',
      title: '创建时间',
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
      width: '180px',
      render: (_: any, record: KnowledgeBase) => (
        <div className="flex space-x-2">
          <button
            onClick={() => navigate(`/knowledge-bases/${record.id}`)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
            title="查看详情"
          >
            <Eye className="h-4 w-4 inline mr-1" />
            详情
          </button>
          <button
            onClick={() => handleEdit(record)}
            className="text-gray-600 hover:text-gray-800 text-sm font-medium"
            title="编辑"
          >
            <Edit className="h-4 w-4 inline mr-1" />
            编辑
          </button>
          <button
            onClick={() => handleDelete(record.id)}
            className="text-red-600 hover:text-red-800 text-sm font-medium"
            title="删除"
          >
            <Trash2 className="h-4 w-4 inline mr-1" />
            删除
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      {/* 页面标题和统计 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">知识库管理</h1>
          <p className="text-gray-600 mt-1">共 {total} 个知识库</p>
        </div>
        <button
          onClick={handleCreate}
          className="btn-primary flex items-center space-x-2"
        >
          <Plus className="h-4 w-4" />
          <span>新建知识库</span>
        </button>
      </div>

      {/* 搜索和筛选 */}
      <div className="card">
        <div className="flex items-center space-x-4">
          <SearchBar
            placeholder="搜索知识库名称或描述"
            value={keyword}
            onChange={setKeyword}
            onSearch={handleSearch}
            className="flex-1"
          />
          <button
            onClick={handleSearch}
            className="btn-primary"
          >
            <Search className="h-4 w-4 mr-2" />
            搜索
          </button>
          <button
            onClick={handleReset}
            className="btn-secondary"
          >
            重置
          </button>
          <button
            onClick={loadData}
            className="btn-secondary"
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            刷新
          </button>
        </div>
      </div>

      {/* 数据表格 */}
      <div className="card">
        <DataTable
          columns={columns}
          data={data}
          loading={loading}
          emptyText="暂无知识库数据"
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

      {/* 新建/编辑弹窗 */}
      <FormModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingItem ? '编辑知识库' : '新建知识库'}
        onSubmit={handleSubmit}
        loading={submitting}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              知识库名称 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              className="form-input"
              placeholder="请输入知识库名称"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              maxLength={100}
            />
            <p className="text-xs text-gray-500 mt-1">1-100个字符</p>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              描述
            </label>
            <textarea
              className="form-textarea"
              placeholder="请输入知识库描述（可选）"
              rows={3}
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              maxLength={500}
            />
            <p className="text-xs text-gray-500 mt-1">最多500个字符</p>
          </div>
        </div>
      </FormModal>
    </div>
  );
};

export default KnowledgeBaseList;