import { useCallback, useEffect, useState } from 'react';
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  Position,
  type Node,
  type Edge,
} from 'reactflow';
import 'reactflow/dist/style.css';

import { apiClient } from '@/services/apiClient';
import { API_PATH } from '@/constants/api-path';
import type { ArchitectureDependenciesResponse } from '@/types/architecture';

type NodeType = 'FRONTEND' | 'BACKEND' | 'DATABASE';

// 노드 타입별 색상 정의
const NODE_COLORS: Record<
  NodeType,
  { bg: string; border: string; shadow: string; text: string; accent: string }
> = {
  FRONTEND: {
    bg: '#1e293b',
    border: '#3b82f6',
    shadow: 'rgba(59, 130, 246, 0.3)',
    text: '#f1f5f9',
    accent: '#3b82f6',
  },
  BACKEND: {
    bg: '#1e293b',
    border: '#10b981',
    shadow: 'rgba(16, 185, 129, 0.3)',
    text: '#f1f5f9',
    accent: '#10b981',
  },
  DATABASE: {
    bg: '#1e293b',
    border: '#f59e0b',
    shadow: 'rgba(245, 158, 11, 0.3)',
    text: '#f1f5f9',
    accent: '#f59e0b',
  },
};

interface ArchitectureProps {
  projectUuid: string;
  isLoading?: boolean;
  onNodeClick?: (nodeId: string, nodeName: string) => void;
}

