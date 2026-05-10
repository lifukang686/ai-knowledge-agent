import { KnowledgeBase } from '@/types/knowledgeBase';
import { ModelProvider, AIModel } from '@/types/modelProvider';
import { BaseEntity, StatusType } from '@/types/common';

// 生成模拟ID
const generateId = () => Math.random().toString(36).substr(2, 9);

// 生成模拟时间
const generateDate = (daysAgo = 0) => {
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  return date.toISOString();
};

// 知识库Mock数据
export const mockKnowledgeBases: KnowledgeBase[] = [
  {
    id: generateId(),
    name: '技术文档知识库',
    description: '包含公司所有技术文档、API文档、开发规范等内容',
    document_count: 156,
    status: 'completed',
    created_at: generateDate(30),
    updated_at: generateDate(2),
  },
  {
    id: generateId(),
    name: '产品知识库',
    description: '产品需求文档、用户手册、功能说明等',
    document_count: 89,
    status: 'processing',
    created_at: generateDate(15),
    updated_at: generateDate(1),
  },
  {
    id: generateId(),
    name: '运维知识库',
    description: '运维手册、故障处理、系统配置等',
    document_count: 234,
    status: 'completed',
    created_at: generateDate(45),
    updated_at: generateDate(5),
  },
];

// 模型提供商Mock数据
export const mockModelProviders: ModelProvider[] = [
  {
    id: generateId(),
    name: 'OpenAI',
    api_base_url: 'https://api.openai.com/v1',
    description: 'OpenAI GPT系列模型',
    default_params: {
      temperature: 0.7,
      max_tokens: 2000,
    },
    is_active: true,
    created_at: generateDate(60),
    updated_at: generateDate(10),
  },
  {
    id: generateId(),
    name: 'Anthropic',
    api_base_url: 'https://api.anthropic.com/v1',
    description: 'Claude系列模型',
    default_params: {
      temperature: 0.8,
      max_tokens: 4000,
    },
    is_active: true,
    created_at: generateDate(45),
    updated_at: generateDate(8),
  },
];

// AI模型Mock数据
export const mockModels: AIModel[] = [
  {
    id: generateId(),
    provider_id: mockModelProviders[0].id,
    name: 'gpt-4',
    model_type: 'chat',
    description: 'GPT-4 聊天模型',
    max_tokens: 8192,
    is_active: true,
    created_at: generateDate(20),
    updated_at: generateDate(5),
  },
  {
    id: generateId(),
    provider_id: mockModelProviders[0].id,
    name: 'text-embedding-ada-002',
    model_type: 'embedding',
    description: '文本嵌入模型',
    max_tokens: 8191,
    is_active: true,
    created_at: generateDate(25),
    updated_at: generateDate(3),
  },
];

// 通用工具函数
export const generateMockList = <T extends BaseEntity>(
  items: T[],
  page: number = 1,
  pageSize: number = 20,
  keyword?: string
) => {
  console.log('generateMockList called with:', { itemsCount: items.length, page, pageSize, keyword });
  let filteredItems = items;
  
  if (keyword) {
    filteredItems = items.filter(item => 
      Object.values(item).some(value => 
        String(value).toLowerCase().includes(keyword.toLowerCase())
      )
    );
    console.log('After filtering:', filteredItems.length);
  }

  const start = (page - 1) * pageSize;
  const end = start + pageSize;
  const paginatedItems = filteredItems.slice(start, end);
  
  console.log('Returning paginated result:', {
    items: paginatedItems.length,
    total: filteredItems.length,
    page,
    pageSize,
  });

  return {
    items: paginatedItems,
    total: filteredItems.length,
    page,
    pageSize,
  };
};

// 模拟延迟
export const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));