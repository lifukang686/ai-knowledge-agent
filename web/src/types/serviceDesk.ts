export type ServiceDeskIntent =
  | 'knowledge_qa'
  | 'create_ticket'
  | 'query_ticket'
  | 'collect_info'
  | 'handoff_human'
  | 'summarize_document';

export type ServiceDeskType = 'AUTO' | 'IT' | 'HR';
export type TicketStatus = 'DRAFT' | 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED' | string;

export interface AgentRunEvent {
  type: string;
  stepOrder?: number;
  toolName?: string;
  payload?: Record<string, any>;
  success?: boolean;
  durationMs?: number;
  message?: string;
  timestamp?: string;
}

export interface ServiceTicketEvent {
  id: string;
  ticketId: string;
  eventType: string;
  fromStatus?: string;
  toStatus?: string;
  operatorId?: string;
  message?: string;
  payload?: string;
  createTime?: string;
}

export interface ServiceDeskAskReq {
  question: string;
  serviceType?: ServiceDeskType;
  knowledgeBaseId?: string;
  conversationId?: string;
}

export interface ServiceDeskAnswerResp {
  answer: string;
  intent: ServiceDeskIntent | string;
  serviceType: ServiceDeskType | string;
  status: string;
  runId: string;
  ticketId?: string;
  ticketNo?: string;
  conversationId?: string;
  approvalRequired?: boolean;
  pendingTicket?: ServiceTicket;
  events?: AgentRunEvent[];
  feedbackSubmitted?: boolean;
}

export interface ServiceDeskStageEvent {
  stage: string;
  message: string;
}

export interface ServiceDeskTokenEvent {
  text: string;
}

export interface ServiceDeskStreamHandlers {
  onStage?: (event: ServiceDeskStageEvent) => void;
  onToken?: (event: ServiceDeskTokenEvent) => void;
  onDone?: (event: ServiceDeskAnswerResp) => void;
  onError?: (event: { message: string }) => void;
}

export interface ServiceTicket {
  id: string;
  ticketNo: string;
  serviceType: string;
  category?: string;
  priority: string;
  status: TicketStatus;
  title: string;
  description?: string;
  agentSummary?: string;
  creatorId?: string;
  assigneeId?: string;
  sourceRunId?: string;
  sourceConversationId?: string;
  events?: ServiceTicketEvent[];
  eventCount?: number;
  createTime?: string;
  updateTime?: string;
}

export interface ServiceDeskFeedbackReq {
  resolved: boolean;
  comment?: string;
}

export interface ServiceDeskFeedbackResp {
  id: string;
  runId: string;
  ticketId?: string;
  resolved: boolean;
  comment?: string;
  userId?: string;
  createTime?: string;
}

export interface ServiceDeskMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  streaming?: boolean;
  stageMessage?: string;
  intent?: string;
  serviceType?: string;
  status?: string;
  runId?: string;
  ticketId?: string;
  ticketNo?: string;
  conversationId?: string;
  approvalRequired?: boolean;
  pendingTicket?: ServiceTicket;
  events?: AgentRunEvent[];
  feedbackSubmitted?: boolean;
}
