import React, { useCallback, useEffect, useState } from 'react';
import { Edit, Plus, RefreshCw, Search, Star, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { ConfirmModal, DataTable, FormModal, Pagination, SearchBar } from '@/components/common';
import { chunkStrategyService } from '@/services/chunkStrategy';
import type {
  ChunkStrategy,
  ChunkType,
  CreateChunkStrategyReq,
  UpdateChunkStrategyReq,
} from '@/types/chunkStrategy';
import { formatDateTime } from '@/utils/format';

const CHUNK_TYPE_OPTIONS: { value: ChunkType; label: string }[] = [
  { value: '按段落', label: '按段落' },
  { value: '按句子', label: '按句子' },
  { value: '按字符', label: '按字符' },
  { value: '按内容归属', label: '按内容归属' },
];

const INITIAL_FORM: CreateChunkStrategyReq = {
  strategyName: '',
  chunkType: '按段落',
  maxSegmentSize: 800,
  overlapSize: 300,
};

type FormErrors = Partial<Record<keyof CreateChunkStrategyReq, string>>;

function validateForm(data: CreateChunkStrategyReq): FormErrors {
  const errors: FormErrors = {};
  if (!data.strategyName.trim()) {
    errors.strategyName = '请输入策略名称';
  }
  if (!data.chunkType) {
    errors.chunkType = '请选择分块类型';
  }
  if (!Number.isFinite(data.maxSegmentSize) || data.maxSegmentSize < 1) {
    errors.maxSegmentSize = '最大字符数必须大于 0';
  }
  if (!Number.isFinite(data.overlapSize) || data.overlapSize < 0) {
    errors.overlapSize = '重叠字符数不能小于 0';
  }
  if (data.maxSegmentSize > 0 && data.overlapSize >= data.maxSegmentSize) {
    errors.overlapSize = '重叠字符数必须小于最大字符数';
  }
  return errors;
}

const ChunkStrategyList: React.FC = () => {
  const [strategies, setStrategies] = useState<ChunkStrategy[]>([]);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [searchKeyword, setSearchKeyword] = useState('');

  const [modalVisible, setModalVisible] = useState(false);
  const [editingStrategy, setEditingStrategy] = useState<ChunkStrategy | null>(null);
  const [formData, setFormData] = useState<CreateChunkStrategyReq>(INITIAL_FORM);
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [submitting, setSubmitting] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<ChunkStrategy | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [defaultingId, setDefaultingId] = useState<string | null>(null);

  const loadStrategies = useCallback(async (page: number, keyword: string, size = pageSize) => {
    setLoading(true);
    try {
      const data = await chunkStrategyService.list({
        page,
        pageSize: size,
        keyword: keyword || undefined,
      });
      setStrategies(data.items);
      setTotal(data.total);
      setCurrent(data.page);
    } catch (err: any) {
      toast.error(err?.message || '加载分块策略失败');
    } finally {
      setLoading(false);
    }
  }, [pageSize]);

  useEffect(() => {
    loadStrategies(1, searchKeyword, pageSize);
  }, [loadStrategies, pageSize, searchKeyword]);

  const openCreateModal = () => {
    setEditingStrategy(null);
    setFormData(INITIAL_FORM);
    setFormErrors({});
    setModalVisible(true);
  };

  const openEditModal = (strategy: ChunkStrategy) => {
    setEditingStrategy(strategy);
    setFormData({
      strategyName: strategy.strategyName,
      chunkType: strategy.chunkType,
      maxSegmentSize: strategy.maxSegmentSize,
      overlapSize: strategy.overlapSize,
    });
    setFormErrors({});
    setModalVisible(true);
  };

  const closeModal = () => {
    if (submitting) return;
    setModalVisible(false);
    setEditingStrategy(null);
  };

  const handleFieldChange = (
    field: keyof CreateChunkStrategyReq,
    value: string | number,
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (formErrors[field]) {
      setFormErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  const handleSubmit = async () => {
    const errors = validateForm(formData);
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    const payload: CreateChunkStrategyReq = {
      strategyName: formData.strategyName.trim(),
      chunkType: formData.chunkType,
      maxSegmentSize: formData.maxSegmentSize,
      overlapSize: formData.overlapSize,
    };

    setSubmitting(true);
    try {
      if (editingStrategy) {
        const updatePayload: UpdateChunkStrategyReq = payload;
        await chunkStrategyService.update(editingStrategy.id, updatePayload);
        toast.success('分块策略已更新');
      } else {
        await chunkStrategyService.create(payload);
        toast.success('分块策略已创建');
      }
      setModalVisible(false);
      setEditingStrategy(null);
      loadStrategies(editingStrategy ? current : 1, searchKeyword, pageSize);
    } catch (err: any) {
      toast.error(err?.message || (editingStrategy ? '更新分块策略失败' : '创建分块策略失败'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await chunkStrategyService.delete(deleteTarget.id);
      toast.success('分块策略已删除');
      setDeleteTarget(null);
      loadStrategies(current, searchKeyword, pageSize);
    } catch (err: any) {
      toast.error(err?.message || '删除分块策略失败');
    } finally {
      setDeleting(false);
    }
  };

  const handleSetDefault = async (strategy: ChunkStrategy) => {
    if (strategy.isDefault) return;
    setDefaultingId(strategy.id);
    try {
      await chunkStrategyService.setDefault(strategy.id);
      toast.success(`已将 "${strategy.strategyName}" 设为默认策略`);
      loadStrategies(current, searchKeyword, pageSize);
    } catch (err: any) {
      toast.error(err?.message || '设为默认策略失败');
    } finally {
      setDefaultingId(null);
    }
  };

  const handleSearch = () => {
    loadStrategies(1, searchKeyword, pageSize);
  };

  const handleReset = () => {
    setSearchKeyword('');
    loadStrategies(1, '', pageSize);
  };

  const handlePageSizeChange = (size: number) => {
    setPageSize(size);
  };

  const columns = [
    {
      key: 'strategyName',
      title: '策略名称',
      render: (value: string, record: ChunkStrategy) => (
        <div>
          <div className="font-medium text-gray-900">{value}</div>
          {record.isDefault && (
            <div className="text-xs text-yellow-700 mt-1 flex items-center">
              <Star className="h-3 w-3 mr-1 fill-yellow-500 text-yellow-500" />
              默认策略
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'chunkType',
      title: '分块类型',
      width: '140px',
      render: (value: ChunkType) => (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
          {value}
        </span>
      ),
    },
    {
      key: 'maxSegmentSize',
      title: '最大字符数',
      width: '120px',
    },
    {
      key: 'overlapSize',
      title: '重叠字符数',
      width: '120px',
    },
    {
      key: 'createTime',
      title: '创建时间',
      width: '180px',
      render: (value: string) => (
        <span className="text-sm text-gray-600">{formatDateTime(value)}</span>
      ),
    },
    {
      key: 'actions',
      title: '操作',
      width: '260px',
      render: (_: unknown, record: ChunkStrategy) => (
        <div className="flex items-center space-x-3">
          <button
            onClick={() => openEditModal(record)}
            className="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center"
          >
            <Edit className="h-4 w-4 mr-1" />
            编辑
          </button>
          <button
            onClick={() => handleSetDefault(record)}
            disabled={record.isDefault || defaultingId === record.id}
            className={`text-sm font-medium flex items-center disabled:cursor-not-allowed ${
              record.isDefault
                ? 'text-yellow-600'
                : 'text-gray-600 hover:text-gray-800 disabled:opacity-50'
            }`}
          >
            <Star className={`h-4 w-4 mr-1 ${record.isDefault ? 'fill-yellow-500' : ''}`} />
            {record.isDefault ? '已默认' : '设为默认'}
          </button>
          <button
            onClick={() => setDeleteTarget(record)}
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
          <h1 className="text-2xl font-bold text-gray-900">分块策略</h1>
          <p className="text-gray-600 mt-1">管理文档入库时使用的文本分块策略和默认策略</p>
        </div>
        <button onClick={openCreateModal} className="btn-primary">
          <Plus className="h-4 w-4 mr-2" />
          新增策略
        </button>
      </div>

      <div className="card">
        <div className="flex items-center space-x-4">
          <SearchBar
            placeholder="搜索策略名称或分块类型"
            value={searchKeyword}
            onChange={setSearchKeyword}
            onSearch={handleSearch}
            className="flex-1"
          />
          <button onClick={handleSearch} className="btn-primary">
            <Search className="h-4 w-4 mr-2" />
            搜索
          </button>
          <button onClick={handleReset} className="btn-secondary">
            重置
          </button>
          <button
            onClick={() => loadStrategies(current, searchKeyword, pageSize)}
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
          data={strategies}
          loading={loading}
          emptyText="暂无分块策略"
        />
        {!loading && strategies.length > 0 && (
          <div className="mt-4">
            <Pagination
              current={current}
              total={total}
              pageSize={pageSize}
              onChange={(page) => loadStrategies(page, searchKeyword, pageSize)}
              showSizeChanger
              onShowSizeChange={handlePageSizeChange}
              showTotal
            />
          </div>
        )}
      </div>

      <FormModal
        isOpen={modalVisible}
        onClose={closeModal}
        title={editingStrategy ? '编辑分块策略' : '新增分块策略'}
        width="max-w-lg"
        onSubmit={handleSubmit}
        loading={submitting}
        submitText={editingStrategy ? '保存' : '新增'}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              策略名称 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              className={`form-input ${formErrors.strategyName ? 'border-red-400 focus:ring-red-500' : ''}`}
              value={formData.strategyName}
              onChange={(e) => handleFieldChange('strategyName', e.target.value)}
              maxLength={100}
            />
            {formErrors.strategyName && (
              <p className="text-xs text-red-500 mt-1">{formErrors.strategyName}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              分块类型 <span className="text-red-500">*</span>
            </label>
            <select
              className={`form-input ${formErrors.chunkType ? 'border-red-400 focus:ring-red-500' : ''}`}
              value={formData.chunkType}
              onChange={(e) => handleFieldChange('chunkType', e.target.value as ChunkType)}
            >
              {CHUNK_TYPE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {formErrors.chunkType && (
              <p className="text-xs text-red-500 mt-1">{formErrors.chunkType}</p>
            )}
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                最大字符数 <span className="text-red-500">*</span>
                <span className="ml-2 text-xs font-normal text-gray-400">建议 500-1200</span>
              </label>
              <input
                type="number"
                min={1}
                className={`form-input ${formErrors.maxSegmentSize ? 'border-red-400 focus:ring-red-500' : ''}`}
                value={formData.maxSegmentSize}
                onChange={(e) => handleFieldChange('maxSegmentSize', Number(e.target.value))}
              />
              {formErrors.maxSegmentSize && (
                <p className="text-xs text-red-500 mt-1">{formErrors.maxSegmentSize}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                重叠字符数 <span className="text-red-500">*</span>
                <span className="ml-2 text-xs font-normal text-gray-400">建议为最大值的 10%-30%</span>
              </label>
              <input
                type="number"
                min={0}
                className={`form-input ${formErrors.overlapSize ? 'border-red-400 focus:ring-red-500' : ''}`}
                value={formData.overlapSize}
                onChange={(e) => handleFieldChange('overlapSize', Number(e.target.value))}
              />
              {formErrors.overlapSize && (
                <p className="text-xs text-red-500 mt-1">{formErrors.overlapSize}</p>
              )}
            </div>
          </div>
        </div>
      </FormModal>

      <ConfirmModal
        isOpen={!!deleteTarget}
        title="删除分块策略"
        message={`确定要删除分块策略 "${deleteTarget?.strategyName}" 吗？此操作不可撤销。`}
        confirmText="确定删除"
        danger
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
};

export default ChunkStrategyList;
