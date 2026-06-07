import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { Book, ChevronDown, Loader2, MessageSquarePlus, RefreshCw, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { ChatMessage, QaConversation, QaConversationMessage, QaResp } from '@/types/qa';
import { KnowledgeBase } from '@/types/knowledgeBase';
import { qaService } from '@/services/qaService';
import { getKnowledgeBases } from '@/services/knowledgeBase';
import ChatHistory from './components/ChatHistory';
import QuestionInput from './components/QuestionInput';

const generateId = (): string =>
  `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;

const formatQaError = (error: any): string => {
  if (axios.isAxiosError(error) && error.code === 'ECONNABORTED') {
    return '问答生成时间较长，前端等待超时。请稍后重试，或缩小知识库范围后再问。';
  }
  return error?.message || '网络异常，请稍后重试';
};

const toTimestamp = (value?: string): number => {
  const time = value ? new Date(value).getTime() : NaN;
  return Number.isNaN(time) ? Date.now() : time;
};

const toChatMessage = (message: QaConversationMessage): ChatMessage => ({
  id: message.id,
  role: message.role,
  content: message.content,
  timestamp: toTimestamp(message.createTime),
  rewrittenQuery: message.rewrittenQuery,
});

const formatConversationTime = (conversation: QaConversation): string => {
  const raw = conversation.lastMessageAt || conversation.updateTime || conversation.createTime;
  if (!raw) return '';
  return new Date(raw).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const QAPage: React.FC = () => {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKB, setSelectedKB] = useState<string>('');
  const [conversations, setConversations] = useState<QaConversation[]>([]);
  const [activeConversation, setActiveConversation] = useState<QaConversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [sending, setSending] = useState(false);
  const [loadingKB, setLoadingKB] = useState(true);
  const [loadingConversations, setLoadingConversations] = useState(false);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [kbDropdownOpen, setKbDropdownOpen] = useState(false);

  const fetchKnowledgeBases = useCallback(async () => {
    try {
      setLoadingKB(true);
      const response = await getKnowledgeBases({ page: 1, pageSize: 100 });
      setKnowledgeBases(response.items);

      if (response.items.length > 0 && !selectedKB) {
        setSelectedKB(response.items[0].id);
      }
    } catch {
      toast.error('获取知识库列表失败');
    } finally {
      setLoadingKB(false);
    }
  }, [selectedKB]);

  const fetchConversations = useCallback(async () => {
    try {
      setLoadingConversations(true);
      const items = await qaService.listConversations({
        knowledgeBaseId: selectedKB || undefined,
        limit: 50,
      });
      setConversations(items);
    } catch {
      toast.error('获取会话列表失败');
    } finally {
      setLoadingConversations(false);
    }
  }, [selectedKB]);

  useEffect(() => {
    fetchKnowledgeBases();
  }, [fetchKnowledgeBases]);

  useEffect(() => {
    fetchConversations();
    setActiveConversation(null);
    setMessages([]);
  }, [fetchConversations]);

  const handleNewConversation = useCallback(async () => {
    try {
      const conversation = await qaService.createConversation(selectedKB || undefined);
      setActiveConversation(conversation);
      setMessages([]);
      setConversations((prev) => [conversation, ...prev.filter((item) => item.id !== conversation.id)]);
    } catch {
      toast.error('新建会话失败');
    }
  }, [selectedKB]);

  const handleSelectConversation = useCallback(async (conversation: QaConversation) => {
    if (sending || conversation.id === activeConversation?.id) return;

    try {
      setLoadingMessages(true);
      setActiveConversation(conversation);
      const history = await qaService.listMessages(conversation.id);
      setMessages(history.map(toChatMessage));
    } catch {
      toast.error('加载会话消息失败');
    } finally {
      setLoadingMessages(false);
    }
  }, [activeConversation?.id, sending]);

  const refreshActiveConversation = useCallback(async (conversationId?: string) => {
    const items = await qaService.listConversations({
      knowledgeBaseId: selectedKB || undefined,
      limit: 50,
    });
    setConversations(items);
    const current = items.find((item) => item.id === conversationId);
    if (current) {
      setActiveConversation(current);
    }
  }, [selectedKB]);

  const handleSend = useCallback(async (question: string) => {
    if (!question.trim() || sending) return;

    const userMessage: ChatMessage = {
      id: generateId(),
      role: 'user',
      content: question.trim(),
      timestamp: Date.now(),
    };

    const assistantMessageId = generateId();
    const assistantPlaceholder: ChatMessage = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      streaming: true,
      streamStage: 'start',
      streamStageMessage: '请求已发送，正在准备回答',
    };

    setMessages((prev) => [...prev, userMessage, assistantPlaceholder]);
    setSending(true);

    try {
      await qaService.askStream(
        {
          question: question.trim(),
          knowledgeBaseId: selectedKB || undefined,
          conversationId: activeConversation?.id,
        },
        {
          onStage: (event) => {
            setMessages((prev) => prev.map((message) => (
              message.id === assistantMessageId
                ? { ...message, streamStage: event.stage, streamStageMessage: event.message }
                : message
            )));
          },
          onToken: (event) => {
            setMessages((prev) => prev.map((message) => (
              message.id === assistantMessageId
                ? { ...message, content: message.content + event.text }
                : message
            )));
          },
          onDone: (resp: QaResp) => {
            setMessages((prev) => prev.map((message) => (
              message.id === assistantMessageId
                ? {
                    ...message,
                    content: resp.answer || message.content,
                    rewrittenQuery: resp.rewrittenQuery,
                    streaming: false,
                    streamStage: undefined,
                    streamStageMessage: undefined,
                  }
                : message
            )));
            refreshActiveConversation(resp.conversationId);
            if (resp.status === 'no_results') {
              toast.info('未找到相关文档内容');
            }
          },
          onError: (event) => {
            setMessages((prev) => prev.map((message) => (
              message.id === assistantMessageId
                ? {
                    ...message,
                    content: `抱歉，请求失败：${event.message}`,
                    streaming: false,
                    streamStage: undefined,
                    streamStageMessage: undefined,
                  }
                : message
            )));
            toast.error(event.message);
          },
        }
      );
    } catch (error: any) {
      const failureReason = formatQaError(error);
      setMessages((prev) => prev.map((message) => (
        message.id === assistantMessageId
          ? {
              ...message,
              content: `抱歉，请求失败：${failureReason}`,
              streaming: false,
              streamStage: undefined,
              streamStageMessage: undefined,
            }
          : message
      )));
      toast.error(failureReason);
    } finally {
      setSending(false);
    }
  }, [activeConversation?.id, refreshActiveConversation, selectedKB, sending]);

  const handleClearCurrent = () => {
    setActiveConversation(null);
    setMessages([]);
    toast.success('已清空当前窗口');
  };

  const handleRefresh = () => {
    fetchKnowledgeBases();
    fetchConversations();
    toast.success('列表已刷新');
  };

  const selectedKBName = knowledgeBases.find((kb) => kb.id === selectedKB)?.name || '选择知识库';

  return (
    <div className="flex h-full bg-gray-50 rounded-lg overflow-hidden">
      <aside className="w-72 flex-shrink-0 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-3 border-b border-gray-200">
          <button
            onClick={handleNewConversation}
            disabled={sending}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 text-sm font-medium
                       text-white bg-blue-600 rounded-lg hover:bg-blue-700
                       disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <MessageSquarePlus className="w-4 h-4" />
            新建对话
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-2">
          {loadingConversations ? (
            <div className="flex items-center justify-center py-8 text-gray-400">
              <Loader2 className="w-5 h-5 animate-spin" />
            </div>
          ) : conversations.length === 0 ? (
            <div className="px-3 py-8 text-center text-sm text-gray-400">暂无历史对话</div>
          ) : (
            <div className="space-y-1">
              {conversations.map((conversation) => (
                <button
                  key={conversation.id}
                  onClick={() => handleSelectConversation(conversation)}
                  className={`w-full text-left px-3 py-2 rounded-lg transition-colors ${
                    activeConversation?.id === conversation.id
                      ? 'bg-blue-50 text-blue-700'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <div className="text-sm font-medium truncate">{conversation.title || '新会话'}</div>
                  <div className="mt-1 flex items-center justify-between gap-2 text-xs text-gray-400">
                    <span>{conversation.messageCount} 条消息</span>
                    <span className="truncate">{formatConversationTime(conversation)}</span>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </aside>

      <section className="min-w-0 flex-1 flex flex-col">
        <header className="flex-shrink-0 bg-white border-b border-gray-200 px-4 py-3">
          <div className="max-w-5xl mx-auto flex items-center justify-between">
            <div className="flex items-center gap-4 min-w-0">
              <h1 className="text-lg font-semibold text-gray-900 whitespace-nowrap">RAG 知识库问答</h1>

              <div className="relative">
                <button
                  onClick={() => setKbDropdownOpen(!kbDropdownOpen)}
                  disabled={loadingKB || sending}
                  className="flex items-center gap-2 px-3 py-1.5 text-sm border border-gray-300 rounded-lg
                             hover:border-blue-400 hover:text-blue-600 transition-colors
                             disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Book className="w-4 h-4" />
                  <span className="max-w-[160px] truncate">
                    {loadingKB ? '加载中...' : selectedKBName}
                  </span>
                  <ChevronDown className={`w-4 h-4 transition-transform ${kbDropdownOpen ? 'rotate-180' : ''}`} />
                </button>

                {kbDropdownOpen && (
                  <>
                    <div className="fixed inset-0 z-10" onClick={() => setKbDropdownOpen(false)} />
                    <div className="absolute top-full left-0 mt-1 w-64 bg-white border border-gray-200 rounded-lg shadow-lg z-20 max-h-60 overflow-y-auto">
                      {knowledgeBases.length === 0 ? (
                        <div className="px-3 py-2 text-sm text-gray-500">暂无知识库</div>
                      ) : (
                        knowledgeBases.map((kb) => (
                          <button
                            key={kb.id}
                            onClick={() => {
                              setSelectedKB(kb.id);
                              setKbDropdownOpen(false);
                            }}
                            className={`w-full text-left px-3 py-2 text-sm hover:bg-gray-50 transition-colors ${
                              kb.id === selectedKB ? 'bg-blue-50 text-blue-700 font-medium' : 'text-gray-700'
                            }`}
                          >
                            <div className="truncate">{kb.name}</div>
                            {kb.document_count != null && (
                              <div className="text-xs text-gray-400">{kb.document_count} 个文档</div>
                            )}
                          </button>
                        ))
                      )}
                    </div>
                  </>
                )}
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={handleRefresh}
                disabled={sending}
                className="flex items-center gap-1 px-3 py-1.5 text-sm text-gray-600 hover:text-gray-800
                           hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50"
              >
                <RefreshCw className="w-4 h-4" />
                <span className="hidden sm:inline">刷新</span>
              </button>
              <button
                onClick={handleClearCurrent}
                disabled={messages.length === 0 || sending}
                className="flex items-center gap-1 px-3 py-1.5 text-sm text-red-600 hover:text-red-700
                           hover:bg-red-50 rounded-lg transition-colors
                           disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Trash2 className="w-4 h-4" />
                <span className="hidden sm:inline">清空</span>
              </button>
            </div>
          </div>
        </header>

        {loadingMessages ? (
          <div className="flex-1 flex items-center justify-center text-gray-400">
            <Loader2 className="w-6 h-6 animate-spin" />
          </div>
        ) : (
          <ChatHistory messages={messages} loading={sending} />
        )}

        <QuestionInput
          onSend={handleSend}
          disabled={!selectedKB || loadingMessages}
          loading={sending}
          className="flex-shrink-0"
        />
      </section>
    </div>
  );
};

export default QAPage;
