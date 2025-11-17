// src/components/LogHeatmapCard.tsx
import { useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { HeatmapResponse } from '@/types/dashboard';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';

interface LogHeatmapCardProps {
  data: HeatmapResponse;
}

const DEFAULT_DAYS = [
  { dayOfWeek: 1, dayName: '월' },
  { dayOfWeek: 2, dayName: '화' },
  { dayOfWeek: 3, dayName: '수' },
  { dayOfWeek: 4, dayName: '목' },
  { dayOfWeek: 5, dayName: '금' },
  { dayOfWeek: 6, dayName: '토' },
  { dayOfWeek: 7, dayName: '일' },
];

// 4시간 단위 버킷
const TIME_BUCKETS = [
  { label: '00-04', start: 0, end: 3 },
  { label: '04-08', start: 4, end: 7 },
  { label: '08-12', start: 8, end: 11 },
  { label: '12-16', start: 12, end: 15 },
  { label: '16-20', start: 16, end: 19 },
  { label: '20-24', start: 20, end: 23 },
];

const getIntensityColor = (intensity: number) => {
  if (intensity >= 0.8) {
    return 'bg-blue-600';
  }
  if (intensity >= 0.6) {
    return 'bg-blue-500';
  }
  if (intensity >= 0.4) {
    return 'bg-blue-400';
  }
  if (intensity >= 0.2) {
    return 'bg-blue-300';
  }
  if (intensity > 0) {
    return 'bg-blue-100';
  }
  return 'bg-gray-100';
};

const LogHeatmapCard = ({ data }: LogHeatmapCardProps) => {
  const normalizedByDay = useMemo(() => {
    const raw = data.heatmap ?? [];

    return DEFAULT_DAYS.map(defaultDay => {
      const existingDay = raw.find(
        d => Number(d.dayOfWeek) === defaultDay.dayOfWeek,
      );

      const hourlyData = Array.from({ length: 24 }, (_, hourIndex) => {
        const existingHour = existingDay?.hourlyData.find(
          h => Number(h.hour) === hourIndex,
        );

        if (existingHour) {
          return existingHour;
        }

        return {
          hour: String(hourIndex),
          count: 0,
          errorCount: 0,
          warnCount: 0,
          infoCount: 0,
          intensity: 0,
        };
      });

      return {
        dayOfWeek: defaultDay.dayOfWeek,
        dayName: existingDay?.dayName ?? defaultDay.dayName,
        hourlyData,
      };
    });
  }, [data]);

  const bucketedMatrix = useMemo(() => {
    return TIME_BUCKETS.map(bucket => {
      const cells = normalizedByDay.map(day => {
        const hoursInRange = day.hourlyData.filter(h => {
          const hNum = Number(h.hour);
          return hNum >= bucket.start && hNum <= bucket.end;
        });

        const totalCount = hoursInRange.reduce((sum, h) => sum + h.count, 0);

        const maxIntensity = hoursInRange.reduce(
          (max, h) => Math.max(max, h.intensity),
          0,
        );

        const totalError = hoursInRange.reduce(
          (sum, h) => sum + h.errorCount,
          0,
        );
        const totalWarn = hoursInRange.reduce((sum, h) => sum + h.warnCount, 0);
        const totalInfo = hoursInRange.reduce((sum, h) => sum + h.infoCount, 0);

        return {
          dayOfWeek: day.dayOfWeek,
          dayName: day.dayName,
          bucketLabel: bucket.label,
          totalCount,
          totalError,
          totalWarn,
          totalInfo,
          intensity: maxIntensity,
        };
      });

      return {
        label: bucket.label,
        cells,
      };
    });
  }, [normalizedByDay]);

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle>요일별 로그 히트맵</CardTitle>
      </CardHeader>
      <CardContent>
        <TooltipProvider>
          <div className="space-y-4">
            {/* 표 헤더 */}
            <div className="flex items-center gap-3">
              <div className="w-12 text-xs font-medium text-gray-500">Hour</div>
              <div className="flex flex-1 gap-1">
                {DEFAULT_DAYS.map(day => (
                  <div
                    key={day.dayOfWeek}
                    className="flex-1 text-center text-xs text-gray-500"
                  >
                    {day.dayName}
                  </div>
                ))}
              </div>
            </div>

            {/* 표 본문 */}
            {bucketedMatrix.map(row => (
              <div key={row.label} className="flex items-center gap-3">
                {/* 시간대 레이블 */}
                <div className="w-12 text-xs font-medium text-gray-700">
                  {row.label}
                </div>

                {/* 요일별 셀 */}
                <div className="flex flex-1 items-center justify-between gap-2">
                  {row.cells.map(cell => (
                    <Tooltip key={`${row.label}-${cell.dayOfWeek}`}>
                      <TooltipTrigger asChild>
                        <div
                          className={`h-6 flex-1 rounded-sm ${getIntensityColor(
                            cell.intensity,
                          )} cursor-pointer transition-all hover:ring-2 hover:ring-blue-700`}
                        />
                      </TooltipTrigger>
                      <TooltipContent>
                        <div className="mb-1 font-semibold">
                          {cell.dayName} {cell.bucketLabel}시 구간
                        </div>
                        <div className="space-y-0.5 text-sm">
                          <div>전체: {cell.totalCount.toLocaleString()}건</div>
                          <div className="text-red-400">
                            ERROR: {cell.totalError.toLocaleString()}건
                          </div>
                          <div className="text-yellow-500">
                            WARN: {cell.totalWarn.toLocaleString()}건
                          </div>
                          <div className="text-green-500">
                            INFO: {cell.totalInfo.toLocaleString()}건
                          </div>
                        </div>
                      </TooltipContent>
                    </Tooltip>
                  ))}
                </div>
              </div>
            ))}

            {/* 범례 */}
            <div className="mt-2 flex items-center justify-center gap-2 border-t pt-3">
              <span className="text-xs text-gray-500">적음</span>
              <div className="flex gap-1">
                <div className="h-4 w-4 rounded-sm bg-gray-100" />
                <div className="h-4 w-4 rounded-sm bg-blue-100" />
                <div className="h-4 w-4 rounded-sm bg-blue-300" />
                <div className="h-4 w-4 rounded-sm bg-blue-400" />
                <div className="h-4 w-4 rounded-sm bg-blue-500" />
                <div className="h-4 w-4 rounded-sm bg-blue-600" />
              </div>
              <span className="text-xs text-gray-500">많음</span>
            </div>
          </div>
        </TooltipProvider>
      </CardContent>
    </Card>
  );
};

export default LogHeatmapCard;
