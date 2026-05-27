import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Eye, Edit, Search, RefreshCw, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { DataTable } from '@/components/common/DataTable';
import { FormModal } from '@/components/common/FormModal';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { Pagination } from '@/components/common/Pagination';
import { SearchBar } from '@/components/common/SearchBar';
import { toolService, type ToolListParams } from '@/services/tool';
import { validateToolForm, type ToolFormErrors } from '@/utils/toolValidation';
import type { ToolItem, CreateToolReq, UpdateToolReq } from '@/types/tool';

const EXECUTOR_TYPES = [
  { value: 'HTTP', label: 'HTTP 接口调用' },
  { value: 'SQL', label: 'SQL 数据库查询' },
  { value: 'LOCAL_METHOD', label: '本地方法调用' },
] as const;

const INITIAL_FORM: CreateToolReq = {
  name: '',
  description: '',
  executorType: 'HTTP',
  executorConfig: '',
  parametersSchema: '',
};

const ToolList: React.FC = () => {
  const navigate = useNavigate();
  const [tools, setTools] = useState<ToolItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [formData, setFormData] = useState<CreateToolReq>(INITIAL_FORM);
  const [formErrors, setFormErrors] = useState<ToolFormErrors>({});
  const [submitting, setSubmitting] = useState(false);

  const [editingTool, setEditingTool] = useState<ToolItem | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editFormData, setEditFormData] = useState<UpdateToolReq>(INITIAL_FORM);
  const [editFormErrors, setEditFormErrors] = useState<ToolFormErrors>({});
  const [editSubmitting, setEditSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<ToolItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  const loadTools = useCallback(async (page: number, keyword: string) => {
    setLoading(true);
    try {
      const params: ToolListParams = { current: page, size: pageSize };
      if (keyword) {
        params.keyword = keyword;
      }
      const data = await toolService.listPage(params);
      setTools(data.items);
      setTotal(data.total);
      setCurrent(data.page);
    } catch {
      toast.error('加载工具列表失败');
    } finally {
      setLoading(false);
    }
  }, [pageSize]);

  useEffect(() => {
    loadTools(1, '');
  }, [loadTools]);

  const handleSearch = useCallback(() => {
    loadTools(1, searchKeyword);
  }, [loadTools, searchKeyword]);

  const handleSearchClear = useCallback(() => {
    setSearchKeyword('');
    loadTools(1, '');
  }, [loadTools]);

  const handleKeywordChange = useCallback((value: string) => {
    const hadKeyword = !!searchKeyword;
    setSearchKeyword(value);
    if (hadKeyword && !value) {
      loadTools(1, '');
    }
  }, [loadTools, searchKeyword]);

  const handlePageChange = useCallback((page: number) => {
    loadTools(page, searchKeyword);
  }, [loadTools, searchKeyword]);

  const openCreateModal = () => {
    setFormData(INITIAL_FORM);
    setFormErrors({});
    setIsCreateModalOpen(true);
  };

  const closeCreateModal = () => {
    if (submitting) return;
    setIsCreateModalOpen(false);
  };

  const handleFieldChange = (field: keyof CreateToolReq, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (formErrors[field as keyof ToolFormErrors]) {
      setFormErrors((prev) => {
        const next = { ...prev };
        delete next[field as keyof ToolFormErrors];
        return next;
      });
    }
  };

  const handleSubmit = async () => {
    const errors = validateToolForm(formData);
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    try {
      const payload: CreateToolReq = {
        ...formData,
        name: formData.name.trim(),
        description: formData.description.trim(),
        executorConfig: formData.executorConfig.trim(),
        parametersSchema: formData.parametersSchema.trim(),
      };
      await toolService.create(payload);
      toast.success(`工具 "${payload.name}" 创建成功`);
      setIsCreateModalOpen(false);
      loadTools(1, searchKeyword);
    } catch (err: any) {
      const message = err?.message || '创建工具失败，请稍后重试';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const openEditModal = (tool: ToolItem) => {
    setEditingTool(tool);
    setEditFormData({
      name: tool.name,
      description: tool.description || '',
      executorType: tool.executorType,
      executorConfig: tool.executorConfig || '',
      parametersSchema: tool.parametersSchema || '',
    });
    setEditFormErrors({});
    setIsEditModalOpen(true);
  };

  const closeEditModal = () => {
    if (editSubmitting) return;
    setIsEditModalOpen(false);
    setEditingTool(null);
  };

  const handleEditFieldChange = (field: keyof UpdateToolReq, value: string) => {
    setEditFormData((prev) => ({ ...prev, [field]: value }));
    if (editFormErrors[field as keyof ToolFormErrors]) {
      setEditFormErrors((prev) => {
        const next = { ...prev };
        delete next[field as keyof ToolFormErrors];
        return next;
      });
    }
  };

  const handleEditSubmit = async () => {
    if (!editingTool) return;

    const errors = validateToolForm({
      ...editFormData,
      executorType: editFormData.executorType as 'HTTP' | 'SQL' | 'LOCAL_METHOD',
    });
    if (Object.keys(errors).length > 0) {
      setEditFormErrors(errors);
      return;
    }

    setEditSubmitting(true);
    try {
      const payload: UpdateToolReq = {
        name: editFormData.name.trim(),
        description: editFormData.description.trim(),
        executorType: editFormData.executorType,
        executorConfig: editFormData.executorConfig.trim(),
        parametersSchema: editFormData.parametersSchema.trim(),
      };
      await toolService.update(editingTool.id, payload);
      toast.success(`工具 "${payload.name}" 更新成功`);
      setIsEditModalOpen(false);
      setEditingTool(null);
      loadTools(current, searchKeyword);
    } catch (err: any) {
      const message = err?.message || '更新工具失败，请稍后重试';
      toast.error(message);
    } finally {
      setEditSubmitting(false);
    }
  };

  const handleDeleteClick = (tool: ToolItem) => {
    setDeleteTarget(tool);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;

    setDeleting(true);
    try {
      await toolService.delete(deleteTarget.id);
      toast.success(`工具 "${deleteTarget.name}" 已删除`);
      setDeleteTarget(null);
      loadTools(current, searchKeyword);
    } catch (err: any) {
      const message = err?.message || '删除工具失败，请稍后重试';
      toast.error(message);
    } finally {
      setDeleting(false);
    }
  };

  const getExecutorTypeLabel = (type: string) =>
    EXECUTOR_TYPES.find((t) => t.value === type)?.label ?? type;

  const columns = [
    {
      key: 'name',
      title: '工具名称',
      render: (value: string, record: ToolItem) => (
        <div>
          <div className="font-medium text-gray-900">{value}</div>
          {record.description && (
            <div className="text-sm text-gray-500 mt-1 max-w-xs truncate">
              {record.description}
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'executorType',
      title: '执行器类型',
      width: '130px',
      render: (value: string) => (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
          {getExecutorTypeLabel(value)}
        </span>
      ),
    },
    {
      key: 'parametersSchema',
      title: '参数 Schema',
      width: '200px',
      render: (value: string) => (
        <span className="text-sm text-gray-500 font-mono">
          {value
            ? value.length > 40
              ? value.substring(0, 40) + '...'
              : value
            : '-'}
        </span>
      ),
    },
    {
      key: 'enabled',
      title: '状态',
      width: '80px',
      render: (value: boolean) =>
        value ? (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
            启用
          </span>
        ) : (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-600">
            禁用
          </span>
        ),
    },
    {
      key: 'actions',
      title: '操作',
      width: '160px',
      render: (_: any, record: ToolItem) => (
        <div className="flex items-center space-x-3">
          <button
            onClick={() => navigate(`/tools/${record.id}`)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium flex items-center"
          >
            <Eye className="h-4 w-4 mr-1" />
            详情
          </button>
          <button
            onClick={() => openEditModal(record)}
            className="text-primary-600 hover:text-primary-800 text-sm font-medium flex items-center"
          >
            <Edit className="h-4 w-4 mr-1" />
            编辑
          </button>
          <button
            onClick={() => handleDeleteClick(record)}
            className="text-red-600 hover:text-red-800 text-sm font-medium flex items-center"
          >
            <Trash2 className="h-4 w-4 mr-1" />
            删除
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">工具管理</h1>
          <p className="text-gray-600 mt-1">
            管理 Agent 可调用的工具定义，支持 HTTP 接口、SQL 查询等多种类型
          </p>
        </div>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus className="h-4 w-4 mr-2" />
          创建工具
        </button>
      </div>

      <div className="card">
        <div className="flex items-center space-x-4">
          <SearchBar
            placeholder="搜索工具名称或描述"
            value={searchKeyword}
            onChange={handleKeywordChange}
            onSearch={handleSearch}
            className="flex-1"
          />
          <button onClick={handleSearch} className="btn-primary">
            <Search className="h-4 w-4 mr-2" />
            搜索
          </button>
          <button onClick={handleSearchClear} className="btn-secondary">
            重置
          </button>
          <button
            onClick={() => loadTools(current, searchKeyword)}
            className="btn-secondary"
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            刷新
          </button>
        </div>
      </div>

      <div className="card">
        <DataTable
          columns={columns}
          data={tools}
          loading={loading}
          emptyText="暂无工具"
        />
        {!loading && tools.length > 0 && (
          <div className="mt-4">
            <Pagination
              current={current}
              total={total}
              pageSize={pageSize}
              onChange={handlePageChange}
              showSizeChanger
              onShowSizeChange={setPageSize}
              showTotal
            />
          </div>
        )}
      </div>

      <FormModal
        isOpen={isCreateModalOpen}
        onClose={closeCreateModal}
        title="创建工具"
        width="max-w-lg"
        onSubmit={handleSubmit}
        loading={submitting}
        submitText="创建"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              工具名称 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              className={`form-input ${formErrors.name ? 'border-red-400 focus:ring-red-500' : ''}`}
              placeholder="例如: querySalesData"
              value={formData.name}
              onChange={(e) => handleFieldChange('name', e.target.value)}
              maxLength={100}
            />
            {formErrors.name && (
              <p className="text-xs text-red-500 mt-1">{formErrors.name}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              工具描述
            </label>
            <textarea
              className="form-textarea"
              placeholder="描述工具的用途和功能，LLM 将据此选择合适的工具..."
              rows={2}
              value={formData.description}
              onChange={(e) => handleFieldChange('description', e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              执行器类型 <span className="text-red-500">*</span>
            </label>
            <select
              className="form-input"
              value={formData.executorType}
              onChange={(e) => handleFieldChange('executorType', e.target.value)}
            >
              {EXECUTOR_TYPES.map(({ value, label }) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              执行器配置 <span className="text-red-500">*</span>
            </label>
            <textarea
              className={`form-textarea font-mono text-sm ${
                formErrors.executorConfig ? 'border-red-400 focus:ring-red-500' : ''
              }`}
              placeholder={
                formData.executorType === 'HTTP'
                  ? '{"url": "https://api.example.com/data", "method": "GET"}'
                  : formData.executorType === 'SQL'
                    ? '{"sql": "SELECT * FROM {table} WHERE 1=1"}'
                    : '{}'
              }
              rows={4}
              value={formData.executorConfig}
              onChange={(e) => handleFieldChange('executorConfig', e.target.value)}
            />
            {formErrors.executorConfig ? (
              <p className="text-xs text-red-500 mt-1">{formErrors.executorConfig}</p>
            ) : (
              <p className="text-xs text-gray-500 mt-1">
                请填写合法的 JSON 格式配置，可使用 {'{参数名}'} 作为占位符
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              参数 Schema
            </label>
            <textarea
              className="form-textarea font-mono text-sm"
              placeholder='{"param1": "string 参数说明", "param2": "number 参数说明"}'
              rows={3}
              value={formData.parametersSchema}
              onChange={(e) => handleFieldChange('parametersSchema', e.target.value)}
            />
            <p className="text-xs text-gray-500 mt-1">
              描述工具接受的参数结构（JSON 格式），LLM 将据此生成调用参数
            </p>
          </div>
        </div>
      </FormModal>

      <FormModal
        isOpen={isEditModalOpen}
        onClose={closeEditModal}
        title="编辑工具"
        width="max-w-lg"
        onSubmit={handleEditSubmit}
        loading={editSubmitting}
        submitText="保存"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              工具名称 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              className={`form-input ${editFormErrors.name ? 'border-red-400 focus:ring-red-500' : ''}`}
              placeholder="例如: querySalesData"
              value={editFormData.name}
              onChange={(e) => handleEditFieldChange('name', e.target.value)}
              maxLength={100}
            />
            {editFormErrors.name && (
              <p className="text-xs text-red-500 mt-1">{editFormErrors.name}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              工具描述
            </label>
            <textarea
              className="form-textarea"
              placeholder="描述工具的用途和功能，LLM 将据此选择合适的工具..."
              rows={2}
              value={editFormData.description}
              onChange={(e) => handleEditFieldChange('description', e.target.value)}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              执行器类型 <span className="text-red-500">*</span>
            </label>
            <select
              className="form-input"
              value={editFormData.executorType}
              onChange={(e) => handleEditFieldChange('executorType', e.target.value)}
            >
              {EXECUTOR_TYPES.map(({ value, label }) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              执行器配置 <span className="text-red-500">*</span>
            </label>
            <textarea
              className={`form-textarea font-mono text-sm ${
                editFormErrors.executorConfig ? 'border-red-400 focus:ring-red-500' : ''
              }`}
              placeholder={
                editFormData.executorType === 'HTTP'
                  ? '{"url": "https://api.example.com/data", "method": "GET"}'
                  : editFormData.executorType === 'SQL'
                    ? '{"sql": "SELECT * FROM {table} WHERE 1=1"}'
                    : '{}'
              }
              rows={4}
              value={editFormData.executorConfig}
              onChange={(e) => handleEditFieldChange('executorConfig', e.target.value)}
            />
            {editFormErrors.executorConfig ? (
              <p className="text-xs text-red-500 mt-1">{editFormErrors.executorConfig}</p>
            ) : (
              <p className="text-xs text-gray-500 mt-1">
                请填写合法的 JSON 格式配置，可使用 {'{参数名}'} 作为占位符
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              参数 Schema
            </label>
            <textarea
              className="form-textarea font-mono text-sm"
              placeholder='{"param1": "string 参数说明", "param2": "number 参数说明"}'
              rows={3}
              value={editFormData.parametersSchema}
              onChange={(e) => handleEditFieldChange('parametersSchema', e.target.value)}
            />
            <p className="text-xs text-gray-500 mt-1">
              描述工具接受的参数结构（JSON 格式），LLM 将据此生成调用参数
            </p>
          </div>
        </div>
      </FormModal>

      <ConfirmModal
        isOpen={!!deleteTarget}
        title="删除工具"
        message={`确定要删除工具 "${deleteTarget?.name}" 吗？此操作不可撤销。`}
        confirmText="确定删除"
        danger
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
};

export default ToolList;