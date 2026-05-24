import React, { useState, useRef, useEffect } from 'react';
import { Send, Loader2, X } from 'lucide-react';

interface QuestionInputProps {
  onSend: (question: string) => void;
  disabled?: boolean;
  loading?: boolean;
  className?: string;
}

const QuestionInput: React.FC<QuestionInputProps> = ({ onSend, disabled = false, loading = false, className = '' }) => {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 150) + 'px';
    }
  }, [value]);

  const handleSend = () => {
    const trimmed = value.trim();
    if (!trimmed || disabled || loading) return;
    onSend(trimmed);
    setValue('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const handleClear = () => {
    setValue('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
    if (e.key === 'Escape') {
      handleClear();
    }
  };

  return (
    <div className={`border-t border-gray-200 bg-white px-4 py-3 ${className}`}>
      <div className="flex items-end gap-3 max-w-4xl mx-auto">
        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="输入您的问题，Enter 发送，Shift+Enter 换行..."
            rows={1}
            disabled={disabled}
            className="w-full px-4 py-3 pr-10 border border-gray-300 rounded-xl resize-none
                       focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                       disabled:bg-gray-100 disabled:cursor-not-allowed
                       text-sm leading-relaxed placeholder-gray-400"
          />
          {value && (
            <button
              onClick={handleClear}
              className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
              aria-label="清空输入"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        <button
          onClick={handleSend}
          disabled={!value.trim() || disabled || loading}
          className="flex-shrink-0 px-5 py-3 bg-blue-600 text-white rounded-xl
                     hover:bg-blue-700 active:bg-blue-800
                     disabled:opacity-50 disabled:cursor-not-allowed
                     transition-colors duration-150"
          aria-label={loading ? '发送中' : '发送'}
        >
          {loading ? (
            <Loader2 className="w-5 h-5 animate-spin" />
          ) : (
            <Send className="w-5 h-5" />
          )}
        </button>
      </div>

      <p className="text-xs text-gray-400 mt-2 text-center">
        Enter 发送 · Shift+Enter 换行 · Esc 清空
      </p>
    </div>
  );
};

export default QuestionInput;