import React from 'react';
import { Routes, Route } from 'react-router-dom';
import { Layout } from '@/components/layout/Layout';

// 知识库管理
import KnowledgeBaseList from '@/pages/knowledge-base/KnowledgeBaseList';
import KnowledgeBaseDetail from '@/pages/knowledge-base/KnowledgeBaseDetail';

// 模型提供商管理
import ModelProviderList from '@/pages/model-provider/ModelProviderList';
import ModelList from '@/pages/model-provider/ModelList';

// 工具管理
import ToolList from '@/pages/tools/ToolList';

// Agent管理
import AgentList from '@/pages/agents/AgentList';
import AgentRuns from '@/pages/agents/AgentRuns';
import AgentRunDetail from '@/pages/agents/AgentRunDetail';

// 工作流管理
import WorkflowList from '@/pages/workflows/WorkflowList';
import WorkflowDetail from '@/pages/workflows/WorkflowDetail';

// 文档管理
import DocumentDetail from '@/pages/knowledge-base/DocumentDetail';

// RAG问答
import QAPage from '@/pages/qa/QAPage';

// 定时任务
import JobList from '@/pages/jobs/JobList';
import JobDetail from '@/pages/jobs/JobDetail';

export const AppRouter: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        {/* 知识库管理 */}
        <Route path="knowledge-bases" element={<KnowledgeBaseList />} />
        <Route path="knowledge-bases/:id" element={<KnowledgeBaseDetail />} />
        <Route path="documents/:id" element={<DocumentDetail />} />
        
        {/* 模型提供商管理 */}
        <Route path="model-providers" element={<ModelProviderList />} />
        <Route path="model-providers/:id/models" element={<ModelList />} />
        
        {/* 工具管理 */}
        <Route path="tools" element={<ToolList />} />
        
        {/* Agent管理 */}
        <Route path="agents" element={<AgentList />} />
        <Route path="agents/:id/runs" element={<AgentRuns />} />
        <Route path="agents/runs/:runId" element={<AgentRunDetail />} />
        
        {/* 工作流管理 */}
        <Route path="workflows" element={<WorkflowList />} />
        <Route path="workflows/:id" element={<WorkflowDetail />} />
        
        {/* RAG问答 */}
        <Route path="qa" element={<QAPage />} />
        
        {/* 定时任务 */}
        <Route path="scheduler/jobs" element={<JobList />} />
        <Route path="scheduler/jobs/:id" element={<JobDetail />} />
        
        {/* 默认重定向到知识库管理 */}
        <Route index element={<KnowledgeBaseList />} />
      </Route>
    </Routes>
  );
};