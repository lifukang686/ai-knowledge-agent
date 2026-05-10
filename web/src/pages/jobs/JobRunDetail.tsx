import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Clock, CheckCircle, XCircle, PlayCircle, AlertCircle, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';
import { jobService } from '../../services/job';
import { JobRun } from '../../types/job';

const JobRunDetail: React.FC = () => {
  const { jobId, runId } = useParams<{ jobId: string; runId: string }>();
  const navigate = useNavigate();
  const [jobRun, setJobRun] = useState<JobRun | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(false);

  // Fetch job run data
  const fetchJobRun = async () => {
    if (!runId) return;
    
    try {
      setLoading(true);
      const run = await jobService.getJobRunById(runId);
      setJobRun(run);
      
      // Auto refresh for running jobs
      if (run.status === 'running') {
        setAutoRefresh(true);
      } else {
        setAutoRefresh(false);
      }
    } catch (error) {
      toast.error('获取任务执行详情失败');
      console.error('Failed to fetch job run:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobRun();
  }, [runId]);

  // Auto refresh for running jobs
  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    if (autoRefresh && jobRun?.status === 'running') {
      interval = setInterval(() => {
        fetchJobRun();
      }, 5000); // Refresh every 5 seconds
    }
    
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, [autoRefresh, jobRun?.status]);

  // Handle refresh
  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      await fetchJobRun();
    } catch (error) {
      toast.error('刷新失败');
      console.error('Failed to refresh:', error);
    } finally {
      setRefreshing(false);
    }
  };

  // Format duration
  const formatDuration = (seconds?: number): string => {
    if (!seconds) return '-';
    if (seconds < 60) return `${seconds}秒`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟${seconds % 60}秒`;
    return `${Math.floor(seconds / 3600)}小时${Math.floor((seconds % 3600) / 60)}分钟`;
  };

  // Get status display
  const getStatusDisplay = (status: string) => {
    const statusMap = {
      pending: { color: 'yellow', icon: Clock, text: '待执行' },
      running: { color: 'blue', icon: PlayCircle, text: '运行中' },
      completed: { color: 'green', icon: CheckCircle, text: '已完成' },
      failed: { color: 'red', icon: XCircle, text: '失败' },
      cancelled: { color: 'gray', icon: AlertCircle, text: '已取消' }
    };
    return statusMap[status as keyof typeof statusMap] || { color: 'gray', icon: AlertCircle, text: status };
  };

  // Format JSON result
  const formatResult = (result?: Record<string, any>): string => {
    if (!result) return '';
    try {
      return JSON.stringify(result, null, 2);
    } catch (e) {
      return String(result);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!jobRun) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500">任务执行记录不存在</p>
      </div>
    );
  }

  const statusDisplay = getStatusDisplay(jobRun.status);
  const StatusIcon = statusDisplay.icon;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <button
            onClick={() => navigate(`/jobs/${jobId}/runs`)}
            className="flex items-center space-x-2 text-gray-600 hover:text-gray-800"
          >
            <ArrowLeft size={20} />
            <span>返回执行历史</span>
          </button>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={() => setAutoRefresh(!autoRefresh)}
            className={`px-3 py-2 rounded-lg text-sm font-medium ${
              autoRefresh 
                ? 'bg-green-100 text-green-800 border border-green-300' 
                : 'bg-gray-100 text-gray-700 border border-gray-300'
            }`}
          >
            {autoRefresh ? '自动刷新开启' : '自动刷新关闭'}
          </button>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} />
            <span>刷新</span>
          </button>
        </div>
      </div>

      {/* Job Run Info Card */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <div className="flex items-start justify-between mb-6">
          <div className="flex items-center space-x-4">
            <div className={`p-3 rounded-full ${
              statusDisplay.color === 'green' ? 'bg-green-100 text-green-600' :
              statusDisplay.color === 'red' ? 'bg-red-100 text-red-600' :
              statusDisplay.color === 'blue' ? 'bg-blue-100 text-blue-600' :
              statusDisplay.color === 'yellow' ? 'bg-yellow-100 text-yellow-600' :
              'bg-gray-100 text-gray-600'
            }`}>
              <StatusIcon size={24} />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">任务执行详情</h1>
              <p className="text-gray-600">{jobRun.job_name}</p>
            </div>
          </div>
          <span className={`px-3 py-1 rounded-full text-sm font-medium ${
            statusDisplay.color === 'green' ? 'bg-green-100 text-green-800' :
            statusDisplay.color === 'red' ? 'bg-red-100 text-red-800' :
            statusDisplay.color === 'blue' ? 'bg-blue-100 text-blue-800' :
            statusDisplay.color === 'yellow' ? 'bg-yellow-100 text-yellow-800' :
            'bg-gray-100 text-gray-800'
          }`}>
            {statusDisplay.text}
          </span>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div>
            <p className="text-sm text-gray-500 mb-1">开始时间</p>
            <p className="font-medium">
              {jobRun.start_time ? new Date(jobRun.start_time).toLocaleString('zh-CN') : '-'}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-500 mb-1">结束时间</p>
            <p className="font-medium">
              {jobRun.end_time ? new Date(jobRun.end_time).toLocaleString('zh-CN') : '-'}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-500 mb-1">持续时间</p>
            <p className="font-medium">{formatDuration(jobRun.duration)}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500 mb-1">执行ID</p>
            <p className="font-mono text-sm text-gray-600">{jobRun.id}</p>
          </div>
        </div>

        {jobRun.error_message && (
          <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-center space-x-2 mb-2">
              <XCircle className="h-5 w-5 text-red-500" />
              <span className="font-medium text-red-800">错误信息</span>
            </div>
            <p className="text-red-700">{jobRun.error_message}</p>
          </div>
        )}

        {jobRun.result && (
          <div className="mt-6">
            <h3 className="text-lg font-semibold text-gray-900 mb-3">执行结果</h3>
            <div className="bg-gray-50 rounded-lg p-4">
              <pre className="text-sm text-gray-800 whitespace-pre-wrap font-mono">
                {formatResult(jobRun.result)}
              </pre>
            </div>
          </div>
        )}
      </div>

      {/* Logs */}
      {jobRun.logs && (
        <div className="bg-white rounded-lg border border-gray-200">
          <div className="p-6 border-b border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900">执行日志</h3>
            <p className="text-sm text-gray-600 mt-1">任务执行过程中的详细日志信息</p>
          </div>
          <div className="p-6">
            <div className="bg-gray-900 text-green-400 rounded-lg p-4 font-mono text-sm max-h-96 overflow-y-auto">
              <pre className="whitespace-pre-wrap">{jobRun.logs}</pre>
            </div>
          </div>
        </div>
      )}

      {/* Configuration */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900">配置信息</h3>
          <p className="text-sm text-gray-600 mt-1">任务执行时的配置参数</p>
        </div>
        <div className="p-6">
          <div className="bg-gray-50 rounded-lg p-4">
            <pre className="text-sm text-gray-800 whitespace-pre-wrap font-mono">
              {JSON.stringify(jobRun.result || {}, null, 2)}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};

export default JobRunDetail;