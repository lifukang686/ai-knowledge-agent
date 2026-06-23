import { ModelProvider, ModelConfig, CreateModelProviderRequest, UpdateModelProviderRequest, ModelProviderQuery, CreateModelConfigRequest, UpdateModelConfigRequest } from '@/types/modelProvider';
import { PaginatedResponse } from '@/types/common';
import { request } from '@/utils/request';

// API 服务
export const modelProviderService = {
  // 获取模型提供商列表
  async getModelProviders(query: ModelProviderQuery): Promise<PaginatedResponse<ModelProvider>> {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === modelProviderService.getModelProviders 调用 ===`);
    console.log('  - 查询参数:', query);
    
    const data = await request.get<ModelProvider[]>('/models/providers');
    
    console.log('  - 后端返回数据:', data);
    console.log('  - 数据长度:', data.length);
    
    // 后端返回的是 List，我们包装成 PaginatedResponse 格式
    return {
      items: data,
      total: data.length,
      page: 1,
      pageSize: data.length
    };
  },

  // 获取模型提供商详情
  async getModelProvider(id: string): Promise<ModelProvider> {
    console.log('获取模型提供商详情，传入id:', id, '类型:', typeof id);
    // 目前后端没有详情接口，暂时用列表中的数据
    const providers = await this.getModelProviders({});
    console.log('所有提供商:', providers.items);
    const provider = providers.items.find((p: ModelProvider) => {
      console.log('比较 - 列表id:', p.id, '类型:', typeof p.id, '=== 传入id:', id, '结果:', p.id === id);
      return String(p.id) === String(id);
    });
    if (!provider) {
      throw new Error('模型提供商不存在');
    }
    console.log('找到的提供商:', provider);
    return provider;
  },

  // 创建模型提供商
  async createModelProvider(data: CreateModelProviderRequest): Promise<{ id: string }> {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === modelProviderService.createModelProvider 调用 ===`);
    console.log('  - 输入数据:', data);
    
    const id = await request.post<string>('/models/providers', data);
    
    console.log('  - 后端返回的 id:', id, '类型:', typeof id);
    
    return { id: id };
  },

  // 更新模型提供商
  async updateModelProvider(id: string, data: UpdateModelProviderRequest): Promise<void> {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === modelProviderService.updateModelProvider 调用 ===`);
    console.log('  - id:', id, '类型:', typeof id);
    console.log('  - 更新数据:', data);
    
    await request.put(`/models/providers/${id}`, data);
    
    console.log('  - 模型提供商更新成功');
  },

  // 删除模型提供商
  async deleteModelProvider(id: string): Promise<void> {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === modelProviderService.deleteModelProvider 调用 ===`);
    console.log('  - id:', id, '类型:', typeof id);
    
    await request.delete(`/models/providers/${id}`);
    
    console.log('  - 模型提供商删除成功');
  },

  // 获取提供商下的模型列表
  async getModelsByProvider(providerId: string): Promise<ModelConfig[]> {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === modelProviderService.getModelsByProvider 调用 ===`);
    console.log('  - providerId:', providerId, '类型:', typeof providerId);
    
    // providerId可能是字符串，直接传递（后端会自动处理
    const result = await request.get<ModelConfig[]>('/models/configs', {
      params: { providerId: providerId }
    });
    
    console.log('  - 后端返回的模型列表:', result);
    console.log('  - 模型数量:', result.length);
    
    return result;
  },

  // 创建模型配置
  async createModelConfig(data: CreateModelConfigRequest): Promise<{ id: string }> {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === modelProviderService.createModelConfig 调用 ===`);
    console.log('  - 输入数据:', data);
    console.log('  - providerId 类型:', typeof data.providerId);
    
    // 直接传递字符串，后端会正确处理
    const id = await request.post<string>('/models/configs', data);
    
    console.log('  - 后端返回的 id:', id, '类型:', typeof id);
    
    return { id: id };
  },

  // 更新模型配置
  async updateModelConfig(id: string, data: UpdateModelConfigRequest): Promise<void> {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === modelProviderService.updateModelConfig 调用 ===`);
    console.log('  - 模型配置 id:', id, '类型:', typeof id);
    console.log('  - 更新数据:', data);
    
    await request.put(`/models/configs/${id}`, data);
    
    console.log('  - 模型配置更新成功');
  },

  // 删除模型配置
  async deleteModelConfig(id: string): Promise<void> {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] === modelProviderService.deleteModelConfig 调用 ===`);
    console.log('  - 模型配置 id:', id, '类型:', typeof id);
    
    await request.delete(`/models/configs/${id}`);
    
    console.log('  - 模型配置删除成功');
  },

  // 设置默认模型提供商
  async setDefaultProvider(id: string): Promise<void> {
    console.log('设置默认提供商, id:', id);
    await request.put(`/models/providers/${id}/default`);
    console.log('默认提供商设置成功');
  },

  // 取消默认模型提供商
  async cancelDefaultProvider(id: string): Promise<void> {
    console.log('取消默认提供商, id:', id);
    await request.delete(`/models/providers/${id}/default`);
    console.log('默认提供商取消成功');
  }
};
