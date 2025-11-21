import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { DashboardSummary } from '@/types/dashboard';

interface DashboardStatsCardsProps {
  stats: DashboardSummary;
}

const DashboardStatsCards = ({ stats }: DashboardStatsCardsProps) => {
  const statsItems = [
    {
      title: '전체 로그',
      value: stats.totalLogs.toLocaleString(),
      color: 'text-blue-600',
      bgColor: 'bg-blue-50',
    },
    {
      title: 'ERROR',
      value: stats.errorCount.toLocaleString(),
      color: 'text-red-600',
      bgColor: 'bg-red-50',
    },
    {
      title: 'WARN',
      value: stats.warnCount.toLocaleString(),
      color: 'text-yellow-600',
      bgColor: 'bg-yellow-50',
    },
    {
      title: 'INFO',
      value: stats.infoCount.toLocaleString(),
      color: 'text-green-600',
      bgColor: 'bg-green-50',
    },
    {
      title: '평균 응답시간',
      value: `${stats.avgResponseTime}ms`,
      color: 'text-purple-600',
      bgColor: 'bg-purple-50',
    },
  ];

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
      {statsItems.map((item, index) => (
        <Card key={index} className={`${item.bgColor} border-none shadow-md`}>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-gray-600">
              {item.title}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${item.color}`}>
              {item.value}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
};

export default DashboardStatsCards;
