import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Search, RefreshCw, Eye, Edit, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

import { AIModel } from '@/types/modelProvider';
import { getProviderModels } from '@/services/modelProvider';
import { DataTable } from '@/components/common/DataTable';
import { SearchBar } from '@/components/common/SearchBar';
import { FormModal } from '@/components/common/FormModal';
import { StatusTag } from '@/components/common/StatusTag';
import { Pagination } from '@/components/common/Pagination';
import { formatDateTime, truncateText } from '@/utils/format';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';

const ModelManagement: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AIModel[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<AIModel | null>(null);

  // 表单状态
  const [formData, setFormData] = useState({
    name: '',
    model_type: 'chat',
    description: '',
    max_tokens: 2048
  });
  const [submitting, setSubmitting] = useState(false);

  // 加载数据
  const loadData = async () => {
    if (!id) return;
    
    setLoading(true);
    try {
      const result = await getProviderModels(id, {
        page,
        pageSize,
        keyword
      });
      setData(result.items);
      setTotal(result.total);
    } catch (error) {
      console.error('加载模型列表失败:', error);
      toast.error('加载模型列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [id, page, pageSize, keyword]);

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

  // 新建模型
  const handleCreate = () => {
    setEditingItem(null);
    setFormData({ 
      name: '', 
      model_type: 'chat',
      description: '',
      max_tokens: 2048
    });
    setModalOpen(true);
  };

  // 编辑模型
  const handleEdit = (item: AIModel) => {
    setEditingItem(item);
    setFormData({
      name: item.name,
      model_type: item.model_type,
      description: item.description || '',
      max_tokens: item.max_tokens || 2048
    });
    setModalOpen(true);
  };

  // 删除模型
  const handleDelete = async (modelId: string) => {
    if (!window.confirm('确定要删除这个模型吗？')) {
      return;
    }

    try {
      // 待确认：后端联调时替换为真实API
      toast.success('模型删除成功');
      loadData();
    } catch (error) {
      console.error('删除模型失败:', error);
      toast.error('删除模型失败');
    }
  };

  // 表单提交
  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      toast.error('请输入模型名称');
      return;
    }

    if (!formData.model_type.trim()) {
      toast.error('请选择模型类型');
      return;
    }

    if (formData.max_tokens <= 0) {
      toast.error('最大Token数必须大于0');
      return;
    }

    setSubmitting(true);
    try {
      if (editingItem) {
        // 更新逻辑 - 待确认：后端联调时替换为真实API
        toast.success('模型更新成功');
      } else {
        // 创建逻辑 - 待确认：后端联调时替换为真实API
        toast.success('模型创建成功');
      }
      
      setModalOpen(false);
      loadData();
    } catch (error) {
      console.error('保存模型失败:', error);
      toast.error('保存模型失败');
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
      title: '模型名称',
      render: (value: string) => (
        <span className="font-medium text-gray-900">{value}</span>
      )
    },
    {
      key: 'model_type',
      title: '模型类型',
      width: '100px',
      render: (value: string) => (
        <span className="px-2 py-1 text-xs font-medium rounded-full bg-blue-100 text-blue-800">
          {value}
        </span>
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
      key: 'max_tokens',
      title: '最大Token',
      width: '100px',
      render: (value: number) => (
        <span className="text-gray-600">{value?.toLocaleString()}</span>
      )
    },
    {
      key: 'is_active',
      title: '状态',
      width: '80px',
      render: (value: boolean) => (
        <StatusTag status={value ? 'completed' : 'failed'} />
      )
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
      width: '120px',
      render: (_: any, record: AIModel) => (
        <div className="flex space-x-2">
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
      {/* 页面标题和返回 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button
            onClick={() => navigate('/model-providers')}
            className="text-gray-600 hover:text-gray-800 mr-4"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">模型管理</h1>
            <p className="text-gray-600 mt-1">提供商ID: {id}</p>
          </div>
        </div>
        
        <button
          onClick={handleCreate}
          className="btn-primary flex items-center space-x-2"
        >
          <Plus className="h-4 w-4" />
          <span>新建模型</span>
        </button>
      </div>

      {/* 搜索和筛选 */}
      <div className="card">
        <div className="flex items-center space-x-4">
          <SearchBar
            placeholder="搜索模型名称或描述"
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
          emptyText="暂无模型数据"
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
        title={editingItem ? '编辑模型' : '新建模型'}
        onSubmit={handleSubmit}
        loading={submitting}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              模型名称 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              className="form-input"
              placeholder="请输入模型名称"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              模型类型 <span className="text-red-500">*</span>
            </label>
            <select
              className="form-input"
              value={formData.model_type}
              onChange={(e) => setFormData({ ...formData, model_type: e.target.value })}
            >
              <option value="chat">聊天模型</option>
              <option value="embedding">嵌入模型</option>
              <option value="completion">补全模型</option>
              <option value="image">图像模型</option>
            </select>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              最大Token数 <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              className="form-input"
              placeholder="2048"
              min="1"
              value={formData.max_tokens}
              onChange={(e) => setFormData({ ...formData, max_tokens: parseInt(e.target.value) || 2048 })}
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              描述
            </label>
            <textarea
              className="form-textarea"
              placeholder="请输入模型描述（可选）"
              rows={3}
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>
        </div>
      </FormModal>
    </div>
  );
};

export default ModelManagement;