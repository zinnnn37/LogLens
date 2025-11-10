import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { DashboardTopErrorsData } from '@/types/dashboard';

interface FrequentErrorsCardProps {
  data: DashboardTopErrorsData;
}

const FrequentErrorsCard = ({ data }: FrequentErrorsCardProps) => {
  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle>자주 발생하는 에러 Top 10</CardTitle>
      </CardHeader>
      <CardContent>
        <div
          className="scrollbar-custom max-h-[500px] space-y-3 overflow-y-auto"
          style={{
            scrollbarWidth: 'thin',
            scrollbarColor: '#d1d5db transparent',
          }}
        >
          {data.errors.map(error => (
            <div
              key={error.rank}
              className="rounded-lg border p-4 transition-colors hover:bg-gray-50"
            >
              {/* 헤더: 순위 + 에러 타입 */}
              <div className="mb-3 flex items-center gap-3">
                <span className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gray-200 text-sm font-bold text-gray-700">
                  {error.rank}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-gray-900">
                    {error.exceptionType}
                  </p>
                </div>
              </div>

              {/* 에러 메시지 */}
              <p className="mb-3 ml-1 text-sm text-gray-700">{error.message}</p>

              {/* 통계 정보 */}
              <div className="mb-2 ml-1 flex items-center gap-2">
                <span className="text-sm font-bold text-gray-900">
                  {error.count.toLocaleString()}건
                </span>
                <span className="text-xs text-gray-500">
                  ({error.percentage}%)
                </span>
              </div>

              {/* 컴포넌트 태그 */}
              {error.components.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {error.components.map(component => (
                    <span
                      key={component.id}
                      className="rounded bg-gray-100 px-2 py-1 text-xs text-gray-600"
                    >
                      {component.name}
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};

export default FrequentErrorsCard;
