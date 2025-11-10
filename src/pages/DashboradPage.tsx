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
import { DUMMY_FREQUENT_ERRORS } from '@/mocks/dummyFrequentErrors';

import { getDashboardOverview } from '@/services/dashboardService';
import type { DashboardSummary } from '@/types/dashboard';

const DashboardPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  const [stats, setStats] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false); 

  useEffect(() => {
    const fetchData = async () => {
      if (!projectUuid) { return; }

      setLoading(true);
      setError(false);
      try {
        const response = await getDashboardOverview({ projectUuid });
        setStats(response.summary);
      } catch (e) {
        console.error('대시보드 통계 조회 실패:', e);
        toast.error('대시보드 정보를 불러오지 못했습니다.');
        setError(true);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [projectUuid]);

  return (
    <div className="font-pretendard space-y-6 p-6 py-1">
      <h1 className="font-godoM text-lg">통계 요약</h1>

      {/* 대시보드 통계 개요 */}
      {loading ? (
        <div className="flex min-h-[120px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          통계 정보를 불러오는 중...
        </div>
      ) : error ? (
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
        <FrequentErrorsCard data={DUMMY_FREQUENT_ERRORS} />
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