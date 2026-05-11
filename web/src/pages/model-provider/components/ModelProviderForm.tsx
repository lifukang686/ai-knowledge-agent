import React, { useState, useEffect } from 'react';
import { Eye, EyeOff, Loader2 } from 'lucide-react';

interface ModelProviderFormProps {
  initialValues?: any;
  isEdit?: boolean;
  onSubmit: (values: any) => Promise<void>;
  onCancel?: () => void;
  submitting?: boolean;
}

const ModelProviderForm: React.FC<ModelProviderFormProps> = ({
  initialValues,
  isEdit = false,
  onSubmit,
  onCancel,
  submitting = false
}) => {
  const [formData, setFormData] = useState({
    name: '',
    apiBaseUrl: '',
    apiKey: '',
    description: ''
  });
  
  const [showApiKey, setShowApiKey] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (initialValues) {
      setFormData({
        name: initialValues.name || '',
        apiBaseUrl: initialValues.apiBaseUrl || '',
        apiKey: initialValues.apiKey || '',
        description: initialValues.description || ''
      });
    }
  }, [initialValues]);

  // 表单验证
  const validate = () => {
    const newErrors: Record<string, string> = {};
    
    if (!formData.name.trim()) {
      newErrors.name = '请输入提供商名称';
    }
    
    // 验证URL格式（如果填写了）
    if (formData.apiBaseUrl.trim() && !isValidUrl(formData.apiBaseUrl)) {
      newErrors.apiBaseUrl = '请输入有效的URL地址';
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
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validate() || submitting) {
      return;
    }
    
    const values = { ...formData };
    await onSubmit(values);
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
            disabled={submitting}
          />
          {errors.name && (
            <p className="mt-1 text-sm text-red-600">{errors.name}</p>
          )}
        </div>

        {/* API地址 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            API地址
          </label>
          <input
            type="url"
            value={formData.apiBaseUrl}
            onChange={(e) => handleChange('apiBaseUrl', e.target.value)}
            placeholder="https://api.example.com/v1"
            className={`form-input ${errors.apiBaseUrl ? 'border-red-500' : ''}`}
            disabled={submitting}
          />
          {errors.apiBaseUrl && (
            <p className="mt-1 text-sm text-red-600">{errors.apiBaseUrl}</p>
          )}
        </div>

        {/* API密钥 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            API密钥
          </label>
          <div className="relative">
            <input
              type={showApiKey ? 'text' : 'password'}
              value={formData.apiKey}
              onChange={(e) => handleChange('apiKey', e.target.value)}
              placeholder="请输入API密钥"
              className="form-input pr-10"
              disabled={submitting}
            />
            <button
              type="button"
              onClick={() => setShowApiKey(!showApiKey)}
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
              disabled={submitting}
            >
              {showApiKey ? (
                <EyeOff className="h-4 w-4" />
              ) : (
                <Eye className="h-4 w-4" />
              )}
            </button>
          </div>
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
            disabled={submitting}
          />
        </div>
      </div>

      {/* 表单底部按钮 */}
      <div className="flex justify-end space-x-3 pt-4 border-t">
        <button
          type="button"
          onClick={onCancel}
          className="btn-secondary"
          disabled={submitting}
        >
          取消
        </button>
        <button
          type="submit"
          className="btn-primary"
          disabled={submitting}
        >
          {submitting ? (
            <>
              <Loader2 className="animate-spin h-4 w-4 mr-2 inline" />
              保存中...
            </>
          ) : (
            isEdit ? '更新' : '创建'
          )}
        </button>
      </div>
    </form>
  );
};

export default ModelProviderForm;
