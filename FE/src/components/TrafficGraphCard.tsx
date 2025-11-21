// src/components/TrafficGraphCard.tsx
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getTrafficGraph } from '@/services/logService';
import type { TrafficGraphResponse } from '@/types/log';
import TrafficGraph from '@/components/TrafficGraph';
import { cn } from '@/lib/utils';

const CardSkeleton = () => (
  <div className="animate-pulse rounded-lg border bg-white p-6 shadow-sm">
    <h2 className="mb-4 h-6 w-1/4 rounded bg-gray-200" />
    <div className="flex min-h-[250px] w-full rounded-md bg-gray-200" />
  </div>
);

const TrafficGraphCard: React.FC = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  const [trafficData, setTrafficData] = useState<TrafficGraphResponse | null>(
    null,
  );
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!projectUuid) {
      setError(new Error('Project UUID가 없습니다.'));
      setIsLoading(false);
      return;
    }

    const fetchTrafficData = async () => {
      try {
        setIsLoading(true);
        setError(null);

        const response = await getTrafficGraph({ projectUuid });
        setTrafficData(response);
      } catch (err) {
        console.error('트래픽 그래프 조회 실패:', err);
        setError(err as Error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchTrafficData();
  }, [projectUuid]);

  if (isLoading) {
    return <CardSkeleton />;
  }

  if (error) {
    return (
      <div className="rounded-lg border border-red-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-base font-semibold text-red-600">
          트래픽 그래프 (오류)
        </h2>
        <div className="flex min-h-[250px] items-center justify-center text-red-500">
          트래픽 데이터를 불러오는 중 오류가 발생했습니다.
        </div>
      </div>
    );
  }

  if (!trafficData || trafficData.dataPoints.length === 0) {
    return (
      <div className="rounded-lg border bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-base font-semibold">트래픽 그래프</h2>
        <div className="flex min-h-[250px] items-center justify-center text-gray-500">
          지난 24시간 동안 집계된 트래픽 데이터가 없습니다.
        </div>
      </div>
    );
  }

  const { summary } = trafficData;

  const summaryStats = [
    {
      key: 'total',
      label: '총 로그',
      value: summary.totalLogs,
      accentClass: 'text-slate-900',
    },
    {
      key: 'fe',
      label: 'FE',
      value: summary.totalFeCount,
      accentClass: 'text-emerald-600',
    },
    {
      key: 'be',
      label: 'BE',
      value: summary.totalBeCount,
      accentClass: 'text-sky-600',
    },
  ] as const;

  return (
    <div className="rounded-lg border bg-white p-6 shadow-sm">
      {/* 카드 제목 */}
      <div className="mb-4">
        <h2 className="text-base font-semibold text-slate-900">
          트래픽 그래프
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          24시간 동안의 FE/BE 트래픽 추이를 확인하세요.
        </p>
      </div>

      <div className="flex flex-col gap-6">
        {/* 그래프 영역 */}
        <div className="flex min-h-[380px] flex-1">
          <TrafficGraph dataPoints={trafficData.dataPoints} />
        </div>

        {/* 통계 요약 */}
        <div className="grid gap-3 sm:grid-cols-3">
          {summaryStats.map(stat => (
            <div
              key={stat.key}
              className="rounded-lg border border-slate-100 bg-slate-50/70 px-3 py-2 text-sm shadow-[inset_0_1px_0_rgba(255,255,255,0.6)]"
            >
              <div className="flex items-center justify-between">
                <p className="text-[11px] tracking-wide text-slate-400 uppercase">
                  {stat.label}
                </p>
                <p className={cn('text-base font-semibold', stat.accentClass)}>
                  {stat.value.toLocaleString('ko-KR')}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default TrafficGraphCard;
