import { request } from '@/utils/request';
import { QaReq, QaResp } from '@/types/qa';

export const qaService = {
  ask: (data: QaReq): Promise<QaResp> => {
    return request.post('/qa', data);
  },
};