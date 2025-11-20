import type { Alert } from '@/types/alert';

export const DUMMY_ALERTS: Alert[] = [
  {
    id: 1,
    level: 'ERROR',
    message: 'Database connection timeout',
    traceId: 'trace-xyz789ghi',
    timestamp: '2025-10-17T10:25:00.123Z',
  },
  {
    id: 2,
    level: 'WARN',
    message: 'Response time exeeded 500ms threshold',
    traceId: 'trace-xyz789ghi',
    timestamp: '2025-10-17T10:20:15.456Z',
  },
  {
    id: 3,
    level: 'WARN',
    message: 'Response time exeeded 500ms threshold',
    traceId: 'trace-xyz789ghi',
    timestamp: '2025-10-17T10:18:30.789Z',
  },
];
