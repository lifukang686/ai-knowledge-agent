import React, { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { MODEL_TYPE_OPTIONS } from '@/types/modelProvider';

interface ModelConfigFormProps {
  onSubmit: (values: { modelName: string; modelType: string; defaultParams?: string }) => Promise<void>;
  onCancel?: () => void;
  submitting?: boolean;
  initialData?: { modelName?: string; modelType?: string; defaultParams?: string };
  isEdit?: boolean;
}

const ModelConfigForm: React.FC<ModelConfigFormProps> = ({
  onSubmit,
  onCancel,
  submitting = false,
  initialData,
  isEdit = false
}) => {
  const [formData, setFormData] = useState({
    modelName: initialData?.modelName || '',
    modelType: initialData?.modelType || 'CHAT',
    defaultParams: initialData?.defaultParams || ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.modelName.trim()) {
      newErrors.modelName = '请输入模型名称';
    }

    if (!formData.modelType) {
      newErrors.modelType = '请选择模型类型';
    }

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate() || submitting) {
      return;
    }

    const values = { ...formData };
    await onSubmit(values);
  };

  const handleChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 gap-6">
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

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            模型类型 <span className="text-red-500">*</span>
          </label>
          <select
            value={formData.modelType}
            onChange={(e) => handleChange('modelType', e.target.value)}
            className={`form-input ${errors.modelType ? 'border-red-500' : ''}`}
            disabled={submitting}
          >
            {MODEL_TYPE_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          {errors.modelType && (
            <p className="mt-1 text-sm text-red-600">{errors.modelType}</p>
          )}
        </div>

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
            isEdit ? '更新' : '添加'
          )}
        </button>
      </div>
    </form>
  );
};

export default ModelConfigForm;