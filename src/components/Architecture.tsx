import { useCallback, useEffect } from 'react';
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

import type {
  ArchitectureData,
  ArchitectureNode,
  ArchitectureEdge,
  NodeType,
} from '@/types/architecture';

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
  INFRA: {
    bg: '#1e293b',
    border: '#f59e0b',
    shadow: 'rgba(245, 158, 11, 0.3)',
    text: '#f1f5f9',
    accent: '#f59e0b',
  },
};

// 레이어별 X 좌표 (가로 방향 레이어 - 간격 증가)
const LAYER_X_POSITIONS: Record<string, number> = {
  PRESENTATION: 50,
  GATEWAY: 450,
  APPLICATION: 850,
  DATA: 1250,
};

interface ArchitectureProps {
  data: ArchitectureData | null;
  isLoading?: boolean;
  onNodeClick?: (nodeId: string, nodeName: string) => void;
}

const Architecture = ({ data, isLoading, onNodeClick }: ArchitectureProps) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // 노드 클릭 핸들러
  const handleNodeClick = useCallback(
    (_event: React.MouseEvent, node: Node) => {
      if (onNodeClick) {
        onNodeClick(node.id, String(node.data.label));
      }
    },
    [onNodeClick],
  );

  // 아키텍처 데이터를 ReactFlow 노드/엣지로 변환
  const convertToFlowElements = useCallback(
    (architectureData: ArchitectureData) => {
      // 노드 변환
      const flowNodes: Node[] = architectureData.nodes.map(
        (node: ArchitectureNode) => {
          const color = NODE_COLORS[node.type];
          const xPosition = LAYER_X_POSITIONS[node.layer] || 0;

          // 같은 레이어 내 노드들을 수직으로 배치
          const nodesInSameLayer = architectureData.nodes.filter(
            n => n.layer === node.layer,
          );
          const indexInLayer = nodesInSameLayer.indexOf(node);
          const yPosition = 100 + indexInLayer * 200;

          return {
            id: node.id,
            type: 'default',
            position: { x: xPosition, y: yPosition },
            data: {
              label: (
                <div className="relative">
                  {/* 상단 액센트 바 */}
                  <div
                    className="absolute top-0 right-0 left-0 h-1 rounded-t"
                    style={{ background: color.accent }}
                  />
                  <div className="px-4 pt-4 pb-3">
                    {/* 타입 배지 */}
                    <div className="mb-2 flex items-center justify-between">
                      <span
                        className="rounded px-2 py-0.5 font-mono text-xs font-semibold tracking-wider uppercase"
                        style={{
                          background: `${color.accent}15`,
                          color: color.accent,
                        }}
                      >
                        {node.type}
                      </span>
                    </div>
                    {/* 노드 이름 */}
                    <div
                      className="mb-1 font-mono text-sm font-bold"
                      style={{ color: color.text }}
                    >
                      {node.name}
                    </div>
                    {/* 기술 스택 */}
                    <div
                      className="font-mono text-xs"
                      style={{ color: `${color.text}99` }}
                    >
                      {node.technology}
                    </div>
                  </div>
                </div>
              ),
            },
            style: {
              background: color.bg,
              border: `2px solid ${color.border}`,
              borderRadius: '8px',
              padding: '0',
              width: 200,
              boxShadow: `0 4px 12px ${color.shadow}, 0 0 0 1px ${color.border}20`,
              cursor: 'pointer',
            },
            sourcePosition: Position.Right,
            targetPosition: Position.Left,
          };
        },
      );

      // 엣지 변환 - 간단한 점선 애니메이션
      const flowEdges: Edge[] = architectureData.edges.map(
        (edge: ArchitectureEdge, index: number) => ({
          id: `edge-${index}`,
          source: edge.source,
          target: edge.target,
          type: 'smoothstep',
          animated: true,
          label: edge.relationship,
          labelStyle: {
            fontSize: 10,
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
            strokeWidth: 2,
          },
        }),
      );

      return { nodes: flowNodes, edges: flowEdges };
    },
    [],
  );

  // 데이터가 변경될 때 노드와 엣지 업데이트
  useEffect(() => {
    if (data) {
      const { nodes: flowNodes, edges: flowEdges } =
        convertToFlowElements(data);
      setNodes(flowNodes);
      setEdges(flowEdges);
    }
  }, [data, convertToFlowElements, setNodes, setEdges]);

  if (isLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center rounded-lg border bg-white shadow-sm">
        <div className="text-gray-500">아키텍처 데이터를 불러오는 중...</div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex h-[400px] items-center justify-center rounded-lg border bg-white shadow-sm">
        <div className="text-gray-500">
          아키텍처 데이터가 없습니다. 기간을 선택해주세요.
        </div>
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
              <span className="font-mono">Project: {data.projectId}</span>
              <span>•</span>
              <span className="font-mono">
                {new Date(data.period.startDate).toLocaleDateString()} -{' '}
                {new Date(data.period.endDate).toLocaleDateString()}
              </span>
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
              const nodeData = data.nodes.find(n => n.id === node.id);
              if (nodeData) {
                return NODE_COLORS[nodeData.type].border;
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
