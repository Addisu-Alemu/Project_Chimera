import { apiFetch } from './client';
import type { ContentBundle } from '../types';

const BASE_URL =
  (import.meta.env.VITE_CONTENT_CREATOR_URL as string | undefined) ??
  'http://localhost:8082';

export async function getContentBundles(): Promise<ContentBundle[]> {
  return apiFetch<ContentBundle[]>(
    'content-creator',
    'getContentBundles',
    `${BASE_URL}/api/v1/content-bundles`
  );
}
