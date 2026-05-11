import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Database, Edit, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

import { ModelConfig, ModelProvider } from '@/types/modelProvider';
import { modelProviderService } from '@/services/modelProvider';
import { DataTable } from '@/components/common/DataTable';
import { FormModal } from '@/components/common/FormModal';
import { formatDateTime } from '@/utils/format';
import ModelConfigForm from './components/ModelConfigForm';

const ModelList: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [models, setModels] = useState<ModelConfig[]>([]);
  const [providerName, setProviderName] = useState('');
  
  const [modalVisible, setModalVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [editingModel, setEditingModel] = useState<ModelConfig | null>(null);
  
  // 使用ref来跟踪是否已加载过数据，防止重复调用
  const hasLoadedRef = useRef(false);
  const isLoadingRef = useRef(false);

  // 加载数据 - 使用useCallback避免不必要的重新渲染
  const loadData = useCallback(async (forceReload: boolean = false) => {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === ModelList.loadData 调用 ===`);
    console.log('  - forceReload:', forceReload);
    console.log('  - 路由参数id:', id, '类型:', typeof id);
    console.log('  - isLoadingRef:', isLoadingRef.current);
    console.log('  - hasLoadedRef:', hasLoadedRef.current);
    
    if (!id) return;
    
    // 防止重复加载 - 除非是强制刷新
    if (!forceReload && isLoadingRef.current) {
      console.log('  - 正在加载中，跳过此次调用');
      return;
    }
    if (!forceReload && hasLoadedRef.current) {
      console.log('  - 已加载过，跳过此次调用');
      return;
    }
    
    isLoadingRef.current = true;
    setLoading(true);
    
    try {
      console.log('  - 开始调用 API: getModelsByProvider');
      
      // 直接从路由参数获取providerId，不需要额外查询
      // 直接获取模型列表
      const modelsData = await modelProviderService.getModelsByProvider(id);
      
      console.log('  - 获取到的模型列表:', modelsData);
      console.log('  - 模型数量:', modelsData.length);
      
      setModels(modelsData);
      
      // 提供商名称暂时先不获取，避免重复请求
      setProviderName('模型配置');
      
      hasLoadedRef.current = true;
      console.log('  - 数据加载完成');
    } catch (error) {
      console.error('加载模型列表失败:', error);
      toast.error('加载模型列表失败');
    } finally {
      setLoading(false);
      isLoadingRef.current = false;
    }
  }, [id]);

  useEffect(() => {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === ModelList useEffect 执行 ===`);
    console.log('  - hasLoadedRef:', hasLoadedRef.current);
    
    // 当id变化时，重置加载状态并重新加载
    if (id) {
      hasLoadedRef.current = false;
      loadData();
    }
    
    // 组件卸载时重置
    return () => {
      console.log('=== ModelList 组件卸载 ===');
      hasLoadedRef.current = false;
    };
  }, [loadData, id]);

  // 添加模型处理
  const handleCreate = () => {
    console.log('打开添加模型弹窗，当前id:', id);
    setEditingModel(null);
    setModalVisible(true);
  };

  // 编辑模型处理
  const handleEdit = (model: ModelConfig) => {
    console.log('打开编辑模型弹窗，模型数据:', model);
    setEditingModel(model);
    setModalVisible(true);
  };

  // 删除模型处理
  const handleDelete = async (modelId: string) => {
    if (!window.confirm('确定要删除这个模型吗？此操作不可恢复。')) {
      return;
    }

    try {
      await modelProviderService.deleteModelConfig(modelId);
      toast.success('模型删除成功');
      // 删除成功后强制刷新数据
      loadData(true);
    } catch (error) {
      console.error('删除模型失败:', error);
      toast.error('删除模型失败');
    }
  };

  // 表单提交处理
  const handleFormSubmit = async (values: { modelName: string; defaultParams?: string }) => {
    if (!id) {
      toast.error('提供商信息缺失，请刷新页面');
      return;
    }
    
    console.log('=== 提交模型配置 ===');
    console.log('是否编辑模式:', !!editingModel);
    console.log('使用路由参数providerId:', id, '类型:', typeof id);
    console.log('表单数据:', values);
    
    setSubmitting(true);
    try {
      if (editingModel) {
        // 编辑模式
        await modelProviderService.updateModelConfig(editingModel.id, values);
        toast.success('模型更新成功');
      } else {
        // 新建模式
        const requestData = {
          providerId: id,  // 直接传递字符串，不转换为数字
          modelName: values.modelName,
          defaultParams: values.defaultParams
        };
        console.log('发送到后端的请求数据:', requestData);
        console.log('请求数据类型检查:', typeof requestData.providerId);
        
        await modelProviderService.createModelConfig(requestData);
        toast.success('模型添加成功');
      }
      
      setModalVisible(false);
      setEditingModel(null);
      
      // 成功后强制刷新数据
      loadData(true);
    } catch (error) {
      console.error('操作失败:', error);
      toast.error(editingModel ? '更新模型失败' : '添加模型失败');
      throw error;
    } finally {
      setSubmitting(false);
    }
  };

  // 表格列配置
  const columns = [
    {
      key: 'modelName',
      title: '模型名称',
      render: (value: string, record: ModelConfig) => (
        <div className="flex items-center">
          <Database className="h-4 w-4 text-gray-400 mr-2" />
          <span className="font-medium text-gray-900">{value}</span>
        </div>
      )
    },
    {
      key: 'defaultParams',
      title: '默认参数',
      render: (value: string) => {
        if (!value) return '-';
        try {
          const parsed = JSON.parse(value);
          return (
            <span className="text-xs text-gray-600 font-mono">
              {JSON.stringify(parsed, null, 2)}
            </span>
          );
        } catch {
          return <span className="text-xs text-gray-600">{value}</span>;
        }
      }
    },
    {
      key: 'createTime',
      title: '创建时间',
      width: '180px',
      render: (value: string) => (
        <span className="text-gray-600 text-sm">
          {formatDateTime(value)}
        </span>
      )
    },
    {
      key: 'actions',
      title: '操作',
      width: '150px',
      render: (_: any, record: ModelConfig) => (
        <div className="flex space-x-3">
          <button
            onClick={() => handleEdit(record)}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center"
            title="编辑"
          >
            <Edit className="h-4 w-4 mr-1" />
            编辑
          </button>
          <button
            onClick={() => handleDelete(record.id)}
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

  console.log('=== ModelList 组件渲染 ===');

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
            onClick={handleCreate}
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
          emptyText="暂无模型，点击上方按钮添加"
        />
      </div>

      {/* 添加/编辑模型弹窗 */}
      <FormModal
        isOpen={modalVisible}
        title={editingModel ? "编辑模型" : "添加模型"}
        onCancel={() => {
          setModalVisible(false);
          setEditingModel(null);
        }}
      >
        <ModelConfigForm
          onSubmit={handleFormSubmit}
          onCancel={() => {
            setModalVisible(false);
            setEditingModel(null);
          }}
          submitting={submitting}
          initialData={editingModel ? {
            modelName: editingModel.modelName,
            defaultParams: editingModel.defaultParams
          } : undefined}
          isEdit={!!editingModel}
        />
      </FormModal>
    </div>
  );
};

export default ModelList;
