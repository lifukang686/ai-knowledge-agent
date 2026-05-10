import { Workflow, WorkflowRun, CreateWorkflowRequest, UpdateWorkflowRequest, RunWorkflowRequest, WorkflowQuery, WorkflowRunQuery } from '@/types/workflow';
import { PaginatedResponse } from '@/types/common';
import { request } from '@/utils/request';
import { generateMockList } from './mockData';

// Mock 数据
const mockWorkflows: Workflow[] = [
  {
    id: '1',
    name: '文档处理工作流',
    description: '自动化文档处理和知识库更新工作流',
    definition: {
      nodes: [
        {
          id: 'start',
          type: 'start',
          name: '开始',
          config: {},
          nextNodeIds: ['upload']
        },
        {
          id: 'upload',
          type: 'tool',
          name: '文档上传',
          config: {
            toolType: 'document_upload',
            parameters: {
              allowedFormats: ['pdf', 'doc', 'docx', 'txt']
            }
          },
          nextNodeIds: ['parse']
        },
        {
          id: 'parse',
          type: 'tool',
          name: '文档解析',
          config: {
            toolType: 'document_parser',
            parameters: {
              extractText: true,
              extractMetadata: true
            }
          },
          nextNodeIds: ['chunk']
        },
        {
          id: 'chunk',
          type: 'tool',
          name: '文本分块',
          config: {
            toolType: 'text_chunker',
            parameters: {
              chunkSize: 1000,
              chunkOverlap: 200
            }
          },
          nextNodeIds: ['embed']
        },
        {
          id: 'embed',
          type: 'tool',
          name: '文本嵌入',
          config: {
            toolType: 'text_embedder',
            parameters: {
              model: 'text-embedding-ada-002'
            }
          },
          nextNodeIds: ['store']
        },
        {
          id: 'store',
          type: 'tool',
          name: '存储索引',
          config: {
            toolType: 'vector_store',
            parameters: {
              collectionName: 'documents'
            }
          },
          nextNodeIds: ['end']
        },
        {
          id: 'end',
          type: 'end',
          name: '结束',
          config: {},
          nextNodeIds: []
        }
      ],
      edges: [
        { id: 'e1', source: 'start', target: 'upload' },
        { id: 'e2', source: 'upload', target: 'parse' },
        { id: 'e3', source: 'parse', target: 'chunk' },
        { id: 'e4', source: 'chunk', target: 'embed' },
        { id: 'e5', source: 'embed', target: 'store' },
        { id: 'e6', source: 'store', target: 'end' }
      ],
      startNodeId: 'start',
      endNodeIds: ['end']
    },
    status: 'active',
    version: 1,
    created_at: '2024-01-15T08:00:00Z',
    updated_at: '2024-01-15T08:00:00Z'
  },
  {
    id: '2',
    name: '问答处理工作流',
    description: '处理用户问答请求的工作流',
    definition: {
      nodes: [
        {
          id: 'start',
          type: 'start',
          name: '开始',
          config: {},
          nextNodeIds: ['retrieve']
        },
        {
          id: 'retrieve',
          type: 'tool',
          name: '知识检索',
          config: {
            toolType: 'knowledge_retriever',
            parameters: {
              topK: 5,
              similarityThreshold: 0.8
            }
          },
          nextNodeIds: ['generate']
        },
        {
          id: 'generate',
          type: 'agent',
          name: '答案生成',
          config: {
            agentId: '1',
            model: 'gpt-4',
            temperature: 0.7
          },
          nextNodeIds: ['validate']
        },
        {
          id: 'validate',
          type: 'condition',
          name: '答案验证',
          config: {
            condition: 'confidence > 0.8',
            trueBranch: 'end',
            falseBranch: 'clarify'
          },
          nextNodeIds: ['clarify', 'end']
        },
        {
          id: 'clarify',
          type: 'agent',
          name: '澄清问题',
          config: {
            agentId: '1',
            model: 'gpt-4',
            temperature: 0.5
          },
          nextNodeIds: ['end']
        },
        {
          id: 'end',
          type: 'end',
          name: '结束',
          config: {},
          nextNodeIds: []
        }
      ],
      edges: [
        { id: 'e1', source: 'start', target: 'retrieve' },
        { id: 'e2', source: 'retrieve', target: 'generate' },
        { id: 'e3', source: 'generate', target: 'validate' },
        { id: 'e4', source: 'validate', target: 'clarify', condition: 'confidence <= 0.8' },
        { id: 'e5', source: 'validate', target: 'end', condition: 'confidence > 0.8' },
        { id: 'e6', source: 'clarify', target: 'end' }
      ],
      startNodeId: 'start',
      endNodeIds: ['end']
    },
    status: 'active',
    version: 2,
    created_at: '2024-01-16T08:00:00Z',
    updated_at: '2024-01-16T08:00:00Z'
  }
];

