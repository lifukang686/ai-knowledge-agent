import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Bot,
  Briefcase,
  CheckCircle2,
  ChevronDown,
  Clock,
  FileText,
  Loader2,
  RefreshCw,
  Send,
  ShieldCheck,
  ThumbsDown,
  ThumbsUp,
  User,
} from 'lucide-react';
import { toast } from 'sonner';
import { getKnowledgeBases } from '@/services/knowledgeBase';
import { serviceDeskService } from '@/services/serviceDesk';
import { KnowledgeBase } from '@/types/knowledgeBase';
import {
  AgentRunEvent,
  ServiceDeskMessage,
  ServiceDeskType,
  ServiceTicket,
} from '@/types/serviceDesk';

const generateId = () => `sd_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;

const intentLabel: Record<string, string> = {
  knowledge_qa: '知识问答',
  create_ticket: '创建工单',
  query_ticket: '查询工单',
  collect_info: '补充信息',
  handoff_human: '人工介入',
  summarize_document: '文档总结',
};

const eventLabel: Record<string, string> = {
  reasoning: '推理',
  tool_call: '工具调用',
  observation: '观察结果',
  final_answer: '最终回答',
  error: '异常',
  plan: '计划',
};

const serviceTypeOptions: Array<{ value: ServiceDeskType; label: string }> = [
  { value: 'AUTO', label: '自动识别' },
  { value: 'IT', label: 'IT 服务' },
  { value: 'HR', label: 'HR 服务' },
];

const formatTime = (timestamp: number) =>
  new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

const badgeClass = (status: string) => {
  switch (status) {
    case 'DRAFT':
      return 'bg-violet-50 text-violet-700 border-violet-100';
    case 'OPEN':
      return 'bg-blue-50 text-blue-700 border-blue-100';
    case 'PROCESSING':
      return 'bg-amber-50 text-amber-700 border-amber-100';
    case 'RESOLVED':
      return 'bg-emerald-50 text-emerald-700 border-emerald-100';
    case 'CLOSED':
      return 'bg-gray-100 text-gray-600 border-gray-200';
    default:
      return 'bg-slate-50 text-slate-600 border-slate-100';
  }
};

const RunEventTimeline: React.FC<{ events?: AgentRunEvent[] }> = ({ events = [] }) => {
  const [open, setOpen] = useState(false);
  if (events.length === 0) return null;

  return (
    <div className="mt-2 rounded-xl border border-slate-200 bg-white">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="w-full flex items-center justify-between px-3 py-2 text-xs text-slate-600 hover:bg-slate-50 rounded-xl"
      >
        <span>查看执行过程 · {events.length} 个事件</span>
        <ChevronDown className={`w-4 h-4 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && (
        <div className="px-3 pb-3 space-y-2">
          {events.map((event, index) => (
            <div key={`${event.type}_${event.timestamp}_${index}`} className="flex gap-2 text-xs">
              <div className="mt-1 w-2 h-2 rounded-full bg-emerald-500" />
              <div className="flex-1">
                <div className="flex flex-wrap gap-2 text-slate-500">
                  <span className="font-medium text-slate-700">{eventLabel[event.type] || event.type}</span>
                  {event.toolName && <span>工具：{event.toolName}</span>}
                  {event.success === false && <span className="text-red-600">失败</span>}
                </div>
                <div className="mt-1 text-slate-700">{event.message || '已记录服务台事件'}</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const ApprovalCard: React.FC<{
  ticket?: ServiceTicket;
  disabled?: boolean;
  onConfirm: () => void;
}> = ({ ticket, disabled, onConfirm }) => {
  if (!ticket || ticket.status !== 'DRAFT') return null;

  return (
    <div className="mt-2 rounded-2xl border border-violet-200 bg-violet-50 p-4 text-sm">
      <div className="flex items-start gap-3">
        <ShieldCheck className="w-5 h-5 text-violet-600 mt-0.5" />
        <div className="flex-1">
          <div className="font-medium text-violet-950">工单草稿待确认</div>
          <div className="mt-1 text-violet-800">{ticket.title}</div>
          <div className="mt-3 grid grid-cols-2 gap-2 text-xs text-violet-700">
            <span>编号：{ticket.ticketNo}</span>
            <span>类型：{ticket.serviceType}</span>
            <span>分类：{ticket.category || '综合'}</span>
            <span>优先级：{ticket.priority}</span>
          </div>
          {ticket.agentSummary && (
            <div className="mt-3 rounded-xl bg-white/70 p-3 text-xs text-violet-900">
              {ticket.agentSummary}
            </div>
          )}
          <button
            type="button"
            disabled={disabled}
            onClick={onConfirm}
            className="mt-3 inline-flex items-center gap-2 rounded-xl bg-violet-600 px-4 py-2 text-white hover:bg-violet-700 disabled:opacity-60"
          >
            {disabled ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
            确认创建
          </button>
        </div>
      </div>
    </div>
  );
};

const FeedbackBar: React.FC<{
  submitted?: boolean;
  disabled?: boolean;
  onSubmit: (resolved: boolean, comment?: string) => void;
}> = ({ submitted, disabled, onSubmit }) => {
  const [comment, setComment] = useState('');
  if (submitted) {
    return <div className="mt-2 text-xs text-emerald-600">已收到你的反馈，谢谢。</div>;
  }

  return (
    <div className="mt-2 rounded-xl border border-slate-200 bg-white p-3 text-xs text-slate-600">
      <div className="mb-2">这次处理是否解决了问题？</div>
      <textarea
        value={comment}
        onChange={(event) => setComment(event.target.value)}
        placeholder="可选：补充一句反馈，方便后续优化服务台 Agent"
        className="mb-2 w-full resize-none rounded-lg border border-slate-200 px-3 py-2 focus:border-blue-400 focus:ring-1 focus:ring-blue-400"
      />
      <div className="flex gap-2">
        <button
          type="button"
          disabled={disabled}
          onClick={() => onSubmit(true, comment)}
          className="inline-flex items-center gap-1 rounded-lg bg-emerald-50 px-3 py-1.5 text-emerald-700 hover:bg-emerald-100 disabled:opacity-60"
        >
          <ThumbsUp className="w-3 h-3" />
          已解决
        </button>
        <button
          type="button"
          disabled={disabled}
          onClick={() => onSubmit(false, comment)}
          className="inline-flex items-center gap-1 rounded-lg bg-amber-50 px-3 py-1.5 text-amber-700 hover:bg-amber-100 disabled:opacity-60"
        >
          <ThumbsDown className="w-3 h-3" />
          未解决
        </button>
      </div>
    </div>
  );
};

const ServiceDeskPage: React.FC = () => {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKB, setSelectedKB] = useState('');
  const [serviceType, setServiceType] = useState<ServiceDeskType>('AUTO');
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<ServiceDeskMessage[]>([]);
  const [tickets, setTickets] = useState<ServiceTicket[]>([]);
  const [sending, setSending] = useState(false);
  const [loadingTickets, setLoadingTickets] = useState(false);
  const [confirmingTicketId, setConfirmingTicketId] = useState<string>();
  const [feedbackRunId, setFeedbackRunId] = useState<string>();
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadKnowledgeBases = useCallback(async () => {
    try {
      const response = await getKnowledgeBases({ page: 1, pageSize: 100 });
      setKnowledgeBases(response.items);
      if (response.items.length > 0 && !selectedKB) {
        setSelectedKB(response.items[0].id);
      }
    } catch {
      toast.error('获取知识库列表失败');
    }
  }, [selectedKB]);

  const loadTickets = useCallback(async () => {
    try {
      setLoadingTickets(true);
      const response = await serviceDeskService.getTickets({ page: 1, pageSize: 10 });
      setTickets(response.items);
    } catch {
      toast.error('获取工单列表失败');
    } finally {
      setLoadingTickets(false);
    }
  }, []);

  useEffect(() => {
    loadKnowledgeBases();
    loadTickets();
  }, [loadKnowledgeBases, loadTickets]);

  const handleConfirmTicket = useCallback(async (messageId: string, ticketId?: string) => {
    if (!ticketId) return;
    try {
      setConfirmingTicketId(ticketId);
      const ticket = await serviceDeskService.confirmTicket(ticketId);
      setMessages((prev) => prev.map((message) => (
        message.id === messageId
          ? {
              ...message,
              approvalRequired: false,
              pendingTicket: ticket,
              status: 'success',
              content: `${message.content}\n\n已确认创建，工单 ${ticket.ticketNo} 当前状态为 ${ticket.status}。`,
            }
          : message
      )));
      await loadTickets();
      toast.success('工单已确认创建');
    } catch (error: any) {
      toast.error(error?.message || '确认工单失败');
    } finally {
      setConfirmingTicketId(undefined);
    }
  }, [loadTickets]);

  const handleSubmitFeedback = useCallback(async (messageId: string, runId?: string, resolved?: boolean, comment?: string) => {
    if (!runId || resolved == null) return;
    try {
      setFeedbackRunId(runId);
      await serviceDeskService.submitFeedback(runId, { resolved, comment: comment?.trim() || undefined });
      setMessages((prev) => prev.map((message) => (
        message.id === messageId ? { ...message, feedbackSubmitted: true } : message
      )));
      toast.success('反馈已提交');
    } catch (error: any) {
      toast.error(error?.message || '提交反馈失败');
    } finally {
      setFeedbackRunId(undefined);
    }
  }, []);

  const handleSend = useCallback(async () => {
    const trimmed = question.trim();
    if (!trimmed || sending) return;

    const userMessage: ServiceDeskMessage = {
      id: generateId(),
      role: 'user',
      content: trimmed,
      timestamp: Date.now(),
    };
    const assistantId = generateId();
    const assistantMessage: ServiceDeskMessage = {
      id: assistantId,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      streaming: true,
      stageMessage: '正在接入企业服务台 Agent',
      events: [],
    };

    setMessages((prev) => [...prev, userMessage, assistantMessage]);
    setQuestion('');
    setSending(true);

    try {
      await serviceDeskService.askStream(
        {
          question: trimmed,
          serviceType,
          knowledgeBaseId: selectedKB || undefined,
        },
        {
          onStage: (event) => {
            setMessages((prev) => prev.map((message) => (
              message.id === assistantId ? { ...message, stageMessage: event.message } : message
            )));
          },
          onToken: (event) => {
            setMessages((prev) => prev.map((message) => (
              message.id === assistantId ? { ...message, content: message.content + event.text } : message
            )));
          },
          onDone: (result) => {
            setMessages((prev) => prev.map((message) => (
              message.id === assistantId
                ? {
                    ...message,
                    content: result.answer || message.content,
                    streaming: false,
                    stageMessage: undefined,
                    intent: result.intent,
                    serviceType: result.serviceType,
                    status: result.status,
                    runId: result.runId,
                    ticketId: result.ticketId,
                    ticketNo: result.ticketNo,
                    conversationId: result.conversationId,
                    approvalRequired: result.approvalRequired,
                    pendingTicket: result.pendingTicket,
                    events: result.events,
                    feedbackSubmitted: result.feedbackSubmitted,
                  }
                : message
            )));
            if (result.ticketNo) {
              loadTickets();
            }
          },
          onError: (event) => {
            setMessages((prev) => prev.map((message) => (
              message.id === assistantId
                ? {
                    ...message,
                    content: `抱歉，服务台处理失败：${event.message}`,
                    streaming: false,
                    stageMessage: undefined,
                  }
                : message
            )));
            toast.error(event.message);
          },
        },
      );
    } catch (error: any) {
      const message = error?.message || '服务台请求失败，请稍后重试';
      setMessages((prev) => prev.map((item) => (
        item.id === assistantId
          ? { ...item, content: `抱歉，服务台处理失败：${message}`, streaming: false, stageMessage: undefined }
          : item
      )));
      toast.error(message);
    } finally {
      setSending(false);
    }
  }, [question, sending, serviceType, selectedKB, loadTickets]);

  const handleKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="h-full bg-slate-50 rounded-lg overflow-hidden">
      <div className="h-full grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_360px]">
        <section className="flex flex-col min-h-0 border-r border-slate-200">
          <header className="bg-white border-b border-slate-200 px-5 py-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h1 className="text-xl font-semibold text-slate-900">企业 IT/HR 服务台 Agent</h1>
                <p className="text-sm text-slate-500 mt-1">
                  自动判断知识问答、创建工单、查询工单或人工介入，写操作先确认再执行。
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                <select
                  value={serviceType}
                  onChange={(event) => setServiceType(event.target.value as ServiceDeskType)}
                  className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
                >
                  {serviceTypeOptions.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
                <select
                  value={selectedKB}
                  onChange={(event) => setSelectedKB(event.target.value)}
                  className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white min-w-[180px]"
                >
                  {knowledgeBases.length === 0 ? (
                    <option value="">暂无知识库</option>
                  ) : (
                    knowledgeBases.map((kb) => (
                      <option key={kb.id} value={kb.id}>{kb.name}</option>
                    ))
                  )}
                </select>
              </div>
            </div>
          </header>

          <main className="flex-1 overflow-y-auto px-5 py-6">
            <div className="max-w-4xl mx-auto space-y-4">
              {messages.length === 0 && (
                <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center shadow-sm">
                  <div className="w-14 h-14 rounded-2xl bg-blue-50 mx-auto flex items-center justify-center mb-4">
                    <Briefcase className="w-7 h-7 text-blue-600" />
                  </div>
                  <h2 className="text-lg font-medium text-slate-900">试试企业服务台场景</h2>
                  <p className="text-sm text-slate-500 mt-2">
                    例如：“VPN 连不上，帮我报修” 或 “查一下我的工单进度”。
                  </p>
                </div>
              )}

              {messages.map((message) => {
                const isUser = message.role === 'user';
                return (
                  <div key={message.id} className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
                    <div className={`flex gap-3 max-w-[86%] ${isUser ? 'flex-row-reverse' : ''}`}>
                      <div className={`w-9 h-9 rounded-full flex items-center justify-center ${
                        isUser ? 'bg-blue-100' : 'bg-emerald-100'
                      }`}>
                        {isUser ? <User className="w-4 h-4 text-blue-600" /> : <Bot className="w-4 h-4 text-emerald-600" />}
                      </div>
                      <div className="space-y-1 flex-1">
                        <div className={`flex items-center gap-2 text-xs text-slate-400 ${isUser ? 'justify-end' : ''}`}>
                          <span>{isUser ? '你' : '服务台 Agent'}</span>
                          <Clock className="w-3 h-3" />
                          <span>{formatTime(message.timestamp)}</span>
                        </div>
                        <div className={`rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-sm ${
                          isUser
                            ? 'bg-blue-600 text-white rounded-tr-sm'
                            : 'bg-white border border-slate-200 text-slate-800 rounded-tl-sm'
                        }`}>
                          {message.content ? (
                            <div className="whitespace-pre-wrap break-words">{message.content}</div>
                          ) : (
                            <div className="flex items-center gap-2 text-slate-500">
                              <Loader2 className="w-4 h-4 animate-spin" />
                              <span>{message.stageMessage || '正在处理'}</span>
                            </div>
                          )}
                        </div>
                        {!isUser && message.streaming && message.content && (
                          <div className="flex items-center gap-1 text-xs text-slate-400">
                            <Loader2 className="w-3 h-3 animate-spin" />
                            <span>{message.stageMessage || '正在生成结果'}</span>
                          </div>
                        )}
                        {!isUser && !message.streaming && (message.intent || message.ticketNo || message.runId) && (
                          <div className="flex flex-wrap gap-2 text-xs text-slate-500">
                            {message.intent && <span className="px-2 py-1 rounded-full bg-slate-100">意图：{intentLabel[message.intent] || message.intent}</span>}
                            {message.serviceType && <span className="px-2 py-1 rounded-full bg-slate-100">类型：{message.serviceType}</span>}
                            {message.ticketNo && <span className="px-2 py-1 rounded-full bg-blue-50 text-blue-700">工单：{message.ticketNo}</span>}
                            {message.runId && <span className="px-2 py-1 rounded-full bg-slate-100">Run：{message.runId}</span>}
                          </div>
                        )}
                        {!isUser && !message.streaming && (
                          <>
                            <ApprovalCard
                              ticket={message.pendingTicket}
                              disabled={confirmingTicketId === message.ticketId}
                              onConfirm={() => handleConfirmTicket(message.id, message.pendingTicket?.id || message.ticketId)}
                            />
                            <RunEventTimeline events={message.events} />
                            {message.runId && (
                              <FeedbackBar
                                submitted={message.feedbackSubmitted}
                                disabled={feedbackRunId === message.runId}
                                onSubmit={(resolved, comment) => handleSubmitFeedback(message.id, message.runId, resolved, comment)}
                              />
                            )}
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
              <div ref={bottomRef} />
            </div>
          </main>

          <footer className="bg-white border-t border-slate-200 px-5 py-4">
            <div className="max-w-4xl mx-auto flex gap-3">
              <textarea
                value={question}
                onChange={(event) => setQuestion(event.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="输入 IT/HR 问题，Enter 发送，Shift+Enter 换行..."
                className="flex-1 min-h-[48px] max-h-[140px] resize-none rounded-xl border border-slate-300 px-4 py-3 text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
              <button
                onClick={handleSend}
                disabled={!question.trim() || sending}
                className="px-5 py-3 rounded-xl bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {sending ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
              </button>
            </div>
          </footer>
        </section>

        <aside className="bg-white min-h-0 flex flex-col">
          <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
            <div>
              <h2 className="font-semibold text-slate-900 flex items-center gap-2">
                <FileText className="w-5 h-5 text-blue-600" />
                我的工单
              </h2>
              <p className="text-xs text-slate-500 mt-1">展示最近 10 条服务台工单，草稿需确认后才打开</p>
            </div>
            <button
              onClick={loadTickets}
              className="p-2 rounded-lg hover:bg-slate-100 text-slate-500"
              aria-label="刷新工单"
            >
              <RefreshCw className={`w-4 h-4 ${loadingTickets ? 'animate-spin' : ''}`} />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {tickets.length === 0 && !loadingTickets && (
              <div className="text-sm text-slate-500 bg-slate-50 border border-slate-100 rounded-xl p-4">
                暂无工单。你可以让 Agent 帮你报修或提交申请。
              </div>
            )}
            {tickets.map((ticket) => (
              <div key={ticket.id} className="border border-slate-200 rounded-xl p-4 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-medium text-slate-900">{ticket.title}</div>
                    <div className="text-xs text-slate-400 mt-1">{ticket.ticketNo}</div>
                  </div>
                  <span className={`text-xs px-2 py-1 rounded-full border ${badgeClass(ticket.status)}`}>
                    {ticket.status}
                  </span>
                </div>
                <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
                  <span className="px-2 py-1 bg-slate-100 rounded-full">{ticket.serviceType}</span>
                  <span className="px-2 py-1 bg-slate-100 rounded-full">{ticket.category || '综合'}</span>
                  <span className="px-2 py-1 bg-slate-100 rounded-full">{ticket.priority}</span>
                  {(ticket.eventCount || 0) > 0 && (
                    <span className="px-2 py-1 bg-slate-100 rounded-full">事件 {ticket.eventCount}</span>
                  )}
                </div>
                {ticket.createTime && (
                  <div className="text-xs text-slate-400 mt-3">{ticket.createTime}</div>
                )}
              </div>
            ))}
          </div>
        </aside>
      </div>
    </div>
  );
};

export default ServiceDeskPage;
