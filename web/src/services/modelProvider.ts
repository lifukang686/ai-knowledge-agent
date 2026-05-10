import { ModelProvider, Model, CreateModelProviderRequest, UpdateModelProviderRequest, ModelProviderQuery } from '@/types/modelProvider';
import { PaginatedResponse } from '@/types/common';
import { request } from '@/utils/request';
import { generateMockList } from './mockData';

// Mock 数据
const mockModelProviders: ModelProvider[] = [
  {
    id: '1',
    name: 'OpenAI',
    apiUrl: 'https://api.openai.com/v1',
    apiKey: 'sk-***', // 实际项目中应该加密存储
    description: 'OpenAI GPT 系列模型提供商',
    defaultParams: {
      temperature: 0.7,
      maxTokens: 2048,
      topP: 1
    },
    status: 'active',
    created_at: '2024-01-15T08:00:00Z',
    updated_at: '2024-01-15T08:00:00Z'
  },
  {
    id: '2',
    name: 'Anthropic',
    apiUrl: 'https://api.anthropic.com/v1',
    apiKey: 'sk-ant-***',
    description: 'Anthropic Claude 系列模型提供商',
    defaultParams: {
      max_tokens: 4096,
      temperature: 0.5
    },
    status: 'active',
    created_at: '2024-01-16T08:00:00Z',
    updated_at: '2024-01-16T08:00:00Z'
  },
  {
    id: '3',
    name: 'Google',
    apiUrl: 'https://generativelanguage.googleapis.com/v1beta',
    apiKey: 'AIza-***',
    description: 'Google Gemini 系列模型提供商',
    defaultParams: {
      temperature: 0.9,
      topK: 40,
      topP: 0.95
    },
    status: 'inactive',
    created_at: '2024-01-17T08:00:00Z',
    updated_at: '2024-01-17T08:00:00Z'
  }
];

const mockModels: Model[] = [
  {
    id: '1',
    providerId: '1',
    name: 'gpt-4',
    modelType: 'chat',
    description: 'GPT-4 模型',
    maxTokens: 8192,
    status: 'active',
    created_at: '2024-01-15T08:00:00Z',
    updated_at: '2024-01-15T08:00:00Z'
  },
  {
    id: '2',
    providerId: '1',
    name: 'text-embedding-ada-002',
    modelType: 'embedding',
    description: '文本嵌入模型',
    maxTokens: 8191,
    status: 'active',
    created_at: '2024-01-15T08:00:00Z',
    updated_at: '2024-01-15T08:00:00Z'
  },
  {
    id: '3',
    providerId: '2',
    name: 'claude-3-sonnet',
    modelType: 'chat',
    description: 'Claude 3 Sonnet 模型',
    maxTokens: 200000,
    status: 'active',
    created_at: '2024-01-16T08:00:00Z',
    updated_at: '2024-01-16T08:00:00Z'
  }
];

// API 服务
export const modelProviderService = {
  // 获取模型提供商列表
  async getModelProviders(query: ModelProviderQuery): Promise<PaginatedResponse<ModelProvider>> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取模型提供商列表:', query);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const filtered = mockModelProviders.filter(provider => {
          if (query.name && !provider.name.toLowerCase().includes(query.name.toLowerCase())) {
            return false;
          }
          if (query.status && provider.status !== query.status) {
            return false;
          }
          return true;
        });

        resolve(generateMockList(filtered, query.page || 1, query.pageSize || 10));
      }, 500);
    });
  },

  // 获取模型提供商详情
  async getModelProvider(id: string): Promise<ModelProvider> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取模型提供商详情:', id);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const provider = mockModelProviders.find(p => p.id === id);
        if (provider) {
          resolve(provider);
        } else {
          reject(new Error('模型提供商不存在'));
        }
      }, 300);
    });
  },

  // 创建模型提供商
  async createModelProvider(data: CreateModelProviderRequest): Promise<{ id: string }> {
    // TODO: 待确认 - 后端接口待实现
    console.log('创建模型提供商:', data);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const newProvider: ModelProvider = {
          id: String(mockModelProviders.length + 1),
          ...data,
          status: 'active',
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        };
        mockModelProviders.push(newProvider);
        resolve({ id: newProvider.id });
      }, 500);
    });
  },

  // 更新模型提供商
  async updateModelProvider(id: string, data: UpdateModelProviderRequest): Promise<void> {
    // TODO: 待确认 - 后端接口待实现
    console.log('更新模型提供商:', id, data);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockModelProviders.findIndex(p => p.id === id);
        if (index !== -1) {
          mockModelProviders[index] = {
            ...mockModelProviders[index],
            ...data,
            updated_at: new Date().toISOString()
          };
          resolve();
        } else {
          reject(new Error('模型提供商不存在'));
        }
      }, 500);
    });
  },

  // 删除模型提供商
  async deleteModelProvider(id: string): Promise<void> {
    // TODO: 待确认 - 后端接口待实现
    console.log('删除模型提供商:', id);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockModelProviders.findIndex(p => p.id === id);
        if (index !== -1) {
          mockModelProviders.splice(index, 1);
          resolve();
        } else {
          reject(new Error('模型提供商不存在'));
        }
      }, 300);
    });
  },

  // 获取提供商下的模型列表
  async getModelsByProvider(providerId: string): Promise<Model[]> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取提供商模型列表:', providerId);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const models = mockModels.filter(m => m.providerId === providerId);
        resolve(models);
      }, 300);
    });
  }
};