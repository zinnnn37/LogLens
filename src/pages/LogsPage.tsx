import DashboardStatsCards from '@/components/DashboardStatsCards';
import { DUMMY_DASHBOARD_STATS } from '@/mocks/dummyDashboardStats';

const LogsPage = () => {
  return (
    <div className="p-6">
      <DashboardStatsCards stats={DUMMY_DASHBOARD_STATS} />
    </div>
  );
};

export default LogsPage;
