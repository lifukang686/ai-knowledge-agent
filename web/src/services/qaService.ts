import { request } from '@/utils/request';
import { QaConversation, QaConversationMessage, QaReq, QaResp, QaStreamHandlers } from '@/types/qa';
import { useAuthStore } from '@/stores/authStore';

const QA_TIMEOUT_MS = 90000;

interface SseEvent {
  event: string;
  data: string;
}

const parseSseEvent = (rawEvent: string): SseEvent | null => {
  const lines = rawEvent.split(/\r?\n/);
  let event = 'message';
  const dataLines: string[] = [];

  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim();
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart());
    }
  }

  if (dataLines.length === 0) {
    return null;
  }
  return { event, data: dataLines.join('\n') };
};

const dispatchSseEvent = (sseEvent: SseEvent, handlers: QaStreamHandlers) => {
  const payload = JSON.parse(sseEvent.data);
  switch (sseEvent.event) {
    case 'stage':
      handlers.onStage?.(payload);
      break;
    case 'token':
      handlers.onToken?.(payload);
      break;
    case 'done':
      handlers.onDone?.(payload);
      break;
    case 'error':
      handlers.onError?.(payload);
      break;
    default:
      break;
  }
};

const readSseStream = async (reader: ReadableStreamDefaultReader<Uint8Array>, handlers: QaStreamHandlers) => {
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() ?? '';

    for (const rawEvent of events) {
      const sseEvent = parseSseEvent(rawEvent.trim());
      if (sseEvent) {
        dispatchSseEvent(sseEvent, handlers);
      }
    }
  }

  const remaining = buffer.trim();
  if (remaining) {
    const sseEvent = parseSseEvent(remaining);
    if (sseEvent) {
      dispatchSseEvent(sseEvent, handlers);
    }
  }
};

export const qaService = {
  ask: (data: QaReq): Promise<QaResp> => {
    return request.post('/qa', data, { timeout: QA_TIMEOUT_MS });
  },

  listConversations: async (params?: { knowledgeBaseId?: string; limit?: number }): Promise<QaConversation[]> => {
    const response = await request.get<any[]>('/qa/conversations', {
      params: {
        knowledgeBaseId: params?.knowledgeBaseId || undefined,
        limit: params?.limit,
      },
    });
    return (response || []).map(mapConversation);
  },

  createConversation: async (knowledgeBaseId?: string): Promise<QaConversation> => {
    const response = await request.post<any>('/qa/conversations', {
      knowledgeBaseId: knowledgeBaseId || undefined,
    });
    return mapConversation(response);
  },

  listMessages: async (conversationId: string): Promise<QaConversationMessage[]> => {
    const response = await request.get<any[]>(`/qa/conversations/${conversationId}/messages`);
    return (response || []).map(mapConversationMessage);
  },

  askStream: async (data: QaReq, handlers: QaStreamHandlers): Promise<void> => {
    const token = useAuthStore.getState().token;
    const response = await fetch('/api/qa/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      throw new Error(`流式问答请求失败：HTTP ${response.status}`);
    }
    if (!response.body) {
      throw new Error('浏览器不支持流式响应读取');
    }

    await readSseStream(response.body.getReader(), handlers);
  },
};

const mapConversation = (api: any): QaConversation => ({
  id: api?.id != null ? String(api.id) : '',
  knowledgeBaseId: api?.knowledgeBaseId != null ? String(api.knowledgeBaseId) : undefined,
  title: api?.title || '新会话',
  status: api?.status || 'active',
  messageCount: api?.messageCount ?? 0,
  lastMessageAt: api?.lastMessageAt,
  createTime: api?.createTime,
  updateTime: api?.updateTime,
});

const mapConversationMessage = (api: any): QaConversationMessage => ({
  id: api?.id != null ? String(api.id) : '',
  conversationId: api?.conversationId != null ? String(api.conversationId) : '',
  role: api?.role === 'assistant' ? 'assistant' : 'user',
  content: api?.content || '',
  rewrittenQuery: api?.rewrittenQuery || undefined,
  status: api?.status || undefined,
  createTime: api?.createTime,
  updateTime: api?.updateTime,
});
