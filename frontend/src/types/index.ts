export interface TrendReport {
  id: string;
  fetchedAt: string;
  sourcePlatforms: string[];
  trendingTopics: { name: string; safetyFilterPassed: boolean }[];
  status: string;
}

export interface ContentBundle {
  id: string;
  trendReportId: string;
  caption: string;
  hashtagSet: string[];
  videoDescription: string;
  safetyFilterPassedAt: string;
  status: string;
}

export interface PostResult {
  id: string;
  contentBundleId: string;
  platform: string;
  publishTimestamp: string;
  status: 'SUCCESS' | 'FAILED' | 'RETRYING' | 'HELD_FOR_HUMAN';
  attemptCount: number;
  failureReason?: string;
}

export interface Transaction {
  id: string;
  type: string;
  amount: number;
  currency: string;
  platform: string;
  contentBundleId: string;
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
  createdAt: string;
  approverId?: string;
  completedAt?: string;
}

export interface FeedbackReport {
  id: string;
  contentBundleId: string;
  confidenceScore: number;
  engagementMetrics: Record<string, number>;
  reviewStatus: 'AUTO_DISPATCHED' | 'HELD_PENDING_REVIEW' | 'HUMAN_APPROVED';
  dispatchedAt?: string;
  agentId?: string;
}

export interface HumanAlert {
  id: string;
  type: 'HIGH_VALUE_TXN' | 'POST_FAILURE' | 'LOW_CONFIDENCE';
  triggeringRecordId: string;
  thresholdValue: number;
  actualValue: number;
  recordLink: string;
  issuedAt: string;
  resolvedAt?: string;
  resolvingOperatorId?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}
