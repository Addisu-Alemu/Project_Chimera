import React, { useState, useCallback } from 'react';
import { getTrendReports } from '../api/trendWatcher';
import { getContentBundles } from '../api/contentCreator';
import { getPostResults } from '../api/actService';
import { getFeedbackReports } from '../api/learnService';
import { usePolling } from '../hooks/usePolling';
import { StatusBadge } from './StatusBadge';
import type {
  TrendReport,
  ContentBundle,
  PostResult,
  FeedbackReport,
} from '../types';

interface PipelineRow {
  trendReport: TrendReport;
  contentBundle?: ContentBundle;
  postResult?: PostResult;
  feedbackReport?: FeedbackReport;
}

function buildRows(
  trendReports: TrendReport[],
  contentBundles: ContentBundle[],
  postResults: PostResult[],
  feedbackReports: FeedbackReport[]
): PipelineRow[] {
  return trendReports.map((tr) => {
    const cb = contentBundles.find((c) => c.trendReportId === tr.id);
    const pr = cb
      ? postResults.find((p) => p.contentBundleId === cb.id)
      : undefined;
    const fr = cb
      ? feedbackReports.find((f) => f.contentBundleId === cb?.id)
      : undefined;
    return { trendReport: tr, contentBundle: cb, postResult: pr, feedbackReport: fr };
  });
}

const Arrow: React.FC = () => (
  <span className="text-slate-500 text-lg font-light select-none">→</span>
);

