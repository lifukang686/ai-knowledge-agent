import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Search, Play, Edit, Trash2, Eye, Bot } from 'lucide-react';
import { toast } from 'sonner';

import { Agent, AgentQuery } from '@/types/agent';
import { agentService } from '@/services/agent';
import { DataTable } from '@/components/common/DataTable';
import { FormModal } from '@/components/common/FormModal';
import { Pagination } from '@/components/common/Pagination';
import { StatusTag } from '@/components/common/StatusTag';
import { formatDateTime } from '@/utils/format';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';
import AgentForm from './components/AgentForm';
import RunAgentModal from './components/RunAgentModal';

const AgentList: React.FC = () => {
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [agents, setAgents] = useState<Agent[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [searchName, setSearchName] = useState('');
  const [searchStatus, setSearchStatus] = useState<string>('');
  
  const [modalVisible, setModalVisible] = useState(false);
  const [editingAgent, setEditingAgent] = useState<Agent | null>(null);
  const [submitting, setSubmitting] = useState(false);
  
  const [runModalVisible, setRunModalVisible] = useState(false);
  const [runningAgent, setRunningAgent] = useState<Agent | null>(null);

  // 加载数据
  const loadAgents = async () => {
    setLoading(true);
    try {
      const query: AgentQuery = {
        name: searchName || undefined,
        status: searchStatus || undefined,
        page,
        pageSize
      };
      
      const result = await agentService.getAgents(query);
      setAgents(result.items);
      setTotal(result.total);
    } catch (error) {
      console.error('加载 Agent 失败:', error);
      toast.error('加载 Agent 失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAgents();
  }, [page, pageSize, searchName, searchStatus]);

  // 搜索处理
  const handleSearch = () => {
    setPage(1);
    loadAgents();
  };

  // 重置搜索
  const handleReset = () => {
    setSearchName('');
    setSearchStatus('');
    setPage(1);
  };

  // 创建/编辑处理
  const handleCreate = () => {
    setEditingAgent(null);
    setModalVisible(true);
  };

  const handleEdit = (agent: Agent) => {
    setEditingAgent(agent);
    setModalVisible(true);
  };

  // 运行 Agent
  const handleRun = (agent: Agent) => {
    setRunningAgent(agent);
    setRunModalVisible(true);
  };

  // 查看运行记录
  const handleViewRuns = (agentId: string) => {
    navigate(`/agents/${agentId}/runs`);
  };

  // 删除处理
  const handleDelete = async (agent: Agent) => {
    if (!window.confirm(`确定要删除 Agent "${agent.name}" 吗？`)) {
      return;
    }

    try {
      await agentService.deleteAgent(agent.id);
      toast.success('删除成功');
      loadAgents();
    } catch (error) {
      console.error('删除失败:', error);
      toast.error('删除失败');
    }
  };

  // 表单提交处理
  const handleFormSubmit = async (values: any) => {
    setSubmitting(true);
    try {
      if (editingAgent) {
        await agentService.updateAgent(editingAgent.id, values);
        toast.success('更新成功');
      } else {
        await agentService.createAgent(values);
        toast.success('创建成功');
      }
      setModalVisible(false);
      loadAgents();
    } catch (error) {
      console.error('提交失败:', error);
      toast.error('提交失败');
      throw error;
    } finally {
      setSubmitting(false);
    }
  };

  // 运行 Agent 处理
  const handleRunSubmit = async (task: string) => {
    if (!runningAgent) return;
    
    try {
      const result = await agentService.runAgent(runningAgent.id, { task });
      toast.success(`Agent 运行已提交，运行ID: ${result.runId}`);
      setRunModalVisible(false);
      navigate(`/agents/${runningAgent.id}/runs`);
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
      title: 'Agent 名称',
      render: (value: string, record: Agent) => (
        <div className="flex items-center">
          <Bot className="h-4 w-4 text-gray-400 mr-2" />
          <div>
            <div className="font-medium text-gray-900">{value}</div>
            {record.description && (
              <div className="text-sm text-gray-500 mt-1">{record.description}</div>
            )}
          </div>
        </div>
      )
    },
    {
      key: 'toolIds',
      title: '工具',
      width: '120px',
      render: (value: string[]) => (
        <span className="text-sm text-gray-600">
          {value.length} 个工具
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
      width: '250px',
      render: (_: any, record: Agent) => (
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
            onClick={() => handleViewRuns(record.id)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium"
          >
            <Eye className="h-4 w-4 inline mr-1" />
            记录
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
          <h1 className="text-2xl font-bold text-gray-900">Agent 管理</h1>
          <p className="text-gray-600 mt-1">管理 AI Agent 配置和运行</p>
        </div>
        <button
          onClick={handleCreate}
          className="btn-primary"
        >
          <Plus className="h-4 w-4 mr-2" />
          创建 Agent
        </button>
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
              placeholder="请输入 Agent 名称"
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
          data={agents}
          loading={loading}
          emptyText="暂无 Agent"
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
        title={editingAgent ? '编辑 Agent' : '创建 Agent'}
        onCancel={() => setModalVisible(false)}
        onSubmit={handleFormSubmit}
        submitting={submitting}
      >
        <AgentForm
          initialValues={editingAgent}
          isEdit={!!editingAgent}
        />
      </FormModal>

      {/* 运行 Agent 弹窗 */}
      <RunAgentModal
        visible={runModalVisible}
        agent={runningAgent}
        onCancel={() => setRunModalVisible(false)}
        onSubmit={handleRunSubmit}
      />
    </div>
  );
};

export default AgentList;