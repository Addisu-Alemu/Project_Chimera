import React, { useState, useCallback } from 'react';
import { getAlerts, approveAlert, rejectAlert } from '../api/actService';
import { usePolling } from '../hooks/usePolling';
import { StatusBadge } from './StatusBadge';
import type { HumanAlert } from '../types';

const ALERT_TYPE_LABELS: Record<HumanAlert['type'], string> = {
  HIGH_VALUE_TXN: 'High-Value Transaction',
  POST_FAILURE: 'Post Failure',
  LOW_CONFIDENCE: 'Low Confidence',
};

const ALERT_TYPE_COLORS: Record<HumanAlert['type'], string> = {
  HIGH_VALUE_TXN: 'bg-orange-900 border-orange-700 text-orange-300',
  POST_FAILURE: 'bg-red-900 border-red-700 text-red-300',
  LOW_CONFIDENCE: 'bg-yellow-900 border-yellow-700 text-yellow-300',
};

interface AlertCardProps {
  alert: HumanAlert;
  onApprove: (id: string) => Promise<void>;
  onReject: (id: string) => Promise<void>;
}

const AlertCard: React.FC<AlertCardProps> = ({ alert, onApprove, onReject }) => {
  const [actionInProgress, setActionInProgress] = useState<'approve' | 'reject' | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleApprove = async () => {
    setActionInProgress('approve');
    setActionError(null);
    try {
      await onApprove(alert.id);
    } catch (err) {
      setActionError(`Approve failed: ${String(err)}`);
    } finally {
      setActionInProgress(null);
    }
  };

  const handleReject = async () => {
    setActionInProgress('reject');
    setActionError(null);
    try {
      await onReject(alert.id);
    } catch (err) {
      setActionError(`Reject failed: ${String(err)}`);
    } finally {
      setActionInProgress(null);
    }
  };

  const isPending = alert.status === 'PENDING';

  return (
    <div className="bg-slate-800 border border-slate-700 rounded-xl p-5 space-y-4 hover:border-slate-600 transition-colors">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <span
            className={`inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold border ${
              ALERT_TYPE_COLORS[alert.type]
            }`}
          >
            {ALERT_TYPE_LABELS[alert.type]}
          </span>
          <StatusBadge status={alert.status} />
        </div>
        <span className="text-xs text-slate-500 whitespace-nowrap">
          {new Date(alert.issuedAt).toLocaleString()}
        </span>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm">
        <div className="space-y-0.5">
          <p className="text-xs text-slate-500 uppercase tracking-wider">Threshold</p>
          <p className="text-slate-200 font-medium">{alert.thresholdValue}</p>
        </div>
        <div className="space-y-0.5">
          <p className="text-xs text-slate-500 uppercase tracking-wider">Actual Value</p>
          <p
            className={`font-medium ${
              alert.type === 'LOW_CONFIDENCE'
                ? alert.actualValue < alert.thresholdValue
                  ? 'text-red-400'
                  : 'text-green-400'
                : alert.actualValue > alert.thresholdValue
                ? 'text-orange-400'
                : 'text-green-400'
            }`}
          >
            {alert.actualValue}
          </p>
        </div>
        <div className="space-y-0.5 col-span-2">
          <p className="text-xs text-slate-500 uppercase tracking-wider">Triggering Record</p>
          <p className="text-slate-300 font-mono text-xs break-all">
            {alert.triggeringRecordId}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2">
        <a
          href={alert.recordLink}
          target="_blank"
          rel="noopener noreferrer"
          className="text-xs text-indigo-400 hover:text-indigo-300 underline underline-offset-2 transition-colors"
        >
          View Record
        </a>
        {alert.resolvedAt && (
          <span className="text-xs text-slate-500">
            Resolved: {new Date(alert.resolvedAt).toLocaleString()}
            {alert.resolvingOperatorId && ` by ${alert.resolvingOperatorId}`}
          </span>
        )}
      </div>

      {actionError && (
        <div className="bg-red-950 border border-red-700 rounded-md px-3 py-2 text-red-300 text-xs">
          {actionError}
        </div>
      )}

      {isPending && (
        <div className="flex gap-3 pt-1">
          <button
            onClick={handleApprove}
            disabled={actionInProgress !== null}
            className="flex-1 py-2 px-4 rounded-lg text-sm font-semibold bg-green-700 hover:bg-green-600 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {actionInProgress === 'approve' ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                Approving…
              </span>
            ) : (
              'Approve'
            )}
          </button>
          <button
            onClick={handleReject}
            disabled={actionInProgress !== null}
            className="flex-1 py-2 px-4 rounded-lg text-sm font-semibold bg-red-700 hover:bg-red-600 text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {actionInProgress === 'reject' ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                Rejecting…
              </span>
            ) : (
              'Reject'
            )}
          </button>
        </div>
      )}
    </div>
  );
};

