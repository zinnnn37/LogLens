import { useCallback, useEffect } from 'react';
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  Position,
  MarkerType,
  type Node,
  type Edge,
} from 'reactflow';
import 'reactflow/dist/style.css';

import type {
  ComponentDependencyData,
  ComponentType,
  ComponentLayer,
  DependencyNode,
} from '@/types/component';

// 컴포넌트 타입별 색상 (다크 테마)
const TYPE_COLORS: Record<
  ComponentType,
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
  BE: {
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
  EXTERNAL: {
    bg: '#1e293b',
    border: '#a855f7',
    shadow: 'rgba(168, 85, 247, 0.3)',
    text: '#f1f5f9',
    accent: '#a855f7',
  },
};

// 레이어별 Y 좌표 (수직 방향 레이어)
const LAYER_Y_POSITIONS: Record<ComponentLayer, number> = {
  PRESENTATION: 50,
  CONTROLLER: 200,
  SERVICE: 350,
  REPOSITORY: 500,
  VALIDATOR: 350,
};

interface ComponentDependencyGraphProps {
  data: ComponentDependencyData | null;
  isLoading?: boolean;
  onClose?: () => void;
}

const ComponentDependencyGraph = ({
  data,
  isLoading,
  onClose,
}: ComponentDependencyGraphProps) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // 의존성 노드들을 평탄화하여 모든 노드 추출
  const flattenDependencyNodes = useCallback(
    (dependencyNodes: DependencyNode[]): DependencyNode[] => {
      const result: DependencyNode[] = [];
      const flatten = (nodes: DependencyNode[]) => {
        nodes.forEach(node => {
          result.push(node);
          if (node.children && node.children.length > 0) {
            flatten(node.children);
          }
        });
      };
      flatten(dependencyNodes);
      return result;
    },
    [],
  );

  // 그래프 데이터를 ReactFlow 노드/엣지로 변환
  const convertToFlowElements = useCallback(
    (componentData: ComponentDependencyData) => {
      const { graph, component, upstream, downstream } = componentData;

      // 모든 downstream 노드 평탄화
      const allDownstream = flattenDependencyNodes(downstream);

      // 중심 컴포넌트는 가운데 배치
      const centerX = 500;
      const centerY = 300;

      const flowNodes: Node[] = graph.nodes.map(graphNode => {
        const color = TYPE_COLORS[graphNode.type] || TYPE_COLORS.BACKEND;
        const isCenterNode = graphNode.id === component.id;

        // upstream은 왼쪽, 중심은 중간, downstream은 오른쪽
        let xPosition = centerX;
        let yPosition = LAYER_Y_POSITIONS[graphNode.layer] || centerY;

        if (isCenterNode) {
          xPosition = centerX;
          yPosition = centerY;
        } else if (upstream.some(u => u.id === graphNode.id)) {
          xPosition = centerX - 300;
          yPosition = centerY;
        } else if (allDownstream.some(d => d.id === graphNode.id)) {
          // depth에 따라 x 좌표 결정
          // const node = allDownstream.find(d => d.id === graphNode.id);
          const depth = findNodeDepth(graphNode.id, downstream);
          xPosition = centerX + 300 + depth * 250;

          // 같은 depth의 노드들을 수직으로 배치
          const nodesAtSameDepth = allDownstream.filter(
            d => findNodeDepth(d.id, downstream) === depth,
          );
          const indexAtDepth = nodesAtSameDepth.findIndex(
            d => d.id === graphNode.id,
          );
          yPosition = centerY - 100 + indexAtDepth * 150;
        }

        // 상세 정보 포함
        const nodeInfo = allDownstream.find(d => d.id === graphNode.id);
        const upstreamInfo = upstream.find(u => u.id === graphNode.id);
        const detailInfo = nodeInfo || upstreamInfo;

        return {
          id: graphNode.id,
          type: 'default',
          position: { x: xPosition, y: yPosition },
          data: {
            label: (
              <div className="relative">
                {/* 상단 액센트 바 */}
                <div
                  className="absolute top-0 right-0 left-0 h-1 rounded-t"
                  style={{
                    background: isCenterNode ? '#ef4444' : color.accent,
                  }}
                />
                <div className="px-3 pt-3 pb-2">
                  {/* 노드 이름 */}
                  <div
                    className="mb-1 font-mono text-xs font-bold"
                    style={{ color: color.text }}
                  >
                    {graphNode.name}
                  </div>
                  {/* 레이어 */}
                  <div
                    className="mb-2 font-mono text-xs uppercase"
                    style={{ color: `${color.text}66` }}
                  >
                    {graphNode.layer}
                  </div>
                  {/* 상세 정보 */}
                  {detailInfo && (
                    <div
                      className="space-y-0.5 border-t pt-2 font-mono text-xs"
                      style={{
                        borderColor: `${color.text}20`,
                        color: `${color.text}99`,
                      }}
                    >
                      <div className="flex justify-between">
                        <span>Calls:</span>
                        <span className="font-bold">
                          {detailInfo.callCount.toLocaleString()}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span>Errors:</span>
                        <span className="font-bold text-red-400">
                          {detailInfo.errorCount}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span>Avg:</span>
                        <span className="font-bold">
                          {detailInfo.averageResponseTime}ms
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ),
          },
          style: {
            background: color.bg,
            border: `2px solid ${isCenterNode ? '#ef4444' : color.border}`,
            borderRadius: '8px',
            padding: '0',
            width: 180,
            boxShadow: isCenterNode
              ? '0 4px 12px rgba(239, 68, 68, 0.4), 0 0 0 1px #ef444420'
              : `0 4px 12px ${color.shadow}, 0 0 0 1px ${color.border}20`,
          },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
        };
      });

      // 엣지 변환 (심플하고 깔끔하게)
      const flowEdges: Edge[] = graph.edges.map((edge, index) => ({
        id: `edge-${index}`,
        source: edge.source,
        target: edge.target,
        type: 'smoothstep',
        animated: true,
        style: {
          stroke: '#64748b',
          strokeWidth: 2,
        },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: '#64748b',
          width: 20,
          height: 20,
        },
      }));

      return { nodes: flowNodes, edges: flowEdges };
    },
    [flattenDependencyNodes],
  );

  // 노드의 depth 찾기 (재귀)
  const findNodeDepth = (
    nodeId: string,
    nodes: DependencyNode[],
    currentDepth = 0,
  ): number => {
    for (const node of nodes) {
      if (node.id === nodeId) {
        return currentDepth;
      }
      if (node.children && node.children.length > 0) {
        const depth = findNodeDepth(nodeId, node.children, currentDepth + 1);
        if (depth !== -1) {
          return depth;
        }
      }
    }
    return -1;
  };

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
      <div className="flex h-[600px] items-center justify-center rounded-lg border bg-white shadow-sm">
        <div className="text-gray-500">의존성 데이터를 불러오는 중...</div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex h-[600px] items-center justify-center rounded-lg border bg-white shadow-sm">
        <div className="text-gray-500">
          컴포넌트를 선택하여 의존성을 확인하세요.
        </div>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border border-gray-200 bg-white shadow-sm">
      {/* 헤더 */}
      <div className="flex items-center justify-between border-b border-gray-200 bg-slate-900 px-5 py-4">
        <div>
          <h2 className="font-mono text-base font-bold text-white">
            {data.component.name}
            <span className="ml-2 rounded bg-red-500/20 px-2 py-0.5 text-xs font-semibold text-red-400">
              Dependency Graph
            </span>
          </h2>
          <div className="mt-1 font-mono text-xs text-slate-400">
            {data.component.packageName}
          </div>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            className="rounded bg-slate-800 px-3 py-1.5 font-mono text-xs font-medium text-slate-300 transition-colors hover:bg-slate-700"
          >
            Close
          </button>
        )}
      </div>

      {/* ReactFlow 다이어그램 */}
      <div className="h-[500px] bg-slate-50">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          fitView
          attributionPosition="bottom-left"
        >
          <Background />
          <Controls />
          <MiniMap
            nodeColor={node => {
              if (node.id === data.component.id) {
                return '#ef4444';
              }
              const nodeData = data.graph.nodes.find(n => n.id === node.id);
              if (nodeData) {
                const colors = TYPE_COLORS[nodeData.type];
                return colors?.border || '#666';
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

      {/* 통계 요약 */}
      <div className="border-t border-gray-200 bg-slate-900 px-5 py-4">
        <div className="grid grid-cols-4 gap-4">
          <div className="rounded-lg border border-blue-500/30 bg-blue-500/10 p-3">
            <div className="font-mono text-xs font-semibold tracking-wider text-blue-400 uppercase">
              Upstream
            </div>
            <div className="mt-1 font-mono text-xl font-bold text-blue-300">
              {data.summary.upstreamCount}
            </div>
          </div>
          <div className="rounded-lg border border-green-500/30 bg-green-500/10 p-3">
            <div className="font-mono text-xs font-semibold tracking-wider text-green-400 uppercase">
              Downstream
            </div>
            <div className="mt-1 font-mono text-xl font-bold text-green-300">
              {data.summary.downstreamCount}
            </div>
          </div>
          <div className="rounded-lg border border-purple-500/30 bg-purple-500/10 p-3">
            <div className="font-mono text-xs font-semibold tracking-wider text-purple-400 uppercase">
              Total Calls
            </div>
            <div className="mt-1 font-mono text-xl font-bold text-purple-300">
              {data.summary.totalCalls.toLocaleString()}
            </div>
          </div>
          <div className="rounded-lg border border-red-500/30 bg-red-500/10 p-3">
            <div className="font-mono text-xs font-semibold tracking-wider text-red-400 uppercase">
              Error Rate
            </div>
            <div className="mt-1 font-mono text-xl font-bold text-red-300">
              {data.summary.overallErrorRate.toFixed(2)}%
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ComponentDependencyGraph;
