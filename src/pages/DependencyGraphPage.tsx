import { useState } from 'react';
import { useParams } from 'react-router-dom';
import Architecture from '@/components/Architecture';
import DependencyComponents from '@/components/DependencyComponents';
import ComponentDependencyGraph from '@/components/ComponentDependencyGraph';
import FloatingChecklist from '@/components/FloatingChecklist';
import { DUMMY_ARCHITECTURE_DATA } from '@/mocks/dummyArchitecture';
import { DUMMY_COMPONENTS_DATA } from '@/mocks/dummyComponents';
import { DUMMY_COMPONENT_DEPENDENCY } from '@/mocks/dummyComponentDependency';

const DependencyGraphPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // TODO: projectUuid를 사용해서 실제 프로젝트 의존성 데이터 가져오기
  console.log('Current project UUID:', projectUuid);

  // 선택된 노드 상태 (backend-api를 클릭하면 컴포넌트 목록 표시)
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  // 선택된 컴포넌트 상태 (컴포넌트를 클릭하면 상세 의존성 그래프 표시)
  const [selectedComponent, setSelectedComponent] = useState<{
    id: string;
    name: string;
  } | null>(null);

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
  const handleComponentClick = (componentId: string, componentName: string) => {
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
              data={DUMMY_COMPONENTS_DATA}
              isLoading={false}
              onClose={handleCloseComponents}
              onComponentClick={handleComponentClick}
            />
          </div>
        )}

        {/* 선택된 컴포넌트의 상세 의존성 그래프 표시 */}
        {selectedComponent && (
          <div className="mt-6">
            <ComponentDependencyGraph
              data={DUMMY_COMPONENT_DEPENDENCY}
              isLoading={false}
              onClose={handleCloseDependencyGraph}
            />
          </div>
        )}
      </div>
      <FloatingChecklist />
    </div>
  );
};

export default DependencyGraphPage;
