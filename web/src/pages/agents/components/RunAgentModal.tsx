import React, { useState } from 'react';

interface RunAgentModalProps {
  visible: boolean;
  agent: any;
  onCancel?: () => void;
  onSubmit?: (task: string) => void;
}

const RunAgentModal: React.FC<RunAgentModalProps> = ({
  visible,
  agent,
  onCancel,
  onSubmit
}) => {
  const [task, setTask] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!visible) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!task.trim()) {
      alert('请输入任务描述');
      return;
    }
    
    setSubmitting(true);
    try {
      await onSubmit?.(task);
      setTask('');
    } catch (error) {
      // 错误已在父组件处理
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl mx-4">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">
            运行 Agent - {agent?.name}
          </h3>
        </div>
        
        <form onSubmit={handleSubmit} className="px-6 py-4">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                任务描述 <span className="text-red-500">*</span>
              </label>
              <textarea
                value={task}
                onChange={(e) => setTask(e.target.value)}
                placeholder="请详细描述您希望 Agent 执行的任务..."
                rows={6}
                className="form-input"
                disabled={submitting}
              />
              <p className="mt-1 text-sm text-gray-500">
                请详细描述您的任务需求，Agent 将根据配置的工具和模型来执行任务
              </p>
            </div>
            
            {agent?.description && (
              <div className="bg-gray-50 rounded-lg p-4">
                <h4 className="text-sm font-medium text-gray-900 mb-2">Agent 说明</h4>
                <p className="text-sm text-gray-600">{agent.description}</p>
              </div>
            )}
          </div>
          
          <div className="flex justify-end space-x-3 pt-4 border-t">
            <button
              type="button"
              onClick={onCancel}
              disabled={submitting}
              className="btn-secondary"
            >
              取消
            </button>
            <button
              type="submit"
              disabled={submitting || !task.trim()}
              className="btn-primary"
            >
              {submitting ? '提交中...' : '运行'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RunAgentModal;