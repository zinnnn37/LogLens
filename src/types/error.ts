export interface FrequentError {
  rank: number;
  errorCode: string;
  errorMessage: string;
  count: number;
  percentage: number;
  firstOccurrence: string;
  lastOccurrence: string;
  affectedUsers: number;
  trend: 'INCREASING' | 'STABLE' | 'DECREASING';
  trendPercentage: number;
  stackTrace: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  components: string[];
}

export interface FrequentErrorsSummary {
  totalErrors: number;
  totalUniqueErrors: number;
  top10Percentage: number;
  mostAffectedComponent: string;
  criticalErrorCount: number;
}

export interface FrequentErrorsData {
  projectId: string;
  period: {
    startDate: string;
    endDate: string;
  };
  errors: FrequentError[];
  summary: FrequentErrorsSummary;
}
