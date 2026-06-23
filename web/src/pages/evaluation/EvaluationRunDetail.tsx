import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';
import { DataTable, Pagination } from '@/components/common';
import { evaluationService } from '@/services/evaluation';
import { EvaluationCaseResult, EvaluationRun } from '@/types/evaluation';

/** 评测运行详情页。 */
const EvaluationRunDetail: React.FC = () => {
  const { runId } = useParams<{ runId: string }>();
  const navigate = useNavigate();
  const [run, setRun] = useState<EvaluationRun | null>(null);
  const [results, setResults] = useState<EvaluationCaseResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);

  /** 同时加载运行汇总和单条结果。 */
  const loadData = useCallback(async (nextPage = page, size = pageSize) => {
    if (!runId) return;
    setLoading(true);
    try {
      const [runData, resultData] = await Promise.all([
        evaluationService.getRun(runId),
        evaluationService.listRunResults(runId, { page: nextPage, pageSize: size }),
      ]);
      setRun(runData);
      setResults(resultData.items);
      setTotal(resultData.total);
      setPage(resultData.page);
    } catch (error: any) {
      toast.error(error?.message || '加载评测运行失败');
      navigate('/evaluations');
    } finally {
      setLoading(false);
    }
  }, [navigate, page, pageSize, runId]);

  useEffect(() => {
    loadData(1, pageSize);
  }, [runId]);

  const columns = [
    {
      key: 'question',
      title: '用例',
      render: (value: string, record: EvaluationCaseResult) => (
        <div className="max-w-xl whitespace-normal">
          <div className="font-medium text-gray-900">{value}</div>
          <div className="text-xs text-gray-500 mt-1">
            {record.expectedStatus ? `期望状态：${record.expectedStatus}` : '未配置状态校验'}
          </div>
        </div>
      ),
    },
    {
      key: 'totalScore',
      title: '总分',
      width: '90px',
      render: (value: number, record: EvaluationCaseResult) => (
        <span className={record.passed ? 'text-emerald-600 font-semibold' : 'text-red-600 font-semibold'}>
          {value.toFixed(1)}
        </span>
      ),
    },
    {
      key: 'latencyMs',
      title: '耗时(ms)',
      width: '100px',
      render: (value: number) => value ?? '-',
    },
    {
      key: 'actualStatus',
      title: '状态',
      width: '100px',
      render: (value: string) => value || '-',
    },
    {
      key: 'actions',
      title: '详情',
      width: '120px',
      render: (_: unknown, record: EvaluationCaseResult) => (
        <details className="group">
          <summary className="cursor-pointer text-primary-700 text-sm">展开</summary>
          <div className="mt-3 space-y-2 text-xs text-gray-700 whitespace-normal">
            <div>改写：{record.rewrittenQuery || '-'}</div>
            <div>答案：{record.actualAnswer || '-'}</div>
            <div>召回：{record.retrievedChunks.map((chunk) => `${chunk.chunkId}`).join(', ') || '-'}</div>
            <div>重排：{record.rerankedChunks.map((chunk) => `${chunk.chunkId}`).join(', ') || '-'}</div>
            <div>评分：召回 {fmt(record.retrievalHitScore)} / 关键词 {fmt(record.keywordScore)} / 状态 {fmt(record.statusScore)}</div>
            <div>明细：{JSON.stringify(record.metricDetail)}</div>
          </div>
        </details>
      ),
    },
  ];

  if (!run) {
    return <div className="text-center py-12 text-gray-500">加载运行结果...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center">
          <button type="button" onClick={() => navigate(-1)} className="text-gray-600 hover:text-gray-800 mr-4">
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{run.name}</h1>
            <p className="text-gray-600 mt-1">评测运行详情</p>
          </div>
        </div>
        <button type="button" onClick={() => loadData(page, pageSize)} className="btn-secondary" disabled={loading}>
          <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
          刷新
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <div className="card"><div className="text-sm text-gray-500">总用例</div><div className="text-2xl font-semibold">{run.totalCount}</div></div>
        <div className="card"><div className="text-sm text-gray-500">通过</div><div className="text-2xl font-semibold text-emerald-600">{run.passedCount}</div></div>
        <div className="card"><div className="text-sm text-gray-500">失败</div><div className="text-2xl font-semibold text-red-600">{run.failedCount}</div></div>
        <div className="card"><div className="text-sm text-gray-500">平均分</div><div className="text-2xl font-semibold">{run.avgScore != null ? run.avgScore.toFixed(1) : '-'}</div></div>
        <div className="card"><div className="text-sm text-gray-500">平均耗时(ms)</div><div className="text-2xl font-semibold">{run.avgLatencyMs ?? '-'}</div></div>
      </div>

      {run.errorMessage && (
        <div className="card border-red-200 bg-red-50 text-red-700">
          {run.errorMessage}
        </div>
      )}

      <div className="card">
        <DataTable columns={columns} data={results} loading={loading} emptyText="暂无运行结果" />
        {total > 0 && (
          <div className="mt-4">
            <Pagination
              current={page}
              total={total}
              pageSize={pageSize}
              onChange={(nextPage) => loadData(nextPage, pageSize)}
              showSizeChanger
              onShowSizeChange={(size) => setPageSize(size)}
            />
          </div>
        )}
      </div>

      <div className="card">
        <Link className="text-primary-700 hover:text-primary-900" to={`/evaluations/datasets/${run.datasetId}`}>
          返回评测集
        </Link>
      </div>
    </div>
  );
};

/** 格式化可空分数。 */
function fmt(value?: number): string {
  return value == null ? '-' : value.toFixed(1);
}

export default EvaluationRunDetail;
