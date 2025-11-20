import type { LogHeatmapData, HourlyData } from '@/types/heatmap';

// 시간대별 랜덤 데이터 생성 헬퍼 함수
const generateHourlyData = (baseMultiplier: number): HourlyData[] => {
  return Array.from({ length: 24 }, (_, hour) => {
    // 근무 시간대(9-18시)에 더 많은 로그 발생
    const timeMultiplier = hour >= 9 && hour <= 18 ? 1.5 : 0.5;
    const count = Math.floor(
      Math.random() * 2000 * baseMultiplier * timeMultiplier + 500,
    );
    const errorCount = Math.floor(count * 0.05);
    const warnCount = Math.floor(count * 0.15);
    const infoCount = count - errorCount - warnCount;
    const intensity = Math.min((count / 3000) * baseMultiplier, 1.0);

    return {
      hour,
      count,
      errorCount,
      warnCount,
      infoCount,
      intensity: Math.round(intensity * 100) / 100,
    };
  });
};

export const DUMMY_HEATMAP_DATA: LogHeatmapData = {
  projectId: 123,
  period: {
    startDate: '2025-10-10T00:00:00Z',
    endDate: '2025-10-17T23:59:59Z',
  },
  heatmap: [
    {
      dayOfWeek: 'MONDAY',
      dayName: '월요일',
      hourlyData: generateHourlyData(1.2),
      totalCount: 28945,
    },
    {
      dayOfWeek: 'TUESDAY',
      dayName: '화요일',
      hourlyData: generateHourlyData(1.3),
      totalCount: 31204,
    },
    {
      dayOfWeek: 'WEDNESDAY',
      dayName: '수요일',
      hourlyData: generateHourlyData(1.5),
      totalCount: 35678,
    },
    {
      dayOfWeek: 'THURSDAY',
      dayName: '목요일',
      hourlyData: generateHourlyData(1.4),
      totalCount: 33421,
    },
    {
      dayOfWeek: 'FRIDAY',
      dayName: '금요일',
      hourlyData: generateHourlyData(1.1),
      totalCount: 27890,
    },
    {
      dayOfWeek: 'SATURDAY',
      dayName: '토요일',
      hourlyData: generateHourlyData(0.6),
      totalCount: 15234,
    },
    {
      dayOfWeek: 'SUNDAY',
      dayName: '일요일',
      hourlyData: generateHourlyData(0.5),
      totalCount: 13271,
    },
  ],
  summary: {
    totalLogs: 185643,
    peakDay: 'WEDNESDAY',
    peakHour: 14,
    peakCount: 4567,
    avgDailyCount: 26520,
    maxIntensity: 1.0,
    minIntensity: 0.12,
  },
  metadata: {
    logLevel: 'ALL',
    timezone: 'Asia/Seoul',
  },
};
