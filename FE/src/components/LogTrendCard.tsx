// src/components/LogTrendCard.tsx

import { useState, useEffect, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { getLogTrend } from '@/services/logService';
import type { LogTrendResponse } from '@/types/log';

import InfoIcon from '@/assets/images/InfoIcon.png';
import WarnIcon from '@/assets/images/WarnIcon.png';
import ErrorIcon from '@/assets/images/ErrorIcon.png';
import LogTrendGraph from './LogTrendGraph';

const CardSkeleton = () => (
  <div className="animate-pulse rounded-lg border bg-white p-6 shadow-sm">
    <h2 className="mb-4 h-6 w-1/3 rounded bg-gray-200"></h2>
    <div className="flex flex-col space-y-6 sm:flex-row sm:space-y-0 sm:space-x-6">
      <div className="space-y-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="flex items-center space-x-3">
            <div className="h-8 w-8 rounded-full bg-gray-200"></div>
            <div>
              <div className="mb-1 h-4 w-10 rounded bg-gray-200"></div>
              <div className="h-7 w-16 rounded bg-gray-200"></div>
            </div>
          </div>
        ))}
      </div>
      <div className="flex min-h-[250px] flex-1 rounded-md bg-gray-200"></div>
    </div>
  </div>
);

const LogTrendCard = () => {
  // 상태
  const [trendData, setTrendData] = useState<LogTrendResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const { projectUuid } = useParams<{ projectUuid: string }>();

  // API 호출
  useEffect(() => {
    if (!projectUuid) {
      setError(new Error('Project UUID가 없습니다.'));
      setIsLoading(false);
      return;
    }

    const fetchTrendData = async () => {
      try {
        setIsLoading(true);
        const response = await getLogTrend({ projectUuid });
        setTrendData(response);
      } catch (err) {
        setError(err as Error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchTrendData();
  }, [projectUuid]);

  const logCounts = useMemo(() => {
    if (!trendData) {
      return { INFO: 0, WARN: 0, ERROR: 0 };
    }

    // dataPoints 배열을 순회하며 각 레벨의 총합을 구함
    return trendData.dataPoints.reduce(
      (acc, point) => {
        acc.INFO += point.infoCount;
        acc.WARN += point.warnCount;
        acc.ERROR += point.errorCount;
        return acc;
      },
      { INFO: 0, WARN: 0, ERROR: 0 },
    );
  }, [trendData]);

  // UI 렌더링을 위한 데이터 배열
  const logLevels = [
    { level: 'INFO', label: 'info', icon: InfoIcon, count: logCounts.INFO },
    { level: 'WARN', label: 'warn', icon: WarnIcon, count: logCounts.WARN },
    { level: 'ERROR', label: 'error', icon: ErrorIcon, count: logCounts.ERROR },
  ];

  if (isLoading) {
    return <CardSkeleton />;
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-base font-semibold text-red-600">
          로그 발생 추이 (오류)
        </h2>
        <div className="flex min-h-[250px] items-center justify-center text-red-500">
          데이터를 불러오는 중 오류가 발생했습니다.
        </div>
      </div>
    );
  }

  if (!trendData || trendData.dataPoints.length === 0) {
    return (
      <div className="rounded-lg border bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-base font-semibold">로그 발생 추이</h2>
        <div className="flex min-h-[250px] items-center justify-center text-gray-500">
          지난 24시간 동안 집계된 로그 데이터가 없습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-lg border bg-white p-6 shadow-sm">
      {/* 카드 제목 */}
      <h2 className="mb-4 text-base font-semibold">로그 발생 추이 </h2>

      {/* 카드 본문 */}
      <div className="flex flex-col gap-6">
        {/* 그래프 영역 */}
        <div className="flex min-h-[420px] flex-1">
          <LogTrendGraph dataPoints={trendData.dataPoints} />
        </div>

        {/* 로그 카운트 리스트 */}
        <div className="grid gap-3 sm:grid-cols-3">
          {logLevels.map(item => (
            <div
              key={item.level}
              className="rounded-lg border border-slate-100 bg-slate-50/70 px-3 py-2 text-sm shadow-[inset_0_1px_0_rgba(255,255,255,0.6)]"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <img
                    src={item.icon}
                    alt={item.label}
                    className="h-7 w-7 rounded-full border border-slate-100 bg-white p-1"
                  />
                  <p className="text-[11px] tracking-wide text-slate-400 uppercase">
                    {item.label}
                  </p>
                </div>
                <p className="text-base font-semibold text-slate-900">
                  {item.count.toLocaleString()}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default LogTrendCard;
