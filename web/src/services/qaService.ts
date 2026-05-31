import { request } from '@/utils/request';
import { QaReq, QaResp, QaStreamHandlers } from '@/types/qa';
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
