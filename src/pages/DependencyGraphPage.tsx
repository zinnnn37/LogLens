import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import Architecture from '@/components/Architecture';
import DependencyComponents from '@/components/DependencyComponents';
import ComponentDependencyGraph from '@/components/ComponentDependencyGraph';
import FloatingChecklist from '@/components/FloatingChecklist';
import { DUMMY_ARCHITECTURE_DATA } from '@/mocks/dummyArchitecture';
import {
  getComponents,
  getComponentDependencies,
} from '@/services/componentService';

const DependencyGraphPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // 선택된 노드 상태 (backend-api를 클릭하면 컴포넌트 목록 표시)
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  // 선택된 컴포넌트 상태 (컴포넌트를 클릭하면 상세 의존성 그래프 표시)
  const [selectedComponent, setSelectedComponent] = useState<{
    id: number;
    name: string;
  } | null>(null);

  // 컴포넌트 목록 조회
  const { data: componentsData, isLoading: isComponentsLoading } = useQuery({
    queryKey: ['components', projectUuid],
    queryFn: () => getComponents(projectUuid!),
    enabled: Boolean(projectUuid) && selectedNode === 'backend-api',
  });

  // 컴포넌트 의존성 조회
  const {
    data: dependencyData,
    isLoading: isDependencyLoading,
    error: dependencyError,
  } = useQuery({
    queryKey: ['componentDependencies', selectedComponent?.id, projectUuid],
    queryFn: () => getComponentDependencies(selectedComponent!.id, projectUuid),
    enabled: Boolean(selectedComponent?.id) && Boolean(projectUuid),
  });

  // 에러 로깅
  if (dependencyError) {
    console.error('Dependency fetch error:', dependencyError);
  }

  // 노드 클릭 핸들러
  const handleNodeClick = (nodeId: string, nodeName: string) => {
    console.log('Node clicked:', nodeId, nodeName);
    // backend-api 노드를 클릭했을 때만 컴포넌트 목록 표시
    if (nodeId === 'backend-api') {
      setSelectedNode(nodeId);
      setSelectedComponent(null); // 컴포넌트 선택 초기화
    }
  };

  // 컴포넌트 클릭 핸들러
  const handleComponentClick = (componentId: number, componentName: string) => {
    console.log('Component clicked:', componentId, componentName);
    setSelectedComponent({ id: componentId, name: componentName });
  };

  // 컴포넌트 목록 닫기
  const handleCloseComponents = () => {
    setSelectedNode(null);
    setSelectedComponent(null);
  };

  // 의존성 그래프 닫기
  const handleCloseDependencyGraph = () => {
    setSelectedComponent(null);
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mx-auto max-w-7xl">
        {/* 페이지 헤더 */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">의존성 그래프</h1>
          <p className="mt-1 text-sm text-gray-500">
            프로젝트의 고수준 아키텍처 흐름을 시각화합니다
          </p>
        </div>

        {/* Architecture 컴포넌트 */}
        <Architecture
          data={DUMMY_ARCHITECTURE_DATA}
          isLoading={false}
          onNodeClick={handleNodeClick}
        />

        {/* 선택된 노드가 backend-api일 때 컴포넌트 목록 표시 */}
        {selectedNode === 'backend-api' && (
          <div className="mt-6">
            <DependencyComponents
              data={componentsData ?? null}
              isLoading={isComponentsLoading}
              onClose={handleCloseComponents}
              onComponentClick={handleComponentClick}
            />
          </div>
        )}

        {/* 선택된 컴포넌트의 상세 의존성 그래프 표시 */}
        {selectedComponent && (
          <div className="mt-6">
            {dependencyError ? (
              <div className="rounded-lg border border-red-200 bg-red-50 p-6 shadow-sm">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-lg font-semibold text-red-900">
                      의존성 데이터 로딩 실패
                    </h3>
                    <p className="mt-1 text-sm text-red-700">
                      컴포넌트 의존성 정보를 불러오는 중 오류가 발생했습니다.
                    </p>
                  </div>
                  <button
                    onClick={handleCloseDependencyGraph}
                    className="rounded-lg px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-100"
                  >
                    닫기
                  </button>
                </div>
              </div>
            ) : (
              <ComponentDependencyGraph
                data={dependencyData ?? null}
                isLoading={isDependencyLoading}
                onClose={handleCloseDependencyGraph}
              />
            )}
          </div>
        )}
      </div>
      <FloatingChecklist />
    </div>
  );
};

export default DependencyGraphPage;
