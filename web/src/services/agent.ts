import { Agent, AgentRun, CreateAgentRequest, UpdateAgentRequest, RunAgentRequest, AgentQuery, AgentRunQuery } from '@/types/agent';
import { PaginatedResponse } from '@/types/common';
import { request } from '@/utils/request';
import { generateMockList } from './mockData';

// Mock 数据
const mockAgents: Agent[] = [
  {
    id: '1',
    name: '知识库助手',
    description: '专门处理知识库相关问题的 AI 助手',
    toolIds: ['1', '2'],
    modelProviderId: '1',
    modelId: '1',
    systemPrompt: '你是一个专业的知识库助手，能够帮助用户查询和管理知识库内容。',
    status: 'active',
    created_at: '2024-01-15T08:00:00Z',
    updated_at: '2024-01-15T08:00:00Z'
  },
  {
    id: '2',
    name: '文档分析助手',
    description: '专门处理文档分析和提取的 AI 助手',
    toolIds: ['3'],
    modelProviderId: '2',
    modelId: '3',
    systemPrompt: '你是一个专业的文档分析助手，能够从文档中提取关键信息。',
    status: 'active',
    created_at: '2024-01-16T08:00:00Z',
    updated_at: '2024-01-16T08:00:00Z'
  },
  {
    id: '3',
    name: '代码生成助手',
    description: '专门处理代码生成和优化的 AI 助手',
    toolIds: ['1', '3'],
    modelProviderId: '1',
    modelId: '1',
    systemPrompt: '你是一个专业的代码生成助手，能够帮助用户生成和优化代码。',
    status: 'inactive',
    created_at: '2024-01-17T08:00:00Z',
    updated_at: '2024-01-17T08:00:00Z'
  }
];

const mockAgentRuns: AgentRun[] = [
  {
    id: '1',
    agentId: '1',
    task: '查询知识库中的 Spring Boot 相关内容',
    status: 'completed',
    result: '在知识库中找到以下 Spring Boot 相关内容：...',
    startedAt: '2024-01-15T10:00:00Z',
    completedAt: '2024-01-15T10:02:30Z',
    duration: 150000,
    created_at: '2024-01-15T10:00:00Z',
    updated_at: '2024-01-15T10:02:30Z'
  },
  {
    id: '2',
    agentId: '1',
    task: '分析文档中的技术要点',
    status: 'running',
    startedAt: '2024-01-15T11:00:00Z',
    created_at: '2024-01-15T11:00:00Z',
    updated_at: '2024-01-15T11:00:00Z'
  },
  {
    id: '3',
    agentId: '2',
    task: '生成用户登录功能的代码',
    status: 'failed',
    error: '模型调用超时，请稍后重试',
    startedAt: '2024-01-15T12:00:00Z',
    completedAt: '2024-01-15T12:05:00Z',
    duration: 300000,
    created_at: '2024-01-15T12:00:00Z',
    updated_at: '2024-01-15T12:05:00Z'
  }
];

// API 服务
export const agentService = {
  // 获取 Agent 列表
  async getAgents(query: AgentQuery): Promise<PaginatedResponse<Agent>> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取 Agent 列表:', query);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const filtered = mockAgents.filter(agent => {
          if (query.name && !agent.name.toLowerCase().includes(query.name.toLowerCase())) {
            return false;
          }
          if (query.status && agent.status !== query.status) {
            return false;
          }
          return true;
        });

        resolve(generateMockList(filtered, query.page || 1, query.pageSize || 10));
      }, 500);
    });
  },

  // 获取 Agent 详情
  async getAgent(id: string): Promise<Agent> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取 Agent 详情:', id);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const agent = mockAgents.find(a => a.id === id);
        if (agent) {
          resolve(agent);
        } else {
          reject(new Error('Agent 不存在'));
        }
      }, 300);
    });
  },

  // 创建 Agent
  async createAgent(data: CreateAgentRequest): Promise<{ id: string }> {
    // TODO: 待确认 - 后端接口待实现
    console.log('创建 Agent:', data);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const newAgent: Agent = {
          id: String(mockAgents.length + 1),
          ...data,
          status: 'active',
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        };
        mockAgents.push(newAgent);
        resolve({ id: newAgent.id });
      }, 500);
    });
  },

  // 更新 Agent
  async updateAgent(id: string, data: UpdateAgentRequest): Promise<void> {
    // TODO: 待确认 - 后端接口待实现
    console.log('更新 Agent:', id, data);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockAgents.findIndex(a => a.id === id);
        if (index !== -1) {
          mockAgents[index] = {
            ...mockAgents[index],
            ...data,
            updated_at: new Date().toISOString()
          };
          resolve();
        } else {
          reject(new Error('Agent 不存在'));
        }
      }, 500);
    });
  },

  // 删除 Agent
  async deleteAgent(id: string): Promise<void> {
    // TODO: 待确认 - 后端接口待实现
    console.log('删除 Agent:', id);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockAgents.findIndex(a => a.id === id);
        if (index !== -1) {
          mockAgents.splice(index, 1);
          resolve();
        } else {
          reject(new Error('Agent 不存在'));
        }
      }, 300);
    });
  },

  // 运行 Agent
  async runAgent(id: string, data: RunAgentRequest): Promise<{ runId: string }> {
    // TODO: 待确认 - 后端接口待实现
    console.log('运行 Agent:', id, data);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const newRun: AgentRun = {
          id: String(mockAgentRuns.length + 1),
          agentId: id,
          task: data.task,
          status: 'pending',
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        };
        mockAgentRuns.push(newRun);
        resolve({ runId: newRun.id });
      }, 500);
    });
  },

  // 获取 Agent 运行记录
  async getAgentRuns(query: AgentRunQuery): Promise<PaginatedResponse<AgentRun>> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取 Agent 运行记录:', query);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        let filtered = mockAgentRuns;
        
        if (query.agentId) {
          filtered = filtered.filter(run => run.agentId === query.agentId);
        }
        
        if (query.status) {
          filtered = filtered.filter(run => run.status === query.status);
        }

        resolve(generateMockList(filtered, query.page || 1, query.pageSize || 10));
      }, 500);
    });
  },

  // 获取 Agent 运行详情
  async getAgentRun(runId: string): Promise<AgentRun> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取 Agent 运行详情:', runId);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const run = mockAgentRuns.find(r => r.id === runId);
        if (run) {
          resolve(run);
        } else {
          reject(new Error('运行记录不存在'));
        }
      }, 300);
    });
  }
};