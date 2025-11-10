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
import { DUMMY_HEATMAP_DATA } from '@/mocks/dummyHeatmap';
// DUMMY_FREQUENT_ERRORS import 제거

import {
  getDashboardOverview,
  getDashboardTopErrors, // 1. 서비스 함수 import
} from '@/services/dashboardService';
import type {
  DashboardSummary,
  DashboardTopErrorsData, // 2. 응답 타입 import
} from '@/types/dashboard';
// 💡 참고: FrequentErrorsCard의 prop 타입을 FrequentErrorsData(types/error) -> DashboardTopErrorsData(types/dashboard)로 수정해야 할 수 있습니다.

const DashboardPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // 통계 개요 상태
  const [stats, setStats] = useState<DashboardSummary | null>(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const [statsError, setStatsError] = useState(false);

  // 3. 자주 발생하는 에러 상태 추가
  const [topErrors, setTopErrors] = useState<DashboardTopErrorsData | null>(null);
  const [topErrorsLoading, setTopErrorsLoading] = useState(true);
  const [topErrorsError, setTopErrorsError] = useState(false);

  useEffect(() => {
    if (!projectUuid) {return;}

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

    // --- 4. 자주 발생하는 에러 조회 ---
    const fetchTopErrors = async () => {
      setTopErrorsLoading(true);
      setTopErrorsError(false);
      try {
        // 기본값으로 10개 조회
        const response = await getDashboardTopErrors({ projectUuid, limit: 10 });
        setTopErrors(response);
      } catch (e) {
        console.error('자주 발생하는 에러 조회 실패:', e);
        toast.error('자주 발생하는 에러 목록을 불러오지 못했습니다.');
        setTopErrorsError(true);
      } finally {
        setTopErrorsLoading(false);
      }
    };

    // 두 API 동시 호출
    fetchOverview();
    fetchTopErrors();
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
        <LogHeatmapCard data={DUMMY_HEATMAP_DATA} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* 5. FrequentErrorsCard 로딩/에러/성공 상태 분기 처리 */}
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

        {/* 오른쪽 카드 자리 */}
        <div className="flex min-h-[300px] items-center justify-center rounded-lg border-2 border-dashed border-gray-300 bg-gray-100">
          <p className="text-gray-400">오른쪽 카드 예정</p>
        </div>
      </div>
      <FloatingChecklist />
    </div>
  );
};

export default DashboardPage;