import React, { useState } from 'react';

interface RunWorkflowModalProps {
  visible: boolean;
  workflow: any;
  onCancel?: () => void;
  onSubmit?: (input: any) => void;
}

const RunWorkflowModal: React.FC<RunWorkflowModalProps> = ({
  visible,
  workflow,
  onCancel,
  onSubmit
}) => {
  const [input, setInput] = useState('{}');
  const [submitting, setSubmitting] = useState(false);

  if (!visible) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!input.trim()) {
      alert('请输入输入参数');
      return;
    }
    
    try {
      const parsedInput = JSON.parse(input);
      setSubmitting(true);
      await onSubmit?.(parsedInput);
      setInput('{}');
    } catch (error) {
      alert('JSON格式不正确: ' + (error as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl mx-4">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">
            运行工作流 - {workflow?.name}
          </h3>
        </div>
        
        <form onSubmit={handleSubmit} className="px-6 py-4">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                输入参数 (JSON) <span className="text-red-500">*</span>
              </label>
              <textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder='{"key": "value"}'
                rows={8}
                className="form-input font-mono text-sm"
                disabled={submitting}
              />
              <p className="mt-1 text-sm text-gray-500">
                请输入工作流运行所需的输入参数，格式为 JSON
              </p>
            </div>
            
            {workflow?.description && (
              <div className="bg-gray-50 rounded-lg p-4">
                <h4 className="text-sm font-medium text-gray-900 mb-2">工作流说明</h4>
                <p className="text-sm text-gray-600">{workflow.description}</p>
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
              disabled={submitting || !input.trim()}
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

export default RunWorkflowModal;