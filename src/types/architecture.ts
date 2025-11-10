export type NodeType = 'FRONTEND' | 'BACKEND' | 'INFRA';
export type LayerType = 'PRESENTATION' | 'GATEWAY' | 'APPLICATION' | 'DATA';
export type RelationshipType = 'HTTP_REQUEST' | 'HTTP_PROXY' | 'DATABASE_QUERY';

export interface ArchitectureNode {
  id: string;
  name: string;
  type: NodeType;
  layer: LayerType;
  technology: string;
}

export interface ArchitectureEdge {
  source: string;
  target: string;
  relationship: RelationshipType;
}

export interface ArchitecturePeriod {
  startDate: string;
  endDate: string;
}

export interface ArchitectureData {
  projectId: string;
  period: ArchitecturePeriod;
  nodes: ArchitectureNode[];
  edges: ArchitectureEdge[];
}

export interface ArchitectureResponse {
  code: number;
  message: string;
  data: ArchitectureData;
  timestamp: string;
}

// 아키텍처 의존성 조회 응답 (DB 정보만)
export interface ArchitectureDependenciesResponse {
  code: string;
  message: string;
  status: number;
  data: {
    databases: string[];
  };
  timestamp: string;
}
