export interface HourlyData {
  hour: number;
  count: number;
  errorCount: number;
  warnCount: number;
  infoCount: number;
  intensity: number;
}

export interface DayHeatmap {
  dayOfWeek: 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
  dayName: string;
  hourlyData: HourlyData[];
  totalCount: number;
}

export interface HeatmapSummary {
  totalLogs: number;
  peakDay: string;
  peakHour: number;
  peakCount: number;
  avgDailyCount: number;
  maxIntensity: number;
  minIntensity: number;
}

export interface LogHeatmapData {
  projectId: number;
  period: {
    startDate: string;
    endDate: string;
  };
  heatmap: DayHeatmap[];
  summary: HeatmapSummary;
  metadata: {
    logLevel: string;
    timezone: string;
  };
}
