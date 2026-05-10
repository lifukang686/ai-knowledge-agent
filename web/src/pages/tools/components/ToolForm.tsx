import React, { useState } from 'react';
import { Tool, ToolCreateRequest, ToolUpdateRequest } from '../../../types/tool';

interface ToolFormProps {
  initialData?: Tool;
  onSubmit: (data: ToolCreateRequest | ToolUpdateRequest) => void;
  onCancel: () => void;
}

const ToolForm: React.FC<ToolFormProps> = ({ initialData, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState({
    name: initialData?.name || '',
    description: initialData?.description || '',
    executor_type: initialData?.executor_type || 'http' as const,
    executor_config: initialData?.executor_config ? JSON.stringify(initialData.executor_config, null, 2) : '{}',
    schema: initialData?.schema ? JSON.stringify(initialData.schema, null, 2) : '{}',
    tags: initialData?.tags?.join(', ') || ''
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = '工具名称不能为空';
    }

    if (!formData.executor_type) {
      newErrors.executor_type = '执行器类型不能为空';
    }

    // Validate JSON fields
    try {
      JSON.parse(formData.executor_config);
    } catch (e) {
      newErrors.executor_config = '执行器配置必须是有效的JSON格式';
    }

    if (formData.schema.trim()) {
      try {
        JSON.parse(formData.schema);
      } catch (e) {
        newErrors.schema = 'Schema必须是有效的JSON格式';
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    const submitData: ToolCreateRequest | ToolUpdateRequest = {
      name: formData.name.trim(),
      description: formData.description.trim() || undefined,
      executor_type: formData.executor_type,
      executor_config: JSON.parse(formData.executor_config),
      schema: formData.schema.trim() ? JSON.parse(formData.schema) : undefined,
      tags: formData.tags.trim() ? formData.tags.split(',').map(tag => tag.trim()).filter(Boolean) : undefined
    };

    onSubmit(submitData);
  };

  const handleChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            工具名称 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.name}
            onChange={(e) => handleChange('name', e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
              errors.name ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="请输入工具名称"
          />
          {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name}</p>}
        </div>

        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            描述
          </label>
          <textarea
            value={formData.description}
            onChange={(e) => handleChange('description', e.target.value)}
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            placeholder="请输入工具描述"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            执行器类型 <span className="text-red-500">*</span>
          </label>
          <select
            value={formData.executor_type}
            onChange={(e) => handleChange('executor_type', e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
              errors.executor_type ? 'border-red-500' : 'border-gray-300'
            }`}
          >
            <option value="http">HTTP</option>
            <option value="function">Function</option>
            <option value="script">Script</option>
          </select>
          {errors.executor_type && <p className="mt-1 text-sm text-red-600">{errors.executor_type}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            标签
          </label>
          <input
            type="text"
            value={formData.tags}
            onChange={(e) => handleChange('tags', e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            placeholder="用逗号分隔多个标签"
          />
        </div>

        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            执行器配置 (JSON) <span className="text-red-500">*</span>
          </label>
          <textarea
            value={formData.executor_config}
            onChange={(e) => handleChange('executor_config', e.target.value)}
            rows={6}
            className={`w-full px-3 py-2 border rounded-lg font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
              errors.executor_config ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder='{\n  "url": "https://api.example.com",\n  "method": "GET"\n}'
          />
          {errors.executor_config && <p className="mt-1 text-sm text-red-600">{errors.executor_config}</p>}
        </div>

        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Schema (JSON)
          </label>
          <textarea
            value={formData.schema}
            onChange={(e) => handleChange('schema', e.target.value)}
            rows={6}
            className={`w-full px-3 py-2 border rounded-lg font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
              errors.schema ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder='{\n  "type": "object",\n  "properties": {\n    "param1": {\n      "type": "string"\n    }\n  }\n}'
          />
          {errors.schema && <p className="mt-1 text-sm text-red-600">{errors.schema}</p>}
        </div>
      </div>

      <div className="flex justify-end space-x-4">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500"
        >
          取消
        </button>
        <button
          type="submit"
          className="px-4 py-2 text-white bg-blue-600 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {initialData ? '更新' : '创建'}
        </button>
      </div>
    </form>
  );
};

export default ToolForm;