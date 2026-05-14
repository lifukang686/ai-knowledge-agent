import React, { useEffect } from 'react';
import { X } from 'lucide-react';

interface FormModalProps {
  isOpen: boolean;
  onCancel?: () => void;
  onClose?: () => void;
  title: string;
  children: React.ReactNode;
  width?: string;
  onSubmit?: () => void;
  loading?: boolean;
  submitText?: string;
}

const noop = () => {};

export const FormModal: React.FC<FormModalProps> = ({
  isOpen,
  onCancel,
  onClose,
  title,
  children,
  width = 'max-w-md',
  onSubmit,
  loading = false,
  submitText = '确定',
}) => {
  const handleCancel = onClose || onCancel || noop;

  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        handleCancel();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleEscape);
      document.body.style.overflow = 'hidden';
    }

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, handleCancel]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
        <div className="fixed inset-0 transition-opacity" onClick={handleCancel}>
          <div className="absolute inset-0 bg-gray-500 opacity-75"></div>
        </div>

        <div className={`inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle ${width} sm:w-full`}>
          <div className="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-medium text-gray-900">{title}</h3>
              <button
                type="button"
                className="text-gray-400 hover:text-gray-600 transition-colors"
                onClick={handleCancel}
                disabled={loading}
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {children}
          </div>

          {onSubmit && (
            <div className="bg-gray-50 px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse">
              <button
                type="button"
                className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-primary-600 text-base font-medium text-white hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                onClick={onSubmit}
                disabled={loading}
              >
                {loading ? (
                  <span className="flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    提交中...
                  </span>
                ) : submitText}
              </button>
              <button
                type="button"
                className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500 sm:mt-0 sm:ml-3 sm:w-auto sm:text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                onClick={handleCancel}
                disabled={loading}
              >
                取消
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};