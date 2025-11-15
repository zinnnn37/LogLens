// src/components/TrafficGraphCard.tsx
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { getTrafficGraph } from '@/services/logService';
import type { TrafficGraphResponse } from '@/types/log';
import TrafficGraph from '@/components/TrafficGraph';

const CardSkeleton = () => (
  <div className="rounded-lg border bg-white p-6 shadow-sm animate-pulse">
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

  return (
    <div className="rounded-lg border bg-white p-6 shadow-sm">
      {/* 카드 제목 + 요약 */}
      <div className="mb-4 flex items-baseline justify-between">
        <h2 className="text-base font-semibold">트래픽 그래프</h2>
        <div className="flex gap-4 text-right text-xs text-slate-500">
          <div>
            <p className="text-[11px]">총 로그</p>
            <p className="text-sm font-semibold text-slate-900">
              {summary.totalLogs.toLocaleString('ko-KR')}
            </p>
          </div>
          <div>
            <p className="text-[11px]">FE</p>
            <p className="text-sm font-semibold text-emerald-600">
              {summary.totalFeCount.toLocaleString('ko-KR')}
            </p>
          </div>
          <div>
            <p className="text-[11px]">BE</p>
            <p className="text-sm font-semibold text-sky-600">
              {summary.totalBeCount.toLocaleString('ko-KR')}
            </p>
          </div>
        </div>
      </div>

      {/* 그래프 영역 */}
      <div className="flex min-h-[250px] w-full items-center justify-center rounded-md bg-gray-50">
        <TrafficGraph dataPoints={trafficData.dataPoints} />

      </div>
    </div>
  );
};

export default TrafficGraphCard;
