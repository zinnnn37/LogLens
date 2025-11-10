export type ComponentType =
  | 'FRONTEND'
  | 'BE'
  | 'BACKEND'
  | 'INFRA'
  | 'EXTERNAL';
export type ComponentLayer =
  | 'PRESENTATION'
  | 'CONTROLLER'
  | 'SERVICE'
  | 'REPOSITORY'
  | 'VALIDATOR';
export type RelationshipType =
  | 'HTTP_REQUEST'
  | 'METHOD_CALL'
  | 'DATABASE_QUERY'
  | 'API_CALL';

export interface ComponentMetrics {
  callCount: number;
  errorCount: number;
  warnCount: number;
  errorRate: number;
  lastMeasuredAt: string;
}

export interface Component {
  id: number;
  name: string;
  type: ComponentType;
  classType: string;
  layer: ComponentLayer;
  packageName: string;
  technology: string;
  metrics: ComponentMetrics | null;
}

export interface ComponentPagination {
  limit: number;
  offset: number;
  total: number;
  hasNext: boolean;
}

export interface ComponentSummary {
  totalComponents: number;
  byType: Record<ComponentType, number>;
  byLayer: Record<ComponentLayer, number>;
}

// 백엔드 응답 형식
export interface ComponentListApiResponse {
  code: string;
  message: string;
  status: number;
  data: {
    projectId: number;
    components: Component[];
  };
  timestamp: string;
}

// 프론트엔드에서 사용할 데이터 형식 (기존 유지)
export interface ComponentListData {
  projectId: string;
  components: Component[];
  pagination: ComponentPagination;
  summary: ComponentSummary;
}

export interface ComponentListResponse {
  code: number;
  message: string;
  data: ComponentListData;
  timestamp: string;
}

// Component Detail Dependency Types
export interface DependencyNode {
  id: string;
  name: string;
  type: ComponentType;
  layer: ComponentLayer;
  relationship: RelationshipType;
  callCount: number;
  errorCount: number;
  averageResponseTime: number;
  children?: DependencyNode[];
}

export interface GraphNode {
  id: string;
  name: string;
  type: ComponentType;
  layer: ComponentLayer;
  metrics?: ComponentMetrics | null;
}

export interface GraphEdge {
  source: string;
  target: string;
  callCount: number;
  relationship: RelationshipType;
}

export interface ComponentDetailInfo {
  id: string;
  name: string;
  type: ComponentType;
  layer: ComponentLayer;
  packageName: string;
  technology: string;
}

export interface DependencySummary {
  upstreamCount: number;
  downstreamCount: number;
  totalDependencies: number;
  maxDepth: number;
  totalCalls: number;
  totalErrors: number;
  overallErrorRate: number;
}

export interface ComponentDependencyData {
  projectId: string;
  component: ComponentDetailInfo;
  upstream: DependencyNode[];
  downstream: DependencyNode[];
  graph: {
    nodes: GraphNode[];
    edges: GraphEdge[];
  };
  summary: DependencySummary;
}

export interface ComponentDependencyResponse {
  code: number;
  message: string;
  data: ComponentDependencyData;
  timestamp: string;
}

// 백엔드 의존성 API 응답 형식
export interface DependencyGraphEdge {
  from: number;
  to: number;
}

export interface DependencyGraphSummary {
  totalComponents: number;
  totalCalls: number;
  totalErrors: number;
  totalWarns: number;
  averageErrorRate: number;
  highestErrorComponent: string | null;
  highestCallComponent: string | null;
}

export interface DependencyFrontendSummary {
  totalTraces: number;
  totalInfoLogs: number;
  totalWarnLogs: number;
  totalErrorLogs: number;
  errorRate: number;
}

export interface ComponentDependencyApiResponse {
  code: string;
  message: string;
  status: number;
  data: {
    components: Component[];
    graph: {
      edges: DependencyGraphEdge[];
    };
    graphSummary: DependencyGraphSummary;
    frontendSummary: DependencyFrontendSummary;
  };
  timestamp: string;
}