export const PipelineMonitor: React.FC = () => {
  const [trendReports, setTrendReports] = useState<TrendReport[]>([]);
  const [contentBundles, setContentBundles] = useState<ContentBundle[]>([]);
  const [postResults, setPostResults] = useState<PostResult[]>([]);
  const [feedbackReports, setFeedbackReports] = useState<FeedbackReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const fetchAll = useCallback(async () => {
    const errs: Record<string, string> = {};

    const [trResult, cbResult, prResult, frResult] = await Promise.allSettled([
      getTrendReports(),
      getContentBundles(),
      getPostResults(),
      getFeedbackReports(),
    ]);

    if (trResult.status === 'fulfilled') {
      setTrendReports(trResult.value);
    } else {
      errs['trend-watcher'] = String(trResult.reason);
    }
    if (cbResult.status === 'fulfilled') {
      setContentBundles(cbResult.value);
    } else {
      errs['content-creator'] = String(cbResult.reason);
    }
    if (prResult.status === 'fulfilled') {
      setPostResults(prResult.value);
    } else {
      errs['act-service'] = String(prResult.reason);
    }
    if (frResult.status === 'fulfilled') {
      setFeedbackReports(frResult.value);
    } else {
      errs['learn-service'] = String(frResult.reason);
    }

    setErrors(errs);
    setLoading(false);
  }, []);

  usePolling(fetchAll, 10000);

  const rows = buildRows(trendReports, contentBundles, postResults, feedbackReports);

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-100">Pipeline Monitor</h2>
        <span className="text-xs text-slate-500">Auto-refresh every 10s</span>
      </div>

      {Object.entries(errors).map(([svc, msg]) => (
        <div
          key={svc}
          className="bg-red-950 border border-red-700 rounded-lg px-4 py-3 text-red-300 text-sm"
        >
          <span className="font-semibold">[{svc}]</span> {msg}
        </div>
      ))}

      {loading ? (
        <div className="flex items-center justify-center h-48 text-slate-400">
          <div className="flex flex-col items-center gap-3">
            <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
            <span className="text-sm">Loading pipeline data...</span>
          </div>
        </div>
      ) : rows.length === 0 ? (
        <div className="flex items-center justify-center h-48 text-slate-500 text-sm">
          No pipeline records found.
        </div>
      ) : (
        <div className="space-y-3">
          {/* Header row */}
          <div className="hidden md:grid grid-cols-[1fr_auto_1fr_auto_1fr_auto_1fr] gap-2 px-4 py-2">
            <span className="text-xs font-semibold text-indigo-400 uppercase tracking-wider">
              PERCEIVE
            </span>
            <span />
            <span className="text-xs font-semibold text-violet-400 uppercase tracking-wider">
              CREATE
            </span>
            <span />
            <span className="text-xs font-semibold text-orange-400 uppercase tracking-wider">
              ACT
            </span>
            <span />
            <span className="text-xs font-semibold text-teal-400 uppercase tracking-wider">
              LEARN
            </span>
          </div>

          {rows.map((row) => (
            <div
              key={row.trendReport.id}
              className="bg-slate-800 border border-slate-700 rounded-xl px-5 py-4 grid grid-cols-1 md:grid-cols-[1fr_auto_1fr_auto_1fr_auto_1fr] gap-3 md:gap-2 items-center hover:border-slate-600 transition-colors"
            >
              {/* PERCEIVE */}
              <div className="space-y-1">
                <p className="text-xs text-indigo-400 font-semibold uppercase tracking-wider md:hidden">
                  PERCEIVE
                </p>
                <p className="text-xs text-slate-400 font-mono truncate" title={row.trendReport.id}>
                  {row.trendReport.id.slice(0, 8)}…
                </p>
                <StatusBadge status={row.trendReport.status} />
                <p className="text-xs text-slate-500">
                  {new Date(row.trendReport.fetchedAt).toLocaleString()}
                </p>
                <div className="flex flex-wrap gap-1 mt-1">
                  {row.trendReport.sourcePlatforms.map((p) => (
                    <span
                      key={p}
                      className="text-xs bg-slate-700 text-slate-300 rounded px-1"
                    >
                      {p}
                    </span>
                  ))}
                </div>
              </div>

              <Arrow />

              {/* CREATE */}
              <div className="space-y-1">
                <p className="text-xs text-violet-400 font-semibold uppercase tracking-wider md:hidden">
                  CREATE
                </p>
                {row.contentBundle ? (
                  <>
                    <p className="text-xs text-slate-400 font-mono truncate" title={row.contentBundle.id}>
                      {row.contentBundle.id.slice(0, 8)}…
                    </p>
                    <StatusBadge status={row.contentBundle.status} />
                    <p
                      className="text-xs text-slate-500 truncate max-w-[160px]"
                      title={row.contentBundle.caption}
                    >
                      {row.contentBundle.caption}
                    </p>
                  </>
                ) : (
                  <span className="text-xs text-slate-600 italic">Not yet created</span>
                )}
              </div>

              <Arrow />

              {/* ACT */}
              <div className="space-y-1">
                <p className="text-xs text-orange-400 font-semibold uppercase tracking-wider md:hidden">
                  ACT
                </p>
                {row.postResult ? (
                  <>
                    <p className="text-xs text-slate-400 font-mono truncate" title={row.postResult.id}>
                      {row.postResult.id.slice(0, 8)}…
                    </p>
                    <StatusBadge status={row.postResult.status} />
                    <p className="text-xs text-slate-500">{row.postResult.platform}</p>
                    {row.postResult.attemptCount > 1 && (
                      <p className="text-xs text-yellow-500">
                        Attempt #{row.postResult.attemptCount}
                      </p>
                    )}
                    {row.postResult.failureReason && (
                      <p
                        className="text-xs text-red-400 truncate max-w-[160px]"
                        title={row.postResult.failureReason}
                      >
                        {row.postResult.failureReason}
                      </p>
                    )}
                  </>
                ) : (
                  <span className="text-xs text-slate-600 italic">Not yet posted</span>
                )}
              </div>

              <Arrow />

              {/* LEARN */}
              <div className="space-y-1">
                <p className="text-xs text-teal-400 font-semibold uppercase tracking-wider md:hidden">
                  LEARN
                </p>
                {row.feedbackReport ? (
                  <>
                    <p className="text-xs text-slate-400 font-mono truncate" title={row.feedbackReport.id}>
                      {row.feedbackReport.id.slice(0, 8)}…
                    </p>
                    <StatusBadge status={row.feedbackReport.reviewStatus} />
                    <p className="text-xs text-slate-400">
                      Confidence:{' '}
                      <span
                        className={
                          row.feedbackReport.confidenceScore >= 0.7
                            ? 'text-green-400'
                            : row.feedbackReport.confidenceScore >= 0.5
                            ? 'text-yellow-400'
                            : 'text-red-400'
                        }
                      >
                        {(row.feedbackReport.confidenceScore * 100).toFixed(1)}%
                      </span>
                    </p>
                  </>
                ) : (
                  <span className="text-xs text-slate-600 italic">No feedback yet</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
