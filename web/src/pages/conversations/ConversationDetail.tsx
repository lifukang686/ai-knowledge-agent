import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Bot, User, Book, Trash2, Archive } from 'lucide-react';
import { toast } from 'sonner';
import { conversationService } from '../../services/conversation';
import { knowledgeBaseService } from '../../services/knowledgeBase';
import { Conversation, Message } from '../../types/conversation';
import { KnowledgeBase } from '../../types/knowledgeBase';
import StatusTag from '../../components/common/StatusTag';

const ConversationDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [knowledgeBase, setKnowledgeBase] = useState<KnowledgeBase | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  // Fetch conversation data
  const fetchConversationData = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      
      // Fetch conversation
      const conv = await conversationService.getConversationById(id);
      setConversation(conv);
      
      // Fetch knowledge base info
      try {
        const kb = await knowledgeBaseService.getKnowledgeBaseById(conv.knowledge_base_id);
        setKnowledgeBase(kb);
      } catch (error) {
        console.error('Failed to fetch knowledge base:', error);
      }
      
      // Fetch messages
      const msgs = await conversationService.getMessages(id);
      setMessages(msgs);
    } catch (error) {
      toast.error('获取对话详情失败');
      console.error('Failed to fetch conversation data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConversationData();
  }, [id]);

  const sendMessage = async () => {
    if (!inputMessage.trim() || !conversation) {
      return;
    }

    try {
      setSending(true);
      const userMessage = inputMessage.trim();
      setInputMessage('');

      // Add user message to UI immediately
      const tempUserMessage: Message = {
        id: `temp_${Date.now()}`,
        conversation_id: conversation.id,
        role: 'user',
        content: userMessage,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      };
      setMessages(prev => [...prev, tempUserMessage]);

      // Send message to API
      const response = await conversationService.sendMessage({
        conversation_id: conversation.id,
        content: userMessage,
        role: 'user'
      });

      // Replace temp message with real messages
      setMessages(prev => {
        const filtered = prev.filter(msg => !msg.id.startsWith('temp_'));
        return [...filtered, response];
      });

      // Update conversation last message time
      setConversation(prev => prev ? {
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

  const handleArchive = async () => {
    if (!conversation) return;
    
    if (!window.confirm('确定要归档这个对话吗？')) {
      return;
    }

    try {
      await conversationService.archiveConversation(conversation.id);
      toast.success('对话已归档');
      navigate('/conversations');
    } catch (error) {
      toast.error('归档对话失败');
      console.error('Failed to archive conversation:', error);
    }
  };

  const handleDelete = async () => {
    if (!conversation) return;
    
    if (!window.confirm('确定要删除这个对话吗？此操作不可恢复。')) {
      return;
    }

    try {
      await conversationService.deleteConversation(conversation.id);
      toast.success('对话已删除');
      navigate('/conversations');
    } catch (error) {
      toast.error('删除对话失败');
      console.error('Failed to delete conversation:', error);
    }
  };

  const formatTime = (timestamp: string) => {
    return new Date(timestamp).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
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
        <p className="text-gray-500">对话不存在</p>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <div className="bg-white border-b border-gray-200 p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <button
                onClick={() => navigate('/conversations')}
                className="flex items-center space-x-2 text-gray-600 hover:text-gray-800"
              >
                <ArrowLeft size={20} />
                <span>返回对话列表</span>
              </button>
              <div>
                <h1 className="text-lg font-semibold text-gray-900">{conversation.title}</h1>
                <div className="flex items-center space-x-4 text-sm text-gray-500">
                  <span>知识库: {knowledgeBase?.name || '未知'}</span>
                  <StatusTag 
                    status={conversation.status} 
                    mapping={{
                      active: { text: '活跃', color: 'green' },
                      completed: { text: '完成', color: 'blue' },
                      archived: { text: '归档', color: 'gray' }
                    }}
                  />
                  <span>{conversation.message_count} 条消息</span>
                </div>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              {conversation.status === 'active' && (
                <button
                  onClick={handleArchive}
                  className="flex items-center space-x-2 px-3 py-2 text-gray-600 hover:text-gray-800 border border-gray-300 rounded-lg hover:bg-gray-50"
                >
                  <Archive size={16} />
                  <span>归档</span>
                </button>
              )}
              <button
                onClick={handleDelete}
                className="flex items-center space-x-2 px-3 py-2 text-red-600 hover:text-red-800 border border-red-300 rounded-lg hover:bg-red-50"
              >
                <Trash2 size={16} />
                <span>删除</span>
              </button>
            </div>
          </div>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.map((message, index) => (
            <div key={message.id} className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-3xl ${message.role === 'user' ? 'order-2' : 'order-1'}`}>
                <div className="flex items-center space-x-2 mb-2">
                  {message.role === 'assistant' ? (
                    <Bot className="h-5 w-5 text-blue-600" />
                  ) : (
                    <User className="h-5 w-5 text-gray-600" />
                  )}
                  <span className="text-sm text-gray-500">{formatTime(message.created_at)}</span>
                </div>
                <div
                  className={`p-4 rounded-lg ${
                    message.role === 'user'
                      ? 'bg-blue-600 text-white'
                      : 'bg-white border border-gray-200 text-gray-900'
                  }`}
                >
                  <div className="whitespace-pre-wrap">{message.content}</div>
                  
                  {/* Sources */}
                  {message.sources && message.sources.length > 0 && (
                    <div className="mt-4 pt-3 border-t border-gray-200">
                      <p className="text-sm font-medium text-gray-700 mb-2">参考来源：</p>
                      <div className="space-y-2">
                        {message.sources.map((source, idx) => (
                          <div key={idx} className="text-xs text-gray-600 bg-gray-50 p-3 rounded border">
                            <div className="flex items-center justify-between mb-1">
                              <span className="font-medium">{source.document_name}</span>
                              <span className="text-blue-600 font-medium">匹配度: {(source.score * 100).toFixed(1)}%</span>
                            </div>
                            <p className="mt-1 text-gray-700">{source.content}</p>
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
        {conversation.status === 'active' && (
          <div className="bg-white border-t border-gray-200 p-4">
            <div className="flex space-x-3">
              <textarea
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="输入您的问题..."
                rows={3}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 resize-none"
                disabled={sending}
              />
              <button
                onClick={sendMessage}
                disabled={!inputMessage.trim() || sending}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                发送
              </button>
            </div>
            <p className="text-xs text-gray-500 mt-2">按 Enter 发送，Shift+Enter 换行</p>
          </div>
        )}
      </div>

      {/* Sidebar - Conversation Info */}
      <div className="w-80 bg-white border-l border-gray-200 p-6">
        <div className="space-y-6">
          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-3">对话信息</h3>
            <div className="space-y-3">
              <div>
                <p className="text-sm text-gray-500">标题</p>
                <p className="font-medium">{conversation.title}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">知识库</p>
                <p className="font-medium">{knowledgeBase?.name || '未知'}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">状态</p>
                <StatusTag 
                  status={conversation.status} 
                  mapping={{
                    active: { text: '活跃', color: 'green' },
                    completed: { text: '完成', color: 'blue' },
                    archived: { text: '归档', color: 'gray' }
                  }}
                />
              </div>
              <div>
                <p className="text-sm text-gray-500">消息数量</p>
                <p className="font-medium">{conversation.message_count} 条</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">创建时间</p>
                <p className="font-medium">{formatTime(conversation.created_at)}</p>
              </div>
              {conversation.last_message_at && (
                <div>
                  <p className="text-sm text-gray-500">最后消息</p>
                  <p className="font-medium">{formatTime(conversation.last_message_at)}</p>
                </div>
              )}
            </div>
          </div>

          <div>
            <h3 className="text-lg font-semibold text-gray-900 mb-3">知识库信息</h3>
            {knowledgeBase ? (
              <div className="space-y-3">
                <div>
                  <p className="text-sm text-gray-500">名称</p>
                  <p className="font-medium">{knowledgeBase.name}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">描述</p>
                  <p className="text-sm text-gray-700">{knowledgeBase.description}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-500">文档数量</p>
                  <p className="font-medium">{knowledgeBase.document_count} 个</p>
                </div>
              </div>
            ) : (
              <p className="text-gray-500">知识库信息加载失败</p>
            )}
          </div>

          {conversation.status === 'active' && (
            <div className="pt-4 border-t border-gray-200">
              <button
                onClick={handleArchive}
                className="w-full flex items-center justify-center space-x-2 px-4 py-2 text-gray-600 hover:text-gray-800 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                <Archive size={16} />
                <span>归档对话</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ConversationDetail;