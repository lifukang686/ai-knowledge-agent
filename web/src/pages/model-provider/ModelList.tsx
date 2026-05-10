import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Edit, Database } from 'lucide-react';
import { toast } from 'sonner';

import { Model } from '@/types/modelProvider';
import { modelProviderService } from '@/services/modelProvider';
import { DataTable } from '@/components/common/DataTable';
import { StatusTag } from '@/components/common/StatusTag';
import { formatDateTime } from '@/utils/format';

const ModelList: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [models, setModels] = useState<Model[]>([]);
  const [providerName, setProviderName] = useState('');

  // 加载模型列表
  const loadModels = async () => {
    if (!id) return;
    
    setLoading(true);
    try {
      // 获取提供商信息
      const provider = await modelProviderService.getModelProvider(id);
      setProviderName(provider.name);
      
      // 获取模型列表
      const data = await modelProviderService.getModelsByProvider(id);
      setModels(data);
    } catch (error) {
      console.error('加载模型列表失败:', error);
      toast.error('加载模型列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadModels();
  }, [id]);

  // 表格列配置
  const columns = [
    {
      key: 'name',
      title: '模型名称',
      render: (value: string, record: Model) => (
        <div className="flex items-center">
          <Database className="h-4 w-4 text-gray-400 mr-2" />
          <span className="font-medium text-gray-900">{value}</span>
        </div>
      )
    },
    {
      key: 'modelType',
      title: '模型类型',
      width: '120px',
      render: (value: string) => {
        const typeMap = {
          chat: '对话',
          embedding: '嵌入',
          completion: '补全'
        };
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
            {typeMap[value as keyof typeof typeMap] || value}
          </span>
        );
      }
    },
    {
      key: 'description',
      title: '描述',
      render: (value: string) => (
        <span className="text-gray-600 text-sm">
          {value || '-'}
        </span>
      )
    },
    {
      key: 'maxTokens',
      title: '最大Token',
      width: '120px',
      render: (value: number) => (
        <span className="text-gray-600 text-sm">
          {value ? value.toLocaleString() : '-'}
        </span>
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
        <span className="text-gray-600 text-sm">
          {formatDateTime(value)}
        </span>
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
            <h1 className="text-2xl font-bold text-gray-900">{providerName} - 模型管理</h1>
            <p className="text-gray-600 mt-1">管理提供商下的 AI 模型配置</p>
          </div>
        </div>
        
        <div className="flex items-center space-x-3">
          <button
            className="btn-primary"
          >
            <Plus className="h-4 w-4 mr-2" />
            添加模型
          </button>
        </div>
      </div>

      {/* 模型列表 */}
      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">模型列表</h2>
          <div className="text-sm text-gray-600">
            共 {models.length} 个模型
          </div>
        </div>
        
        <DataTable
          columns={columns}
          data={models}
          loading={loading}
          emptyText="暂无模型"
        />
      </div>

      {/* 待确认提示 */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-yellow-800">待确认功能</h3>
            <div className="mt-2 text-sm text-yellow-700">
              <p>模型管理功能（添加、编辑、删除模型）待后端接口确认后实现</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ModelList;