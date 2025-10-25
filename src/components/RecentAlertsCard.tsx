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
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className="flex items-start gap-4 rounded-lg border p-4 hover:bg-gray-50 transition-colors"
            >
              <span
                className={`px-3 py-1.5 rounded text-xs font-semibold ${getLevelColor(alert.level)}`}
              >
                {alert.level}
              </span>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {alert.message}
                </p>
                <div className="flex items-center gap-2 mt-2 text-xs text-gray-500">
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
