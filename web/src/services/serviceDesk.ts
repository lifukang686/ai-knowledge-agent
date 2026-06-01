import { request } from '@/utils/request';
import { ListResponse } from '@/types/common';
import {
  AgentRunEvent,
  ServiceDeskAnswerResp,
  ServiceDeskAskReq,
  ServiceDeskFeedbackReq,
  ServiceDeskFeedbackResp,
  ServiceDeskStreamHandlers,
  ServiceTicketEvent,
  ServiceTicket,
} from '@/types/serviceDesk';
import { useAuthStore } from '@/stores/authStore';

interface SseEvent {
  event: string;
  data: string;
}

const mapId = (value: any): string | undefined => (value != null ? String(value) : undefined);

const mapAnswer = (api: any): ServiceDeskAnswerResp => ({
  answer: api?.answer ?? '',
  intent: api?.intent ?? '',
  serviceType: api?.serviceType ?? 'AUTO',
  status: api?.status ?? '',
  runId: mapId(api?.runId) ?? '',
  ticketId: mapId(api?.ticketId),
  ticketNo: api?.ticketNo ?? undefined,
  conversationId: mapId(api?.conversationId),
  approvalRequired: Boolean(api?.approvalRequired),
  pendingTicket: api?.pendingTicket ? mapTicket(api.pendingTicket) : undefined,
  events: Array.isArray(api?.events) ? api.events.map(mapAgentRunEvent) : [],
  feedbackSubmitted: Boolean(api?.feedbackSubmitted),
});

const mapAgentRunEvent = (api: any): AgentRunEvent => ({
  type: api?.type ?? '',
  stepOrder: api?.stepOrder ?? undefined,
  toolName: api?.toolName ?? undefined,
  payload: api?.payload ?? undefined,
  success: api?.success ?? undefined,
  durationMs: api?.durationMs ?? undefined,
  message: api?.message ?? undefined,
  timestamp: api?.timestamp ?? undefined,
});

const mapTicketEvent = (api: any): ServiceTicketEvent => ({
  id: mapId(api?.id) ?? '',
  ticketId: mapId(api?.ticketId) ?? '',
  eventType: api?.eventType ?? '',
  fromStatus: api?.fromStatus ?? undefined,
  toStatus: api?.toStatus ?? undefined,
  operatorId: mapId(api?.operatorId),
  message: api?.message ?? undefined,
  payload: api?.payload ?? undefined,
  createTime: api?.createTime ?? undefined,
});

const mapTicket = (api: any): ServiceTicket => ({
  id: mapId(api?.id) ?? '',
  ticketNo: api?.ticketNo ?? '',
  serviceType: api?.serviceType ?? '',
  category: api?.category ?? undefined,
  priority: api?.priority ?? '',
  status: api?.status ?? '',
  title: api?.title ?? '',
  description: api?.description ?? undefined,
  agentSummary: api?.agentSummary ?? undefined,
  creatorId: mapId(api?.creatorId),
  assigneeId: mapId(api?.assigneeId),
  sourceRunId: mapId(api?.sourceRunId),
  sourceConversationId: mapId(api?.sourceConversationId),
  events: Array.isArray(api?.events) ? api.events.map(mapTicketEvent) : [],
  eventCount: api?.eventCount ?? 0,
  createTime: api?.createTime ?? undefined,
  updateTime: api?.updateTime ?? undefined,
});

const mapFeedback = (api: any): ServiceDeskFeedbackResp => ({
  id: mapId(api?.id) ?? '',
  runId: mapId(api?.runId) ?? '',
  ticketId: mapId(api?.ticketId),
  resolved: Boolean(api?.resolved),
  comment: api?.comment ?? undefined,
  userId: mapId(api?.userId),
  createTime: api?.createTime ?? undefined,
});

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

const dispatchSseEvent = (sseEvent: SseEvent, handlers: ServiceDeskStreamHandlers) => {
  const payload = JSON.parse(sseEvent.data);
  switch (sseEvent.event) {
    case 'stage':
      handlers.onStage?.(payload);
      break;
    case 'token':
      handlers.onToken?.(payload);
      break;
    case 'done':
      handlers.onDone?.(mapAnswer(payload));
      break;
    case 'error':
      handlers.onError?.(payload);
      break;
    default:
      break;
  }
};

const readSseStream = async (
  reader: ReadableStreamDefaultReader<Uint8Array>,
  handlers: ServiceDeskStreamHandlers,
) => {
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

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

export const serviceDeskService = {
  ask: async (data: ServiceDeskAskReq): Promise<ServiceDeskAnswerResp> => {
    const response = await request.post<any>('/service-desk/ask', data, { timeout: 90000 });
    return mapAnswer(response);
  },

  askStream: async (data: ServiceDeskAskReq, handlers: ServiceDeskStreamHandlers): Promise<void> => {
    const token = useAuthStore.getState().token;
    const response = await fetch('/api/service-desk/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      throw new Error(`服务台流式请求失败：HTTP ${response.status}`);
    }
    if (!response.body) {
      throw new Error('浏览器不支持流式响应读取');
    }

    await readSseStream(response.body.getReader(), handlers);
  },

  getTickets: async (params: {
    page?: number;
    pageSize?: number;
    status?: string;
    serviceType?: string;
  }): Promise<ListResponse<ServiceTicket>> => {
    const response = await request.get<{
      items: any[];
      total: number;
      page: number;
      pageSize: number;
    }>('/service-desk/tickets', { params });
    return {
      items: Array.isArray(response?.items) ? response.items.map(mapTicket) : [],
      total: response?.total ?? 0,
      page: response?.page ?? params.page ?? 1,
      pageSize: response?.pageSize ?? params.pageSize ?? 10,
    };
  },

  getTicket: async (id: string): Promise<ServiceTicket> => {
    const response = await request.get<any>(`/service-desk/tickets/${id}`);
    return mapTicket(response);
  },

  confirmTicket: async (id: string): Promise<ServiceTicket> => {
    const response = await request.post<any>(`/service-desk/tickets/${id}/confirm`);
    return mapTicket(response);
  },

  submitFeedback: async (runId: string, data: ServiceDeskFeedbackReq): Promise<ServiceDeskFeedbackResp> => {
    const response = await request.post<any>(`/service-desk/runs/${runId}/feedback`, data);
    return mapFeedback(response);
  },

  getRun: async (runId: string) => {
    return request.get<any>(`/service-desk/runs/${runId}`);
  },
};
