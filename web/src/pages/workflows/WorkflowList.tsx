import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, Edit, Trash2, Eye, Play, Code } from 'lucide-react';
import { toast } from 'sonner';

import { Workflow, WorkflowQuery } from '@/types/workflow';
import { workflowService } from '@/services/workflow';
import { DataTable } from '@/components/common/DataTable';
import { FormModal } from '@/components/common/FormModal';
import { Pagination } from '@/components/common/Pagination';
import { StatusTag } from '@/components/common/StatusTag';
import { formatDateTime } from '@/utils/format';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';
import WorkflowForm from './components/WorkflowForm';
import RunWorkflowModal from './components/RunWorkflowModal';

const WorkflowList: React.FC = () => {
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [searchName, setSearchName] = useState('');
  const [searchStatus, setSearchStatus] = useState<string>('');
  
  const [modalVisible, setModalVisible] = useState(false);
  const [editingWorkflow, setEditingWorkflow] = useState<Workflow | null>(null);
  const [submitting, setSubmitting] = useState(false);
  
  const [runModalVisible, setRunModalVisible] = useState(false);
  const [runningWorkflow, setRunningWorkflow] = useState<Workflow | null>(null);

  // 加载数据
  const loadWorkflows = async () => {
    setLoading(true);
    try {
      const query: WorkflowQuery = {
        name: searchName || undefined,
        status: searchStatus || undefined,
        page,
        pageSize
      };
      
      const result = await workflowService.getWorkflows(query);
      setWorkflows(result.items);
      setTotal(result.total);
    } catch (error) {
      console.error('加载工作流失败:', error);
      toast.error('加载工作流失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWorkflows();
  }, [page, pageSize, searchName, searchStatus]);

  // 搜索处理
  const handleSearch = () => {
    setPage(1);
    loadWorkflows();
  };

  // 重置搜索
  const handleReset = () => {
    setSearchName('');
    setSearchStatus('');
    setPage(1);
  };

  // 创建/编辑处理
  const handleCreate = () => {
    setEditingWorkflow(null);
    setModalVisible(true);
  };

  const handleEdit = (workflow: Workflow) => {
    setEditingWorkflow(workflow);
    setModalVisible(true);
  };

  // 查看详情
  const handleViewDetail = (workflowId: string) => {
    navigate(`/workflows/${workflowId}`);
  };

  // 运行工作流
  const handleRun = (workflow: Workflow) => {
    setRunningWorkflow(workflow);
    setRunModalVisible(true);
  };

  // 查看运行记录
  const handleViewRuns = () => {
    navigate('/workflow-runs');
  };

  // 删除处理
  const handleDelete = async (workflow: Workflow) => {
    if (!window.confirm(`确定要删除工作流 "${workflow.name}" 吗？`)) {
      return;
    }

    try {
      await workflowService.deleteWorkflow(workflow.id);
      toast.success('删除成功');
      loadWorkflows();
    } catch (error) {
      console.error('删除失败:', error);
      toast.error('删除失败');
    }
  };

  // 表单提交处理
  const handleFormSubmit = async (values: any) => {
    setSubmitting(true);
    try {
      if (editingWorkflow) {
        await workflowService.updateWorkflow(editingWorkflow.id, values);
        toast.success('更新成功');
      } else {
        await workflowService.createWorkflow(values);
        toast.success('创建成功');
      }
      setModalVisible(false);
      loadWorkflows();
    } catch (error) {
      console.error('提交失败:', error);
      toast.error('提交失败');
      throw error;
    } finally {
      setSubmitting(false);
    }
  };

  // 运行工作流处理
  const handleRunSubmit = async (input: any) => {
    if (!runningWorkflow) return;
    
    try {
      const result = await workflowService.runWorkflow(runningWorkflow.id, { input });
      toast.success(`工作流运行已提交，运行ID: ${result.runId}`);
      setRunModalVisible(false);
      navigate('/workflow-runs');
    } catch (error) {
      console.error('运行失败:', error);
      toast.error('运行失败');
      throw error;
    }
  };

  // 表格列配置
  const columns = [
    {
      key: 'name',
      title: '工作流名称',
      render: (value: string, record: Workflow) => (
        <div>
          <div className="font-medium text-gray-900">{value}</div>
          {record.description && (
            <div className="text-sm text-gray-500 mt-1">{record.description}</div>
          )}
        </div>
      )
    },
    {
      key: 'version',
      title: '版本',
      width: '80px',
      render: (value: number) => (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
          v{value}
        </span>
      )
    },
    {
      key: 'status',
      title: '状态',
      width: '100px',
      render: (value: string) => <StatusTag status={value} />
    },
    {
      key: 'created_at',
      title: '创建时间',
      width: '150px',
      render: (value: string) => (
        <span className="text-sm text-gray-600">
          {formatDateTime(value)}
        </span>
      )
    },
    {
      key: 'actions',
      title: '操作',
      width: '300px',
      render: (_: any, record: Workflow) => (
        <div className="flex items-center space-x-2">
          <button
            onClick={() => handleRun(record)}
            className="text-green-600 hover:text-green-800 text-sm font-medium"
            disabled={record.status !== 'active'}
          >
            <Play className="h-4 w-4 inline mr-1" />
            运行
          </button>
          <button
            onClick={() => handleViewDetail(record.id)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
          >
            <Eye className="h-4 w-4 inline mr-1" />
            详情
          </button>
          <button
            onClick={() => handleEdit(record)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
          >
            <Edit className="h-4 w-4 inline mr-1" />
            编辑
          </button>
          <button
            onClick={() => handleDelete(record)}
            className="text-red-600 hover:text-red-800 text-sm font-medium"
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
      {/* 页面标题 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">工作流模板管理</h1>
          <p className="text-gray-600 mt-1">管理和配置 AI 工作流模板</p>
        </div>
        <div className="flex items-center space-x-3">
          <button
            onClick={handleViewRuns}
            className="btn-secondary"
          >
            <Code className="h-4 w-4 mr-2" />
            运行记录
          </button>
          <button
            onClick={handleCreate}
            className="btn-primary"
          >
            <Plus className="h-4 w-4 mr-2" />
            创建工作流
          </button>
        </div>
      </div>

      {/* 搜索区域 */}
      <div className="card">
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex-1 min-w-64">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              名称搜索
            </label>
            <input
              type="text"
              value={searchName}
              onChange={(e) => setSearchName(e.target.value)}
              placeholder="请输入工作流名称"
              className="form-input"
            />
          </div>
          
          <div className="w-32">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              状态
            </label>
            <select
              value={searchStatus}
              onChange={(e) => setSearchStatus(e.target.value)}
              className="form-input"
            >
              <option value="">全部</option>
              <option value="active">启用</option>
              <option value="inactive">禁用</option>
            </select>
          </div>
          
          <div className="flex space-x-3">
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
          </div>
        </div>
      </div>

      {/* 数据表格 */}
      <div className="card">
        <DataTable
          columns={columns}
          data={workflows}
          loading={loading}
          emptyText="暂无工作流模板"
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

      {/* 创建/编辑弹窗 */}
      <FormModal
        visible={modalVisible}
        title={editingWorkflow ? '编辑工作流' : '创建工作流'}
        onCancel={() => setModalVisible(false)}
        onSubmit={handleFormSubmit}
        submitting={submitting}
      >
        <WorkflowForm
          initialValues={editingWorkflow}
          isEdit={!!editingWorkflow}
        />
      </FormModal>

      {/* 运行工作流弹窗 */}
      <RunWorkflowModal
        visible={runModalVisible}
        workflow={runningWorkflow}
        onCancel={() => setRunModalVisible(false)}
        onSubmit={handleRunSubmit}
      />
    </div>
  );
};

export default WorkflowList;