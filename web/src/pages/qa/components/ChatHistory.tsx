import React, { useRef, useEffect } from 'react';
import { MessageSquare } from 'lucide-react';
import { ChatMessage } from '@/types/qa';
import MessageBubble from './MessageBubble';

interface ChatHistoryProps {
  messages: ChatMessage[];
  loading: boolean;
}

const ChatHistory: React.FC<ChatHistoryProps> = ({ messages, loading }) => {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  if (messages.length === 0 && !loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-center px-6">
          <div className="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <MessageSquare className="w-8 h-8 text-blue-400" />
          </div>
          <h3 className="text-lg font-medium text-gray-700 mb-2">开始 RAG 问答</h3>
          <p className="text-sm text-gray-500 max-w-xs">
            选择知识库后在下方输入问题，AI 将基于知识库内容为您生成回答
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto px-4 py-6">
      <div className="max-w-4xl mx-auto">
        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} />
        ))}
        <div ref={bottomRef} />
      </div>
    </div>
  );
};

export default ChatHistory;
