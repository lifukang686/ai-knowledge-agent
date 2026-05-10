import React, { useState } from 'react';
import { Job, JobCreateRequest, JobUpdateRequest } from '../../../types/job';

interface JobFormProps {
  initialData?: Job;
  onSubmit: (data: JobCreateRequest | JobUpdateRequest) => void;
  onCancel: () => void;
}

const JobForm: React.FC<JobFormProps> = ({ initialData, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState({
    name: initialData?.name || '',
    description: initialData?.description || '',
    job_type: initialData?.job_type || 'agent' as const,
    schedule: initialData?.schedule || '',
    config: initialData?.config ? JSON.stringify(initialData.config, null, 2) : '{}'
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = '任务名称不能为空';
    }

    if (!formData.job_type) {
      newErrors.job_type = '任务类型不能为空';
    }

    if (!formData.schedule.trim()) {
      newErrors.schedule = '调度表达式不能为空';
    } else {
      // Basic cron validation (5 or 6 fields)
      const cronPattern = /^(\*|[0-9,-\/]+)\s+(\*|[0-9,-\/]+)\s+(\*|[0-9,-\/]+)\s+(\*|[0-9,-\/]+)\s+(\*|[0-9,-\/]+)(\s+(\*|[0-9,-\/]+))?$/;
      if (!cronPattern.test(formData.schedule.trim())) {
        newErrors.schedule = 'Cron表达式格式不正确';
      }
    }

    // Validate JSON config
    try {
      JSON.parse(formData.config);
    } catch (e) {
      newErrors.config = '配置必须是有效的JSON格式';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    const submitData: JobCreateRequest | JobUpdateRequest = {
      name: formData.name.trim(),
      description: formData.description.trim() || undefined,
      job_type: formData.job_type,
      schedule: formData.schedule.trim(),
      config: JSON.parse(formData.config)
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

  const getCronExamples = () => {
    const examples = [
      { expression: '0 2 * * *', description: '每天凌晨2点' },
      { expression: '*/30 * * * *', description: '每30分钟' },
      { expression: '0 */6 * * *', description: '每6小时' },
      { expression: '0 9 * * 1-5', description: '工作日早上9点' },
      { expression: '0 0 1 * *', description: '每月1号午夜' }
    ];

    return examples;
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            任务名称 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.name}
            onChange={(e) => handleChange('name', e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
              errors.name ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="请输入任务名称"
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
            placeholder="请输入任务描述"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            任务类型 <span className="text-red-500">*</span>
          </label>
          <select
            value={formData.job_type}
            onChange={(e) => handleChange('job_type', e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
              errors.job_type ? 'border-red-500' : 'border-gray-300'
            }`}
          >
            <option value="agent">Agent</option>
            <option value="workflow">Workflow</option>
            <option value="document_processing">文档处理</option>
          </select>
          {errors.job_type && <p className="mt-1 text-sm text-red-600">{errors.job_type}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            调度表达式 (Cron) <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.schedule}
            onChange={(e) => handleChange('schedule', e.target.value)}
            className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-mono text-sm ${
              errors.schedule ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder="0 2 * * *"
          />
          {errors.schedule && <p className="mt-1 text-sm text-red-600">{errors.schedule}</p>}
          
          {/* Cron examples */}
          <div className="mt-2 text-xs text-gray-500">
            <p className="font-medium mb-1">常用表达式：</p>
            {getCronExamples().map((example, index) => (
              <div key={index} className="flex justify-between mb-1">
                <code className="bg-gray-100 px-1 rounded">{example.expression}</code>
                <span>{example.description}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            任务配置 (JSON) <span className="text-red-500">*</span>
          </label>
          <textarea
            value={formData.config}
            onChange={(e) => handleChange('config', e.target.value)}
            rows={8}
            className={`w-full px-3 py-2 border rounded-lg font-mono text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
              errors.config ? 'border-red-500' : 'border-gray-300'
            }`}
            placeholder='{\n  "param1": "value1",\n  "param2": "value2"\n}'
          />
          {errors.config && <p className="mt-1 text-sm text-red-600">{errors.config}</p>}
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

export default JobForm;