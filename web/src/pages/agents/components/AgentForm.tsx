import React, { useState, useEffect } from 'react';
import { modelProviderService } from '@/services/modelProvider';
import { toolService } from '@/services/tool';

interface AgentFormProps {
  initialValues?: any;
  isEdit?: boolean;
  onSubmit?: (values: any) => void;
}

const AgentForm: React.FC<AgentFormProps> = ({
  initialValues,
  isEdit = false,
  onSubmit
}) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    toolIds: [] as string[],
    modelProviderId: '',
    modelId: '',
    systemPrompt: '',
    status: 'active'
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [providers, setProviders] = useState<any[]>([]);
  const [models, setModels] = useState<any[]>([]);
  const [tools, setTools] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (initialValues) {
      setFormData({
        name: initialValues.name || '',
        description: initialValues.description || '',
        toolIds: initialValues.toolIds || [],
        modelProviderId: initialValues.modelProviderId || '',
        modelId: initialValues.modelId || '',
        systemPrompt: initialValues.systemPrompt || '',
        status: initialValues.status || 'active'
      });
    }
    loadInitialData();
  }, [initialValues]);

  // 加载初始数据
  const loadInitialData = async () => {
    setLoading(true);
    try {
      // 加载模型提供商
      const providersResult = await modelProviderService.getModelProviders({ page: 1, pageSize: 100 });
      setProviders(providersResult.items);
      
      // 加载工具
      const toolsResult = await toolService.getTools({ page: 1, pageSize: 100 });
      setTools(toolsResult.items);
      
      // 如果有初始的提供商ID，加载对应的模型
      if (initialValues?.modelProviderId) {
        const modelsResult = await modelProviderService.getModelsByProvider(initialValues.modelProviderId);
        setModels(modelsResult);
      }
    } catch (error) {
      console.error('加载初始数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 提供商变更处理
  const handleProviderChange = async (providerId: string) => {
    setFormData(prev => ({ ...prev, modelProviderId: providerId, modelId: '' }));
    
    if (providerId) {
      try {
        const modelsResult = await modelProviderService.getModelsByProvider(providerId);
        setModels(modelsResult);
      } catch (error) {
        console.error('加载模型失败:', error);
        setModels([]);
      }
    } else {
      setModels([]);
    }
  };

  // 表单验证
  const validate = () => {
    const newErrors: Record<string, string> = {};
    
    if (!formData.name.trim()) {
      newErrors.name = '请输入 Agent 名称';
    }
    
    if (!formData.modelProviderId) {
      newErrors.modelProviderId = '请选择模型提供商';
    }
    
    if (!formData.modelId) {
      newErrors.modelId = '请选择模型';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // 提交处理
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validate()) {
      return;
    }
    
    onSubmit?.(formData);
  };

  // 输入变更处理
  const handleChange = (field: string, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // 清除对应字段的错误
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-32">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 gap-6">
        {/* 名称 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Agent 名称 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.name}
            onChange={(e) => handleChange('name', e.target.value)}
            placeholder="请输入 Agent 名称"
            className={`form-input ${errors.name ? 'border-red-500' : ''}`}
          />
          {errors.name && (
            <p className="mt-1 text-sm text-red-600">{errors.name}</p>
          )}
        </div>

        {/* 描述 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            描述
          </label>
          <textarea
            value={formData.description}
            onChange={(e) => handleChange('description', e.target.value)}
            placeholder="请输入 Agent 描述"
            rows={3}
            className="form-input"
          />
        </div>

        {/* 模型提供商 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            模型提供商 <span className="text-red-500">*</span>
          </label>
          <select
            value={formData.modelProviderId}
            onChange={(e) => handleProviderChange(e.target.value)}
            className={`form-input ${errors.modelProviderId ? 'border-red-500' : ''}`}
          >
            <option value="">请选择模型提供商</option>
            {providers.map(provider => (
              <option key={provider.id} value={provider.id}>
                {provider.name}
              </option>
            ))}
          </select>
          {errors.modelProviderId && (
            <p className="mt-1 text-sm text-red-600">{errors.modelProviderId}</p>
          )}
        </div>

        {/* 模型 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            模型 <span className="text-red-500">*</span>
          </label>
          <select
            value={formData.modelId}
            onChange={(e) => handleChange('modelId', e.target.value)}
            disabled={!formData.modelProviderId || models.length === 0}
            className={`form-input ${errors.modelId ? 'border-red-500' : ''}`}
          >
            <option value="">请选择模型</option>
            {models.map(model => (
              <option key={model.id} value={model.id}>
                {model.name}
              </option>
            ))}
          </select>
          {errors.modelId && (
            <p className="mt-1 text-sm text-red-600">{errors.modelId}</p>
          )}
          {!formData.modelProviderId && (
            <p className="mt-1 text-sm text-gray-500">请先选择模型提供商</p>
          )}
        </div>

        {/* 工具 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            工具
          </label>
          <div className="space-y-2 max-h-32 overflow-y-auto border border-gray-300 rounded-lg p-3">
            {tools.map(tool => (
              <label key={tool.id} className="flex items-center">
                <input
                  type="checkbox"
                  checked={formData.toolIds.includes(tool.id)}
                  onChange={(e) => {
                    const toolIds = e.target.checked
                      ? [...formData.toolIds, tool.id]
                      : formData.toolIds.filter(id => id !== tool.id);
                    handleChange('toolIds', toolIds);
                  }}
                  className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <span className="ml-2 text-sm text-gray-700">
                  {tool.name}
                  {tool.description && (
                    <span className="text-gray-500 ml-1">- {tool.description}</span>
                  )}
                </span>
              </label>
            ))}
          </div>
          {tools.length === 0 && (
            <p className="text-sm text-gray-500">暂无可用工具</p>
          )}
        </div>

        {/* 系统提示词 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            系统提示词
          </label>
          <textarea
            value={formData.systemPrompt}
            onChange={(e) => handleChange('systemPrompt', e.target.value)}
            placeholder="请输入系统提示词，用于定义 Agent 的行为和角色"
            rows={4}
            className="form-input"
          />
          <p className="mt-1 text-sm text-gray-500">
            定义 Agent 的行为和角色，留空将使用默认提示词
          </p>
        </div>

        {/* 状态 */}
        {isEdit && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              状态
            </label>
            <select
              value={formData.status}
              onChange={(e) => handleChange('status', e.target.value)}
              className="form-input"
            >
              <option value="active">启用</option>
              <option value="inactive">禁用</option>
            </select>
          </div>
        )}
      </div>

      {/* 表单底部按钮 */}
      <div className="flex justify-end space-x-3 pt-4 border-t">
        <button
          type="button"
          onClick={() => onSubmit?.(null)}
          className="btn-secondary"
        >
          取消
        </button>
        <button
          type="submit"
          className="btn-primary"
        >
          {isEdit ? '更新' : '创建'}
        </button>
      </div>
    </form>
  );
};

export default AgentForm;