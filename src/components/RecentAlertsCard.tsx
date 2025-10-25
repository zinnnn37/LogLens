import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { Alert } from '@/types/alert';

interface RecentAlertsCardProps {
  alerts: Alert[];
}

const RecentAlertsCard = ({ alerts }: RecentAlertsCardProps) => {
  const getLevelColor = (level: Alert['level']) => {
    switch (level) {
      case 'ERROR':
        return 'text-red-600 bg-red-100';
      case 'WARN':
        return 'text-yellow-600 bg-yellow-100';
      case 'INFO':
        return 'text-blue-600 bg-blue-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleString('ko-KR', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle>최근 알림</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {alerts.map(alert => (
            <div
              key={alert.id}
              className="flex items-start gap-4 rounded-lg border p-4 transition-colors hover:bg-gray-50"
            >
              <span
                className={`rounded px-3 py-1.5 text-xs font-semibold ${getLevelColor(alert.level)}`}
              >
                {alert.level}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-medium text-gray-900">
                  {alert.message}
                </p>
                <div className="mt-2 flex items-center gap-2 text-xs text-gray-500">
                  <span className="truncate">{alert.traceId}</span>
                  <span>•</span>
                  <span>{formatTimestamp(alert.timestamp)}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};

export default RecentAlertsCard;
