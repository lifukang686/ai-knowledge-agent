import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { BarChart3, Play, Plus, RefreshCw, Search, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { ConfirmModal, DataTable, FormModal, Pagination, SearchBar } from '@/components/common';
import { evaluationService } from '@/services/evaluation';
import { getKnowledgeBases } from '@/services/knowledgeBase';
import { EvaluationDataset, EvaluationDatasetReq } from '@/types/evaluation';
import { KnowledgeBase } from '@/types/knowledgeBase';
import { formatDateTime } from '@/utils/format';

/** 新建评测集表单默认值。 */
const INITIAL_FORM: EvaluationDatasetReq = {
  name: '',
  description: '',
  knowledgeBaseId: '',
};

/** 评测集列表页。 */
const EvaluationDatasetList: React.FC = () => {
  const navigate = useNavigate();
  const [datasets, setDatasets] = useState<EvaluationDataset[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [loading, setLoading] = useState(false);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [formData, setFormData] = useState<EvaluationDatasetReq>(INITIAL_FORM);
  const [submitting, setSubmitting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<EvaluationDataset | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [runningId, setRunningId] = useState<string | null>(null);

  /** 加载评测集分页数据。 */
  const loadDatasets = useCallback(async (page = current, size = pageSize) => {
    setLoading(true);
    try {
      const data = await evaluationService.listDatasets({
        page,
        pageSize: size,
        keyword: keyword || undefined,
      });
      setDatasets(data.items);
      setTotal(data.total);
      setCurrent(data.page);
    } catch (error: any) {
      toast.error(error?.message || '加载评测集失败');
    } finally {
      setLoading(false);
    }
  }, [current, keyword, pageSize]);

  /** 加载知识库下拉数据。 */
  const loadKnowledgeBases = useCallback(async () => {
    try {
      const data = await getKnowledgeBases({ page: 1, pageSize: 100 });
      setKnowledgeBases(data.items);
    } catch {
      toast.error('加载知识库失败');
    }
  }, []);

  useEffect(() => {
    loadDatasets(1, pageSize);
    loadKnowledgeBases();
  }, []);

  /** 打开新建评测集弹窗。 */
  const openCreate = () => {
    setFormData(INITIAL_FORM);
    setModalOpen(true);
  };

  /** 提交新建评测集。 */
  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      toast.error('请输入评测集名称');
      return;
    }
    setSubmitting(true);
    try {
      const id = await evaluationService.createDataset({
        ...formData,
        name: formData.name.trim(),
      });
      toast.success('评测集已创建');
      setModalOpen(false);
      navigate(`/evaluations/datasets/${id}`);
    } catch (error: any) {
      toast.error(error?.message || '创建评测集失败');
    } finally {
      setSubmitting(false);
    }
  };

  /** 同步运行评测集并跳转结果页。 */
  const handleRun = async (dataset: EvaluationDataset) => {
    setRunningId(dataset.id);
    try {
      const runId = await evaluationService.runDataset(dataset.id);
      toast.success('评测运行已完成');
      navigate(`/evaluations/runs/${runId}`);
    } catch (error: any) {
      toast.error(error?.message || '运行评测失败');
    } finally {
      setRunningId(null);
    }
  };

  /** 删除评测集及其关联数据。 */
  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await evaluationService.deleteDataset(deleteTarget.id);
      toast.success('评测集已删除');
      setDeleteTarget(null);
      loadDatasets(current, pageSize);
    } catch (error: any) {
      toast.error(error?.message || '删除评测集失败');
    } finally {
      setDeleting(false);
    }
  };

  const columns = [
    {
      key: 'name',
      title: '评测集',
      render: (value: string, record: EvaluationDataset) => (
        <div>
          <Link to={`/evaluations/datasets/${record.id}`} className="font-medium text-primary-700 hover:text-primary-900">
            {value}
          </Link>
          <div className="text-xs text-gray-500 mt-1">{record.description || '暂无描述'}</div>
        </div>
      ),
    },
    {
      key: 'knowledgeBaseId',
      title: '知识库',
      width: '180px',
      render: (value: string) => knowledgeBases.find((kb) => kb.id === value)?.name || value || '-',
    },
    {
      key: 'caseCount',
      title: '用例数',
      width: '90px',
    },
    {
      key: 'lastAvgScore',
      title: '最近得分',
      width: '120px',
      render: (value: number, record: EvaluationDataset) => (
        <span className={record.lastRunStatus === 'FAILED' ? 'text-red-600' : 'text-gray-700'}>
          {value != null ? value.toFixed(1) : '-'}
        </span>
      ),
    },
    {
      key: 'createTime',
      title: '创建时间',
      width: '180px',
      render: (value: string) => formatDateTime(value),
    },
    {
      key: 'actions',
      title: '操作',
      width: '240px',
      render: (_: unknown, record: EvaluationDataset) => (
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => handleRun(record)}
            disabled={runningId === record.id}
            className="text-emerald-600 hover:text-emerald-800 text-sm font-medium flex items-center disabled:opacity-50"
          >
            <Play className="h-4 w-4 mr-1" />
            {runningId === record.id ? '运行中' : '运行'}
          </button>
          <Link to={`/evaluations/datasets/${record.id}`} className="text-blue-600 hover:text-blue-800 text-sm font-medium">
            详情
          </Link>
          <button
            type="button"
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
          <h1 className="text-2xl font-bold text-gray-900 flex items-center">
            <BarChart3 className="h-6 w-6 mr-2 text-primary-600" />
            评测中心
          </h1>
          <p className="text-gray-600 mt-1">管理 RAG 问答评测集，批量验证召回和回答质量</p>
        </div>
        <button type="button" onClick={openCreate} className="btn-primary">
          <Plus className="h-4 w-4 mr-2" />
          新建评测集
        </button>
      </div>

      <div className="card">
        <div className="flex items-center space-x-4">
          <SearchBar
            placeholder="搜索评测集名称或描述"
            value={keyword}
            onChange={setKeyword}
            onSearch={() => loadDatasets(1, pageSize)}
            className="flex-1"
          />
          <button type="button" onClick={() => loadDatasets(1, pageSize)} className="btn-primary">
            <Search className="h-4 w-4 mr-2" />
            搜索
          </button>
          <button type="button" onClick={() => { setKeyword(''); loadDatasets(1, pageSize); }} className="btn-secondary">
            重置
          </button>
          <button type="button" onClick={() => loadDatasets(current, pageSize)} className="btn-secondary" disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            刷新
          </button>
        </div>
      </div>

      <div className="card">
        <DataTable columns={columns} data={datasets} loading={loading} emptyText="暂无评测集" />
        {total > 0 && (
          <div className="mt-4">
            <Pagination
              current={current}
              total={total}
              pageSize={pageSize}
              onChange={(page) => loadDatasets(page, pageSize)}
              showSizeChanger
              onShowSizeChange={(size) => setPageSize(size)}
            />
          </div>
        )}
      </div>

      <FormModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title="新建评测集"
        width="max-w-lg"
        onSubmit={handleSubmit}
        loading={submitting}
        submitText="创建"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">名称</label>
            <input className="form-input" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">知识库</label>
            <select className="form-input" value={formData.knowledgeBaseId || ''} onChange={(e) => setFormData({ ...formData, knowledgeBaseId: e.target.value })}>
              <option value="">不限知识库</option>
              {knowledgeBases.map((kb) => <option key={kb.id} value={kb.id}>{kb.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">描述</label>
            <textarea className="form-input min-h-[90px]" value={formData.description || ''} onChange={(e) => setFormData({ ...formData, description: e.target.value })} />
          </div>
        </div>
      </FormModal>

      <ConfirmModal
        isOpen={!!deleteTarget}
        title="删除评测集"
        message={`确定删除评测集 "${deleteTarget?.name}" 吗？相关用例和运行结果也会被删除。`}
        confirmText="确定删除"
        danger
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
};

export default EvaluationDatasetList;
