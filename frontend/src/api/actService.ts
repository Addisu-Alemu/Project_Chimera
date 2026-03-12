import { apiFetch } from './client';
import type { PostResult, Transaction, HumanAlert } from '../types';

const BASE_URL =
  (import.meta.env.VITE_ACT_SERVICE_URL as string | undefined) ??
  'http://localhost:8083';

export async function getPostResults(): Promise<PostResult[]> {
  return apiFetch<PostResult[]>(
    'act-service',
    'getPostResults',
    `${BASE_URL}/api/v1/post-results`
  );
}

export async function getTransactions(): Promise<Transaction[]> {
  return apiFetch<Transaction[]>(
    'act-service',
    'getTransactions',
    `${BASE_URL}/api/v1/transactions`
  );
}

export async function getAlerts(): Promise<HumanAlert[]> {
  return apiFetch<HumanAlert[]>(
    'act-service',
    'getAlerts',
    `${BASE_URL}/api/v1/alerts`
  );
}

export async function approveAlert(id: string): Promise<HumanAlert> {
  return apiFetch<HumanAlert>(
    'act-service',
    `approveAlert(${id})`,
    `${BASE_URL}/api/v1/alerts/${id}/approve`,
    { method: 'POST' }
  );
}

export async function rejectAlert(id: string): Promise<HumanAlert> {
  return apiFetch<HumanAlert>(
    'act-service',
    `rejectAlert(${id})`,
    `${BASE_URL}/api/v1/alerts/${id}/reject`,
    { method: 'POST' }
  );
}
