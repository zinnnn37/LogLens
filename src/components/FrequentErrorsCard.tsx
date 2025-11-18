import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { DashboardTopErrorsData } from '@/types/dashboard';
import NotFoundIllust from '@/assets/images/NotFoundIllust.png';
import { useNavigate, useParams } from 'react-router-dom';

interface FrequentErrorsCardProps {
  data: DashboardTopErrorsData;
}

const FrequentErrorsCard = ({ data }: FrequentErrorsCardProps) => {
  const navigate = useNavigate();
  const { projectUuid } = useParams<{ projectUuid: string }>();

  const handleErrorClick = (message: string) => {
    if (projectUuid) {
      navigate(
        `/project/${projectUuid}/logs?keyword=${encodeURIComponent(message)}&logLevel=ERROR`,
      );
    }
  };

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle>자주 발생하는 에러 Top 10</CardTitle>
      </CardHeader>
      <CardContent>
        {data.errors.length === 0 ? (
          <div className="flex h-[200px] flex-col items-center justify-center gap-2 text-gray-500">
            <img src={NotFoundIllust} alt="No data" className="h-40 w-40" />
            <p className="mb-10">표시할 에러 데이터가 없어요</p>
          </div>
        ) : (
          <div className="max-h-[500px] space-y-3 overflow-x-hidden overflow-y-auto pr-2 [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-slate-200 [&::-webkit-scrollbar-track]:bg-transparent">
            {data.errors.map(error => (
              <div
                key={error.rank}
                className="cursor-pointer rounded-lg border p-4 transition-colors hover:border-gray-300 hover:bg-gray-50"
                onClick={() => handleErrorClick(error.message)}
              >
                {/* 헤더: 순위 + 에러 타입 */}
                <div className="mb-3 flex items-start gap-3">
                  <span className="mt-0.5 flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full bg-gray-200 text-sm font-bold text-gray-700">
                    {error.rank}
                  </span>
                  <div className="min-w-0 flex-1">
                    {/* .뒤에 줄바꿈 */}
                    <p className="text-sm font-semibold break-words text-gray-900">
                      {error.exceptionType.replace(/\./g, '.\u200B')}
                    </p>
                  </div>
                </div>

                {/* 에러 메시지 */}
                <p className="mb-3 ml-1 text-sm break-words text-gray-700">
                  {error.message}
                </p>

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
        )}
      </CardContent>
    </Card>
  );
};

export default FrequentErrorsCard;
