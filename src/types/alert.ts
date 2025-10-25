export interface Alert {
  id: number;
  level: 'ERROR' | 'WARN' | 'INFO';
  message: string;
  traceId: string;
  timestamp: string;
}
