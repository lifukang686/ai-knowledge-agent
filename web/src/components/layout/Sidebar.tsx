import React, { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  BarChart3,
  Bot,
  Brain,
  ChevronDown,
  ChevronRight,
  Database,
  MessageSquare,
  SlidersHorizontal,
} from 'lucide-react';

const menuItems = [
  { path: '/knowledge-bases', icon: Database, label: '知识库管理' },
  { path: '/model-providers', icon: Brain, label: '模型提供商' },
  { path: '/qa', icon: MessageSquare, label: 'RAG问答' },
  { path: '/service-desk', icon: Bot, label: '服务台Agent' },
  { path: '/evaluations', icon: BarChart3, label: '评测中心' },
];

export const Sidebar: React.FC = () => {
  const location = useLocation();
  const [strategyOpen, setStrategyOpen] = useState(false);

  const isActive = (path: string) => location.pathname.startsWith(path);

  useEffect(() => {
    if (isActive('/chunk-strategies')) {
      setStrategyOpen(true);
    }
  }, [location.pathname]);

  return (
    <aside className="w-64 bg-white shadow-sm border-r border-gray-200 flex-shrink-0 overflow-y-auto">
      <div className="p-6">
        <h1 className="text-xl font-bold text-gray-900 mb-8">知识库AI平台</h1>

        <nav className="space-y-2">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.path);

            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                  active
                    ? 'bg-primary-100 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`}
              >
                <Icon className="mr-3 h-5 w-5" />
                {item.label}
              </Link>
            );
          })}

          <div>
            <button
              type="button"
              onClick={() => setStrategyOpen((open) => !open)}
              className={`w-full flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                isActive('/chunk-strategies')
                  ? 'bg-primary-100 text-primary-700'
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
              }`}
            >
              <SlidersHorizontal className="mr-3 h-5 w-5" />
              <span className="flex-1 text-left">策略管理</span>
              {strategyOpen ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </button>

            {strategyOpen && (
              <div className="mt-1 ml-8 space-y-1">
                <Link
                  to="/chunk-strategies"
                  className={`block px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                    isActive('/chunk-strategies')
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                  }`}
                >
                  分块策略
                </Link>
              </div>
            )}
          </div>
        </nav>
      </div>
    </aside>
  );
};
