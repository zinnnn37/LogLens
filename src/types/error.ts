export interface FrequentError {
  rank: number;
  exceptionType: string;
  message: string;
  count: number;
  percentage: number;
  firstOccurrence: string;
  lastOccurrence: string;
  stackTrace: string;
  components: {
    id: number;
    name: string;
  }[];
}

export interface FrequentErrorsSummary {
  totalErrors: number;
  uniqueErrorTypes: number;
  top10Percentage: number;
}

export interface FrequentErrorsData {
  projectId: number;
  period: {
    startTime: string;
    endTime: string;
  };
  errors: FrequentError[];
  summary: FrequentErrorsSummary;
}

export interface FrequentErrorsResponse {
  code: number;
  message: string;
  data: FrequentErrorsData;
  timestamp: string;
}
