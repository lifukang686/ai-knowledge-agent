import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Send, Bot, User, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';
import { conversationService } from '../../services/conversation';
import { Conversation, Message, MessageCreateRequest } from '../../types/conversation';

const ConversationDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Fetch conversation and messages
  const fetchConversationData = async () => {
    if (!id) return;

    try {
      setLoading(true);
      const [convData, messagesData] = await Promise.all([
        conversationService.getConversationById(id),
        conversationService.getMessages(id)
      ]);
      
      setConversation(convData);
      setMessages(messagesData);
    } catch (error) {
      toast.error('获取会话详情失败');
      console.error('Failed to fetch conversation:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConversationData();
  }, [id]);

  // Scroll to bottom when messages change
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSendMessage = async () => {
    if (!inputValue.trim() || !id || sending) return;

    const messageContent = inputValue.trim();
    setInputValue('');
    setSending(true);

    try {
      const messageData: MessageCreateRequest = {
        conversation_id: id,
        content: messageContent,
        role: 'user'
      };

      const response = await conversationService.sendMessage(messageData);
      
      // Refresh messages
      const updatedMessages = await conversationService.getMessages(id);
      setMessages(updatedMessages);
      
      // Update conversation message count
      if (conversation) {
        setConversation(prev => prev ? {
          ...prev,
          message_count: prev.message_count + 2 // User message + assistant response
        } : null);
      }
    } catch (error) {
      toast.error('发送消息失败');
      console.error('Failed to send message:', error);
    } finally {
      setSending(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const formatMessageTime = (timestamp: string) => {
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

  if (!conversation) {
    return (
      <div className="text-center py-8">
        <p className="text-gray-500">会话不存在</p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <button
              onClick={() => navigate('/qa')}
              className="flex items-center space-x-2 text-gray-600 hover:text-gray-800"
            >
              <ArrowLeft size={20} />
              <span>返回问答</span>
            </button>
            <div>
              <h1 className="text-xl font-semibold text-gray-900">{conversation.title}</h1>
              <p className="text-sm text-gray-600">
                {conversation.message_count} 条消息 · 状态: {conversation.status}
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-3">
            <button
              onClick={fetchConversationData}
              className="flex items-center space-x-2 px-3 py-2 text-gray-600 hover:text-gray-800 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              <RefreshCw size={16} />
              <span>刷新</span>
            </button>
          </div>
        </div>
      </div>

      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto px-6 py-4">
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-gray-500">
            <Bot className="h-16 w-16 text-gray-300 mb-4" />
            <h3 className="text-lg font-medium mb-2">开始对话</h3>
            <p className="text-center max-w-md">
              在下方输入框中输入您的问题，开始与AI助手对话。
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {messages.map((message) => (
              <div key={message.id} className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`flex items-start space-x-3 max-w-3xl ${message.role === 'user' ? 'flex-row-reverse space-x-reverse' : ''}`}>
                  <div className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
                    message.role === 'user' 
                      ? 'bg-blue-600 text-white' 
                      : 'bg-green-600 text-white'
                  }`}>
                    {message.role === 'user' ? <User size={16} /> : <Bot size={16} />}
                  </div>
                  <div className={`flex-1 ${message.role === 'user' ? 'text-right' : ''}`}>
                    <div className={`inline-block px-4 py-3 rounded-lg ${
                      message.role === 'user'
                        ? 'bg-blue-600 text-white'
                        : 'bg-white border border-gray-200 text-gray-900'
                    }`}>
                      <div className="whitespace-pre-wrap">{message.content}</div>
                    </div>
                    <div className="text-xs text-gray-500 mt-1">
                      {formatMessageTime(message.created_at)}
                    </div>
                    
                    {/* Sources */}
                    {message.sources && message.sources.length > 0 && (
                      <div className="mt-3 text-left">
                        <p className="text-xs text-gray-500 mb-2">参考来源：</p>
                        <div className="space-y-2">
                          {message.sources.map((source, index) => (
                            <div key={index} className="text-xs bg-gray-50 border border-gray-200 rounded p-2">
                              <div className="flex items-center justify-between mb-1">
                                <span className="font-medium text-gray-700">{source.document_name}</span>
                                <span className="text-gray-500">匹配度: {(source.score * 100).toFixed(1)}%</span>
                              </div>
                              <div className="text-gray-600 truncate">{source.content}</div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
            {sending && (
              <div className="flex justify-start">
                <div className="flex items-start space-x-3">
                  <div className="flex-shrink-0 w-8 h-8 rounded-full bg-green-600 text-white flex items-center justify-center">
                    <Bot size={16} />
                  </div>
                  <div className="bg-white border border-gray-200 rounded-lg px-4 py-3">
                    <div className="flex items-center space-x-2 text-gray-500">
                      <RefreshCw size={16} className="animate-spin" />
                      <span>正在思考中...</span>
                    </div>
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Input Area */}
      <div className="bg-white border-t border-gray-200 px-6 py-4">
        <div className="flex items-end space-x-3">
          <div className="flex-1">
            <textarea
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="输入您的问题..."
              disabled={sending}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg resize-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-50 disabled:cursor-not-allowed"
              rows={3}
            />
            <div className="flex justify-between items-center mt-2">
              <span className="text-xs text-gray-500">
                按 Enter 发送，Shift+Enter 换行
              </span>
              <span className="text-xs text-gray-500">
                {inputValue.length}/1000
              </span>
            </div>
          </div>
          <button
            onClick={handleSendMessage}
            disabled={!inputValue.trim() || sending}
            className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
          >
            <Send size={16} />
            <span>发送</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConversationDetail;