import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, RefreshCw, Clock, CheckCircle, XCircle, PlayCircle, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import { jobService } from '../../services/job';
import { Job, JobRun } from '../../types/job';
import DataTable from '../../components/common/DataTable';
import StatusTag from '../../components/common/StatusTag';

const JobRuns: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [job, setJob] = useState<Job | null>(null);
  const [runs, setRuns] = useState<JobRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [searchParams, setSearchParams] = useState({
    page: 1,
    pageSize: 10,
    status: ''
  });
  const [total, setTotal] = useState(0);

  // Fetch job and runs data
  const fetchData = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      const [jobData, runsResponse] = await Promise.all([
        jobService.getJobById(id),
        jobService.getJobRuns({
          job_id: id,
          page: searchParams.page,
          pageSize: searchParams.pageSize,
          status: searchParams.status
        })
      ]);
      
      setJob(jobData);
      setRuns(runsResponse.data);
      setTotal(runsResponse.total);
    } catch (error) {
      toast.error('获取任务执行历史失败');
      console.error('Failed to fetch job runs:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [id, searchParams]);

  // Handle refresh
  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      await fetchData();
    } catch (error) {
      toast.error('刷新失败');
      console.error('Failed to refresh:', error);
    } finally {
      setRefreshing(false);
    }
  };

  // Handle search
  const handleSearch = (values: Record<string, string>) => {
    setSearchParams(prev => ({
      ...prev,
      page: 1,
      status: values.status || ''
    }));
  };

  // Handle reset
  const handleReset = () => {
    setSearchParams({
      page: 1,
      pageSize: 10,
      status: ''
    });
  };

  // Format duration
  const formatDuration = (seconds?: number): string => {
    if (!seconds) return '-';
    if (seconds < 60) return `${seconds}秒`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`;
    return `${Math.floor(seconds / 3600)}小时${Math.floor((seconds % 3600) / 60)}分钟`;
  };

  // Get status color and icon
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

  // Table columns
  const columns = [
    {
      title: '执行时间',
      key: 'start_time',
      render: (_: any, record: JobRun) => (
        <div className="text-sm">
          <div className="font-medium text-gray-900">
            {record.start_time ? new Date(record.start_time).toLocaleDateString('zh-CN') : '-'}
          </div>
          <div className="text-gray-500">
            {record.start_time ? new Date(record.start_time).toLocaleTimeString('zh-CN') : '-'}
          </div>
        </div>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => {
        const statusDisplay = getStatusDisplay(status);
        const IconComponent = statusDisplay.icon;
        
        return (
          <div className="flex items-center space-x-2">
            <StatusTag status={status} mapping={{
              pending: { text: '待执行', color: 'yellow' },
              running: { text: '运行中', color: 'blue' },
              completed: { text: '已完成', color: 'green' },
              failed: { text: '失败', color: 'red' },
              cancelled: { text: '已取消', color: 'gray' }
            }} />
            {status === 'running' && (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
            )}
          </div>
        );
      }
    },
    {
      title: '持续时间',
      key: 'duration',
      render: (_: any, record: JobRun) => (
        <span className="text-sm text-gray-600">
          {formatDuration(record.duration)}
        </span>
      )
    },
    {
      title: '结果',
      key: 'result',
      render: (_: any, record: JobRun) => {
        if (record.status === 'completed' && record.result) {
          return (
            <div className="text-sm text-green-600">
              <div>执行成功</div>
              {record.result.processed_documents && (
                <div className="text-gray-500">处理了 {record.result.processed_documents} 个文档</div>
              )}
            </div>
          );
        } else if (record.status === 'failed' && record.error_message) {
          return (
            <div className="text-sm text-red-600">
              <div>执行失败</div>
              <div className="text-gray-500 truncate max-w-xs">{record.error_message}</div>
            </div>
          );
        }
        return <span className="text-gray-500">-</span>;
      }
    },
    {
      title: '结束时间',
      key: 'end_time',
      render: (_: any, record: JobRun) => (
        <div className="text-sm text-gray-500">
          {record.end_time ? (
            <>
              <div>{new Date(record.end_time).toLocaleDateString('zh-CN')}</div>
              <div>{new Date(record.end_time).toLocaleTimeString('zh-CN')}</div>
            </>
          ) : (
            <div>-</div>
          )}
        </div>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: JobRun) => (
        <div className="flex space-x-2">
          <button
            onClick={() => navigate(`/jobs/${id}/runs/${record.id}`)}
            className="p-1 text-blue-600 hover:text-blue-800"
            title="查看详情"
          >
            <Eye size={16} />
          </button>
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
        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} />
          <span>刷新</span>
        </button>
      </div>

      {/* Job Info Card */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{job.name}</h1>
            <p className="text-gray-600 mt-1">
              {job.description || '暂无描述'}
            </p>
          </div>
          <StatusTag 
            status={job.status} 
            mapping={{
              enabled: { text: '启用', color: 'green' },
              disabled: { text: '禁用', color: 'gray' },
              running: { text: '运行中', color: 'blue' }
            }}
          />
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <p className="text-sm text-gray-500">任务类型</p>
            <p className="font-medium">
              {job.job_type === 'agent' ? 'Agent任务' : 
               job.job_type === 'workflow' ? '工作流任务' : '文档处理任务'}
            </p>
          </div>
          <div>
            <p className="text-sm text-gray-500">调度表达式</p>
            <p className="font-mono text-sm">{job.schedule}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">总运行次数</p>
            <p className="font-medium">{job.run_count}</p>
          </div>
          <div>
            <p className="text-sm text-gray-500">成功率</p>
            <p className="font-medium">
              {job.run_count > 0 ? Math.round((job.success_count / job.run_count) * 100) : 0}%
            </p>
          </div>
        </div>

        {job.last_run_at && (
          <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p className="text-sm text-gray-500">最后运行时间</p>
              <p className="font-medium">
                {new Date(job.last_run_at).toLocaleString('zh-CN')}
              </p>
            </div>
            {job.next_run_at && (
              <div>
                <p className="text-sm text-gray-500">下次运行时间</p>
                <p className="font-medium">
                  {new Date(job.next_run_at).toLocaleString('zh-CN')}
                </p>
              </div>
            )}
            <div>
              <p className="text-sm text-gray-500">成功次数</p>
              <p className="font-medium text-green-600">{job.success_count}</p>
            </div>
            <div>
              <p className="text-sm text-gray-500">失败次数</p>
              <p className="font-medium text-red-600">{job.failure_count}</p>
            </div>
          </div>
        )}
      </div>

      {/* Search Bar */}
      <SearchBar
        fields={[
          {
            name: 'status',
            label: '执行状态',
            type: 'select',
            options: [
              { label: '全部', value: '' },
              { label: '待执行', value: 'pending' },
              { label: '运行中', value: 'running' },
              { label: '已完成', value: 'completed' },
              { label: '失败', value: 'failed' },
              { label: '已取消', value: 'cancelled' }
            ]
          }
        ]}
        onSearch={handleSearch}
        onReset={handleReset}
      />

      {/* Job Runs Table */}
      <div className="bg-white rounded-lg border border-gray-200">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">执行历史</h2>
          <p className="text-sm text-gray-600 mt-1">
            共 {total} 次执行记录
          </p>
        </div>
        <div className="p-6">
          <DataTable
            columns={columns}
            dataSource={runs}
            loading={loading}
            pagination={{
              current: searchParams.page,
              pageSize: searchParams.pageSize,
              total,
              onChange: (page, pageSize) => setSearchParams(prev => ({
                ...prev,
                page,
                pageSize: pageSize || 10
              }))
            }}
          />
        </div>
      </div>

      {/* Empty State */}
      {runs.length === 0 && !loading && (
        <div className="bg-white rounded-lg border border-gray-200 p-12 text-center">
          <Clock className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">暂无执行记录</h3>
          <p className="text-gray-500">该任务还没有执行历史，等待下次调度或手动触发执行。</p>
        </div>
      )}
    </div>
  );
};

export default JobRuns;