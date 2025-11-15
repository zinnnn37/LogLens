// src/pages/DashboardPage.tsx
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { Loader2, AlertCircle } from 'lucide-react';

import DashboardStatsCards from '@/components/DashboardStatsCards';
import RecentAlertsCard from '@/components/RecentAlertsCard';
import LogHeatmapCard from '@/components/LogHeatmapCard';
import FrequentErrorsCard from '@/components/FrequentErrorsCard';
import FloatingChecklist from '@/components/FloatingChecklist';

import { DUMMY_ALERTS } from '@/mocks/dummyAlerts';

import {
  getDashboardOverview,
  getDashboardTopErrors,
  getLogHeatmap,
  getDashboardApiStats,
} from '@/services/dashboardService';
import type {
  DashboardSummary,
  DashboardTopErrorsData,
  HeatmapResponse,
  DashboardApiStatsData,
} from '@/types/dashboard';
import ApiStatsCard from '@/components/ApiStatsCard';

const DashboardPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // 통계 개요 상태
  const [stats, setStats] = useState<DashboardSummary | null>(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const [statsError, setStatsError] = useState(false);

  // 자주 발생하는 에러 상태
  const [topErrors, setTopErrors] = useState<DashboardTopErrorsData | null>(
    null,
  );
  const [topErrorsLoading, setTopErrorsLoading] = useState(true);
  const [topErrorsError, setTopErrorsError] = useState(false);

  // 히트맵 상태
  const [heatmapData, setHeatmapData] = useState<HeatmapResponse | null>(null);
  const [heatmapLoading, setHeatmapLoading] = useState(true);
  const [heatmapError, setHeatmapError] = useState(false);

  // API 호출 통계 상태
  const [apiStats, setApiStats] = useState<DashboardApiStatsData | null>(null);
  const [apiStatsLoading, setApiStatsLoading] = useState(true);
  const [apiStatsError, setApiStatsError] = useState(false);

  useEffect(() => {
    if (!projectUuid) {
      return;
    }

    // --- 1. 통계 개요 조회 ---
    const fetchOverview = async () => {
      setStatsLoading(true);
      setStatsError(false);
      try {
        const response = await getDashboardOverview({ projectUuid });
        setStats(response.summary);
      } catch (e) {
        console.error('대시보드 통계 조회 실패:', e);
        toast.error('대시보드 통계 정보를 불러오지 못했습니다.');
        setStatsError(true);
      } finally {
        setStatsLoading(false);
      }
    };

    // 자주 발행하는 에러
    const fetchTopErrors = async () => {
      setTopErrorsLoading(true);
      setTopErrorsError(false);
      try {
        // 기본값으로 10개 조회
        const response = await getDashboardTopErrors({
          projectUuid,
          limit: 10,
        });
        setTopErrors(response);
      } catch (e) {
        console.error('자주 발생하는 에러 조회 실패:', e);
        toast.error('자주 발생하는 에러 목록을 불러오지 못했습니다.');
        setTopErrorsError(true);
      } finally {
        setTopErrorsLoading(false);
      }
    };

    // 히트맵
    const fetchHeatmap = async () => {
      setHeatmapLoading(true);
      setHeatmapError(false);
      try {
        // 일단 디폴트로 호출중
        const now = new Date();
        const endTime = now.toISOString();
        const startTime = new Date(
          now.getTime() - 7 * 24 * 60 * 60 * 1000,
        ).toISOString();

        const response = await getLogHeatmap({
          projectUuid,
          startTime,
          endTime,
          logLevel: 'ALL',
        });
        setHeatmapData(response);
      } catch (e) {
        console.error('히트맵 데이터 조회 실패:', e);
        toast.error('로그 히트맵 정보를 불러오지 못했습니다.');
        setHeatmapError(true);
      } finally {
        setHeatmapLoading(false);
      }
    };

    const fetchApiStats = async () => {
      setApiStatsLoading(true);
      setApiStatsError(false);
      try {
        const response = await getDashboardApiStats({
          projectUuid,
        });
        setApiStats(response);
      } catch (e) {
        console.error('API 호출 통계 조회 실패:', e);
        toast.error('API 호출 통계 정보를 불러오지 못했습니다.');
        setApiStatsError(true);
      } finally {
        setApiStatsLoading(false);
      }
    };

    // API 동시 호출
    fetchOverview();
    fetchTopErrors();
    fetchHeatmap();
    fetchApiStats();
  }, [projectUuid]);

  return (
    <div className="font-pretendard space-y-6 p-6 py-1">
      <h1 className="font-godoM text-lg">통계 요약</h1>

      {/* 대시보드 통계 개요 */}
      {statsLoading ? (
        <div className="flex min-h-[120px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          통계 정보를 불러오는 중...
        </div>
      ) : statsError ? (
        <div className="flex min-h-[120px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
          <AlertCircle className="mr-2 h-5 w-5" />
          통계 정보를 불러올 수 없습니다.
        </div>
      ) : stats ? (
        <DashboardStatsCards stats={stats} />
      ) : null}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <RecentAlertsCard alerts={DUMMY_ALERTS} />

        {/* 히트맵 */}
        {heatmapLoading ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            히트맵을 불러오는 중...
          </div>
        ) : heatmapError ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
            <AlertCircle className="mr-2 h-5 w-5" />
            히트맵을 불러올 수 없습니다.
          </div>
        ) : heatmapData ? (
          <LogHeatmapCard data={heatmapData} />
        ) : (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            히트맵 데이터가 없습니다.
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* 자주 발생하는 에러 */}
        {topErrorsLoading ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            에러 목록을 불러오는 중...
          </div>
        ) : topErrorsError ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
            <AlertCircle className="mr-2 h-5 w-5" />
            에러 목록을 불러올 수 없습니다.
          </div>
        ) : topErrors ? (
          <FrequentErrorsCard data={topErrors} />
        ) : (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            데이터가 없습니다.
          </div>
        )}

        {/* API 호출 통계 카드 */}
        {apiStatsLoading ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            API 호출 통계를 불러오는 중...
          </div>
        ) : apiStatsError ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
            <AlertCircle className="mr-2 h-5 w-5" />
            API 호출 통계를 불러올 수 없습니다.
          </div>
        ) : apiStats ? (
          <ApiStatsCard data={apiStats} />
        ) : (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            API 통계 데이터가 없습니다.
          </div>
        )}
      </div>
      <FloatingChecklist />
    </div>
  );
};

export default DashboardPage;