const Architecture = ({
  projectUuid,
  isLoading: externalLoading,
  onNodeClick,
}: ArchitectureProps) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [databases, setDatabases] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 노드 클릭 핸들러
  const handleNodeClick = useCallback(
    (_event: React.MouseEvent, node: Node) => {
      if (onNodeClick) {
        onNodeClick(node.id, String(node.data.label));
      }
    },
    [onNodeClick],
  );

  // DB 정보를 가져오는 함수
  const fetchDatabases = useCallback(async () => {
    if (!projectUuid) {
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const response = await apiClient.get<
        ArchitectureDependenciesResponse['data']
      >(API_PATH.ARCHITECTURE_DEPENDENCIES, {
        projectUuid,
      });

      setDatabases(response.databases || []);
    } catch (err) {
      console.error('Failed to fetch databases:', err);
      setError('데이터베이스 정보를 불러오는데 실패했습니다.');
      setDatabases([]);
    } finally {
      setIsLoading(false);
    }
  }, [projectUuid]);

  // 고정된 아키텍처 노드와 DB 노드를 생성하는 함수
  const createArchitectureNodes = useCallback((dbList: string[]) => {
    const flowNodes: Node[] = [];
    const flowEdges: Edge[] = [];

    // 1. Frontend 노드 (고정 - React)
    const frontendColor = NODE_COLORS.FRONTEND;
    flowNodes.push({
      id: 'frontend-react',
      type: 'default',
      position: { x: 80, y: 180 },
      data: {
        label: (
          <div className="relative">
            <div
              className="absolute top-0 right-0 left-0 h-0.5 rounded-t"
              style={{ background: frontendColor.accent }}
            />
            <div className="px-2 pt-1.5 pb-1.5">
              <div className="mb-0.5 flex items-center justify-between">
                <span
                  className="rounded px-1 py-0.5 font-mono text-[8px] font-semibold tracking-wider uppercase"
                  style={{
                    background: `${frontendColor.accent}15`,
                    color: frontendColor.accent,
                  }}
                >
                  FRONTEND
                </span>
              </div>
              <div
                className="mb-0.5 font-mono text-[10px] font-bold"
                style={{ color: frontendColor.text }}
              >
                React
              </div>
              <div
                className="font-mono text-[8px]"
                style={{ color: `${frontendColor.text}99` }}
              >
                Web Application
              </div>
            </div>
          </div>
        ),
      },
      style: {
        background: frontendColor.bg,
        border: `1px solid ${frontendColor.border}`,
        borderRadius: '4px',
        padding: '0',
        width: 100,
        boxShadow: `0 1px 4px ${frontendColor.shadow}, 0 0 0 1px ${frontendColor.border}20`,
        cursor: 'pointer',
      },
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
    });

    // 2. Backend 노드 (고정 - Spring Boot)
    const backendColor = NODE_COLORS.BACKEND;
    flowNodes.push({
      id: 'backend-api',
      type: 'default',
      position: { x: 280, y: 180 },
      data: {
        label: (
          <div className="relative">
            <div
              className="absolute top-0 right-0 left-0 h-0.5 rounded-t"
              style={{ background: backendColor.accent }}
            />
            <div className="px-2 pt-1.5 pb-1.5">
              <div className="mb-0.5 flex items-center justify-between">
                <span
                  className="rounded px-1 py-0.5 font-mono text-[8px] font-semibold tracking-wider uppercase"
                  style={{
                    background: `${backendColor.accent}15`,
                    color: backendColor.accent,
                  }}
                >
                  BACKEND
                </span>
              </div>
              <div
                className="mb-0.5 font-mono text-[10px] font-bold"
                style={{ color: backendColor.text }}
              >
                Spring Boot
              </div>
              <div
                className="font-mono text-[8px]"
                style={{ color: `${backendColor.text}99` }}
              >
                API Server
              </div>
            </div>
          </div>
        ),
      },
      style: {
        background: backendColor.bg,
        border: `1px solid ${backendColor.border}`,
        borderRadius: '4px',
        padding: '0',
        width: 100,
        boxShadow: `0 1px 4px ${backendColor.shadow}, 0 0 0 1px ${backendColor.border}20`,
        cursor: 'pointer',
      },
      sourcePosition: Position.Right,
      targetPosition: Position.Left,
    });

    // Frontend -> Backend 엣지
    flowEdges.push({
      id: 'edge-frontend-backend',
      source: 'frontend-react',
      target: 'backend-api',
      type: 'smoothstep',
      animated: true,
      label: 'HTTP_REQUEST',
      labelStyle: {
        fontSize: 8,
        fill: '#475569',
        fontWeight: 600,
        fontFamily: 'monospace',
      },
      labelBgStyle: {
        fill: '#f8fafc',
        fillOpacity: 0.95,
      },
      style: {
        stroke: '#3b82f6',
        strokeWidth: 1,
      },
    });

    // 3. Database 노드들 (동적 - API에서 받아온 정보)
    const databaseColor = NODE_COLORS.DATABASE;
    dbList.forEach((db, index) => {
      const nodeId = `database-${db.toLowerCase()}-${index}`;
      const yPosition = 100 + index * 100;

      flowNodes.push({
        id: nodeId,
        type: 'default',
        position: { x: 500, y: yPosition },
        data: {
          label: (
            <div className="relative">
              <div
                className="absolute top-0 right-0 left-0 h-0.5 rounded-t"
                style={{ background: databaseColor.accent }}
              />
              <div className="px-2 pt-1.5 pb-1.5">
                <div className="mb-0.5 flex items-center justify-between">
                  <span
                    className="rounded px-1 py-0.5 font-mono text-[8px] font-semibold tracking-wider uppercase"
                    style={{
                      background: `${databaseColor.accent}15`,
                      color: databaseColor.accent,
                    }}
                  >
                    DATABASE
                  </span>
                </div>
                <div
                  className="mb-0.5 font-mono text-[10px] font-bold"
                  style={{ color: databaseColor.text }}
                >
                  {db}
                </div>
                <div
                  className="font-mono text-[8px]"
                  style={{ color: `${databaseColor.text}99` }}
                >
                  Data Storage
                </div>
              </div>
            </div>
          ),
        },
        style: {
          background: databaseColor.bg,
          border: `1px solid ${databaseColor.border}`,
          borderRadius: '4px',
          padding: '0',
          width: 100,
          boxShadow: `0 1px 4px ${databaseColor.shadow}, 0 0 0 1px ${databaseColor.border}20`,
          cursor: 'pointer',
        },
        sourcePosition: Position.Right,
        targetPosition: Position.Left,
      });

      // Backend -> Database 엣지
      flowEdges.push({
        id: `edge-backend-${nodeId}`,
        source: 'backend-api',
        target: nodeId,
        type: 'smoothstep',
        animated: true,
        label: 'DATABASE_QUERY',
        labelStyle: {
          fontSize: 8,
          fill: '#475569',
          fontWeight: 600,
          fontFamily: 'monospace',
        },
        labelBgStyle: {
          fill: '#f8fafc',
          fillOpacity: 0.95,
        },
        style: {
          stroke: '#10b981',
          strokeWidth: 1,
        },
      });
    });

    return { nodes: flowNodes, edges: flowEdges };
  }, []);

  // 프로젝트 UUID가 변경될 때 DB 정보 가져오기
  useEffect(() => {
    if (projectUuid) {
      fetchDatabases();
    }
  }, [projectUuid, fetchDatabases]);

  // DB 정보가 변경될 때 노드와 엣지 업데이트
  useEffect(() => {
    const { nodes: flowNodes, edges: flowEdges } =
      createArchitectureNodes(databases);
    setNodes(flowNodes);
    setEdges(flowEdges);
  }, [databases, createArchitectureNodes, setNodes, setEdges]);

  const loading = isLoading || externalLoading;

  if (loading) {
    return (
      <div className="flex h-[400px] items-center justify-center rounded-lg border bg-white shadow-sm">
        <div className="text-gray-500">아키텍처 데이터를 불러오는 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex h-[400px] items-center justify-center rounded-lg border bg-white shadow-sm">
        <div className="text-red-500">{error}</div>
      </div>
    );
  }

  if (!projectUuid) {
    return (
      <div className="flex h-[400px] items-center justify-center rounded-lg border bg-white shadow-sm">
        <div className="text-gray-500">프로젝트를 선택해주세요.</div>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      {/* 헤더 */}
      <div className="border-b border-gray-200 bg-slate-900 px-5 py-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-mono text-base font-bold text-white">
              Architecture Overview
            </h2>
            <div className="mt-1 flex items-center gap-3 text-xs text-slate-400">
              <span className="font-mono">Project: {projectUuid}</span>
              {databases.length > 0 && (
                <>
                  <span>•</span>
                  <span className="font-mono">
                    Databases: {databases.join(', ')}
                  </span>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ReactFlow 다이어그램 */}
      <div className="h-[450px] bg-slate-50">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onNodeClick={handleNodeClick}
          fitView
          attributionPosition="bottom-left"
        >
          <Background />
          <Controls />
          <MiniMap
            nodeColor={node => {
              if (node.id.startsWith('frontend-')) {
                return NODE_COLORS.FRONTEND.border;
              }
              if (node.id === 'backend-api' || node.id.startsWith('backend-')) {
                return NODE_COLORS.BACKEND.border;
              }
              if (node.id.startsWith('database-')) {
                return NODE_COLORS.DATABASE.border;
              }
              return '#666';
            }}
            nodeStrokeWidth={3}
            zoomable
            pannable
            style={{
              width: 120,
              height: 80,
            }}
          />
        </ReactFlow>
      </div>

      {/* 범례 */}
      <div className="border-t border-gray-200 bg-white px-5 py-3">
        <div className="flex items-center gap-6">
          <div className="font-mono text-xs font-semibold tracking-wider text-gray-500 uppercase">
            Legend
          </div>
          {Object.entries(NODE_COLORS).map(([type, colors]) => (
            <div key={type} className="flex items-center gap-2">
              <div
                className="h-3 w-3 rounded border-2"
                style={{
                  background: colors.bg,
                  borderColor: colors.border,
                }}
              />
              <span className="font-mono text-xs font-medium text-gray-700">
                {type}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Architecture;
