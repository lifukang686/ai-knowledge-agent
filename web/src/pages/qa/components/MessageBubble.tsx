import React from 'react';
import { Bot, User, Clock, RefreshCw } from 'lucide-react';
import { ChatMessage } from '@/types/qa';

interface MessageBubbleProps {
  message: ChatMessage;
}

const formatTime = (timestamp: number): string => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  });
};

const MessageBubble: React.FC<MessageBubbleProps> = ({ message }) => {
  const isUser = message.role === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
      <div className={`flex ${isUser ? 'flex-row-reverse' : 'flex-row'} max-w-[80%] items-start gap-2`}>
        <div
          className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
            isUser ? 'bg-blue-100' : 'bg-green-100'
          }`}
        >
          {isUser ? (
            <User className="w-4 h-4 text-blue-600" />
          ) : (
            <Bot className="w-4 h-4 text-green-600" />
          )}
        </div>

        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-gray-600">
              {isUser ? '你' : 'AI 助手'}
            </span>
            <Clock className="w-3 h-3 text-gray-400" />
            <span className="text-xs text-gray-400">{formatTime(message.timestamp)}</span>
          </div>

          <div
            className={`px-4 py-3 rounded-2xl text-sm leading-relaxed ${
              isUser
                ? 'bg-blue-600 text-white rounded-tr-sm'
                : 'bg-white border border-gray-200 text-gray-800 rounded-tl-sm shadow-sm'
            }`}
          >
            <div className="whitespace-pre-wrap break-words">{message.content}</div>
          </div>

          {!isUser && message.rewrittenQuery && (
            <div className="flex items-center gap-1 mt-1">
              <RefreshCw className="w-3 h-3 text-gray-400" />
              <span className="text-xs text-gray-400 truncate">
                改写查询：{message.rewrittenQuery}
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MessageBubble;