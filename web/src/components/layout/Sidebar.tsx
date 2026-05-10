import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { 
  Database, 
  Brain, 
  Wrench, 
  Bot, 
  Workflow, 
  Clock,
  MessageSquare,
  Settings
} from 'lucide-react';

const menuItems = [
  { path: '/knowledge-bases', icon: Database, label: '知识库管理' },
  { path: '/model-providers', icon: Brain, label: '模型提供商' },
  { path: '/tools', icon: Wrench, label: '工具管理' },
  { path: '/agents', icon: Bot, label: 'Agent管理' },
  { path: '/workflows', icon: Workflow, label: '工作流管理' },
  { path: '/scheduler/jobs', icon: Clock, label: '定时任务' },
  { path: '/qa', icon: MessageSquare, label: 'RAG问答' },
];

export const Sidebar: React.FC = () => {
  const location = useLocation();

  const isActive = (path: string) => {
    return location.pathname.startsWith(path);
  };

  return (
    <aside className="w-64 bg-white shadow-sm border-r border-gray-200 min-h-screen">
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
        </nav>
      </div>
    </aside>
  );
};