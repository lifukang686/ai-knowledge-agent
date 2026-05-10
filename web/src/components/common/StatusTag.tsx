import React from 'react';
import { getStatusColor, getStatusText } from '@/utils/format';

interface StatusTagProps {
  status: string;
  className?: string;
}

export const StatusTag: React.FC<StatusTagProps> = ({ status, className = '' }) => {
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(status)} ${className}`}>
      {getStatusText(status)}
    </span>
  );
};