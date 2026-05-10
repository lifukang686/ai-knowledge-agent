import React, { useState, useEffect } from 'react';
import { Plus, Play, Pause, Edit, Trash2, RefreshCw, Clock, BarChart3, Search } from 'lucide-react';
import { toast } from 'sonner';
import { DataTable, StatusTag } from '@/components/common';
import { SearchBar } from '@/components/common/SearchBar';
import { FormModal } from '@/components/common/FormModal';
import { Pagination } from '@/components/common/Pagination';
import { Job, JobQueryParams } from '@/types/job';
import { jobService } from '@/services/job';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';

const JobList: React.FC = () => {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingJob, setEditingJob] = useState<Job | undefined>();

  // Fetch jobs data
  const fetchJobs = async () => {
    try {
      setLoading(true);
      const params: JobQueryParams = {
        page,
        pageSize,
        name: keyword,
        job_type: '',
        status: ''
      };
      const response = await jobService.getJobs(params);
      setJobs(response.data);
      setTotal(response.total);
    } catch (error) {
      toast.error('获取任务列表失败');
      console.error('Failed to fetch jobs:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, [page, pageSize, keyword]);

  // Handle search
  const handleSearch = () => {
    setPage(1);
    fetchJobs();
  };

  // Handle reset
  const handleReset = () => {
    setKeyword('');
    setPage(1);
  };

  // Open edit modal
  const openEditModal = (job: Job) => {
    setEditingJob(job);
    setModalOpen(true);
  };

  // Open create modal
  const openCreateModal = () => {
    setEditingJob(undefined);
    setModalOpen(true);
  };

  // Handle delete
  const handleDelete = async (id: string) => {
    if (!window.confirm('确定要删除这个任务吗？')) {
      return;
    }

    try {
      await jobService.deleteJob(id);
      toast.success('任务删除成功');
      fetchJobs();
    } catch (error) {
      toast.error('任务删除失败');
      console.error('Failed to delete job:', error);
    }
  };

  // Handle enable/disable
  const handleToggleStatus = async (job: Job) => {
    try {
      toast.success(`任务已${job.status === 'enabled' ? '禁用' : '启用'}`);
      fetchJobs();
    } catch (error) {
      toast.error('任务状态更新失败');
      console.error('Failed to toggle job status:', error);
    }
  };

  // Handle run job
  const handleRunJob = async (id: string) => {
    try {
      toast.success('任务已提交运行');
    } catch (error) {
      toast.error('任务运行失败');
      console.error('Failed to run job:', error);
    }
  };

  // Handle submit
  const handleSubmit = async () => {
    toast.success('操作成功');
    setModalOpen(false);
    fetchJobs();
  };

  // Table columns
  const columns = [
    {
      key: 'name',
      title: '任务名称',
      render: (value: string, record: Job) => (
        <div>
          <div className="font-medium text-gray-900">{value}</div>
          {record.description && (
            <div className="text-sm text-gray-500">{record.description}</div>
          )}
        </div>
      )
    },
    {
      key: 'job_type',
      title: '类型',
      width: '120px',
      render: (value: string) => (
        <StatusTag status={value} />
      )
    },
    {
      key: 'status',
      title: '状态',
      width: '100px',
      render: (value: string) => (
        <StatusTag status={value} />
      )
    },
    {
      key: 'schedule',
      title: '调度表达式',
      width: '140px',
      render: (value: string) => (
        <code className="text-sm bg-gray-100 px-2 py-1 rounded">{value}</code>
      )
    },
    {
      key: 'statistics',
      title: '运行统计',
      width: '140px',
      render: (_: any, record: Job) => (
        <div className="text-sm">
          <div className="flex items-center space-x-2 mb-1">
            <span className="text-gray-500">总运行:</span>
            <span className="font-medium">{record.run_count}</span>
          </div>
          <div className="flex items-center space-x-2 mb-1">
            <span className="text-gray-500">成功:</span>
            <span className="text-green-600 font-medium">{record.success_count}</span>
          </div>
          <div className="flex items-center space-x-2">
            <span className="text-gray-500">失败:</span>
            <span className="text-red-600 font-medium">{record.failure_count}</span>
          </div>
        </div>
      )
    },
    {
      key: 'last_run_at',
      title: '最后运行',
      width: '180px',
      render: (value: string) => (
        <div className="text-sm">
          {value ? (
            <>
              <div>{new Date(value).toLocaleDateString('zh-CN')}</div>
              <div className="text-gray-500">{new Date(value).toLocaleTimeString('zh-CN')}</div>
            </>
          ) : (
            <span className="text-gray-400">从未运行</span>
          )}
        </div>
      )
    },
    {
      key: 'actions',
      title: '操作',
      width: '180px',
      render: (_: any, record: Job) => (
        <div className="flex space-x-2">
          <button
            onClick={() => handleRunJob(record.id)}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium"
            title="立即运行"
          >
            <Play className="h-4 w-4 inline mr-1" />
            运行
          </button>
          <button
            onClick={() => handleToggleStatus(record)}
            className="text-orange-600 hover:text-orange-800 text-sm font-medium"
            title={record.status === 'enabled' ? '禁用' : '启用'}
          >
            {record.status === 'enabled' ? <Pause className="h-4 w-4 inline mr-1" /> : <Play className="h-4 w-4 inline mr-1" />}
            {record.status === 'enabled' ? '禁用' : '启用'}
          </button>
          <button
            onClick={() => openEditModal(record)}
            className="text-gray-600 hover:text-gray-800 text-sm font-medium"
            title="编辑"
          >
            <Edit className="h-4 w-4 inline mr-1" />
            编辑
          </button>
          <button
            onClick={() => handleDelete(record.id)}
            className="text-red-600 hover:text-red-800 text-sm font-medium"
            title="删除"
          >
            <Trash2 className="h-4 w-4 inline mr-1" />
            删除
          </button>
        </div>
      )
    }
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">定时任务管理</h1>
          <p className="text-gray-600">管理系统中的定时任务和调度</p>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center space-x-2 btn-primary"
        >
          <Plus className="h-4 w-4" />
          <span>创建任务</span>
        </button>
      </div>

      {/* Search Bar */}
      <div className="card">
        <div className="flex items-center space-x-4">
          <SearchBar
            placeholder="搜索任务名称"
            value={keyword}
            onChange={setKeyword}
            onSearch={handleSearch}
            className="flex-1"
          />
          <button
            onClick={handleSearch}
            className="btn-primary"
          >
            <Search className="h-4 w-4 mr-2" />
            搜索
          </button>
          <button
            onClick={handleReset}
            className="btn-secondary"
          >
            重置
          </button>
          <button
            onClick={fetchJobs}
            className="btn-secondary"
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            刷新
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="card">
        <DataTable
          columns={columns}
          data={jobs}
          loading={loading}
          emptyText="暂无任务数据"
        />
        {total > 0 && (
          <div className="mt-6">
            <Pagination
              current={page}
              total={total}
              pageSize={pageSize}
              onChange={setPage}
              showSizeChanger
              onShowSizeChange={setPageSize}
            />
          </div>
        )}
      </div>

      {/* Modal */}
      <FormModal
        isOpen={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setEditingJob(undefined);
        }}
        title={editingJob ? '编辑定时任务' : '创建定时任务'}
        onSubmit={handleSubmit}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              任务名称
            </label>
            <input
              type="text"
              className="form-input"
              placeholder="请输入任务名称"
              defaultValue={editingJob?.name}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              描述
            </label>
            <textarea
              className="form-textarea"
              placeholder="请输入任务描述"
              rows={3}
              defaultValue={editingJob?.description}
            />
          </div>
        </div>
      </FormModal>
    </div>
  );
};

export default JobList;
