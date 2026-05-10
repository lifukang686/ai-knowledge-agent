import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, RefreshCw, Clock, BarChart3, FileText, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import { toast } from 'sonner';
import { jobService } from '../../services/job';
import { Job, JobRun } from '../../types/job';
import { StatusTag, DataTable } from '../../components/common';

const JobDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [job, setJob] = useState<Job | null>(null);
  const [jobRuns, setJobRuns] = useState<JobRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(false);

  // Fetch job data
  const fetchJobData = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      const jobData = await jobService.getJobById(id);
      setJob(jobData);
      
      // Fetch job runs
      const runsResponse = await jobService.getJobRuns({
        job_id: id,
        page: 1,
        pageSize: 20
      });
      setJobRuns(runsResponse.data);
    } catch (error) {
      toast.error('获取任务详情失败');
      console.error('Failed to fetch job data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobData();
  }, [id]);

  // Auto refresh for running jobs
  useEffect(() => {
    let interval: NodeJS.Timeout;
    
    if (autoRefresh && jobRuns.some(run => run.status === 'running')) {
      interval = setInterval(() => {
        fetchJobData();
      }, 5000); // Refresh every 5 seconds
    }
    
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, [autoRefresh, jobRuns]);

  // Handle refresh
  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      await fetchJobData();
    } catch (error) {
      toast.error('刷新失败');
      console.error('Failed to refresh:', error);
    } finally {
      setRefreshing(false);
    }
  };

  // Handle cancel job run
  const handleCancelRun = async (runId: string) => {
    try {
      await jobService.cancelJobRun(runId);
      toast.success('任务运行已取消');
      fetchJobData();
    } catch (error) {
      toast.error('取消任务运行失败');
      console.error('Failed to cancel job run:', error);
    }
  };

  // Format duration
  const formatDuration = (seconds: number): string => {
    if (seconds < 60) {
      return `${seconds}秒`;
    } else if (seconds < 3600) {
      return `${Math.floor(seconds / 60)}分${seconds % 60}秒`;
    } else {
      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      return `${hours}小时${minutes}分`;
    }
  };

  // Format success rate
  const formatSuccessRate = (job: Job): string => {
    if (job.run_count === 0) return '0%';
    return `${Math.round((job.success_count / job.run_count) * 100)}%`;
  };

  // Get status icon
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'completed':
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case 'failed':
        return <XCircle className="h-5 w-5 text-red-500" />;
      case 'running':
        return <Clock className="h-5 w-5 text-blue-500 animate-spin" />;
      case 'cancelled':
        return <AlertCircle className="h-5 w-5 text-orange-500" />;
      default:
        return <Clock className="h-5 w-5 text-gray-500" />;
    }
  };

  // Table columns for job runs
  const runColumns = [
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <div className="flex items-center space-x-2">
          {getStatusIcon(status)}
          <StatusTag status={status} mapping={{
            pending: { text: '待处理', color: 'yellow' },
            running: { text: '运行中', color: 'blue' },
            completed: { text: '完成', color: 'green' },
            failed: { text: '失败', color: 'red' },
            cancelled: { text: '已取消', color: 'orange' }
          }} />
        </div>
      )
    },
    {
      title: '开始时间',
      dataIndex: 'start_time',
      key: 'start_time',
      width: 160,
      render: (text: string) => (
        <div className="text-sm">
          {text ? (
            <>
              <div>{new Date(text).toLocaleDateString('zh-CN')}</div>
              <div className="text-gray-500">{new Date(text).toLocaleTimeString('zh-CN')}</div>
            </>
          ) : (
            <span className="text-gray-400">-</span>
          )}
        </div>
      )
    },
    {
      title: '结束时间',
      dataIndex: 'end_time',
      key: 'end_time',
      width: 160,
      render: (text: string) => (
        <div className="text-sm">
          {text ? (
            <>
              <div>{new Date(text).toLocaleDateString('zh-CN')}</div>
              <div className="text-gray-500">{new Date(text).toLocaleTimeString('zh-CN')}</div>
            </>
          ) : (
            <span className="text-gray-400">运行中</span>
          )}
        </div>
      )
    },
    {
      title: '运行时长',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      render: (text: number, record: JobRun) => (
        <span className="text-sm">
          {record.status === 'running' ? (
            <span className="text-blue-600">运行中...</span>
          ) : text ? (
            formatDuration(text)
          ) : (
            '-'
          )}
        </span>
      )
    },
    {
      title: '结果',
      dataIndex: 'result',
      key: 'result',
      render: (result: any, record: JobRun) => (
        <div className="text-sm">
          {record.status === 'failed' && record.error_message ? (
            <div className="text-red-600" title={record.error_message}>
              {record.error_message.length > 50 
                ? `${record.error_message.substring(0, 50)}...` 
                : record.error_message}
            </div>
          ) : result ? (
            <div className="text-green-600">
              {Object.keys(result).map(key => (
                <div key={key}>{key}: {result[key]}</div>
              ))}
            </div>
          ) : (
            <span className="text-gray-400">-</span>
          )}
        </div>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: any, record: JobRun) => (
        <div className="flex space-x-1">
          {record.status === 'running' && (
            <button
              onClick={() => handleCancelRun(record.id)}
              className="p-1 text-red-600 hover:text-red-800"
              title="取消运行"
            >
              <XCircle size={16} />
            </button>
          )}
          {record.logs && (
            <button
              onClick={() => {
                // Show logs in a modal or expand view
                alert(`运行日志:\n${record.logs}`);
              }}
              className="p-1 text-blue-600 hover:text-blue-800"
              title="查看日志"
            >
              <FileText size={16} />
            </button>
          )}
        </div>
      )
    }
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500">任务不存在</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <button
            onClick={() => navigate('/jobs')}
            className="flex items-center space-x-2 text-gray-600 hover:text-gray-800"
          >
            <ArrowLeft size={20} />
            <span>返回任务列表</span>
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

      {/* Job Info Card */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{job.name}</h1>
            {job.description && (
              <p className="text-gray-600 mt-1">{job.description}</p>
            )}
          </div>
          <div className="flex items-center space-x-2">
            <StatusTag 
              status={job.status} 
              mapping={{
                enabled: { text: '启用', color: 'green' },
                disabled: { text: '禁用', color: 'gray' },
                running: { text: '运行中', color: 'blue' }
              }}
            />
            <StatusTag 
              status={job.job_type} 
              mapping={{
                agent: { text: 'Agent', color: 'blue' },
                workflow: { text: 'Workflow', color: 'green' },
                document_processing: { text: '文档处理', color: 'purple' }
              }}
            />
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center space-x-2 mb-2">
              <Clock className="h-5 w-5 text-gray-400" />
              <span className="text-sm text-gray-500">调度表达式</span>
            </div>
            <code className="text-sm font-mono bg-white px-2 py-1 rounded border">
              {job.schedule}
            </code>
          </div>
          
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center space-x-2 mb-2">
              <BarChart3 className="h-5 w-5 text-gray-400" />
              <span className="text-sm text-gray-500">总运行次数</span>
            </div>
            <div className="text-2xl font-bold text-gray-900">{job.run_count}</div>
          </div>
          
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center space-x-2 mb-2">
              <CheckCircle className="h-5 w-5 text-green-400" />
              <span className="text-sm text-gray-500">成功次数</span>
            </div>
            <div className="text-2xl font-bold text-green-600">{job.success_count}</div>
          </div>
          
          <div className="bg-gray-50 p-4 rounded-lg">
            <div className="flex items-center space-x-2 mb-2">
              <XCircle className="h-5 w-5 text-red-400" />
              <span className="text-sm text-gray-500">失败次数</span>
            </div>
            <div className="text-2xl font-bold text-red-600">{job.failure_count}</div>
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <p className="text-sm text-gray-500">成功率</p>
            <p className="text-lg font-semibold">{formatSuccessRate(job)}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">创建者</p>
            <p className="text-lg font-semibold">{job.created_by}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">创建时间</p>
            <p className="text-lg font-semibold">{new Date(job.created_at).toLocaleDateString('zh-CN')}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">最后运行</p>
            <p className="text-lg font-semibold">
              {job.last_run_at ? new Date(job.last_run_at).toLocaleDateString('zh-CN') : '从未运行'}
            </p>
          </div>
        </div>
      </div>

      {/* Job Runs */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">运行历史</h2>
          <p className="text-sm text-gray-600 mt-1">任务的执行历史和状态</p>
        </div>
        <div className="p-6">
          <DataTable
            columns={runColumns}
            dataSource={jobRuns}
            pagination={{
              current: 1,
              pageSize: 20,
              total: jobRuns.length,
              showSizeChanger: true,
              showQuickJumper: true
            }}
          />
        </div>
      </div>

      {/* Empty State */}
      {jobRuns.length === 0 && (
        <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
          <Clock className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无运行记录</h3>
          <p className="text-gray-500">该任务尚未执行或没有运行记录</p>
        </div>
      )}
    </div>
  );
};

export default JobDetail;