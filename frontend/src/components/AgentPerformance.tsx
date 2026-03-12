import React, { useState, useCallback } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { getFeedbackReports } from '../api/learnService';
import { usePolling } from '../hooks/usePolling';
import type { FeedbackReport } from '../types';

const AGENT_COLORS = [
  '#6366f1', // indigo
  '#22d3ee', // cyan
  '#a78bfa', // violet
  '#34d399', // emerald
  '#f59e0b', // amber
  '#f472b6', // pink
  '#fb923c', // orange
  '#4ade80', // green
];

interface ChartPoint {
  time: string;
  [agentId: string]: number | string;
}

function buildChartData(reports: FeedbackReport[]): {
  data: ChartPoint[];
  agents: string[];
} {
  const sorted = [...reports].sort((a, b) => {
    const aTime = a.dispatchedAt ?? '';
    const bTime = b.dispatchedAt ?? '';
    return aTime.localeCompare(bTime);
  });

  const agentSet = new Set<string>();
  sorted.forEach((r) => {
    const agentId = r.agentId ?? r.contentBundleId.slice(0, 8);
    agentSet.add(agentId);
  });
  const agents = Array.from(agentSet);

  // Group by dispatch time (minute-level granularity)
  const timeMap = new Map<string, ChartPoint>();
  sorted.forEach((r) => {
    const agentId = r.agentId ?? r.contentBundleId.slice(0, 8);
    const rawTime = r.dispatchedAt
      ? new Date(r.dispatchedAt)
      : new Date(r.id.slice(0, 13).replace('T', ' '));
    const timeKey = rawTime.toLocaleString(undefined, {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

    if (!timeMap.has(timeKey)) {
      timeMap.set(timeKey, { time: timeKey });
    }
    const point = timeMap.get(timeKey)!;
    point[agentId] = Number(r.confidenceScore.toFixed(4));
  });

  return { data: Array.from(timeMap.values()), agents };
}

interface CustomTooltipProps {
  active?: boolean;
  payload?: { name: string; value: number; color: string }[];
  label?: string;
}

const CustomTooltip: React.FC<CustomTooltipProps> = ({ active, payload, label }) => {
  if (!active || !payload || payload.length === 0) return null;
  return (
    <div className="bg-slate-800 border border-slate-600 rounded-lg p-3 shadow-xl text-xs">
      <p className="text-slate-400 mb-2 font-medium">{label}</p>
      {payload.map((entry) => (
        <div key={entry.name} className="flex items-center gap-2 py-0.5">
          <span
            className="w-2.5 h-2.5 rounded-full flex-shrink-0"
            style={{ backgroundColor: entry.color }}
          />
          <span className="text-slate-300">{entry.name}:</span>
          <span className="font-semibold" style={{ color: entry.color }}>
            {(entry.value * 100).toFixed(1)}%
          </span>
        </div>
      ))}
    </div>
  );
};

export const AgentPerformance: React.FC = () => {
  const [reports, setReports] = useState<FeedbackReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const fetchReports = useCallback(async () => {
    try {
      const data = await getFeedbackReports();
      setReports(data);
      setFetchError(null);
    } catch (err) {
      setFetchError(String(err));
    } finally {
      setLoading(false);
    }
  }, []);

  usePolling(fetchReports, 10000);

  const { data: chartData, agents } = buildChartData(reports);

  // Summary stats per agent
  const agentStats = agents.map((agentId) => {
    const agentReports = reports.filter(
      (r) => (r.agentId ?? r.contentBundleId.slice(0, 8)) === agentId
    );
    const scores = agentReports.map((r) => r.confidenceScore);
    const avg = scores.length > 0 ? scores.reduce((a, b) => a + b, 0) / scores.length : 0;
    const latest = scores[scores.length - 1] ?? 0;
    return { agentId, avg, latest, count: scores.length };
  });

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-100">Agent Performance</h2>
        <span className="text-xs text-slate-500">Auto-refresh every 10s</span>
      </div>

      {fetchError && (
        <div className="bg-red-950 border border-red-700 rounded-lg px-4 py-3 text-red-300 text-sm">
          <span className="font-semibold">[learn-service]</span> Failed to load feedback reports:{' '}
          {fetchError}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-48 text-slate-400">
          <div className="flex flex-col items-center gap-3">
            <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
            <span className="text-sm">Loading performance data...</span>
          </div>
        </div>
      ) : reports.length === 0 ? (
        <div className="flex items-center justify-center h-48 text-slate-500 text-sm">
          No feedback reports available.
        </div>
      ) : (
        <>
          {/* Agent summary cards */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-3">
            {agentStats.map((stat, idx) => (
              <div
                key={stat.agentId}
                className="bg-slate-800 border border-slate-700 rounded-lg p-3 space-y-1"
              >
                <div className="flex items-center gap-1.5">
                  <span
                    className="w-2 h-2 rounded-full flex-shrink-0"
                    style={{ backgroundColor: AGENT_COLORS[idx % AGENT_COLORS.length] }}
                  />
                  <p className="text-xs font-mono text-slate-400 truncate" title={stat.agentId}>
                    {stat.agentId}
                  </p>
                </div>
                <p className="text-lg font-bold text-slate-100">
                  {(stat.avg * 100).toFixed(1)}%
                </p>
                <p className="text-xs text-slate-500">avg confidence</p>
                <p className="text-xs text-slate-400">
                  Latest:{' '}
                  <span
                    className={
                      stat.latest >= 0.7
                        ? 'text-green-400'
                        : stat.latest >= 0.5
                        ? 'text-yellow-400'
                        : 'text-red-400'
                    }
                  >
                    {(stat.latest * 100).toFixed(1)}%
                  </span>
                </p>
                <p className="text-xs text-slate-600">{stat.count} reports</p>
              </div>
            ))}
          </div>

          {/* Line chart */}
          <div className="bg-slate-800 border border-slate-700 rounded-xl p-5">
            <h3 className="text-sm font-semibold text-slate-300 mb-4">
              Confidence Score Over Time
            </h3>
            {chartData.length < 2 ? (
              <div className="flex items-center justify-center h-40 text-slate-500 text-sm">
                Not enough data points to render a trend (need at least 2 reports with timestamps).
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={340}>
                <LineChart
                  data={chartData}
                  margin={{ top: 5, right: 20, left: 0, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis
                    dataKey="time"
                    tick={{ fill: '#94a3b8', fontSize: 11 }}
                    tickLine={false}
                    axisLine={{ stroke: '#334155' }}
                  />
                  <YAxis
                    domain={[0, 1]}
                    tickFormatter={(v: number) => `${(v * 100).toFixed(0)}%`}
                    tick={{ fill: '#94a3b8', fontSize: 11 }}
                    tickLine={false}
                    axisLine={{ stroke: '#334155' }}
                    width={48}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend
                    wrapperStyle={{ fontSize: 12, color: '#94a3b8', paddingTop: '12px' }}
                  />
                  {agents.map((agentId, idx) => (
                    <Line
                      key={agentId}
                      type="monotone"
                      dataKey={agentId}
                      stroke={AGENT_COLORS[idx % AGENT_COLORS.length]}
                      strokeWidth={2}
                      dot={{ r: 3, strokeWidth: 0 }}
                      activeDot={{ r: 5 }}
                      connectNulls
                    />
                  ))}
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>

          {/* Raw reports table */}
          <div className="bg-slate-800 border border-slate-700 rounded-xl overflow-hidden">
            <div className="px-5 py-3 border-b border-slate-700">
              <h3 className="text-sm font-semibold text-slate-300">
                Feedback Reports ({reports.length})
              </h3>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead>
                  <tr className="border-b border-slate-700">
                    <th className="px-4 py-2.5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      Agent ID
                    </th>
                    <th className="px-4 py-2.5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      Confidence
                    </th>
                    <th className="px-4 py-2.5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      Review Status
                    </th>
                    <th className="px-4 py-2.5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      Dispatched At
                    </th>
                    <th className="px-4 py-2.5 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                      Content Bundle
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-700">
                  {reports.map((r) => {
                    const agentId = r.agentId ?? r.contentBundleId.slice(0, 8);
                    const agentIdx = agents.indexOf(agentId);
                    return (
                      <tr key={r.id} className="hover:bg-slate-750 transition-colors">
                        <td className="px-4 py-2.5">
                          <div className="flex items-center gap-1.5">
                            <span
                              className="w-2 h-2 rounded-full flex-shrink-0"
                              style={{
                                backgroundColor:
                                  AGENT_COLORS[agentIdx % AGENT_COLORS.length],
                              }}
                            />
                            <span className="font-mono text-xs text-slate-300">{agentId}</span>
                          </div>
                        </td>
                        <td className="px-4 py-2.5">
                          <span
                            className={`font-semibold ${
                              r.confidenceScore >= 0.7
                                ? 'text-green-400'
                                : r.confidenceScore >= 0.5
                                ? 'text-yellow-400'
                                : 'text-red-400'
                            }`}
                          >
                            {(r.confidenceScore * 100).toFixed(1)}%
                          </span>
                        </td>
                        <td className="px-4 py-2.5">
                          <span
                            className={`text-xs font-medium ${
                              r.reviewStatus === 'AUTO_DISPATCHED'
                                ? 'text-green-400'
                                : r.reviewStatus === 'HUMAN_APPROVED'
                                ? 'text-blue-400'
                                : 'text-yellow-400'
                            }`}
                          >
                            {r.reviewStatus}
                          </span>
                        </td>
                        <td className="px-4 py-2.5 text-xs text-slate-500">
                          {r.dispatchedAt ? new Date(r.dispatchedAt).toLocaleString() : '—'}
                        </td>
                        <td className="px-4 py-2.5 font-mono text-xs text-slate-400">
                          <span title={r.contentBundleId}>{r.contentBundleId.slice(0, 8)}…</span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