export const AlertInbox: React.FC = () => {
  const [alerts, setAlerts] = useState<HumanAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const fetchAlerts = useCallback(async () => {
    try {
      const data = await getAlerts();
      setAlerts(data);
      setFetchError(null);
    } catch (err) {
      setFetchError(String(err));
    } finally {
      setLoading(false);
    }
  }, []);

  usePolling(fetchAlerts, 10000);

  const handleApprove = useCallback(async (id: string) => {
    const updated = await approveAlert(id);
    setAlerts((prev) => prev.map((a) => (a.id === id ? updated : a)));
  }, []);

  const handleReject = useCallback(async (id: string) => {
    const updated = await rejectAlert(id);
    setAlerts((prev) => prev.map((a) => (a.id === id ? updated : a)));
  }, []);

  const pendingAlerts = alerts.filter((a) => a.status === 'PENDING');
  const resolvedAlerts = alerts.filter((a) => a.status !== 'PENDING');

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h2 className="text-xl font-semibold text-slate-100">Human Alert Inbox</h2>
          {pendingAlerts.length > 0 && (
            <span className="bg-red-600 text-white text-xs font-bold rounded-full px-2 py-0.5">
              {pendingAlerts.length} pending
            </span>
          )}
        </div>
        <span className="text-xs text-slate-500">Auto-refresh every 10s</span>
      </div>

      {fetchError && (
        <div className="bg-red-950 border border-red-700 rounded-lg px-4 py-3 text-red-300 text-sm">
          <span className="font-semibold">[act-service]</span> Failed to load alerts: {fetchError}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-48 text-slate-400">
          <div className="flex flex-col items-center gap-3">
            <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
            <span className="text-sm">Loading alerts...</span>
          </div>
        </div>
      ) : (
        <>
          {pendingAlerts.length === 0 && resolvedAlerts.length === 0 ? (
            <div className="flex items-center justify-center h-48 text-slate-500 text-sm">
              No alerts found.
            </div>
          ) : (
            <div className="space-y-6">
              {pendingAlerts.length > 0 && (
                <section className="space-y-3">
                  <h3 className="text-sm font-semibold text-yellow-400 uppercase tracking-wider">
                    Pending Approval ({pendingAlerts.length})
                  </h3>
                  {pendingAlerts.map((alert) => (
                    <AlertCard
                      key={alert.id}
                      alert={alert}
                      onApprove={handleApprove}
                      onReject={handleReject}
                    />
                  ))}
                </section>
              )}

              {resolvedAlerts.length > 0 && (
                <section className="space-y-3">
                  <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wider">
                    Resolved ({resolvedAlerts.length})
                  </h3>
                  {resolvedAlerts.map((alert) => (
                    <AlertCard
                      key={alert.id}
                      alert={alert}
                      onApprove={handleApprove}
                      onReject={handleReject}
                    />
                  ))}
                </section>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
};
