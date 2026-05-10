import React, { useState, useEffect } from 'react';
import { Send, Bot, User, Book, RefreshCw, Plus, Search } from 'lucide-react';
import { toast } from 'sonner';
import { StatusTag } from '@/components/common';
import { SearchBar } from '@/components/common/SearchBar';
import { conversationService } from '@/services/conversation';
import { knowledgeBaseService } from '@/services/knowledgeBase';
import { Conversation, Message } from '@/types/conversation';
import { KnowledgeBase } from '@/types/knowledgeBase';

const QAPage: React.FC = () => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [selectedKB, setSelectedKB] = useState<string>('');
  const [selectedConversation, setSelectedConversation] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [creatingConversation, setCreatingConversation] = useState(false);

  // Fetch initial data
  useEffect(() => {
    fetchInitialData();
  }, []);

  // Fetch messages when conversation changes
  useEffect(() => {
    if (selectedConversation) {
      fetchMessages(selectedConversation.id);
    }
  }, [selectedConversation]);

  const fetchInitialData = async () => {
    try {
      setLoading(true);
      
      // Fetch knowledge bases
      const kbResponse = await knowledgeBaseService.getKnowledgeBases({ page: 1, pageSize: 100 });
      setKnowledgeBases(kbResponse.items);
      
      if (kbResponse.items.length > 0) {
        const firstKbId = kbResponse.items[0].id;
        setSelectedKB(firstKbId);
        // Fetch conversations for first knowledge base
        fetchConversations(firstKbId);
      }
    } catch (error) {
      toast.error('获取初始数据失败');
      console.error('Failed to fetch initial data:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchConversations = async (knowledgeBaseId: string) => {
    try {
      const response = await conversationService.getConversations({
        knowledge_base_id: knowledgeBaseId,
        page: 1,
        pageSize: 20
      });
      let convs = response.data;
      
      // Apply keyword filtering
      if (keyword) {
        convs = convs.filter(conv => 
          conv.title.toLowerCase().includes(keyword.toLowerCase())
        );
      }
      
      setConversations(convs);
      
      // Auto select first conversation
      if (convs.length > 0 && (!selectedConversation || selectedConversation.knowledge_base_id !== knowledgeBaseId)) {
        setSelectedConversation(convs[0]);
      } else if (convs.length === 0) {
        setSelectedConversation(null);
        setMessages([]);
      }
    } catch (error) {
      toast.error('获取对话列表失败');
      console.error('Failed to fetch conversations:', error);
    }
  };

  const fetchMessages = async (conversationId: string) => {
    try {
      const response = await conversationService.getMessages(conversationId);
      setMessages(response);
    } catch (error) {
      toast.error('获取消息失败');
      console.error('Failed to fetch messages:', error);
    }
  };

  const handleKnowledgeBaseChange = (kbId: string) => {
    setSelectedKB(kbId);
    fetchConversations(kbId);
  };

  const createNewConversation = async () => {
    if (!selectedKB) {
      toast.error('请先选择知识库');
      return;
    }

    try {
      setCreatingConversation(true);
      const newConversation = await conversationService.createConversation({
        title: `新对话 ${new Date().toLocaleString('zh-CN')}`,
        knowledge_base_id: selectedKB
      });
      
      setConversations(prev => [newConversation, ...prev]);
      setSelectedConversation(newConversation);
      toast.success('创建新对话成功');
    } catch (error) {
      toast.error('创建对话失败');
      console.error('Failed to create conversation:', error);
    } finally {
      setCreatingConversation(false);
    }
  };

  const sendMessage = async () => {
    if (!inputMessage.trim() || !selectedConversation) {
      return;
    }

    try {
      setSending(true);
      const userMessage = inputMessage.trim();
      setInputMessage('');

      // Add user message to UI immediately
      const tempUserMessage: Message = {
        id: `temp_${Date.now()}`,
        conversation_id: selectedConversation.id,
        role: 'user',
        content: userMessage,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      };
      setMessages(prev => [...prev, tempUserMessage]);

      // Send message to API
      const response = await conversationService.sendMessage({
        conversation_id: selectedConversation.id,
        content: userMessage,
        role: 'user'
      });

      // Replace temp message with real messages
      setMessages(prev => {
        const filtered = prev.filter(msg => !msg.id.startsWith('temp_'));
        return [...filtered, response];
      });

      // Update conversation last message time
      setSelectedConversation(prev => prev ? {
        ...prev,
        last_message_at: new Date().toISOString(),
        message_count: prev.message_count + 2
      } : null);

    } catch (error) {
      toast.error('发送消息失败');
      console.error('Failed to send message:', error);
      // Remove temp message on error
      setMessages(prev => prev.filter(msg => !msg.id.startsWith('temp_')));
    } finally {
      setSending(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const handleSearch = () => {
    fetchConversations(selectedKB);
  };

  const handleReset = () => {
    setKeyword('');
    fetchConversations(selectedKB);
  };

  const formatTime = (timestamp: string) => {
    return new Date(timestamp).toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <div className="w-80 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-xl font-bold text-gray-900">RAG问答</h1>
            <button
              onClick={createNewConversation}
              disabled={creatingConversation}
              className="flex items-center space-x-2 px-3 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
            >
              <Plus size={16} />
              <span>新对话</span>
            </button>
          </div>
          
          {/* Knowledge Base Selector */}
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              选择知识库
            </label>
            <select
              value={selectedKB}
              onChange={(e) => handleKnowledgeBaseChange(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            >
              {knowledgeBases.map(kb => (
                <option key={kb.id} value={kb.id}>
                  {kb.name}
                </option>
              ))}
            </select>
          </div>

          {/* Search */}
          <div className="flex items-center space-x-2">
            <SearchBar
              placeholder="搜索对话"
              value={keyword}
              onChange={setKeyword}
              onSearch={handleSearch}
              className="flex-1"
            />
            <button
              onClick={handleReset}
              className="btn-secondary"
            >
              重置
            </button>
          </div>
        </div>

        {/* Conversations List */}
        <div className="flex-1 overflow-y-auto">
          {conversations.length === 0 ? (
            <div className="p-4 text-center text-gray-500">
              <Book className="h-12 w-12 mx-auto mb-2 text-gray-300" />
              <p>暂无对话</p>
              <p className="text-sm">点击"新对话"开始</p>
            </div>
          ) : (
            <div className="p-2">
              {conversations.map(conversation => (
                <div
                  key={conversation.id}
                  onClick={() => setSelectedConversation(conversation)}
                  className={`p-3 rounded-lg cursor-pointer mb-2 transition-colors ${
                    selectedConversation?.id === conversation.id
                      ? 'bg-primary-50 border border-primary-200'
                      : 'hover:bg-gray-50 border border-transparent'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <h3 className="font-medium text-gray-900 truncate">{conversation.title}</h3>
                    <StatusTag status={conversation.status} />
                  </div>
                  <div className="text-sm text-gray-500">
                    <p>{conversation.message_count} 条消息</p>
                    {conversation.last_message_at && (
                      <p>{formatTime(conversation.last_message_at)}</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Chat Area */}
      <div className="flex-1 flex flex-col">
        {selectedConversation ? (
          <>
            {/* Chat Header */}
            <div className="p-4 bg-white border-b border-gray-200">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-semibold text-gray-900">{selectedConversation.title}</h2>
                  <p className="text-sm text-gray-500">
                    {selectedConversation.message_count} 条消息 · 最后更新 {formatTime(selectedConversation.updated_at)}
                  </p>
                </div>
                <button
                  onClick={() => fetchMessages(selectedConversation.id)}
                  className="flex items-center space-x-2 px-3 py-2 text-gray-600 hover:text-gray-800"
                >
                  <RefreshCw size={16} />
                  <span>刷新</span>
                </button>
              </div>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {messages.map((message, index) => (
                <div key={message.id} className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-2xl ${message.role === 'user' ? 'order-2' : 'order-1'}`}>
                    <div className="flex items-center space-x-2 mb-2">
                      {message.role === 'assistant' ? (
                        <Bot className="h-5 w-5 text-primary-600" />
                      ) : (
                        <User className="h-5 w-5 text-gray-600" />
                      )}
                      <span className="text-sm text-gray-500">{formatTime(message.created_at)}</span>
                    </div>
                    <div
                      className={`p-3 rounded-lg ${
                        message.role === 'user'
                          ? 'bg-primary-600 text-white'
                          : 'bg-white border border-gray-200 text-gray-900'
                      }`}
                    >
                      <div className="whitespace-pre-wrap">{message.content}</div>
                      
                      {/* Sources */}
                      {message.sources && message.sources.length > 0 && (
                        <div className="mt-3 pt-3 border-t border-gray-200">
                          <p className="text-sm font-medium text-gray-700 mb-2">参考来源：</p>
                          <div className="space-y-2">
                            {message.sources.map((source, idx) => (
                              <div key={idx} className="text-xs text-gray-600 bg-gray-50 p-2 rounded">
                                <div className="flex items-center justify-between">
                                  <span className="font-medium">{source.document_name}</span>
                                  <span className="text-primary-600">匹配度：{(source.score * 100).toFixed(1)}%</span>
                                </div>
                                <p className="mt-1 truncate">{source.content}</p>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Input Area */}
            <div className="p-4 bg-white border-t border-gray-200">
              <div className="flex space-x-3">
                <textarea
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="输入您的问题..."
                  rows={2}
                  className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 resize-none"
                  disabled={sending}
                />
                <button
                  onClick={sendMessage}
                  disabled={!inputMessage.trim() || sending}
                  className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Send size={20} />
                </button>
              </div>
              <p className="text-xs text-gray-500 mt-2">按 Enter 发送，Shift+Enter 换行</p>
            </div>
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center bg-gray-50">
            <div className="text-center">
              <Book className="h-16 w-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">开始对话</h3>
              <p className="text-gray-500">选择一个对话或创建新对话开始问答</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default QAPage;
