import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Edit, Play, Plus, RefreshCw, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { ConfirmModal, DataTable, FormModal, Pagination } from '@/components/common';
import { evaluationService } from '@/services/evaluation';
import { EvaluationCase, EvaluationCaseReq, EvaluationDataset } from '@/types/evaluation';
import { formatDateTime } from '@/utils/format';

/** 新建用例表单默认值。 */
const INITIAL_CASE: EvaluationCaseReq = {
  question: '',
  expectedAnswer: '',
  expectedKeywords: [],
  expectedChunkIds: [],
  expectedStatus: '',
  metadata: '',
  enabled: true,
};

/** 将多行或逗号分隔文本拆成数组。 */
const splitText = (value: string): string[] =>
  value.split(/[\n,，]/).map((item) => item.trim()).filter(Boolean);

/** 评测集详情和用例管理页。 */
const EvaluationDatasetDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [dataset, setDataset] = useState<EvaluationDataset | null>(null);
  const [cases, setCases] = useState<EvaluationCase[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [caseModalOpen, setCaseModalOpen] = useState(false);
  const [editingCase, setEditingCase] = useState<EvaluationCase | null>(null);
  const [caseForm, setCaseForm] = useState<EvaluationCaseReq>(INITIAL_CASE);
  const [keywordsText, setKeywordsText] = useState('');
  const [chunkIdsText, setChunkIdsText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<EvaluationCase | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [running, setRunning] = useState(false);

  /** 加载评测集摘要。 */
  const loadDataset = useCallback(async () => {
    if (!id) return;
    try {
      setDataset(await evaluationService.getDataset(id));
    } catch (error: any) {
      toast.error(error?.message || '加载评测集失败');
      navigate('/evaluations');
    }
  }, [id, navigate]);

  /** 加载用例分页数据。 */
  const loadCases = useCallback(async (nextPage = page, size = pageSize) => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await evaluationService.listCases(id, { page: nextPage, pageSize: size });
      setCases(data.items);
      setTotal(data.total);
      setPage(data.page);
    } catch (error: any) {
      toast.error(error?.message || '加载评测用例失败');
    } finally {
      setLoading(false);
    }
  }, [id, page, pageSize]);

  useEffect(() => {
    loadDataset();
    loadCases(1, pageSize);
  }, [id]);

  /** 打开新建用例弹窗。 */
  const openCreateCase = () => {
    setEditingCase(null);
    setCaseForm(INITIAL_CASE);
    setKeywordsText('');
    setChunkIdsText('');
    setCaseModalOpen(true);
  };

  /** 打开编辑用例弹窗。 */
  const openEditCase = (item: EvaluationCase) => {
    setEditingCase(item);
    setCaseForm({
      question: item.question,
      expectedAnswer: item.expectedAnswer || '',
      expectedKeywords: item.expectedKeywords,
      expectedChunkIds: item.expectedChunkIds,
      expectedStatus: item.expectedStatus || '',
      metadata: item.metadata || '',
      enabled: item.enabled,
    });
    setKeywordsText(item.expectedKeywords.join('\n'));
    setChunkIdsText(item.expectedChunkIds.join('\n'));
    setCaseModalOpen(true);
  };

  /** 保存用例表单。 */
  const handleCaseSubmit = async () => {
    if (!id) return;
    if (!caseForm.question?.trim()) {
      toast.error('请输入评测问题');
      return;
    }
    const payload: EvaluationCaseReq = {
      ...caseForm,
      question: caseForm.question.trim(),
      expectedKeywords: splitText(keywordsText),
      expectedChunkIds: splitText(chunkIdsText),
    };
    setSubmitting(true);
    try {
      if (editingCase) {
        await evaluationService.updateCase(editingCase.id, payload);
        toast.success('用例已更新');
      } else {
        await evaluationService.createCase(id, payload);
        toast.success('用例已创建');
      }
      setCaseModalOpen(false);
      loadDataset();
      loadCases(editingCase ? page : 1, pageSize);
    } catch (error: any) {
      toast.error(error?.message || '保存用例失败');
    } finally {
      setSubmitting(false);
    }
  };

  /** 删除当前选中的用例。 */
  const handleDeleteCase = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await evaluationService.deleteCase(deleteTarget.id);
      toast.success('用例已删除');
      setDeleteTarget(null);
      loadDataset();
      loadCases(page, pageSize);
    } catch (error: any) {
      toast.error(error?.message || '删除用例失败');
    } finally {
      setDeleting(false);
    }
  };

  /** 手动运行当前评测集。 */
  const handleRun = async () => {
    if (!id) return;
    setRunning(true);
    try {
      const runId = await evaluationService.runDataset(id);
      toast.success('评测运行已完成');
      navigate(`/evaluations/runs/${runId}`);
    } catch (error: any) {
      toast.error(error?.message || '运行评测失败');
    } finally {
      setRunning(false);
    }
  };

  const columns = [
    {
      key: 'question',
      title: '问题',
      render: (value: string, record: EvaluationCase) => (
        <div className="max-w-xl">
          <div className="font-medium text-gray-900 whitespace-normal">{value}</div>
          <div className="text-xs text-gray-500 mt-1 whitespace-normal">
            关键词：{record.expectedKeywords.length ? record.expectedKeywords.join('、') : '-'}
          </div>
        </div>
      ),
    },
    {
      key: 'expectedStatus',
      title: '期望状态',
      width: '120px',
      render: (value: string) => value || '-',
    },
    {
      key: 'expectedChunkIds',
      title: '期望 Chunk',
      width: '160px',
      render: (value: string[]) => value.length ? value.join(', ') : '-',
    },
    {
      key: 'enabled',
      title: '启用',
      width: '80px',
      render: (value: boolean) => value ? '是' : '否',
    },
    {
      key: 'createTime',
      title: '创建时间',
      width: '170px',
      render: (value: string) => formatDateTime(value),
    },
    {
      key: 'actions',
      title: '操作',
      width: '170px',
      render: (_: unknown, record: EvaluationCase) => (
        <div className="flex items-center gap-3">
          <button type="button" onClick={() => openEditCase(record)} className="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center">
            <Edit className="h-4 w-4 mr-1" />
            编辑
          </button>
          <button type="button" onClick={() => setDeleteTarget(record)} className="text-red-600 hover:text-red-800 text-sm font-medium flex items-center">
            <Trash2 className="h-4 w-4 mr-1" />
            删除
          </button>
        </div>
      ),
    },
  ];

  if (!dataset) {
    return <div className="text-center py-12 text-gray-500">加载评测集...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button type="button" onClick={() => navigate('/evaluations')} className="text-gray-600 hover:text-gray-800 mr-4">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{dataset.name}</h1>
            <p className="text-gray-600 mt-1">{dataset.description || '暂无描述'}</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button type="button" onClick={() => loadCases(page, pageSize)} className="btn-secondary" disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
            刷新
          </button>
          <button type="button" onClick={openCreateCase} className="btn-secondary">
            <Plus className="h-4 w-4 mr-2" />
            新增用例
          </button>
          <button type="button" onClick={handleRun} className="btn-primary" disabled={running}>
            <Play className="h-4 w-4 mr-2" />
            {running ? '运行中' : '运行评测'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="card">
          <div className="text-sm text-gray-500">用例数量</div>
          <div className="text-2xl font-semibold text-gray-900 mt-1">{dataset.caseCount}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500">最近得分</div>
          <div className="text-2xl font-semibold text-gray-900 mt-1">{dataset.lastAvgScore != null ? dataset.lastAvgScore.toFixed(1) : '-'}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500">最近状态</div>
          <div className="text-lg font-medium text-gray-900 mt-1">{dataset.lastRunStatus || '-'}</div>
        </div>
        <div className="card">
          <div className="text-sm text-gray-500">最近运行</div>
          <div className="text-lg font-medium text-primary-700 mt-1">
            {dataset.lastRunId ? <Link to={`/evaluations/runs/${dataset.lastRunId}`}>查看结果</Link> : '-'}
          </div>
        </div>
      </div>

      <div className="card">
        <DataTable columns={columns} data={cases} loading={loading} emptyText="暂无评测用例" />
        {total > 0 && (
          <div className="mt-4">
            <Pagination
              current={page}
              total={total}
              pageSize={pageSize}
              onChange={(nextPage) => loadCases(nextPage, pageSize)}
              showSizeChanger
              onShowSizeChange={(size) => setPageSize(size)}
            />
          </div>
        )}
      </div>

      <FormModal
        isOpen={caseModalOpen}
        onClose={() => setCaseModalOpen(false)}
        title={editingCase ? '编辑评测用例' : '新增评测用例'}
        width="max-w-2xl"
        onSubmit={handleCaseSubmit}
        loading={submitting}
        submitText={editingCase ? '保存' : '新增'}
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">问题</label>
            <textarea className="form-input min-h-[90px]" value={caseForm.question} onChange={(e) => setCaseForm({ ...caseForm, question: e.target.value })} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">标准答案</label>
            <textarea className="form-input min-h-[90px]" value={caseForm.expectedAnswer || ''} onChange={(e) => setCaseForm({ ...caseForm, expectedAnswer: e.target.value })} />
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">期望关键词</label>
              <textarea className="form-input min-h-[90px]" placeholder="每行一个，或用逗号分隔" value={keywordsText} onChange={(e) => setKeywordsText(e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">期望 Chunk ID</label>
              <textarea className="form-input min-h-[90px]" placeholder="每行一个，或用逗号分隔" value={chunkIdsText} onChange={(e) => setChunkIdsText(e.target.value)} />
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">期望状态</label>
              <select className="form-input" value={caseForm.expectedStatus || ''} onChange={(e) => setCaseForm({ ...caseForm, expectedStatus: e.target.value })}>
                <option value="">不校验</option>
                <option value="success">success</option>
                <option value="no_results">no_results</option>
              </select>
            </div>
            <label className="flex items-center gap-2 text-sm text-gray-700 mt-8">
              <input type="checkbox" checked={caseForm.enabled !== false} onChange={(e) => setCaseForm({ ...caseForm, enabled: e.target.checked })} />
              启用该用例
            </label>
          </div>
        </div>
      </FormModal>

      <ConfirmModal
        isOpen={!!deleteTarget}
        title="删除评测用例"
        message="确定删除该评测用例吗？相关历史结果也会被删除。"
        confirmText="确定删除"
        danger
        onConfirm={handleDeleteCase}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
};

export default EvaluationDatasetDetail;
