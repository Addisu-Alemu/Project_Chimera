import React from 'react';

interface StatusBadgeProps {
  status: string;
}

function resolveColor(status: string): string {
  const s = status.toUpperCase();
  if (
    s === 'SUCCESS' ||
    s === 'APPROVED' ||
    s === 'AUTO_DISPATCHED' ||
    s === 'COMPLETED'
  ) {
    return 'bg-green-900 text-green-300 border border-green-700';
  }
  if (s === 'FAILED' || s === 'REJECTED') {
    return 'bg-red-900 text-red-300 border border-red-700';
  }
  if (
    s === 'RETRYING' ||
    s === 'PENDING_APPROVAL' ||
    s === 'HELD_FOR_HUMAN' ||
    s === 'HELD_PENDING_REVIEW' ||
    s === 'PENDING'
  ) {
    return 'bg-yellow-900 text-yellow-300 border border-yellow-700';
  }
  if (s === 'HUMAN_APPROVED') {
    return 'bg-blue-900 text-blue-300 border border-blue-700';
  }
  return 'bg-slate-700 text-slate-300 border border-slate-600';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  return (
    <span
      className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold tracking-wide ${resolveColor(status)}`}
    >
      {status}
    </span>
  );
};
