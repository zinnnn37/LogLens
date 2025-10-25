import DashboardStatsCards from '@/components/DashboardStatsCards';
import RecentAlertsCard from '@/components/RecentAlertsCard';
import LogHeatmapCard from '@/components/LogHeatmapCard';
import { DUMMY_DASHBOARD_STATS } from '@/mocks/dummyDashboardStats';
import { DUMMY_ALERTS } from '@/mocks/dummyAlerts';
import { DUMMY_HEATMAP_DATA } from '@/mocks/dummyHeatmap';

const LogsPage = () => {
  return (
    <div className="font-pretendard space-y-6 p-6 py-1">
      <h1 className="font-godoM text-lg">통계 요약</h1>
      <DashboardStatsCards stats={DUMMY_DASHBOARD_STATS} />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <RecentAlertsCard alerts={DUMMY_ALERTS} />
        <LogHeatmapCard data={DUMMY_HEATMAP_DATA} />
      </div>
    </div>
  );
};

export default LogsPage;
