import { apiFetch } from './client';
import type { FeedbackReport } from '../types';

const BASE_URL =
  (import.meta.env.VITE_LEARN_SERVICE_URL as string | undefined) ??
  'http://localhost:8084';

export async function getFeedbackReports(): Promise<FeedbackReport[]> {
  return apiFetch<FeedbackReport[]>(
    'learn-service',
    'getFeedbackReports',
    `${BASE_URL}/api/v1/feedback-reports`
  );
}
