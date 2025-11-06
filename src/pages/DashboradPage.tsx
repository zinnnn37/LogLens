import { useParams } from 'react-router-dom';
import DashboardStatsCards from '@/components/DashboardStatsCards';
import RecentAlertsCard from '@/components/RecentAlertsCard';
import LogHeatmapCard from '@/components/LogHeatmapCard';
import FrequentErrorsCard from '@/components/FrequentErrorsCard';
import FloatingChecklist from '@/components/FloatingChecklist';
import { DUMMY_DASHBOARD_STATS } from '@/mocks/dummyDashboardStats';
import { DUMMY_ALERTS } from '@/mocks/dummyAlerts';
import { DUMMY_HEATMAP_DATA } from '@/mocks/dummyHeatmap';
import { DUMMY_FREQUENT_ERRORS } from '@/mocks/dummyFrequentErrors';

const DashboardPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // TODO: projectUuid를 사용해서 실제 프로젝트 데이터 가져오기
  console.log('Current project UUID:', projectUuid);

  return (
    <div className="font-pretendard space-y-6 p-6 py-1">
      <h1 className="font-godoM text-lg">통계 요약</h1>
      <DashboardStatsCards stats={DUMMY_DASHBOARD_STATS} />

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