const mockWorkflowRuns: WorkflowRun[] = [
  {
    id: '1',
    workflowId: '1',
    workflowName: '文档处理工作流',
    status: 'completed',
    input: {
      documentId: 'doc123',
      fileName: 'spring-boot-guide.pdf',
      knowledgeBaseId: 'kb1'
    },
    output: {
      processedChunks: 15,
      indexedVectors: 15,
      processingTime: 125000
    },
    startedAt: '2024-01-15T10:00:00Z',
    completedAt: '2024-01-15T10:02:05Z',
    duration: 125000,
    currentNodeId: 'end',
    nodeExecutions: [
      {
        id: 'ne1',
        workflowRunId: '1',
        nodeId: 'upload',
        nodeName: '文档上传',
        nodeType: 'tool',
        status: 'completed',
        input: { documentId: 'doc123' },
        output: { uploadStatus: 'success', filePath: '/docs/spring-boot-guide.pdf' },
        startedAt: '2024-01-15T10:00:00Z',
        completedAt: '2024-01-15T10:00:10Z',
        duration: 10000,
        created_at: '2024-01-15T10:00:00Z',
        updated_at: '2024-01-15T10:00:10Z'
      }
    ],
    created_at: '2024-01-15T10:00:00Z',
    updated_at: '2024-01-15T10:02:05Z'
  }
];

// API 服务
export const workflowService = {
  // 获取工作流列表
  async getWorkflows(query: WorkflowQuery): Promise<PaginatedResponse<Workflow>> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取工作流列表:', query);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const filtered = mockWorkflows.filter(workflow => {
          if (query.name && !workflow.name.toLowerCase().includes(query.name.toLowerCase())) {
            return false;
          }
          if (query.status && workflow.status !== query.status) {
            return false;
          }
          return true;
        });

        resolve(generateMockList(filtered, query.page || 1, query.pageSize || 10));
      }, 500);
    });
  },

  // 获取工作流详情
  async getWorkflow(id: string): Promise<Workflow> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取工作流详情:', id);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const workflow = mockWorkflows.find(w => w.id === id);
        if (workflow) {
          resolve(workflow);
        } else {
          reject(new Error('工作流不存在'));
        }
      }, 300);
    });
  },

  // 创建工作流
  async createWorkflow(data: CreateWorkflowRequest): Promise<{ id: string }> {
    // TODO: 待确认 - 后端接口待实现
    console.log('创建工作流:', data);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const newWorkflow: Workflow = {
          id: String(mockWorkflows.length + 1),
          ...data,
          status: 'active',
          version: 1,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        };
        mockWorkflows.push(newWorkflow);
        resolve({ id: newWorkflow.id });
      }, 500);
    });
  },

  // 更新工作流
  async updateWorkflow(id: string, data: UpdateWorkflowRequest): Promise<void> {
    // TODO: 待确认 - 后端接口待实现
    console.log('更新工作流:', id, data);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockWorkflows.findIndex(w => w.id === id);
        if (index !== -1) {
          mockWorkflows[index] = {
            ...mockWorkflows[index],
            ...data,
            version: mockWorkflows[index].version + 1,
            updated_at: new Date().toISOString()
          };
          resolve();
        } else {
          reject(new Error('工作流不存在'));
        }
      }, 500);
    });
  },

  // 删除工作流
  async deleteWorkflow(id: string): Promise<void> {
    // TODO: 待确认 - 后端接口待实现
    console.log('删除工作流:', id);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const index = mockWorkflows.findIndex(w => w.id === id);
        if (index !== -1) {
          mockWorkflows.splice(index, 1);
          resolve();
        } else {
          reject(new Error('工作流不存在'));
        }
      }, 300);
    });
  },

  // 运行工作流
  async runWorkflow(id: string, data: RunWorkflowRequest): Promise<{ runId: string }> {
    // TODO: 待确认 - 后端接口待实现
    console.log('运行工作流:', id, data);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        const newRun: WorkflowRun = {
          id: String(mockWorkflowRuns.length + 1),
          workflowId: id,
          workflowName: mockWorkflows.find(w => w.id === id)?.name || '未知工作流',
          status: 'pending',
          input: data.input,
          nodeExecutions: [],
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        };
        mockWorkflowRuns.push(newRun);
        resolve({ runId: newRun.id });
      }, 500);
    });
  },

  // 获取工作流运行记录
  async getWorkflowRuns(query: WorkflowRunQuery): Promise<PaginatedResponse<WorkflowRun>> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取工作流运行记录:', query);
    
    // Mock 实现
    return new Promise((resolve) => {
      setTimeout(() => {
        let filtered = mockWorkflowRuns;
        
        if (query.workflowId) {
          filtered = filtered.filter(run => run.workflowId === query.workflowId);
        }
        
        if (query.status) {
          filtered = filtered.filter(run => run.status === query.status);
        }

        resolve(generateMockList(filtered, query.page || 1, query.pageSize || 10));
      }, 500);
    });
  },

  // 获取工作流运行详情
  async getWorkflowRun(runId: string): Promise<WorkflowRun> {
    // TODO: 待确认 - 后端接口待实现
    console.log('获取工作流运行详情:', runId);
    
    // Mock 实现
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const run = mockWorkflowRuns.find(r => r.id === runId);
        if (run) {
          resolve(run);
        } else {
          reject(new Error('运行记录不存在'));
        }
      }, 300);
    });
  }
};