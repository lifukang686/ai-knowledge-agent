import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, Eye } from 'lucide-react';
import { toast } from 'sonner';

import { ModelProvider, ModelProviderQuery } from '@/types/modelProvider';
import { modelProviderService } from '@/services/modelProvider';
import { DataTable } from '@/components/common/DataTable';
import { FormModal } from '@/components/common/FormModal';
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
        filteredItems = filteredItems.filter(p => 
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
    setModalVisible(true);
  };

  const handleViewModels = (providerId: string) => {
    navigate(`/model-providers/${providerId}/models`);
  };

  // 表单提交处理
  const handleFormSubmit = async (values: any) => {
    console.log('提交表单数据:', values);
    setSubmitting(true);
    try {
      await modelProviderService.createModelProvider(values);
      toast.success('创建成功');
      setModalVisible(false);
      loadProviders(searchName);
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
      key: 'apiBaseUrl',
      title: 'API地址',
      render: (value: string) => (
        <span className="text-sm text-gray-600 font-mono">{value || '-'}</span>
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
      width: '120px',
      render: (_: any, record: ModelProvider) => (
        <div className="flex items-center space-x-2">
          <button
            onClick={() => handleViewModels(record.id)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
          >
            <Eye className="h-4 w-4 inline mr-1" />
            模型
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

      {/* 创建弹窗 */}
      <FormModal
        isOpen={modalVisible}
        title="创建模型提供商"
        onCancel={() => setModalVisible(false)}
      >
        <ModelProviderForm
          onSubmit={handleFormSubmit}
          onCancel={() => setModalVisible(false)}
          submitting={submitting}
        />
      </FormModal>
    </div>
  );
};

export default ModelProviderList;
