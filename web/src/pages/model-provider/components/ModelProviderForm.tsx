import React, { useState, useEffect } from 'react';
import { Eye, EyeOff } from 'lucide-react';

interface ModelProviderFormProps {
  initialValues?: any;
  isEdit?: boolean;
  onSubmit?: (values: any) => void;
}

const ModelProviderForm: React.FC<ModelProviderFormProps> = ({
  initialValues,
  isEdit = false,
  onSubmit
}) => {
  const [formData, setFormData] = useState({
    name: '',
    apiUrl: '',
    apiKey: '',
    description: '',
    defaultParams: '{}',
    status: 'active'
  });
  
  const [showApiKey, setShowApiKey] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (initialValues) {
      setFormData({
        name: initialValues.name || '',
        apiUrl: initialValues.apiUrl || '',
        apiKey: initialValues.apiKey || '',
        description: initialValues.description || '',
        defaultParams: JSON.stringify(initialValues.defaultParams || {}, null, 2),
        status: initialValues.status || 'active'
      });
    }
  }, [initialValues]);

  // 表单验证
  const validate = () => {
    const newErrors: Record<string, string> = {};
    
    if (!formData.name.trim()) {
      newErrors.name = '请输入提供商名称';
    }
    
    if (!formData.apiUrl.trim()) {
      newErrors.apiUrl = '请输入API地址';
    } else if (!isValidUrl(formData.apiUrl)) {
      newErrors.apiUrl = '请输入有效的URL地址';
    }
    
    if (!formData.apiKey.trim()) {
      newErrors.apiKey = '请输入API密钥';
    }
    
    // 验证JSON格式
    if (formData.defaultParams.trim()) {
      try {
        JSON.parse(formData.defaultParams);
      } catch (error) {
        newErrors.defaultParams = 'JSON格式不正确';
      }
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // URL验证
  const isValidUrl = (url: string) => {
    try {
      new URL(url);
      return true;
    } catch {
      return false;
    }
  };

  // 提交处理
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validate()) {
      return;
    }
    
    const values = {
      ...formData,
      defaultParams: formData.defaultParams.trim() ? JSON.parse(formData.defaultParams) : {}
    };
    
    onSubmit?.(values);
  };

  // 输入变更处理
  const handleChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // 清除对应字段的错误
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 gap-6">
        {/* 名称 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            提供商名称 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.name}
            onChange={(e) => handleChange('name', e.target.value)}
            placeholder="请输入提供商名称"
            className={`form-input ${errors.name ? 'border-red-500' : ''}`}
          />
          {errors.name && (
            <p className="mt-1 text-sm text-red-600">{errors.name}</p>
          )}
        </div>

        {/* API地址 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            API地址 <span className="text-red-500">*</span>
          </label>
          <input
            type="url"
            value={formData.apiUrl}
            onChange={(e) => handleChange('apiUrl', e.target.value)}
            placeholder="https://api.example.com/v1"
            className={`form-input ${errors.apiUrl ? 'border-red-500' : ''}`}
          />
          {errors.apiUrl && (
            <p className="mt-1 text-sm text-red-600">{errors.apiUrl}</p>
          )}
        </div>

        {/* API密钥 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            API密钥 <span className="text-red-500">*</span>
          </label>
          <div className="relative">
            <input
              type={showApiKey ? 'text' : 'password'}
              value={formData.apiKey}
              onChange={(e) => handleChange('apiKey', e.target.value)}
              placeholder="请输入API密钥"
              className={`form-input pr-10 ${errors.apiKey ? 'border-red-500' : ''}`}
            />
            <button
              type="button"
              onClick={() => setShowApiKey(!showApiKey)}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
            >
              {showApiKey ? (
                <EyeOff className="h-4 w-4" />
              ) : (
                <Eye className="h-4 w-4" />
              )}
            </button>
          </div>
          {errors.apiKey && (
            <p className="mt-1 text-sm text-red-600">{errors.apiKey}</p>
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
            placeholder="请输入提供商描述"
            rows={3}
            className="form-input"
          />
        </div>

        {/* 默认参数 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            默认参数 (JSON)
          </label>
          <textarea
            value={formData.defaultParams}
            onChange={(e) => handleChange('defaultParams', e.target.value)}
            placeholder='{\n  "temperature": 0.7,\n  "maxTokens": 2048\n}'
            rows={6}
            className={`form-input font-mono text-sm ${errors.defaultParams ? 'border-red-500' : ''}`}
          />
          {errors.defaultParams && (
            <p className="mt-1 text-sm text-red-600">{errors.defaultParams}</p>
          )}
          <p className="mt-1 text-sm text-gray-500">
            请输入有效的 JSON 格式参数，例如：{`{"temperature": 0.7, "maxTokens": 2048}`}
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

export default ModelProviderForm;