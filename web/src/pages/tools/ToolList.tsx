import React, { useState, useEffect } from 'react';
import { Plus, Search, RefreshCw, Edit, Trash2, Eye } from 'lucide-react';
import { toast } from 'sonner';
import { DataTable, StatusTag } from '@/components/common';
import { SearchBar } from '@/components/common/SearchBar';
import { FormModal } from '@/components/common/FormModal';
import { Pagination } from '@/components/common/Pagination';
import { Tool, ToolQueryParams } from '@/types/tool';
import { toolService } from '@/services/tool';
import { DEFAULT_PAGE_SIZE } from '@/utils/constants';

const ToolList: React.FC = () => {
  const [tools, setTools] = useState<Tool[]>([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [total, setTotal] = useState(0);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTool, setEditingTool] = useState<Tool | undefined>();

  // Fetch tools data
  const fetchTools = async () => {
    try {
      setLoading(true);
      const params: ToolQueryParams = {
        page,
        pageSize,
        name: keyword,
        executor_type: ''
      };
      const response = await toolService.getTools(params);
      setTools(response.data);
      setTotal(response.total);
    } catch (error) {
      toast.error('获取工具列表失败');
      console.error('Failed to fetch tools:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTools();
  }, [page, pageSize, keyword]);

  // Handle search
  const handleSearch = () => {
    setPage(1);
    fetchTools();
  };

  // Handle reset
  const handleReset = () => {
    setKeyword('');
    setPage(1);
  };

  // Open edit modal
  const openEditModal = (tool: Tool) => {
    setEditingTool(tool);
    setModalOpen(true);
  };

  // Open create modal
  const openCreateModal = () => {
    setEditingTool(undefined);
    setModalOpen(true);
  };

  // Handle delete
  const handleDelete = async (id: string) => {
    if (!window.confirm('确定要删除这个工具吗？')) {
      return;
    }

    try {
      await toolService.deleteTool(id);
      toast.success('工具删除成功');
      fetchTools();
    } catch (error) {
      toast.error('工具删除失败');
      console.error('Failed to delete tool:', error);
    }
  };

  // Handle submit
  const handleSubmit = async () => {
    toast.success('操作成功');
    setModalOpen(false);
    fetchTools();
  };

  // Table columns
  const columns = [
    {
      key: 'name',
      title: '名称',
      render: (value: string, record: Tool) => (
        <div>
          <div className="font-medium text-gray-900">{value}</div>
          {record.description && (
            <div className="text-sm text-gray-500">{record.description}</div>
          )}
        </div>
      )
    },
    {
      key: 'executor_type',
      title: '执行器类型',
      width: '120px',
      render: (value: string) => (
        <StatusTag status={value} />
      )
    },
    {
      key: 'tags',
      title: '标签',
      render: (tags: string[]) => (
        <div className="flex flex-wrap gap-1">
          {tags?.map(tag => (
            <span key={tag} className="px-2 py-1 text-xs bg-gray-100 text-gray-700 rounded-full">
              {tag}
            </span>
          ))}
        </div>
      )
    },
    {
      key: 'created_at',
      title: '创建时间',
      width: '180px',
      render: (value: string) => (
        <span className="text-sm text-gray-500">
          {new Date(value).toLocaleString('zh-CN')}
        </span>
      )
    },
    {
      key: 'actions',
      title: '操作',
      width: '150px',
      render: (_: any, record: Tool) => (
        <div className="flex space-x-2">
          <button
            onClick={() => openEditModal(record)}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium"
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
          <h1 className="text-2xl font-bold text-gray-900">工具管理</h1>
          <p className="text-gray-600">管理Agent可调用工具定义</p>
        </div>
        <button
          onClick={openCreateModal}
          className="flex items-center space-x-2 btn-primary"
        >
          <Plus className="h-4 w-4" />
          <span>创建工具</span>
        </button>
      </div>

      {/* Search Bar */}
      <div className="card">
        <div className="flex items-center space-x-4">
          <SearchBar
            placeholder="搜索工具名称"
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
            onClick={fetchTools}
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
          data={tools}
          loading={loading}
          emptyText="暂无工具数据"
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
          setEditingTool(undefined);
        }}
        title={editingTool ? '编辑工具' : '创建工具'}
        onSubmit={handleSubmit}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              工具名称
            </label>
            <input
              type="text"
              className="form-input"
              placeholder="请输入工具名称"
              defaultValue={editingTool?.name}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              描述
            </label>
            <textarea
              className="form-textarea"
              placeholder="请输入工具描述"
              rows={3}
              defaultValue={editingTool?.description}
            />
          </div>
        </div>
      </FormModal>
    </div>
  );
};

export default ToolList;
