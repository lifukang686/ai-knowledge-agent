import type { CreateToolReq } from '@/types/tool';

export interface ToolFormErrors {
  name?: string;
  executorConfig?: string;
}

export function validateToolForm(data: CreateToolReq): ToolFormErrors {
  const errors: ToolFormErrors = {};
  if (!data.name.trim()) {
    errors.name = '工具名称不能为空';
  } else if (data.name.length > 100) {
    errors.name = '工具名称不能超过100个字符';
  }
  if (!data.executorConfig.trim()) {
    errors.executorConfig = '执行器配置不能为空';
  } else {
    try {
      JSON.parse(data.executorConfig.trim());
    } catch {
      errors.executorConfig = '执行器配置需要是合法的 JSON 格式';
    }
  }
  return errors;
}