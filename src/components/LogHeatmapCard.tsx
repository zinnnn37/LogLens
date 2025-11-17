import { useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { HeatmapResponse, DayOfWeek } from '@/types/dashboard';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';

interface LogHeatmapCardProps {
  data?: HeatmapResponse;
}

const DAY_CONFIG: { key: DayOfWeek; label: string }[] = [
  { key: 'MONDAY', label: '월' },
  { key: 'TUESDAY', label: '화' },
  { key: 'WEDNESDAY', label: '수' },
  { key: 'THURSDAY', label: '목' },
  { key: 'FRIDAY', label: '금' },
  { key: 'SATURDAY', label: '토' },
  { key: 'SUNDAY', label: '일' },
];

// 4시간 단위 버킷
const TIME_BUCKETS = [
  { label: '00-04시', start: 0, end: 3 },
  { label: '04-08시', start: 4, end: 7 },
  { label: '08-12시', start: 8, end: 11 },
  { label: '12-16시', start: 12, end: 15 },
  { label: '16-20시', start: 16, end: 19 },
  { label: '20-24시', start: 20, end: 23 },
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
    const raw = data?.heatmap ?? [];

    return DAY_CONFIG.map(config => {
      const existingDay = raw.find(d => d.dayOfWeek === config.key);

      const hourlyData = Array.from({ length: 24 }, (_, hourIndex) => {
        const existingHour = existingDay?.hourlyData.find(
          h => h.hour === hourIndex,
        );

        if (existingHour) {
          return existingHour;
        }

        return {
          hour: hourIndex,
          count: 0,
          errorCount: 0,
          warnCount: 0,
          infoCount: 0,
          intensity: 0,
        };
      });

      return {
        dayKey: config.key,
        dayName: config.label,
        hourlyData,
      };
    });
  }, [data]);

  // 4시간 단위 데이터 집계
  const bucketedMatrix = useMemo(() => {
    return TIME_BUCKETS.map(bucket => {
      const cells = normalizedByDay.map(day => {
        const hoursInRange = day.hourlyData.filter(h => {
          return h.hour >= bucket.start && h.hour <= bucket.end;
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
          dayKey: day.dayKey,
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
    <Card className="h-full shadow-sm">
      <CardHeader className="pb-4">
        <CardTitle className="text-lg font-bold">
          주간 로그 발생 히트맵
        </CardTitle>
      </CardHeader>
      <CardContent>
        <TooltipProvider>
          <div className="space-y-3">
            {/* 요일 헤더 */}
            <div className="flex items-center gap-3 pl-12">
              {DAY_CONFIG.map(day => (
                <div
                  key={day.key}
                  className="flex-1 text-center text-xs font-medium text-gray-500"
                >
                  {day.label}
                </div>
              ))}
            </div>

            {/* 히트맵 본문 */}
            {bucketedMatrix.map(row => (
              <div key={row.label} className="flex items-center gap-3">
                <div className="w-12 text-xs font-medium whitespace-nowrap text-gray-500">
                  {row.label.split('시')[0]}
                </div>

                <div className="flex flex-1 items-center justify-between gap-2">
                  {row.cells.map(cell => (
                    <Tooltip key={`${row.label}-${cell.dayKey}`}>
                      <TooltipTrigger asChild>
                        <div
                          className={`h-8 flex-1 rounded-md ${getIntensityColor(
                            cell.intensity,
                          )} cursor-pointer transition-all duration-200 hover:scale-105 hover:ring-2 hover:ring-blue-400/50`}
                        />
                      </TooltipTrigger>
                      <TooltipContent side="top" className="p-3 text-xs">
                        <div className="mb-2 border-b pb-1 text-sm font-bold">
                          {cell.dayName}요일 {cell.bucketLabel}
                        </div>
                        <div className="grid grid-cols-2 gap-x-4 gap-y-1">
                          <span className="text-gray-500">전체 로그</span>
                          <span className="text-right font-mono font-medium">
                            {cell.totalCount.toLocaleString()}
                          </span>

                          <span className="font-medium text-red-500">
                            Error
                          </span>
                          <span className="text-right font-mono text-red-600">
                            {cell.totalError.toLocaleString()}
                          </span>

                          <span className="font-medium text-amber-500">
                            Warn
                          </span>
                          <span className="text-right font-mono text-amber-600">
                            {cell.totalWarn.toLocaleString()}
                          </span>

                          <span className="font-medium text-blue-500">
                            Info
                          </span>
                          <span className="text-right font-mono text-blue-600">
                            {cell.totalInfo.toLocaleString()}
                          </span>
                        </div>
                      </TooltipContent>
                    </Tooltip>
                  ))}
                </div>
              </div>
            ))}

            {/* 범례 */}
            <div className="mt-6 flex items-center justify-end gap-3 border-t pt-4">
              <span className="text-[10px] text-gray-400">빈도 낮음</span>
              <div className="flex gap-1">
                <div className="h-3 w-3 rounded-sm bg-gray-100" />
                <div className="h-3 w-3 rounded-sm bg-blue-100" />
                <div className="h-3 w-3 rounded-sm bg-blue-300" />
                <div className="h-3 w-3 rounded-sm bg-blue-400" />
                <div className="h-3 w-3 rounded-sm bg-blue-500" />
                <div className="h-3 w-3 rounded-sm bg-blue-600" />
              </div>
              <span className="text-[10px] text-gray-400">빈도 높음</span>
            </div>
          </div>
        </TooltipProvider>
      </CardContent>
    </Card>
  );
};

export default LogHeatmapCard;
