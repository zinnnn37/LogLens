import type {
  ComponentListData,
  Component,
  ComponentStatus,
} from '@/types/component';

interface DependencyComponentsProps {
  data: ComponentListData | null;
  isLoading?: boolean;
  onClose?: () => void;
  onComponentClick?: (componentId: string, componentName: string) => void;
}

// 상태별 배지 색상
const STATUS_COLORS: Record<
  ComponentStatus,
  { bg: string; text: string; border: string }
> = {
  ACTIVE: {
    bg: 'bg-green-50',
    text: 'text-green-700',
    border: 'border-green-200',
  },
  WARNING: {
    bg: 'bg-yellow-50',
    text: 'text-yellow-700',
    border: 'border-yellow-200',
  },
  ERROR: { bg: 'bg-red-50', text: 'text-red-700', border: 'border-red-200' },
};

const DependencyComponents = ({
  data,
  isLoading,
  onClose,
  onComponentClick,
}: DependencyComponentsProps) => {
  if (isLoading) {
    return (
      <div className="rounded-lg border bg-white p-6 shadow-sm">
        <div className="text-center text-gray-500">
          컴포넌트 목록을 불러오는 중...
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="rounded-lg border bg-white p-6 shadow-sm">
        <div className="text-center text-gray-500">
          컴포넌트 데이터가 없습니다.
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-lg border bg-white shadow-sm">
      {/* 헤더 */}
      <div className="flex items-center justify-between border-b p-4">
        <div>
          <h2 className="text-lg font-semibold">컴포넌트 목록</h2>
          <p className="mt-1 text-sm text-gray-500">
            총 {data.summary.totalComponents}개의 컴포넌트
          </p>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-gray-600 hover:bg-gray-100"
          >
            닫기
          </button>
        )}
      </div>

      {/* 요약 정보 */}
      <div className="grid grid-cols-3 gap-4 border-b p-4">
        <div className="rounded-lg bg-blue-50 p-3">
          <div className="text-xs text-blue-600">레이어별</div>
          <div className="mt-1 space-y-1 text-xs">
            {Object.entries(data.summary.byLayer).map(([layer, count]) => (
              <div key={layer} className="flex justify-between">
                <span className="text-gray-600">{layer}</span>
                <span className="font-medium text-gray-900">{count}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="rounded-lg bg-green-50 p-3">
          <div className="text-xs text-green-600">타입별</div>
          <div className="mt-1 space-y-1 text-xs">
            {Object.entries(data.summary.byType)
              .filter(([, count]) => count > 0)
              .map(([type, count]) => (
                <div key={type} className="flex justify-between">
                  <span className="text-gray-600">{type}</span>
                  <span className="font-medium text-gray-900">{count}</span>
                </div>
              ))}
          </div>
        </div>

        <div className="rounded-lg bg-purple-50 p-3">
          <div className="text-xs text-purple-600">상태별</div>
          <div className="mt-1 space-y-1 text-xs">
            {Object.entries(data.summary.byStatus)
              .filter(([, count]) => count > 0)
              .map(([status, count]) => (
                <div key={status} className="flex justify-between">
                  <span className="text-gray-600">{status}</span>
                  <span className="font-medium text-gray-900">{count}</span>
                </div>
              ))}
          </div>
        </div>
      </div>

      {/* 컴포넌트 그리드 */}
      <div className="max-h-[500px] overflow-y-auto p-4">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
          {data.components.map((component: Component) => {
            const statusColor = STATUS_COLORS[component.status];
            return (
              <div
                key={component.id}
                className="cursor-pointer rounded-lg border bg-white p-4 shadow-sm transition-all hover:border-blue-300 hover:shadow-md"
                onClick={() => onComponentClick?.(component.id, component.name)}
              >
                {/* 컴포넌트 이름 및 상태 */}
                <div className="mb-2 flex items-start justify-between">
                  <div className="flex-1">
                    <h3 className="font-semibold text-gray-900">
                      {component.name}
                    </h3>
                    <p className="mt-0.5 text-xs text-gray-500">
                      {component.packageName}
                    </p>
                  </div>
                  <span
                    className={`ml-2 rounded-full border px-2 py-0.5 text-xs font-medium ${statusColor.bg} ${statusColor.text} ${statusColor.border}`}
                  >
                    {component.status}
                  </span>
                </div>

                {/* 메타 정보 */}
                <div className="mb-3 space-y-1 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">레이어</span>
                    <span className="font-medium text-gray-700">
                      {component.layer}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">기술</span>
                    <span className="font-medium text-gray-700">
                      {component.technology}
                    </span>
                  </div>
                </div>

                {/* 의존성 정보 */}
                <div className="flex items-center gap-3 border-t pt-3 text-xs">
                  <div className="flex items-center gap-1">
                    <span className="text-gray-500">Upstream:</span>
                    <span className="font-medium text-blue-600">
                      {component.dependencies.upstreamCount}
                    </span>
                  </div>
                  <div className="flex items-center gap-1">
                    <span className="text-gray-500">Downstream:</span>
                    <span className="font-medium text-green-600">
                      {component.dependencies.downstreamCount}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 페이지네이션 정보 */}
      {data.pagination.total > 0 && (
        <div className="border-t p-4">
          <div className="text-center text-xs text-gray-500">
            {data.pagination.offset + 1} -{' '}
            {Math.min(
              data.pagination.offset + data.pagination.limit,
              data.pagination.total,
            )}{' '}
            / {data.pagination.total}
            {data.pagination.hasNext && ' • 더 보기 가능'}
          </div>
        </div>
      )}
    </div>
  );
};

export default DependencyComponents;
