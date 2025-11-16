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

const LogHeatmapCard = ({ data }: LogHeatmapCardProps) => {
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
    // 0 또는 0.2 미만
    if (intensity > 0) {
      return 'bg-blue-100';
    }
    // 0일 때
    return 'bg-gray-100';
  };

  // 시간대 레이블 (0, 6, 12, 18, 23 표시)
  const hourLabels = [0, 6, 12, 18, 23];

  const normalizedHeatmap = useMemo(() => {
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
        dayOfWeek: existingDay?.dayOfWeek ?? String(defaultDay.dayOfWeek),
        dayName: existingDay?.dayName ?? defaultDay.dayName,
        hourlyData,
      };
    });
  }, [data]);

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle>요일별 로그 히트맵</CardTitle>
      </CardHeader>
      <CardContent>
        <TooltipProvider>
          <div className="space-y-2">
            {/* 시간 레이블 */}
            <div className="flex items-center gap-2">
              <div className="w-12" />
              <div className="flex flex-1 gap-0.5">
                {Array.from({ length: 24 }).map((_, hour) => (
                  <div
                    key={hour}
                    className="flex h-5 flex-1 items-center justify-center"
                  >
                    {hourLabels.includes(hour) && (
                      <span className="text-xs text-gray-500">{hour}</span>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* 히트맵 */}
            {normalizedHeatmap.map(day => (
              <div key={day.dayOfWeek} className="flex items-center gap-3">
                {/* 요일 레이블 */}
                <div className="w-12 text-xs font-medium text-gray-700">
                  {day.dayName}
                </div>

                {/* 시간대별 셀 */}
                <div className="flex flex-1 gap-0.5">
                  {day.hourlyData.map(hourData => (
                    <Tooltip key={hourData.hour}>
                      <TooltipTrigger asChild>
                        <div
                          className={`h-6 flex-1 rounded-sm ${getIntensityColor(
                            hourData.intensity,
                          )} group relative cursor-pointer transition-all hover:ring-2 hover:ring-blue-700`}
                        />
                      </TooltipTrigger>
                      <TooltipContent>
                        <div className="mb-1 font-semibold">
                          {day.dayName} {hourData.hour}시
                        </div>
                        <div className="space-y-0.5">
                          <div>전체: {hourData.count.toLocaleString()}건</div>
                          <div className="text-red-300">
                            ERROR: {hourData.errorCount.toLocaleString()}건
                          </div>
                          <div className="text-yellow-300">
                            WARN: {hourData.warnCount.toLocaleString()}건
                          </div>
                          <div className="text-green-300">
                            INFO: {hourData.infoCount.toLocaleString()}건
                          </div>
                        </div>
                      </TooltipContent>
                    </Tooltip>
                  ))}
                </div>
              </div>
            ))}

            {/* 범례 */}
            <div className="flex items-center justify-center gap-2 border-t pt-3">
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
