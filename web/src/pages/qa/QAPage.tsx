import React, { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { Book, RefreshCw, Trash2, ChevronDown } from 'lucide-react';
import { toast } from 'sonner';
import { ChatMessage, QaResp } from '@/types/qa';
import { KnowledgeBase } from '@/types/knowledgeBase';
import { qaService } from '@/services/qaService';
import { getKnowledgeBases } from '@/services/knowledgeBase';
import ChatHistory from './components/ChatHistory';
import QuestionInput from './components/QuestionInput';

const STORAGE_KEY = 'rag_qa_messages';

const loadMessages = (): ChatMessage[] => {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

const saveMessages = (messages: ChatMessage[]) => {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages));
  } catch { /* storage full, ignore */ }
};

const generateId = (): string =>
  `msg_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;

const formatQaError = (error: any): string => {
  if (axios.isAxiosError(error) && error.code === 'ECONNABORTED') {
    return '问答生成时间较长，前端等待超时。请稍后重试，或缩小知识库范围后再问。';
  }
  return error?.message || '网络异常，请稍后重试';
};

const QAPage: React.FC = () => {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKB, setSelectedKB] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>(loadMessages);
  const [sending, setSending] = useState(false);
  const [loadingKB, setLoadingKB] = useState(true);
  const [kbDropdownOpen, setKbDropdownOpen] = useState(false);

  useEffect(() => {
    fetchKnowledgeBases();
  }, []);

  useEffect(() => {
    saveMessages(messages);
  }, [messages]);

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
  }, [selectedKB, sending]);

  const handleClearHistory = () => {
    setMessages([]);
    sessionStorage.removeItem(STORAGE_KEY);
    toast.success('对话历史已清空');
  };

  const handleRefresh = () => {
    fetchKnowledgeBases();
    toast.success('知识库列表已刷新');
  };

  const selectedKBName = knowledgeBases.find((kb) => kb.id === selectedKB)?.name || '选择知识库';

  return (
    <div className="flex flex-col h-full bg-gray-50 rounded-lg overflow-hidden">
      {/* Header */}
      <header className="flex-shrink-0 bg-white border-b border-gray-200 px-4 py-3">
        <div className="max-w-5xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-4">
            <h1 className="text-lg font-semibold text-gray-900">RAG 知识库问答</h1>

            {/* Knowledge Base Dropdown */}
            <div className="relative">
              <button
                onClick={() => setKbDropdownOpen(!kbDropdownOpen)}
                disabled={loadingKB}
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
              className="flex items-center gap-1 px-3 py-1.5 text-sm text-gray-600 hover:text-gray-800
                         hover:bg-gray-100 rounded-lg transition-colors"
            >
              <RefreshCw className="w-4 h-4" />
              <span className="hidden sm:inline">刷新</span>
            </button>
            <button
              onClick={handleClearHistory}
              disabled={messages.length === 0}
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

      {/* Chat History */}
      <ChatHistory messages={messages} loading={sending} />

      {/* Input */}
      <QuestionInput
        onSend={handleSend}
        disabled={!selectedKB}
        loading={sending}
        className="flex-shrink-0"
      />
    </div>
  );
};

export default QAPage;
