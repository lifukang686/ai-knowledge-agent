import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, RefreshCw, Code, Play } from 'lucide-react';
import { toast } from 'sonner';

import { Workflow, WorkflowNode, WorkflowEdge } from '@/types/workflow';
import { workflowService } from '@/services/workflow';
import { formatDateTime } from '@/utils/format';
import { StatusTag } from '@/components/common/StatusTag';
import RunWorkflowModal from './components/RunWorkflowModal';

const WorkflowDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [workflow, setWorkflow] = useState<Workflow | null>(null);
  
  const [runModalVisible, setRunModalVisible] = useState(false);

  // 加载工作流详情
  const loadWorkflow = async () => {
    if (!id) return;
    
    setLoading(true);
    try {
      const data = await workflowService.getWorkflow(id);
      setWorkflow(data);
    } catch (error) {
      console.error('加载工作流详情失败:', error);
      toast.error('加载工作流详情失败');
      navigate('/workflows');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorkflow();
  }, [id]);

  // 运行工作流
  const handleRun = () => {
    if (workflow) {
      setRunModalVisible(true);
    }
  };

  // 运行工作流处理
  const handleRunSubmit = async (input: any) => {
    if (!workflow) return;
    
    try {
      const result = await workflowService.runWorkflow(workflow.id, { input });
      toast.success(`工作流运行已提交，运行ID: ${result.runId}`);
      setRunModalVisible(false);
      navigate('/workflow-runs');
    } catch (error) {
      console.error('运行失败:', error);
      toast.error('运行失败');
      throw error;
    }
  };

  // 获取节点类型显示文本
  const getNodeTypeText = (type: string) => {
    const typeMap: Record<string, string> = {
      start: '开始',
      end: '结束',
      agent: 'Agent',
      tool: '工具',
      condition: '条件',
      parallel: '并行'
    };
    return typeMap[type] || type;
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!workflow) {
    return (
      <div className="text-center py-12 text-gray-500">
        工作流不存在
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button
            onClick={() => navigate('/workflows')}
            className="text-gray-600 hover:text-gray-800 mr-4"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{workflow.name}</h1>
            <p className="text-gray-600 mt-1">
              {workflow.description || '暂无描述'}
            </p>
          </div>
        </div>
        
        <div className="flex items-center space-x-3">
          <button
            onClick={loadWorkflow}
            className="btn-secondary"
          >
            <RefreshCw className="h-4 w-4 mr-2" />
            刷新
          </button>
          
          <button
            onClick={handleRun}
            className="btn-primary"
          >
            <Play className="h-4 w-4 mr-2" />
            运行工作流
          </button>
        </div>
      </div>

      {/* 基本信息卡片 */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div>
            <p className="text-sm text-gray-600">版本</p>
            <p className="text-lg font-semibold text-gray-900">v{workflow.version}</p>
          </div>
          
          <div>
            <p className="text-sm text-gray-600">状态</p>
            <StatusTag status={workflow.status} />
          </div>
          
          <div>
            <p className="text-sm text-gray-600">创建时间</p>
            <p className="text-sm text-gray-900">
              {formatDateTime(workflow.created_at)}
            </p>
          </div>
          
          <div>
            <p className="text-sm text-gray-600">更新时间</p>
            <p className="text-sm text-gray-900">
              {formatDateTime(workflow.updated_at)}
            </p>
          </div>
        </div>
      </div>

      {/* 工作流定义预览 */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-gray-900">工作流定义</h2>
          <div className="flex items-center text-sm text-gray-600">
            <Code className="h-4 w-4 mr-1" />
            JSON 格式
          </div>
        </div>
        
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
          <pre className="text-sm text-gray-900 overflow-x-auto">
            {JSON.stringify(workflow.definition, null, 2)}
          </pre>
        </div>
      </div>

      {/* 节点列表 */}
      <div className="card">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">节点列表</h2>
        
        <div className="space-y-4">
          {workflow.definition.nodes.map((node: WorkflowNode) => (
            <div key={node.id} className="border border-gray-200 rounded-lg p-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center mr-3">
                    <span className="text-sm font-medium text-primary-700">
                      {node.id.charAt(0).toUpperCase()}
                    </span>
                  </div>
                  <div>
                    <h3 className="font-medium text-gray-900">{node.name}</h3>
                    <p className="text-sm text-gray-600">
                      {getNodeTypeText(node.type)} - ID: {node.id}
                    </p>
                  </div>
                </div>
                
                <div className="text-right">
                  <p className="text-sm text-gray-600">下一个节点</p>
                  <p className="text-sm font-medium text-gray-900">
                    {node.nextNodeIds.length > 0 ? node.nextNodeIds.join(', ') : '无'}
                  </p>
                </div>
              </div>
              
              {Object.keys(node.config).length > 0 && (
                <div className="mt-3 pt-3 border-t border-gray-200">
                  <p className="text-sm text-gray-600 mb-2">配置参数：</p>
                  <pre className="text-xs text-gray-700 bg-gray-50 p-2 rounded">
                    {JSON.stringify(node.config, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* 连接关系 */}
      <div className="card">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">连接关系</h2>
        
        <div className="space-y-2">
          {workflow.definition.edges.map((edge: WorkflowEdge) => (
            <div key={edge.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <div className="flex items-center">
                <div className="w-2 h-2 bg-primary-500 rounded-full mr-3"></div>
                <span className="text-sm font-medium text-gray-900">
                  {workflow.definition.nodes.find(n => n.id === edge.source)?.name || edge.source}
                </span>
                <span className="mx-2 text-gray-400">→</span>
                <span className="text-sm text-gray-700">
                  {workflow.definition.nodes.find(n => n.id === edge.target)?.name || edge.target}
                </span>
              </div>
              
              {edge.condition && (
                <div className="text-sm text-gray-600">
                  条件: {edge.condition}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* 待确认提示 */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-yellow-800">待确认功能</h3>
            <div className="mt-2 text-sm text-yellow-700">
              <p>工作流可视化编辑器待后端接口确认后实现，当前仅支持 JSON 格式定义。</p>
              <p className="mt-1">实际项目中应提供图形化拖拽界面来创建工作流。</p>
            </div>
          </div>
        </div>
      </div>

      {/* 运行工作流弹窗 */}
      <RunWorkflowModal
        visible={runModalVisible}
        workflow={workflow}
        onCancel={() => setRunModalVisible(false)}
        onSubmit={handleRunSubmit}
      />
    </div>
  );
};

export default WorkflowDetail;