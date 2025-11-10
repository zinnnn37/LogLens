import { apiClient } from './apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  ComponentListData,
  Component,
  ComponentType,
  ComponentLayer,
  ComponentDependencyData,
  GraphNode,
  GraphEdge,
} from '@/types/component';

/**
 * 컴포넌트 목록 조회
 */
export const getComponents = async (
  projectUuid: string,
): Promise<ComponentListData> => {
  // apiClient.get은 이미 response.data.data를 반환함
  const data = await apiClient.get<{
    projectId: number;
    components: Component[];
  }>(API_PATH.DASHBOARD_COMPONENTS, { projectUuid });

  // 백엔드 응답을 프론트엔드 형식으로 변환
  const components = data.components;

  // summary 데이터 생성
  const byType: Record<ComponentType, number> = {
    FRONTEND: 0,
    BE: 0,
    BACKEND: 0,
    INFRA: 0,
    EXTERNAL: 0,
  };

  const byLayer: Record<ComponentLayer, number> = {
    PRESENTATION: 0,
    CONTROLLER: 0,
    SERVICE: 0,
    REPOSITORY: 0,
    VALIDATOR: 0,
  };

  components.forEach((component: Component) => {
    if (component.type in byType) {
      byType[component.type]++;
    }
    if (component.layer in byLayer) {
      byLayer[component.layer]++;
    }
  });

  return {
    projectId: data.projectId.toString(),
    components: components,
    pagination: {
      limit: components.length,
      offset: 0,
      total: components.length,
      hasNext: false,
    },
    summary: {
      totalComponents: components.length,
      byType,
      byLayer,
    },
  };
};

/**
 * 컴포넌트 의존성 조회
 */
export const getComponentDependencies = async (
  componentId: number,
  projectUuid?: string,
): Promise<ComponentDependencyData> => {
  console.log('Fetching dependencies for component:', componentId);

  const params = projectUuid ? { projectUuid } : undefined;

  const data = await apiClient.get<{
    components: Component[];
    graph: {
      edges: { from: number; to: number }[];
    };
    graphSummary: {
      totalComponents: number;
      totalCalls: number;
      totalErrors: number;
      totalWarns: number;
      averageErrorRate: number;
      highestErrorComponent: string | null;
      highestCallComponent: string | null;
    };
    frontendSummary: {
      totalTraces: number;
      totalInfoLogs: number;
      totalWarnLogs: number;
      totalErrorLogs: number;
      errorRate: number;
    };
  }>(API_PATH.COMPONENT_DEPENDENCIES(componentId), params);

  // 중심 컴포넌트 찾기
  const centerComponent = data.components.find(c => c.id === componentId);
  if (!centerComponent) {
    throw new Error('Center component not found');
  }

  console.log('=== API 응답 데이터 ===');
  console.log('컴포넌트 수:', data.components.length);
  console.log('엣지 수:', data.graph.edges.length);
  console.log('엣지:', data.graph.edges);

  // 간단하게 변환: GraphNode와 GraphEdge만 생성 (metrics 포함)
  const nodes: GraphNode[] = data.components.map(comp => ({
    id: comp.id.toString(),
    name: comp.name,
    type: comp.type,
    layer: comp.layer,
    metrics: comp.metrics,
  }));

  const edges: GraphEdge[] = data.graph.edges.map(edge => ({
    source: edge.from.toString(),
    target: edge.to.toString(),
    callCount: 0,
    relationship: 'METHOD_CALL' as const,
  }));

  // upstream/downstream 계산 (단순화)
  const upstreamCount = data.graph.edges.filter(
    e => e.to === componentId,
  ).length;
  const downstreamCount = data.graph.edges.filter(
    e => e.from === componentId,
  ).length;

  return {
    projectId: '1',
    component: {
      id: centerComponent.id.toString(),
      name: centerComponent.name,
      type: centerComponent.type,
      layer: centerComponent.layer,
      packageName: centerComponent.packageName,
      technology: centerComponent.technology,
    },
    upstream: [], // 더미 데이터 (그래프 렌더링에서는 안 씀)
    downstream: [], // 더미 데이터
    graph: {
      nodes,
      edges,
    },
    summary: {
      upstreamCount,
      downstreamCount,
      totalDependencies: data.graphSummary.totalComponents - 1,
      maxDepth: 0,
      totalCalls: data.graphSummary.totalCalls,
      totalErrors: data.graphSummary.totalErrors,
      overallErrorRate: data.graphSummary.averageErrorRate,
    },
  };
};
