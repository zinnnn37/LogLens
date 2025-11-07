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


  // BFS로 노드의 레벨 계산 (선택된 컴포넌트 기준)
  const calculateNodeLevels = useCallback((
    edges: { source: string; target: string }[],
    centerNodeId: string
  ) => {
    const levels = new Map<string, number>();
    const visited = new Set<string>();

    // 중심 노드는 레벨 0
    levels.set(centerNodeId, 0);
    visited.add(centerNodeId);

    // BFS 큐: [nodeId, level]
    const queue: [string, number][] = [[centerNodeId, 0]];

    while (queue.length > 0) {
      const [currentNode, currentLevel] = queue.shift()!;

      // downstream 탐색: 현재 노드에서 나가는 엣지 (오른쪽으로)
      edges.forEach(edge => {
        if (edge.source === currentNode && !visited.has(edge.target)) {
          visited.add(edge.target);
          levels.set(edge.target, currentLevel + 1);
          queue.push([edge.target, currentLevel + 1]);
        }
      });

      // upstream 탐색: 현재 노드로 들어오는 엣지 (왼쪽으로)
      edges.forEach(edge => {
        if (edge.target === currentNode && !visited.has(edge.source)) {
          visited.add(edge.source);
          levels.set(edge.source, currentLevel - 1);
          queue.push([edge.source, currentLevel - 1]);
        }
      });
    }

    return levels;
  }, []);

  // 그래프 데이터를 ReactFlow 노드/엣지로 변환
  const convertToFlowElements = useCallback(
    (componentData: ComponentDependencyData) => {
      const { graph, component } = componentData;

      // 노드 레벨 계산 (BFS로 연결된 노드만 찾음)
      const nodeLevels = calculateNodeLevels(graph.edges, component.id);

      // 레벨이 계산된 노드만 = 실제로 연결된 노드만
      const connectedNodes = graph.nodes.filter(node =>
        nodeLevels.has(node.id)
      );

      // 레벨별로 노드 그룹화
      const nodesByLevel = new Map<number, typeof connectedNodes>();
      connectedNodes.forEach(node => {
        const level = nodeLevels.get(node.id)!;
        if (!nodesByLevel.has(level)) {
          nodesByLevel.set(level, []);
        }
        nodesByLevel.get(level)!.push(node);
      });

      console.log('=== 그래프 디버깅 ===');
      console.log('전체 노드 수:', graph.nodes.length);
      console.log('연결된 노드 수:', connectedNodes.length);
      console.log('중심 컴포넌트:', component.name, `(ID: ${component.id})`);
      console.log('노드별 레벨:');
      connectedNodes.forEach(node => {
        const level = nodeLevels.get(node.id);
        const direction = level === 0 ? '중심' : level! < 0 ? `← upstream (${Math.abs(level!)})` : `downstream (${level}) →`;
        console.log(`  - ${node.name}: ${direction}`);
      });

      // 레이아웃 설정
      const centerX = 600;
      const centerY = 300;
      const horizontalGap = 350;
      const verticalGap = 180;

      const flowNodes: Node[] = connectedNodes.map(graphNode => {
        const color = TYPE_COLORS[graphNode.type] || TYPE_COLORS.BACKEND;
        const isCenterNode = graphNode.id === component.id;

        // 레벨 기반 위치 계산
        const level = nodeLevels.get(graphNode.id) ?? 0;
        const nodesAtLevel = nodesByLevel.get(level) || [];
        const indexAtLevel = nodesAtLevel.findIndex(n => n.id === graphNode.id);
        const totalAtLevel = nodesAtLevel.length;

        const xPosition = centerX + level * horizontalGap;
        const yPosition = centerY - (totalAtLevel - 1) * verticalGap / 2 + indexAtLevel * verticalGap;

        // 상세 정보는 일단 null (나중에 개선 가능)
        const detailInfo = {
          callCount: 0,
          errorCount: 0,
          averageResponseTime: 0,
        };

        return {
          id: graphNode.id,
          type: 'default',
          position: { x: xPosition, y: yPosition },
          data: {
            label: (
              <div className="relative">
                {/* 상단 액센트 바 */}
                <div
                  className="absolute top-0 right-0 left-0 h-1.5 rounded-t-lg"
                  style={{
                    background: isCenterNode
                      ? 'linear-gradient(90deg, #ef4444, #dc2626)'
                      : `linear-gradient(90deg, ${color.accent}, ${color.border})`,
                  }}
                />
                <div className="px-4 pt-4 pb-3">
                  {/* 노드 이름 */}
                  <div
                    className="mb-1.5 font-semibold text-sm leading-tight"
                    style={{ color: color.text }}
                  >
                    {graphNode.name}
                  </div>
                  {/* 레이어 배지 */}
                  <div className="mb-3">
                    <span
                      className="inline-block rounded-full px-2.5 py-0.5 text-xs font-medium uppercase tracking-wide"
                      style={{
                        backgroundColor: `${color.accent}20`,
                        color: color.accent,
                      }}
                    >
                      {graphNode.layer}
                    </span>
                  </div>
                  {/* 상세 정보 */}
                  {detailInfo && (
                    <div
                      className="space-y-1.5 border-t pt-2.5 text-xs"
                      style={{
                        borderColor: `${color.text}15`,
                        color: `${color.text}cc`,
                      }}
                    >
                      <div className="flex items-center justify-between">
                        <span className="flex items-center gap-1.5">
                          <span className="h-1.5 w-1.5 rounded-full bg-blue-400"></span>
                          Calls
                        </span>
                        <span className="font-semibold tabular-nums">
                          {detailInfo.callCount.toLocaleString()}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="flex items-center gap-1.5">
                          <span className="h-1.5 w-1.5 rounded-full bg-red-400"></span>
                          Errors
                        </span>
                        <span className="font-semibold tabular-nums text-red-400">
                          {detailInfo.errorCount}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="flex items-center gap-1.5">
                          <span className="h-1.5 w-1.5 rounded-full bg-green-400"></span>
                          Avg
                        </span>
                        <span className="font-semibold tabular-nums">
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
            background: isCenterNode
              ? 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)'
              : color.bg,
            border: isCenterNode
              ? '3px solid #ef4444'
              : `2px solid ${color.border}`,
            borderRadius: '12px',
            padding: '0',
            width: 220,
            boxShadow: isCenterNode
              ? '0 8px 24px rgba(239, 68, 68, 0.35), 0 0 0 1px rgba(239, 68, 68, 0.2)'
              : `0 4px 16px ${color.shadow}, 0 0 0 1px ${color.border}15`,
          },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
        };
      });

      // 연결된 노드 사이의 엣지만 필터링
      const connectedNodeIdSet = new Set(connectedNodes.map(n => n.id));
      const filteredEdges = graph.edges.filter(edge =>
        connectedNodeIdSet.has(edge.source) && connectedNodeIdSet.has(edge.target)
      );

      // 엣지 변환
      const flowEdges: Edge[] = filteredEdges.map((edge, index) => {
        // 중심 노드와 직접 연결된 엣지인지 확인
        const isDirectEdge = edge.source === component.id || edge.target === component.id;

        return {
          id: `edge-${index}`,
          source: edge.source,
          target: edge.target,
          type: 'smoothstep',
          animated: isDirectEdge, // 중심 노드와 직접 연결된 엣지만 애니메이션
          style: {
            stroke: isDirectEdge ? '#3b82f6' : '#94a3b8', // 파란색 vs 회색
            strokeWidth: isDirectEdge ? 3 : 2,
          },
          markerEnd: {
            type: MarkerType.ArrowClosed,
            color: isDirectEdge ? '#3b82f6' : '#94a3b8',
            width: 20,
            height: 20,
          },
        };
      });

      return { nodes: flowNodes, edges: flowEdges };
    },
    [calculateNodeLevels],
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
      <div className="h-[600px] bg-gradient-to-br from-slate-50 to-slate-100">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          fitView
          fitViewOptions={{ padding: 0.2 }}
          attributionPosition="bottom-left"
          minZoom={0.1}
          maxZoom={1.5}
        >
          <Background gap={16} size={1} color="#cbd5e1" />
          <Controls
            className="rounded-lg border border-gray-300 bg-white shadow-lg"
            showInteractive={false}
          />
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
            className="rounded-lg border-2 border-gray-300 bg-white shadow-lg"
            style={{
              width: 140,
              height: 100,
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
