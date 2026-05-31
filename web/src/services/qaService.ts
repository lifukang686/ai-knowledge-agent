import { request } from '@/utils/request';
import { QaReq, QaResp } from '@/types/qa';

const QA_TIMEOUT_MS = 90000;

export const qaService = {
  ask: (data: QaReq): Promise<QaResp> => {
    return request.post('/qa', data, { timeout: QA_TIMEOUT_MS });
  },
};
