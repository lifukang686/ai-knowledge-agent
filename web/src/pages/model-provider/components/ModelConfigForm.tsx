import React, { useState, useEffect } from 'react';
import { Loader2 } from 'lucide-react';

interface ModelConfigFormProps {
  onSubmit: (values: { modelName: string; defaultParams?: string }) => Promise<void>;
  onCancel?: () => void;
  submitting?: boolean;
}

const ModelConfigForm: React.FC<ModelConfigFormProps> = ({
  onSubmit,
  onCancel,
  submitting = false
}) => {
  const [formData, setFormData] = useState({
    modelName: '',
    defaultParams: ''
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});

  // 表单验证
  const validate = () => {
    const newErrors: Record<string, string> = {};
    
    if (!formData.modelName.trim()) {
      newErrors.modelName = '请输入模型名称';
    }
    
    // 验证JSON格式（如果填写了）
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
        {/* 模型名称 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            模型名称 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.modelName}
            onChange={(e) => handleChange('modelName', e.target.value)}
            placeholder="如 gpt-3.5-turbo"
            className={`form-input ${errors.modelName ? 'border-red-500' : ''}`}
            disabled={submitting}
          />
          {errors.modelName && (
            <p className="mt-1 text-sm text-red-600">{errors.modelName}</p>
          )}
        </div>

        {/* 默认参数 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            默认参数 (JSON)
          </label>
          <textarea
            value={formData.defaultParams}
            onChange={(e) => handleChange('defaultParams', e.target.value)}
            placeholder='{
  "temperature": 0.7,
  "max_tokens": 2048
}'
            rows={6}
            className={`form-input font-mono text-sm ${errors.defaultParams ? 'border-red-500' : ''}`}
            disabled={submitting}
          />
          {errors.defaultParams && (
            <p className="mt-1 text-sm text-red-600">{errors.defaultParams}</p>
          )}
          <p className="mt-1 text-sm text-gray-500">
            请输入有效的 JSON 格式参数（可选）
          </p>
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
            '添加'
          )}
        </button>
      </div>
    </form>
  );
};

export default ModelConfigForm;
