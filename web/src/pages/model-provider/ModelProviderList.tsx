import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, Edit, Trash2, Eye } from 'lucide-react';
import { toast } from 'sonner';

import { ModelProvider, ModelProviderQuery } from '@/types/modelProvider';
import { modelProviderService } from '@/services/modelProvider';
import { DataTable } from '@/components/common/DataTable';
import { FormModal } from '@/components/common/FormModal';
import { Pagination } from '@/components/common/Pagination';
import { StatusTag } from '@/components/common/StatusTag';
import { formatDateTime } from '@/utils/format';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';
import ModelProviderForm from './components/ModelProviderForm';

const ModelProviderList: React.FC = () => {
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [providers, setProviders] = useState<ModelProvider[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [searchName, setSearchName] = useState('');
  const [searchStatus, setSearchStatus] = useState<string>('');
  
  const [modalVisible, setModalVisible] = useState(false);
  const [editingProvider, setEditingProvider] = useState<ModelProvider | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // 加载数据
  const loadProviders = async () => {
    setLoading(true);
    try {
      const query: ModelProviderQuery = {
        name: searchName || undefined,
        status: searchStatus || undefined,
        page,
        pageSize
      };
      
      const result = await modelProviderService.getModelProviders(query);
      setProviders(result.items);
      setTotal(result.total);
    } catch (error) {
      console.error('加载模型提供商失败:', error);
      toast.error('加载模型提供商失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProviders();
  }, [page, pageSize, searchName, searchStatus]);

  // 搜索处理
  const handleSearch = () => {
    setPage(1);
    loadProviders();
  };

  // 重置搜索
  const handleReset = () => {
    setSearchName('');
    setSearchStatus('');
    setPage(1);
  };

  // 创建/编辑处理
  const handleCreate = () => {
    setEditingProvider(null);
    setModalVisible(true);
  };

  const handleEdit = (provider: ModelProvider) => {
    setEditingProvider(provider);
    setModalVisible(true);
  };

  const handleViewModels = (providerId: string) => {
    navigate(`/model-providers/${providerId}/models`);
  };

  // 删除处理
  const handleDelete = async (provider: ModelProvider) => {
    if (!window.confirm(`确定要删除模型提供商 "${provider.name}" 吗？`)) {
      return;
    }

    try {
      await modelProviderService.deleteModelProvider(provider.id);
      toast.success('删除成功');
      loadProviders();
    } catch (error) {
      console.error('删除失败:', error);
      toast.error('删除失败');
    }
  };

  // 表单提交处理
  const handleFormSubmit = async (values: any) => {
    setSubmitting(true);
    try {
      if (editingProvider) {
        await modelProviderService.updateModelProvider(editingProvider.id, values);
        toast.success('更新成功');
      } else {
        await modelProviderService.createModelProvider(values);
        toast.success('创建成功');
      }
      setModalVisible(false);
      loadProviders();
    } catch (error) {
      console.error('提交失败:', error);
      toast.error('提交失败');
      throw error;
    } finally {
      setSubmitting(false);
    }
  };

  // 表格列配置
  const columns = [
    {
      key: 'name',
      title: '名称',
      render: (value: string, record: ModelProvider) => (
        <div>
          <div className="font-medium text-gray-900">{value}</div>
          {record.description && (
            <div className="text-sm text-gray-500 mt-1">{record.description}</div>
          )}
        </div>
      )
    },
    {
      key: 'apiUrl',
      title: 'API 地址',
      render: (value: string) => (
        <span className="text-sm text-gray-600 font-mono">{value}</span>
      )
    },
    {
      key: 'status',
      title: '状态',
      width: '100px',
      render: (value: string) => <StatusTag status={value} />
    },
    {
      key: 'created_at',
      title: '创建时间',
      width: '150px',
      render: (value: string) => (
        <span className="text-sm text-gray-600">
          {formatDateTime(value)}
        </span>
      )
    },
    {
      key: 'actions',
      title: '操作',
      width: '200px',
      render: (_: any, record: ModelProvider) => (
        <div className="flex items-center space-x-2">
          <button
            onClick={() => handleViewModels(record.id)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
          >
            <Eye className="h-4 w-4 inline mr-1" />
            模型
          </button>
          <button
            onClick={() => handleEdit(record)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
          >
            <Edit className="h-4 w-4 inline mr-1" />
            编辑
          </button>
          <button
            onClick={() => handleDelete(record)}
            className="text-red-600 hover:text-red-800 text-sm font-medium"
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
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">模型提供商管理</h1>
          <p className="text-gray-600 mt-1">管理 AI 模型提供商配置</p>
        </div>
        <button
          onClick={handleCreate}
          className="btn-primary"
        >
          <Plus className="h-4 w-4 mr-2" />
          创建提供商
        </button>
      </div>

      {/* 搜索区域 */}
      <div className="card">
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex-1 min-w-64">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              名称搜索
            </label>
            <input
              type="text"
              value={searchName}
              onChange={(e) => setSearchName(e.target.value)}
              placeholder="请输入提供商名称"
              className="form-input"
            />
          </div>
          
          <div className="w-32">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              状态
            </label>
            <select
              value={searchStatus}
              onChange={(e) => setSearchStatus(e.target.value)}
              className="form-input"
            >
              <option value="">全部</option>
              <option value="active">启用</option>
              <option value="inactive">禁用</option>
            </select>
          </div>
          
          <div className="flex space-x-3">
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
          </div>
        </div>
      </div>

      {/* 数据表格 */}
      <div className="card">
        <DataTable
          columns={columns}
          data={providers}
          loading={loading}
          emptyText="暂无模型提供商"
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

      {/* 创建/编辑弹窗 */}
      <FormModal
        visible={modalVisible}
        title={editingProvider ? '编辑模型提供商' : '创建模型提供商'}
        onCancel={() => setModalVisible(false)}
        onSubmit={handleFormSubmit}
        submitting={submitting}
      >
        <ModelProviderForm
          initialValues={editingProvider}
          isEdit={!!editingProvider}
        />
      </FormModal>
    </div>
  );
};

export default ModelProviderList;