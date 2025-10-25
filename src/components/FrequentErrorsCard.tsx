import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { FrequentErrorsData } from '@/types/error';

interface FrequentErrorsCardProps {
  data: FrequentErrorsData;
}

const FrequentErrorsCard = ({ data }: FrequentErrorsCardProps) => {
  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'HIGH':
        return 'bg-red-100 text-red-700';
      case 'MEDIUM':
        return 'bg-yellow-100 text-yellow-700';
      case 'LOW':
        return 'bg-blue-100 text-blue-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  const getTrendIcon = (trend: string, percentage: number) => {
    if (trend === 'INCREASING') {
      return (
        <span className="px-2 py-0.5 bg-red-100 text-red-700 rounded text-xs font-semibold">
          ↑ {Math.abs(percentage)}%
        </span>
      );
    }
    if (trend === 'DECREASING') {
      return (
        <span className="px-2 py-0.5 bg-green-100 text-green-700 rounded text-xs font-semibold">
          ↓ {Math.abs(percentage)}%
        </span>
      );
    }
    return (
      <span className="px-2 py-0.5 bg-gray-100 text-gray-700 rounded text-xs font-semibold">
        → {percentage}%
      </span>
    );
  };

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle>자주 발생하는 에러 Top 10</CardTitle>
      </CardHeader>
      <CardContent>
        <div
          className="max-h-[500px] space-y-3 overflow-y-auto scrollbar-custom"
          style={{
            scrollbarWidth: 'thin',
            scrollbarColor: '#d1d5db transparent'
          }}
        >
          {data.errors.map(error => (
            <div
              key={error.rank}
              className="rounded-lg border p-4 transition-colors hover:bg-gray-50"
            >
              {/* 헤더: 순위 + 에러코드 + 심각도 */}
              <div className="mb-3 flex items-center gap-3">
                <span className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gray-200 text-sm font-bold text-gray-700">
                  {error.rank}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-gray-900">
                    {error.errorCode}
                  </p>
                </div>
                <span
                  className={`flex-shrink-0 rounded px-2.5 py-1 text-xs font-semibold ${getSeverityColor(error.severity)}`}
                >
                  {error.severity}
                </span>
              </div>

              {/* 에러 메시지 */}
              <p className="mb-3 ml-1 text-sm text-gray-700">
                {error.errorMessage}
              </p>

              {/* 통계 정보 */}
              <div className="mb-2 ml-1 flex items-center gap-4">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-bold text-gray-900">
                    {error.count.toLocaleString()}건
                  </span>
                  <span className="text-xs text-gray-500">
                    ({error.percentage}%)
                  </span>
                </div>
                <span className="text-xs text-gray-500">•</span>
                <span className="text-xs text-gray-600">
                  영향받은 사용자 {error.affectedUsers.toLocaleString()}명
                </span>
              </div>

              {/* 컴포넌트 태그 + 트렌드 뱃지 */}
              <div className="flex items-center justify-between">
                {error.components.length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {error.components.map((component, idx) => (
                      <span
                        key={idx}
                        className="rounded bg-gray-100 px-2 py-1 text-xs text-gray-600"
                      >
                        {component}
                      </span>
                    ))}
                  </div>
                )}
                <div className={error.components.length === 0 ? "ml-auto" : ""}>
                  {getTrendIcon(error.trend, error.trendPercentage)}
                </div>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};

export default FrequentErrorsCard;
