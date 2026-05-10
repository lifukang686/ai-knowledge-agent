import { Tool, ToolCreateRequest, ToolUpdateRequest, ToolQueryParams, PaginatedResponse, BaseEntity } from '../types/tool';

// Mock data for tools
const mockTools: Tool[] = [
  {
    id: '1',
    name: 'Web Search',
    description: 'Search the web for information',
    executor_type: 'http',
    executor_config: {
      url: 'https://api.search.com/search',
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    },
    schema: {
      type: 'object',
      properties: {
        query: { type: 'string', description: 'Search query' }
      },
      required: ['query']
    },
    tags: ['search', 'web'],
    created_at: '2024-01-15T10:30:00Z',
    updated_at: '2024-01-15T10:30:00Z'
  },
  {
    id: '2',
    name: 'File Reader',
    description: 'Read and parse file content',
    executor_type: 'function',
    executor_config: {
      function_name: 'read_file',
      supported_formats: ['txt', 'pdf', 'docx']
    },
    schema: {
      type: 'object',
      properties: {
        file_path: { type: 'string', description: 'Path to the file' }
      },
      required: ['file_path']
    },
    tags: ['file', 'read'],
    created_at: '2024-01-16T14:20:00Z',
    updated_at: '2024-01-16T14:20:00Z'
  },
  {
    id: '3',
    name: 'Database Query',
    description: 'Execute SQL queries on database',
    executor_type: 'script',
    executor_config: {
      script_path: '/scripts/db_query.py',
      timeout: 30,
      max_results: 1000
    },
    schema: {
      type: 'object',
      properties: {
        query: { type: 'string', description: 'SQL query' },
        database: { type: 'string', description: 'Database name' }
      },
      required: ['query']
    },
    tags: ['database', 'sql'],
    created_at: '2024-01-17T09:15:00Z',
    updated_at: '2024-01-17T09:15:00Z'
  }
];

export const toolService = {
  // Get tools list with pagination and filtering
  async getTools(params: ToolQueryParams): Promise<PaginatedResponse<Tool>> {
    // Simulate API delay
    await new Promise(resolve => setTimeout(resolve, 300));
    
    let filteredTools = [...mockTools];
    
    // Apply filters
    if (params.name) {
      filteredTools = filteredTools.filter(tool => 
        tool.name.toLowerCase().includes(params.name.toLowerCase())
      );
    }
    
    if (params.executor_type) {
      filteredTools = filteredTools.filter(tool => 
        tool.executor_type === params.executor_type
      );
    }
    
    if (params.tags && params.tags.length > 0) {
      filteredTools = filteredTools.filter(tool => 
        params.tags!.some(tag => tool.tags?.includes(tag))
      );
    }
    
    // Apply pagination
    const start = (params.page - 1) * params.pageSize;
    const end = start + params.pageSize;
    const paginatedTools = filteredTools.slice(start, end);
    
    return {
      data: paginatedTools,
      total: filteredTools.length,
      page: params.page,
      pageSize: params.pageSize
    };
  },

  // Get tool by ID
  async getToolById(id: string): Promise<Tool> {
    await new Promise(resolve => setTimeout(resolve, 200));
    
    const tool = mockTools.find(tool => tool.id === id);
    if (!tool) {
      throw new Error('Tool not found');
    }
    
    return tool;
  },

  // Create new tool
  async createTool(data: ToolCreateRequest): Promise<Tool> {
    await new Promise(resolve => setTimeout(resolve, 400));
    
    const newTool: Tool = {
      id: String(Date.now()),
      ...data,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString()
    };
    
    mockTools.unshift(newTool);
    return newTool;
  },

  // Update existing tool
  async updateTool(id: string, data: ToolUpdateRequest): Promise<Tool> {
    await new Promise(resolve => setTimeout(resolve, 400));
    
    const toolIndex = mockTools.findIndex(tool => tool.id === id);
    if (toolIndex === -1) {
      throw new Error('Tool not found');
    }
    
    const updatedTool = {
      ...mockTools[toolIndex],
      ...data,
      updated_at: new Date().toISOString()
    };
    
    mockTools[toolIndex] = updatedTool;
    return updatedTool;
  },

  // Delete tool
  async deleteTool(id: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const toolIndex = mockTools.findIndex(tool => tool.id === id);
    if (toolIndex === -1) {
      throw new Error('Tool not found');
    }
    
    mockTools.splice(toolIndex, 1);
  }
};