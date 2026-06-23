import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { Layout } from '@/components/layout/Layout';
import { useAuthStore } from '@/stores/authStore';

// 认证页面
import LoginPage from '@/pages/auth/LoginPage';
import RegisterPage from '@/pages/auth/RegisterPage';

// 知识库管理
import KnowledgeBaseList from '@/pages/knowledge-base/KnowledgeBaseList';
import KnowledgeBaseDetail from '@/pages/knowledge-base/KnowledgeBaseDetail';

// 模型提供商管理
import ModelProviderList from '@/pages/model-provider/ModelProviderList';
import ModelList from '@/pages/model-provider/ModelList';

// 文档管理
import DocumentDetail from '@/pages/knowledge-base/DocumentDetail';

// RAG问答
import QAPage from '@/pages/qa/QAPage';

// 企业服务台
import ServiceDeskPage from '@/pages/service-desk/ServiceDeskPage';

// RAG评测
import EvaluationDatasetList from '@/pages/evaluation/EvaluationDatasetList';
import EvaluationDatasetDetail from '@/pages/evaluation/EvaluationDatasetDetail';
import EvaluationRunDetail from '@/pages/evaluation/EvaluationRunDetail';

import ChunkStrategyList from '@/pages/chunk-strategy/ChunkStrategyList';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

export const AppRouter: React.FC = () => {
  return (
    <Routes>
      {/* 登录页面 - 公开路由 */}
      <Route path="/login" element={
        <PublicRoute>
          <LoginPage />
        </PublicRoute>
      } />

      <Route path="/register" element={
        <PublicRoute>
          <RegisterPage />
        </PublicRoute>
      } />

      {/* 受保护的路由 */}
      <Route path="/" element={
        <ProtectedRoute>
          <Layout />
        </ProtectedRoute>
      }>
        {/* 知识库管理 */}
        <Route path="knowledge-bases" element={<KnowledgeBaseList />} />
        <Route path="knowledge-bases/:id" element={<KnowledgeBaseDetail />} />
        <Route path="documents/:id" element={<DocumentDetail />} />
        
        {/* 模型提供商管理 */}
        <Route path="model-providers" element={<ModelProviderList />} />
        <Route path="model-providers/:id/models" element={<ModelList />} />
        
        {/* RAG问答 */}
        <Route path="qa" element={<QAPage />} />

        {/* 企业服务台 */}
        <Route path="service-desk" element={<ServiceDeskPage />} />

        {/* RAG评测 */}
        <Route path="evaluations" element={<EvaluationDatasetList />} />
        <Route path="evaluations/datasets/:id" element={<EvaluationDatasetDetail />} />
        <Route path="evaluations/runs/:runId" element={<EvaluationRunDetail />} />
        
        <Route path="chunk-strategies" element={<ChunkStrategyList />} />
        
        {/* 默认重定向到知识库管理 */}
        <Route index element={<KnowledgeBaseList />} />
      </Route>

      {/* 404 重定向到首页 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};
