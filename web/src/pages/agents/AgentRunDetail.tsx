import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Clock, CheckCircle, XCircle, AlertCircle, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';

import { AgentRun } from '@/types/agent';
import { agentService } from '@/services/agent';
import { formatDateTime, formatDuration } from '@/utils/format';
import { StatusTag } from '@/components/common';

const AgentRunDetail: React.FC = () => {
  const { runId } = useParams<{ runId: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [run, setRun] = useState<AgentRun | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  // 加载运行详情
  const loadRunDetail = async () => {
    if (!runId) return;
    
    setLoading(true);
    try {
      const data = await agentService.getAgentRun(runId);
      setRun(data);
    } catch (error) {
      console.error('加载运行详情失败:', error);
      toast.error('加载运行详情失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRunDetail();
  }, [runId]);

  // 刷新状态
  const handleRefresh = async () => {
    setRefreshing(true);
    try {
      await loadRunDetail();
    } finally {
      setRefreshing(false);
    }
  };

  // 状态图标
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'completed':
        return <CheckCircle className="h-8 w-8 text-green-500" />;
      case 'running':
        return <Clock className="h-8 w-8 text-blue-500 animate-spin" />;
      case 'failed':
        return <XCircle className="h-8 w-8 text-red-500" />;
      case 'pending':
        return <AlertCircle className="h-8 w-8 text-yellow-500" />;
      default:
        return <AlertCircle className="h-8 w-8 text-gray-500" />;
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!run) {
    return (
      <div className="text-center py-12 text-gray-500">
        运行记录不存在
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button
            onClick={() => navigate(-1)}
            className="text-gray-600 hover:text-gray-800 mr-4"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Agent 运行详情</h1>
            <p className="text-gray-600 mt-1">查看 Agent 运行的详细信息和结果</p>
          </div>
        </div>
        
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="btn-secondary"
        >
          <RefreshCw className={`h-4 w-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
          刷新
        </button>
      </div>

      {/* 状态卡片 */}
      <div className="card">
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            {getStatusIcon(run.status)}
            <div className="ml-4">
              <h2 className="text-xl font-semibold text-gray-900">运行状态</h2>
              <div className="flex items-center mt-1">
                <StatusTag status={run.status} />
                {run.duration && (
                  <span className="ml-3 text-sm text-gray-600">
                    耗时: {formatDuration(run.duration)}
                  </span>
                )}
              </div>
            </div>
          </div>
          
          <div className="text-right">
            <div className="text-sm text-gray-600">运行ID</div>
            <div className="text-sm font-mono text-gray-900">{run.id}</div>
          </div>
        </div>
      </div>

      {/* 任务信息 */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">任务信息</h3>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              任务描述
            </label>
            <div className="bg-gray-50 rounded-lg p-4">
              <p className="text-gray-900">{run.task}</p>
            </div>
          </div>
          
          <div className="grid grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                开始时间
              </label>
              <p className="text-gray-900">
                {run.startedAt ? formatDateTime(run.startedAt) : '-'}
              </p>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                完成时间
              </label>
              <p className="text-gray-900">
                {run.completedAt ? formatDateTime(run.completedAt) : '-'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 运行结果 */}
      {run.status === 'completed' && run.result && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">运行结果</h3>
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <pre className="text-sm text-gray-900 whitespace-pre-wrap font-sans">
              {run.result}
            </pre>
          </div>
        </div>
      )}

      {/* 错误信息 */}
      {run.status === 'failed' && run.error && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">错误信息</h3>
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <pre className="text-sm text-red-800 whitespace-pre-wrap font-sans">
              {run.error}
            </pre>
          </div>
        </div>
      )}

      {/* 待确认提示 */}
      {(run.status === 'running' || run.status === 'pending') && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <Clock className="h-5 w-5 text-blue-400 animate-spin" />
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-blue-800">运行中</h3>
              <div className="mt-1 text-sm text-blue-700">
                <p>Agent 正在处理任务，请稍后刷新查看结果。</p>
                <p className="mt-1">待确认：实际项目中应支持实时状态更新或轮询。</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AgentRunDetail;