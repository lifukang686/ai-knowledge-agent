import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, Eye, Edit, Trash2, Star } from 'lucide-react';
import { toast } from 'sonner';

import { ModelProvider, ModelProviderQuery } from '@/types/modelProvider';
import { modelProviderService } from '@/services/modelProvider';
import { DataTable } from '@/components/common/DataTable';
import { FormModal } from '@/components/common/FormModal';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { SearchBar } from '@/components/common/SearchBar';
import { formatDateTime } from '@/utils/format';
import ModelProviderForm from './components/ModelProviderForm';

const ModelProviderList: React.FC = () => {
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [providers, setProviders] = useState<ModelProvider[]>([]);
  const [searchName, setSearchName] = useState('');
  const [searchInput, setSearchInput] = useState('');
  
  const [modalVisible, setModalVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingProvider, setEditingProvider] = useState<ModelProvider | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ModelProvider | null>(null);
  const [deleting, setDeleting] = useState(false);
  
  // 使用ref来跟踪是否已加载过数据，防止重复调用
  const hasLoadedRef = useRef(false);
  const isLoadingRef = useRef(false);

  // 加载数据 - 使用useCallback避免不必要的重新渲染
  const loadProviders = useCallback(async (keyword: string = '') => {
    console.log('=== ModelProviderList.loadProviders 调用 ===');
    console.log('  - keyword:', keyword);
    console.log('  - isLoadingRef:', isLoadingRef.current);
    console.log('  - hasLoadedRef:', hasLoadedRef.current);
    
    // 防止重复加载
    if (isLoadingRef.current) {
      console.log('  - 正在加载中，跳过此次调用');
      return;
    }
    
    isLoadingRef.current = true;
    setLoading(true);
    
    try {
      console.log('  - 开始调用API: /api/models/providers');
      const query: ModelProviderQuery = {
        name: keyword || undefined
      };
      
      const result = await modelProviderService.getModelProviders(query);
      console.log('  - API返回数据:', result);
      
      // 根据搜索名称过滤（因为后端还不支持分页搜索）
      let filteredItems = result.items;
      if (keyword) {
        filteredItems = filteredItems.filter((p: ModelProvider) => 
          p.name.toLowerCase().includes(keyword.toLowerCase())
        );
      }
      setProviders(filteredItems);
      hasLoadedRef.current = true;
      console.log('  - 数据加载完成，provider数量:', filteredItems.length);
    } catch (error) {
      console.error('加载模型提供商失败:', error);
      toast.error('加载模型提供商失败');
    } finally {
      setLoading(false);
      isLoadingRef.current = false;
    }
  }, []);

  // 初始加载 - 只在组件挂载时执行一次
  useEffect(() => {
    console.log('=== ModelProviderList useEffect 执行 ===');
    console.log('  - hasLoadedRef:', hasLoadedRef.current);
    
    // 只在首次加载时调用
    if (!hasLoadedRef.current) {
      console.log('  - 执行初始加载');
      loadProviders();
    } else {
      console.log('  - 已加载过，跳过');
    }
    
    // 组件卸载时重置
    return () => {
      console.log('=== ModelProviderList 组件卸载 ===');
      hasLoadedRef.current = false;
    };
  }, [loadProviders]);

  // 搜索处理
  const handleSearch = (keyword: string) => {
    console.log('=== ModelProviderList.handleSearch ===');
    console.log('  - 搜索关键词:', keyword);
    setSearchName(keyword);
    loadProviders(keyword);
  };

  // 创建处理
  const handleCreate = () => {
    setEditingProvider(null);
    setModalVisible(true);
  };

  // 编辑处理
  const handleEdit = (provider: ModelProvider) => {
    console.log('编辑模型提供商:', provider);
    setEditingProvider(provider);
    setModalVisible(true);
  };

  // 删除处理
  const handleDeleteClick = (provider: ModelProvider) => {
    setDeleteTarget(provider);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;

    setDeleting(true);
    try {
      await modelProviderService.deleteModelProvider(deleteTarget.id);
      toast.success('删除成功');
      setDeleteTarget(null);
      loadProviders(searchName);
    } catch (error) {
      console.error('删除模型提供商失败:', error);
      toast.error('删除模型提供商失败');
    } finally {
      setDeleting(false);
    }
  };

  const handleSetDefault = async (provider: ModelProvider) => {
    try {
      if (provider.isDefault) {
        await modelProviderService.cancelDefaultProvider(provider.id);
        toast.success(`已取消 "${provider.name}" 的默认状态`);
      } else {
        await modelProviderService.setDefaultProvider(provider.id);
        toast.success(`已将 "${provider.name}" 设为默认提供商`);
      }
      loadProviders(searchName);
    } catch (error) {
      console.error('设置默认提供商失败:', error);
      toast.error('操作失败');
    }
  };

  const handleViewModels = (providerId: string) => {
    navigate(`/model-providers/${providerId}/models`);
  };

  // 表单提交处理
  const handleFormSubmit = async (values: any) => {
    console.log('提交表单数据:', values, '编辑模式:', !!editingProvider);
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
      setEditingProvider(null);
      loadProviders(searchName);
    } catch (error) {
      console.error('提交失败:', error);
      toast.error(editingProvider ? '更新失败' : '创建失败');
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
      key: 'apiBaseUrl',
      title: 'API地址',
      render: (value: string) => (
        <span className="text-sm text-gray-600 font-mono">{value || '-'}</span>
      )
    },
    {
      key: 'isDefault',
      title: '默认',
      width: '80px',
      render: (value: boolean) => (
        value ? (
          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
            <Star className="h-3 w-3 mr-1 fill-yellow-500 text-yellow-500" />
            默认
          </span>
        ) : (
          <span className="text-sm text-gray-400">-</span>
        )
      )
    },
    {
      key: 'createTime',
      title: '创建时间',
      width: '180px',
      render: (value: string) => (
        <span className="text-sm text-gray-600">
          {formatDateTime(value)}
        </span>
      )
    },
    {
      key: 'actions',
      title: '操作',
      width: '280px',
      render: (_: any, record: ModelProvider) => (
        <div className="flex items-center space-x-3">
          <button
            onClick={() => handleEdit(record)}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center"
            title="编辑"
          >
            <Edit className="h-4 w-4 mr-1" />
            编辑
          </button>
          <button
            onClick={() => handleViewModels(record.id)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium flex items-center"
          >
            <Eye className="h-4 w-4 mr-1" />
            模型
          </button>
          <button
            onClick={() => handleSetDefault(record)}
            className={`text-sm font-medium flex items-center ${
              record.isDefault
                ? 'text-yellow-600 hover:text-yellow-800'
                : 'text-gray-600 hover:text-gray-800'
            }`}
            title={record.isDefault ? '取消默认' : '设为默认'}
          >
            <Star className={`h-4 w-4 mr-1 ${record.isDefault ? 'fill-yellow-500' : ''}`} />
            {record.isDefault ? '取消默认' : '默认'}
          </button>
          <button
            onClick={() => handleDeleteClick(record)}
            className="text-red-600 hover:text-red-800 text-sm font-medium flex items-center"
            title="删除"
          >
            <Trash2 className="h-4 w-4 mr-1" />
            删除
          </button>
        </div>
      )
    }
  ];

  console.log('=== ModelProviderList 组件渲染 ===');

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
        <SearchBar
          placeholder="搜索提供商名称"
          value={searchInput}
          onChange={setSearchInput}
          onSearch={() => handleSearch(searchInput)}
        />
      </div>

      {/* 数据表格 */}
      <div className="card">
        <DataTable
          columns={columns}
          data={providers}
          loading={loading}
          emptyText="暂无模型提供商"
        />
      </div>

      {/* 创建/编辑弹窗 */}
      <FormModal
        isOpen={modalVisible}
        title={editingProvider ? "编辑模型提供商" : "创建模型提供商"}
        onCancel={() => {
          setModalVisible(false);
          setEditingProvider(null);
        }}
      >
        <ModelProviderForm
          initialValues={editingProvider ? {
            name: editingProvider.name,
            apiBaseUrl: editingProvider.apiBaseUrl || '',
            apiKey: editingProvider.apiKey || '',
            description: editingProvider.description || ''
          } : undefined}
          isEdit={!!editingProvider}
          onSubmit={handleFormSubmit}
          onCancel={() => {
            setModalVisible(false);
            setEditingProvider(null);
          }}
          submitting={submitting}
        />
      </FormModal>

      {/* 删除确认弹窗 */}
      <ConfirmModal
        isOpen={!!deleteTarget}
        title="删除模型提供商"
        message="该操作会删除其供应商的模型配置，确定要删除吗？"
        confirmText="确定删除"
        danger
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
};

export default ModelProviderList;
