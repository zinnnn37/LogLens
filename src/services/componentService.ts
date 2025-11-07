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

  // 그래프를 기반으로 upstream과 downstream 분류
  const upstreamIds = new Set<number>();
  const downstreamIds = new Set<number>();

  data.graph.edges.forEach(edge => {
    if (edge.to === componentId) {
      upstreamIds.add(edge.from);
    }
    if (edge.from === componentId) {
      downstreamIds.add(edge.to);
    }
  });

  // upstream과 downstream을 DependencyNode 형식으로 변환
  const upstream = Array.from(upstreamIds)
    .map(id => {
      const comp = data.components.find(c => c.id === id);
      if (!comp) return null;
      return {
        id: comp.id.toString(),
        name: comp.name,
        type: comp.type,
        layer: comp.layer,
        relationship: 'METHOD_CALL' as const,
        callCount: comp.metrics?.callCount || 0,
        errorCount: comp.metrics?.errorCount || 0,
        averageResponseTime: 0,
      };
    })
    .filter((node): node is NonNullable<typeof node> => node !== null);

  // downstream을 재귀적으로 구성
  const buildDownstreamTree = (nodeId: number, visited = new Set<number>()) => {
    if (visited.has(nodeId)) return null;
    visited.add(nodeId);

    const comp = data.components.find(c => c.id === nodeId);
    if (!comp) return null;

    const childIds = data.graph.edges
      .filter(edge => edge.from === nodeId)
      .map(edge => edge.to);

    const children = childIds
      .map(childId => buildDownstreamTree(childId, visited))
      .filter((node): node is NonNullable<typeof node> => node !== null);

    return {
      id: comp.id.toString(),
      name: comp.name,
      type: comp.type,
      layer: comp.layer,
      relationship: 'METHOD_CALL' as const,
      callCount: comp.metrics?.callCount || 0,
      errorCount: comp.metrics?.errorCount || 0,
      averageResponseTime: 0,
      children: children.length > 0 ? children : undefined,
    };
  };

  const downstream = Array.from(downstreamIds)
    .map(id => buildDownstreamTree(id))
    .filter((node): node is NonNullable<typeof node> => node !== null);

  // GraphNode와 GraphEdge 변환
  const nodes: GraphNode[] = data.components.map(comp => ({
    id: comp.id.toString(),
    name: comp.name,
    type: comp.type,
    layer: comp.layer,
  }));

  const edges: GraphEdge[] = data.graph.edges.map(edge => ({
    source: edge.from.toString(),
    target: edge.to.toString(),
    callCount: 0,
    relationship: 'METHOD_CALL' as const,
  }));

  return {
    projectId: '1', // projectId는 응답에 없으므로 임시값
    component: {
      id: centerComponent.id.toString(),
      name: centerComponent.name,
      type: centerComponent.type,
      layer: centerComponent.layer,
      packageName: centerComponent.packageName,
      technology: centerComponent.technology,
    },
    upstream,
    downstream,
    graph: {
      nodes,
      edges,
    },
    summary: {
      upstreamCount: upstream.length,
      downstreamCount: downstream.length,
      totalDependencies: data.graphSummary.totalComponents - 1, // 중심 컴포넌트 제외
      maxDepth: 0, // 백엔드에서 제공하지 않음
      totalCalls: data.graphSummary.totalCalls,
      totalErrors: data.graphSummary.totalErrors,
      overallErrorRate: data.graphSummary.averageErrorRate,
    },
  };
};
