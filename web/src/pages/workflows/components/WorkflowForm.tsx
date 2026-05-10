import React, { useState, useEffect } from 'react';

interface WorkflowFormProps {
  initialValues?: any;
  isEdit?: boolean;
  onSubmit?: (values: any) => void;
}

const WorkflowForm: React.FC<WorkflowFormProps> = ({
  initialValues,
  isEdit = false,
  onSubmit
}) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    definition: '',
    status: 'active'
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (initialValues) {
      setFormData({
        name: initialValues.name || '',
        description: initialValues.description || '',
        definition: JSON.stringify(initialValues.definition || getDefaultDefinition(), null, 2),
        status: initialValues.status || 'active'
      });
    } else {
      setFormData(prev => ({
        ...prev,
        definition: JSON.stringify(getDefaultDefinition(), null, 2)
      }));
    }
  }, [initialValues]);

  // 默认工作流定义
  const getDefaultDefinition = () => ({
    nodes: [
      {
        id: 'start',
        type: 'start',
        name: '开始',
        config: {},
        nextNodeIds: ['step1']
      },
      {
        id: 'step1',
        type: 'tool',
        name: '处理步骤1',
        config: {
          toolType: 'example_tool',
          parameters: {}
        },
        nextNodeIds: ['end']
      },
      {
        id: 'end',
        type: 'end',
        name: '结束',
        config: {},
        nextNodeIds: []
      }
    ],
    edges: [
      { id: 'e1', source: 'start', target: 'step1' },
      { id: 'e2', source: 'step1', target: 'end' }
    ],
    startNodeId: 'start',
    endNodeIds: ['end']
  });

  // 表单验证
  const validate = () => {
    const newErrors: Record<string, string> = {};
    
    if (!formData.name.trim()) {
      newErrors.name = '请输入工作流名称';
    }
    
    // 验证JSON格式
    if (formData.definition.trim()) {
      try {
        JSON.parse(formData.definition);
      } catch (error) {
        newErrors.definition = 'JSON格式不正确: ' + (error as Error).message;
      }
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
    
    const values = {
      ...formData,
      definition: JSON.parse(formData.definition)
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
            工作流名称 <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={formData.name}
            onChange={(e) => handleChange('name', e.target.value)}
            placeholder="请输入工作流名称"
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
            placeholder="请输入工作流描述"
            rows={3}
            className="form-input"
          />
        </div>

        {/* 定义 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            工作流定义 (JSON) <span className="text-red-500">*</span>
          </label>
          <textarea
            value={formData.definition}
            onChange={(e) => handleChange('definition', e.target.value)}
            placeholder="请输入工作流定义"
            rows={15}
            className={`form-input font-mono text-sm ${errors.definition ? 'border-red-500' : ''}`}
          />
          {errors.definition && (
            <p className="mt-1 text-sm text-red-600">{errors.definition}</p>
          )}
          <p className="mt-1 text-sm text-gray-500">
            定义工作流的节点、连接关系和配置参数，格式为 JSON
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

export default WorkflowForm;