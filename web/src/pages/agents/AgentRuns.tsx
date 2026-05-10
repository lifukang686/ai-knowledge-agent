import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, RefreshCw, Clock, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';

import { AgentRun, AgentRunQuery } from '@/types/agent';
import { agentService } from '@/services/agent';
import { DataTable, Pagination, StatusTag } from '@/components/common';
import { formatDateTime, formatDuration } from '@/utils/format';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';

const AgentRuns: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(false);
  const [runs, setRuns] = useState<AgentRun[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [statusFilter, setStatusFilter] = useState<string>('');

  // 加载运行记录
  const loadRuns = async () => {
    if (!id) return;
    
    setLoading(true);
    try {
      const query: AgentRunQuery = {
        agentId: id,
        status: statusFilter || undefined,
        page,
        pageSize
      };
      
      const result = await agentService.getAgentRuns(query);
      setRuns(result.items);
      setTotal(result.total);
    } catch (error) {
      console.error('加载运行记录失败:', error);
      toast.error('加载运行记录失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRuns();
  }, [id, page, pageSize, statusFilter]);

  // 查看运行详情
  const handleViewDetail = (runId: string) => {
    navigate(`/agents/runs/${runId}`);
  };

  // 刷新状态
  const handleRefresh = () => {
    loadRuns();
  };

  // 状态图标
  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'completed':
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'running':
        return <Clock className="h-4 w-4 text-blue-500 animate-spin" />;
      case 'failed':
        return <XCircle className="h-4 w-4 text-red-500" />;
      case 'pending':
        return <AlertCircle className="h-4 w-4 text-yellow-500" />;
      default:
        return <AlertCircle className="h-4 w-4 text-gray-500" />;
    }
  };

  // 表格列配置
  const columns = [
    {
      key: 'task',
      title: '任务',
      render: (value: string, record: AgentRun) => (
        <div>
          <div className="flex items-center mb-1">
            {getStatusIcon(record.status)}
            <span className="ml-2 font-medium text-gray-900">
              {value.length > 50 ? `${value.substring(0, 50)}...` : value}
            </span>
          </div>
          {record.status === 'failed' && record.error && (
            <div className="text-sm text-red-600 mt-1">
              错误: {record.error}
            </div>
          )}
          {record.status === 'completed' && record.result && (
            <div className="text-sm text-gray-600 mt-1">
              结果: {record.result.length > 100 ? `${record.result.substring(0, 100)}...` : record.result}
            </div>
          )}
        </div>
      )
    },
    {
      key: 'status',
      title: '状态',
      width: '100px',
      render: (value: string) => <StatusTag status={value} />
    },
    {
      key: 'duration',
      title: '耗时',
      width: '100px',
      render: (value: number, record: AgentRun) => {
        if (record.status === 'running' || record.status === 'pending') {
          return <span className="text-gray-500">-</span>;
        }
        return (
          <span className="text-sm text-gray-600">
            {value ? formatDuration(value) : '-'}
          </span>
        );
      }
    },
    {
      key: 'created_at',
      title: '开始时间',
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
      width: '100px',
      render: (_: any, record: AgentRun) => (
        <button
          onClick={() => handleViewDetail(record.id)}
          className="text-primary-600 hover:text-primary-800 text-sm font-medium"
        >
          详情
        </button>
      )
    }
  ];

  return (
    <div className="space-y-6">
      {/* 页面标题和返回 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button
            onClick={() => navigate('/agents')}
            className="text-gray-600 hover:text-gray-800 mr-4"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Agent 运行记录</h1>
            <p className="text-gray-600 mt-1">查看 Agent 的历史运行记录和状态</p>
          </div>
        </div>
        
        <button
          onClick={handleRefresh}
          className="btn-secondary"
        >
          <RefreshCw className="h-4 w-4 mr-2" />
          刷新
        </button>
      </div>

      {/* 状态筛选 */}
      <div className="card">
        <div className="flex flex-wrap items-end gap-4">
          <div className="w-32">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              状态筛选
            </label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="form-input"
            >
              <option value="">全部</option>
              <option value="pending">待处理</option>
              <option value="running">运行中</option>
              <option value="completed">已完成</option>
              <option value="failed">失败</option>
            </select>
          </div>
          
          <button
            onClick={() => setStatusFilter('')}
            className="btn-secondary"
          >
            重置
          </button>
        </div>
      </div>

      {/* 运行记录列表 */}
      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">运行记录</h2>
          <div className="text-sm text-gray-600">
            共 {total} 条记录
          </div>
        </div>
        
        <DataTable
          columns={columns}
          data={runs}
          loading={loading}
          emptyText="暂无运行记录"
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
    </div>
  );
};

export default AgentRuns;