import React, { useState, useCallback } from 'react';
import { getTransactions } from '../api/actService';
import { usePolling } from '../hooks/usePolling';
import { StatusBadge } from './StatusBadge';
import type { Transaction } from '../types';

export const TransactionLedger: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const fetchTransactions = useCallback(async () => {
    try {
      const data = await getTransactions();
      setTransactions(data);
      setFetchError(null);
    } catch (err) {
      setFetchError(String(err));
    } finally {
      setLoading(false);
    }
  }, []);

  usePolling(fetchTransactions, 10000);

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h2 className="text-xl font-semibold text-slate-100">Transaction Ledger</h2>
          <span className="text-xs bg-slate-700 text-slate-400 border border-slate-600 rounded px-2 py-0.5">
            Read-only
          </span>
        </div>
        <span className="text-xs text-slate-500">Auto-refresh every 10s</span>
      </div>

      {fetchError && (
        <div className="bg-red-950 border border-red-700 rounded-lg px-4 py-3 text-red-300 text-sm">
          <span className="font-semibold">[act-service]</span> Failed to load transactions:{' '}
          {fetchError}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-48 text-slate-400">
          <div className="flex flex-col items-center gap-3">
            <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
            <span className="text-sm">Loading transactions...</span>
          </div>
        </div>
      ) : transactions.length === 0 ? (
        <div className="flex items-center justify-center h-48 text-slate-500 text-sm">
          No transactions found.
        </div>
      ) : (
        <div className="overflow-x-auto rounded-xl border border-slate-700">
          <table className="w-full text-sm text-left">
            <thead>
              <tr className="bg-slate-800 border-b border-slate-700">
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  ID
                </th>
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  Type
                </th>
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  Amount
                </th>
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  Currency
                </th>
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  Platform
                </th>
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  Content Bundle
                </th>
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  Status
                </th>
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  Timestamp
                </th>
                <th className="px-4 py-3 text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                  Approver
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700">
              {transactions.map((tx, idx) => (
                <tr
                  key={tx.id}
                  className={`${
                    idx % 2 === 0 ? 'bg-slate-900' : 'bg-slate-800'
                  } hover:bg-slate-750 transition-colors`}
                >
                  <td className="px-4 py-3 font-mono text-xs text-slate-400 whitespace-nowrap">
                    <span title={tx.id}>{tx.id.slice(0, 8)}…</span>
                  </td>
                  <td className="px-4 py-3 text-slate-300 whitespace-nowrap">{tx.type}</td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span
                      className={`font-semibold ${
                        tx.amount > 500 ? 'text-orange-400' : 'text-slate-200'
                      }`}
                    >
                      {tx.amount.toLocaleString(undefined, {
                        minimumFractionDigits: 2,
                        maximumFractionDigits: 2,
                      })}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-slate-400 whitespace-nowrap">{tx.currency}</td>
                  <td className="px-4 py-3 text-slate-300 whitespace-nowrap">{tx.platform}</td>
                  <td className="px-4 py-3 font-mono text-xs text-slate-400 whitespace-nowrap">
                    <span title={tx.contentBundleId}>{tx.contentBundleId.slice(0, 8)}…</span>
                  </td>
                  <td className="px-4 py-3 whitespace-nowrap">
                    <StatusBadge status={tx.status} />
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-500 whitespace-nowrap">
                    {new Date(tx.createdAt).toLocaleString()}
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-400 whitespace-nowrap">
                    {tx.approverId ?? (
                      <span className="text-slate-600 italic">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="bg-slate-800 border-t border-slate-700 px-4 py-2 text-xs text-slate-500">
            {transactions.length} record{transactions.length !== 1 ? 's' : ''} — append-only view
          </div>
        </div>
      )}
    </div>
  );
};
