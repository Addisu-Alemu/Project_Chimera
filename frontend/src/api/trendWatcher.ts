import { apiFetch } from './client';
import type { TrendReport } from '../types';

const BASE_URL =
  (import.meta.env.VITE_TREND_WATCHER_URL as string | undefined) ??
  'http://localhost:8081';

export async function getTrendReports(): Promise<TrendReport[]> {
  return apiFetch<TrendReport[]>(
    'trend-watcher',
    'getTrendReports',
    `${BASE_URL}/api/v1/trend-reports`
  );
}
