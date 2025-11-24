// src/components/FlowSimulation.tsx
import { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';
import type { TraceFlowResponse } from '@/types/log';

type FlowData = TraceFlowResponse;

interface Props {
  flowData: FlowData; // 호출부: flowData={response.data.data}
  height?: number;
  width?: number;
  speed?: 0.5 | 1 | 2 | 4;
  onClose?: () => void;
  onSeqChange?: (seq: number) => void; // 시퀀스 변경 콜백
}

interface D3Node extends d3.SimulationNodeDatum {
  id: number;
  name: string;
  layer: string;
  totalDuration: number;
  callCount: number;
  errorCount: number;
  warnCount: number;
  infoCount: number;
}
interface D3Link extends d3.SimulationLinkDatum<D3Node> {
  source: number | D3Node;
  target: number | D3Node;
}

// SVG 노드에 저장할 데이터 타입
interface SVGNodeWithData extends SVGSVGElement {
  __linkSel__?: d3.Selection<
    SVGPathElement | d3.BaseType,
    D3Link,
    SVGGElement,
    unknown
  >;
}

// Edge 데이터 타입
interface EdgeData {
  source: D3Node;
  target: D3Node;
}

// Edge 엘리먼트 타입
interface EdgeElement extends SVGPathElement {
  __data__: EdgeData;
}

const getLayerColor = (layer: string) => {
  switch (layer.toUpperCase()) {
    case 'CONTROLLER':
      return {
        glow: 'rgba(96,165,250,.22)',
        border: '#3b82f6',
        accent: '#3b82f6',
        bg: '#1e293b',
        shadow: 'rgba(59, 130, 246, 0.3)',
      };
    case 'SERVICE':
      return {
        glow: 'rgba(52,211,153,.22)',
        border: '#10b981',
        accent: '#10b981',
        bg: '#1e293b',
        shadow: 'rgba(16, 185, 129, 0.3)',
      };
    case 'REPOSITORY':
      return {
        glow: 'rgba(167,139,250,.22)',
        border: '#a855f7',
        accent: '#a855f7',
        bg: '#1e293b',
        shadow: 'rgba(168, 85, 247, 0.3)',
      };
    default:
      return {
        glow: 'rgba(148,163,184,.18)',
        border: '#94a3b8',
        accent: '#94a3b8',
        bg: '#1e293b',
        shadow: 'rgba(148, 163, 184, 0.3)',
      };
  }
};

const getLogLevelColor = (logLevel: string) => {
  switch (logLevel.toUpperCase()) {
    case 'ERROR':
      return '#ef4444'; // red
    case 'WARN':
      return '#f59e0b'; // amber
    case 'INFO':
      return '#3b82f6'; // blue
    default:
      return '#3b82f6'; // default blue
  }
};

const FlowSimulation = ({
  flowData,
  width = 2400,
  height = 800,
  speed = 1,
  onClose,
  onSeqChange,
}: Props) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const [seq, setSeq] = useState(0); // 0부터 시작 (timeline 인덱스)

  // seq가 변경될 때마다 부모에게 알림
  useEffect(() => {
    onSeqChange?.(seq);
  }, [seq, onSeqChange]);

  // 다음/이전 단계로 이동
  const nextStep = () => {
    if (seq < flowData.timeline.length - 1) {
      setSeq(s => s + 1);
    } else {
      // 마지막 단계에서 다음 버튼 누르면 처음으로
      setSeq(0);
    }
  };

  const prevStep = () => {
    if (seq > 0) {
      setSeq(s => s - 1);
    }
  };

  // 키보드 이벤트 (스페이스바: 다음, 백스페이스/왼쪽 화살표: 이전)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === 'Space') {
        e.preventDefault();
        setSeq(s => {
          if (s < flowData.timeline.length - 1) {
            return s + 1;
          }
          // 마지막 단계에서 스페이스바 누르면 처음으로
          return 0;
        });
      } else if (e.code === 'ArrowLeft' || e.code === 'Backspace') {
        e.preventDefault();
        setSeq(s => {
          if (s > 0) {
            return s - 1;
          }
          return s;
        });
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        setSeq(s => {
          if (s < flowData.timeline.length - 1) {
            return s + 1;
          }
          // 마지막 단계에서 오른쪽 화살표 누르면 처음으로
          return 0;
        });
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [flowData.timeline.length]);

  // -----------------------------
  // 1) 초기 그래프 구축 (Hook은 최상단, 내부에서만 가드)
  // -----------------------------
  useEffect(() => {
    if (!svgRef.current) {
      return;
    }
    if (!flowData?.components?.length || !flowData?.timeline?.length) {
      return;
    }

    const { components, timeline, graph } = flowData;

    const svg = d3.select(svgRef.current);
    svg.selectAll('*').remove();

    // glow filter (노드만 살짝)
    const defs = svg.append('defs');
    const glow = defs
      .append('filter')
      .attr('id', 'node-glow')
      .attr('x', '-50%')
      .attr('y', '-50%')
      .attr('width', '200%')
      .attr('height', '200%');
    glow
      .append('feGaussianBlur')
      .attr('stdDeviation', '7')
      .attr('result', 'blur');
    const merge = glow.append('feMerge');
    merge.append('feMergeNode').attr('in', 'blur');
    merge.append('feMergeNode').attr('in', 'SourceGraphic');

    // 줌/팬
    const g = svg.append('g');
    svg.call(
      d3
        .zoom<SVGSVGElement, unknown>()
        .scaleExtent([0.3, 3])
        .on('zoom', ev => {
          g.attr('transform', ev.transform);
        }),
    );

    // 노드 - timeline 데이터 집계
    const nodeMetrics = new Map<
      number,
      {
        totalDuration: number;
        callCount: number;
        errorCount: number;
        warnCount: number;
        infoCount: number;
      }
    >();

    // timeline에서 각 컴포넌트별 메트릭 계산
    timeline.forEach(t => {
      const existing = nodeMetrics.get(t.componentId) || {
        totalDuration: 0,
        callCount: 0,
        errorCount: 0,
        warnCount: 0,
        infoCount: 0,
      };

      existing.totalDuration += t.duration;
      existing.callCount += 1;

      t.logs.forEach(log => {
        if (log.logLevel === 'ERROR') {
          existing.errorCount += 1;
        } else if (log.logLevel === 'WARN') {
          existing.warnCount += 1;
        } else if (log.logLevel === 'INFO') {
          existing.infoCount += 1;
        }
      });

      nodeMetrics.set(t.componentId, existing);
    });

    // timeline에 실제로 등장한 컴포넌트만 표시
    const timelineComponentIds = new Set(timeline.map(t => t.componentId));
    const nodes: D3Node[] = components
      .filter(c => timelineComponentIds.has(c.id))
      .map(c => {
        const metrics = nodeMetrics.get(c.id) || {
          totalDuration: 0,
          callCount: 0,
          errorCount: 0,
          warnCount: 0,
          infoCount: 0,
        };
        return {
          id: c.id,
          name: c.name,
          layer: c.layer ?? 'UNKNOWN',
          ...metrics,
        };
      })
      .filter(n => n.name && n.name.trim().length > 0); // 이름이 없는 노드 제거

    const nodeIds = new Set(nodes.map(n => n.id));

    // 무방향 키
    const pairKey = (a: number, b: number) =>
      `${Math.min(a, b)}-${Math.max(a, b)}`;

    // base 간선 (유효 id만, 무방향 dedupe)
    const undirected = new Map<string, { a: number; b: number }>();
    graph.edges.forEach(e => {
      if (!nodeIds.has(e.from) || !nodeIds.has(e.to)) {
        return;
      }
      undirected.set(pairKey(e.from, e.to), {
        a: Math.min(e.from, e.to),
        b: Math.max(e.from, e.to),
      });
    });

    // timeline 간선도 같은 무방향 세트에 합치기(중복 방지)
    for (let i = 0; i < timeline.length - 1; i += 1) {
      const a = timeline[i].componentId;
      const b = timeline[i + 1].componentId;
      if (!nodeIds.has(a) || !nodeIds.has(b)) {
        continue;
      }
      undirected.set(pairKey(a, b), { a: Math.min(a, b), b: Math.max(a, b) });
    }

    // 최종 렌더링용 링크(유향처럼 보이지만 실제로는 한 쌍당 1개 경로)
    const links: D3Link[] = Array.from(undirected.values()).map(p => ({
      // forceLink는 source/target이 필요하므로 정렬된 한 쪽을 source로 고정
      source: p.a,
      target: p.b,
    }));

    // 포스
    const laneX = (layer: string) =>
      layer.toUpperCase() === 'CONTROLLER'
        ? width * 0.22
        : layer.toUpperCase() === 'SERVICE'
          ? width * 0.5
          : width * 0.78;

    const sim = d3
      .forceSimulation(nodes)
      .force(
        'link',
        d3
          .forceLink<D3Node, D3Link>(links)
          .id(d => d.id)
          .distance(350)
          .strength(0.08),
      )
      .force('charge', d3.forceManyBody().strength(-350))
      .force('collide', d3.forceCollide().radius(130))
      .force('x', d3.forceX<D3Node>(d => laneX(d.layer)).strength(0.22))
      .force('y', d3.forceY(height / 2).strength(0.06));

    // 링크 (요청: 모든 간선은 얇은 회색 기본선, 화살표/강조 없음)
    const linkG = g.append('g').attr('class', 'links');
    const linkSel = linkG
      .selectAll('path')
      .data(links)
      .join('path')
      .attr('fill', 'none')
      .attr('stroke', '#cbd5e1') // 기본 회색
      .attr('stroke-opacity', 0.55) // 살짝 투명
      .attr('stroke-width', 1.6) // 얇게
      .attr('stroke-linecap', 'round');

    // 노드
    const nodeG = g.append('g').attr('class', 'nodes');
    const nodeSel = nodeG
      .selectAll<SVGGElement, D3Node>('g')
      .data(nodes)
      .join('g')
      .style('cursor', 'pointer');

    const minNodeWidth = 180;
    const nodeHeight = 100;
    const borderRadius = 10;
    const horizontalPadding = 20;

    // 컴포넌트 이름 텍스트 (상단)
    const textSel = nodeSel
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', '-22')
      .style('fontWeight', 600)
      .style('fontSize', '13px')
      .style('fill', '#f1f5f9')
      .text(d => d.name);

    // 각 노드의 텍스트 너비를 측정하여 저장
    const nodeWidths = new Map<number, number>();
    textSel.each(function (d) {
      const textWidth = (this as SVGTextElement).getComputedTextLength();
      const calculatedWidth = Math.max(
        minNodeWidth,
        textWidth + horizontalPadding * 2,
      );
      nodeWidths.set(d.id, calculatedWidth);
    });

    // 외곽 glow (사각형) - 동적 너비
    nodeSel
      .insert('rect', 'text')
      .attr('class', 'glow-rect')
      .attr('x', d => -(nodeWidths.get(d.id) ?? minNodeWidth) / 2 - 4)
      .attr('y', -nodeHeight / 2 - 4)
      .attr('width', d => (nodeWidths.get(d.id) ?? minNodeWidth) + 8)
      .attr('height', nodeHeight + 8)
      .attr('rx', borderRadius + 2)
      .attr('fill', d => getLayerColor(d.layer).glow)
      .attr('opacity', 0.18);

    // 메인 사각형 배경 (다크 테마) - 동적 너비
    nodeSel
      .insert('rect', 'text')
      .attr('class', 'main-rect')
      .attr('x', d => -(nodeWidths.get(d.id) ?? minNodeWidth) / 2)
      .attr('y', -nodeHeight / 2)
      .attr('width', d => nodeWidths.get(d.id) ?? minNodeWidth)
      .attr('height', nodeHeight)
      .attr('rx', borderRadius)
      .attr('fill', d => getLayerColor(d.layer).bg)
      .attr('stroke', d => getLayerColor(d.layer).border)
      .attr('stroke-width', 2.5)
      .attr('filter', 'url(#node-glow)');

    // Layer 배지 (컴포넌트 이름과 적당한 간격)
    nodeSel
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', '-4')
      .style('fontSize', '10px')
      .style('fontWeight', 500)
      .style('fill', d => getLayerColor(d.layer).border)
      .style('opacity', 0.8)
      .text(d => d.layer);

    // 메트릭 정보 (Duration, Calls) - Layer와 큰 간격
    nodeSel
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', '16')
      .style('fontSize', '9px')
      .style('fill', '#94a3b8')
      .text(d => `${d.totalDuration}ms · ${d.callCount} calls`);

    // 로그 레벨 카운트 (에러/경고만 표시)
    nodeSel
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', '32')
      .style('fontSize', '9px')
      .style('fill', '#94a3b8')
      .text(d => {
        const parts = [];
        if (d.errorCount > 0) {
          parts.push(`❌ ${d.errorCount}`);
        }
        if (d.warnCount > 0) {
          parts.push(`⚠️ ${d.warnCount}`);
        }
        if (parts.length === 0) {
          return '✓ No issues';
        }
        return parts.join(' ');
      });

    // 드래그
    const drag = d3
      .drag<SVGGElement, D3Node>()
      .on('start', ev => {
        if (!ev.active) {
          sim.alphaTarget(0.3).restart();
        }
        const n = ev.subject;
        n.fx = n.x ?? 0;
        n.fy = n.y ?? 0;
      })
      .on('drag', ev => {
        const n = ev.subject;
        n.fx = ev.x;
        n.fy = ev.y;
      })
      .on('end', ev => {
        if (!ev.active) {
          sim.alphaTarget(0);
        }
        const n = ev.subject;
        n.fx = undefined;
        n.fy = undefined;
      });
    nodeSel.call(drag);

    // 곡선 경로(살짝만 휘어짐)
    const qcurve = (sx: number, sy: number, tx: number, ty: number) => {
      const dx = tx - sx;
      const dy = ty - sy;
      const c = 0.12;
      const mx = (sx + tx) / 2 - dy * c;
      const my = (sy + ty) / 2 + dx * c;
      return `M${sx},${sy} Q${mx},${my} ${tx},${ty}`;
    };

    sim.on('tick', () => {
      linkSel.attr('d', (l: D3Link) => {
        const source = l.source as D3Node;
        const target = l.target as D3Node;
        return qcurve(
          source.x ?? 0,
          source.y ?? 0,
          target.x ?? 0,
          target.y ?? 0,
        );
      });
      nodeSel.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    // 저장: 애니메이션에서 사용
    (svg.node() as SVGNodeWithData).__linkSel__ = linkSel;

    return () => {
      sim.stop();
    };
  }, [flowData, width, height]);

  // -----------------------------
  // 2) 타임라인 애니메이션 (한 선에서 양방향 particle)
  // -----------------------------
  useEffect(() => {
    const svg = svgRef.current ? d3.select(svgRef.current) : null;
    if (!svg || !flowData?.timeline?.length) {
      return;
    }

    const { timeline } = flowData;
    if (seq >= timeline.length) {
      return;
    }

    // 현재 노드 강조
    const cur = timeline[seq];

    // 모든 노드를 기본 상태로
    svg.selectAll('.nodes g').attr('opacity', 1);
    svg.selectAll('.nodes g .glow-rect').attr('opacity', 0.18);

    // 모든 노드 배경을 두 톤 밝게 (비활성화 느낌)
    svg
      .selectAll<SVGGElement, D3Node>('.nodes g .main-rect')
      .attr('fill', '#475569'); // #1e293b에서 두 톤 밝게

    // 현재 노드만 강한 glow + 원래 어두운 배경으로
    const currentNode = svg
      .selectAll<SVGGElement, D3Node>('.nodes g')
      .filter(d => d.id === cur.componentId);

    currentNode
      .select('.glow-rect')
      .transition()
      .duration(180 / speed)
      .attr('opacity', 0.8);

    // 현재 노드 배경을 원래 어두운 색으로 (활성화 느낌)
    currentNode
      .select('.main-rect')
      .transition()
      .duration(180 / speed)
      .attr('fill', '#1e293b'); // 원래 기본 색상

    // 링크 selection
    const linkSel = (svg.node() as SVGNodeWithData).__linkSel__;
    if (!linkSel) {
      return;
    }

    // seq > 0이면 이전 단계에서 현재 단계로 오는 particle 표시
    if (seq > 0) {
      const prev = timeline[seq - 1];

      // 무방향 매칭: (a,b) 또는 (b,a)
      const edgeSel = linkSel.filter((d: D3Link) => {
        const source = d.source as D3Node;
        const target = d.target as D3Node;
        const sid = typeof source === 'object' ? source.id : source;
        const tid = typeof target === 'object' ? target.id : target;
        return (
          (sid === prev.componentId && tid === cur.componentId) ||
          (sid === cur.componentId && tid === prev.componentId)
        );
      });

      // 현재 스텝의 로그 레벨 결정 (가장 심각한 레벨 선택)
      const getStepLogLevel = () => {
        if (!cur.logs || cur.logs.length === 0) {
          return 'INFO';
        }
        const hasError = cur.logs.some(log => log.logLevel === 'ERROR');
        const hasWarn = cur.logs.some(log => log.logLevel === 'WARN');
        if (hasError) {
          return 'ERROR';
        }
        if (hasWarn) {
          return 'WARN';
        }
        return 'INFO';
      };

      const logLevel = getStepLogLevel();
      const particleColor = getLogLevelColor(logLevel);

      // 간단한 사각형 particle
      const shootParticle = (
        edge: SVGPathElement,
        forward: boolean,
        color: string,
        ms = 900,
      ) => {
        const L = edge.getTotalLength();

        // path가 비어있거나 길이가 0이면 particle 생성하지 않음
        if (!L || L === 0) {
          return;
        }

        // 원형 particle 생성
        const particle = d3
          .select(edge.parentNode as SVGGElement)
          .append('circle')
          .attr('r', 16)
          .attr('fill', color)
          .attr('stroke', 'white')
          .attr('stroke-width', 2)
          .attr('opacity', 0.95)
          .style('filter', `drop-shadow(0 0 14px ${color})`);

        const t0 = performance.now();
        const loop = (now: number) => {
          const t = Math.min(1, (now - t0) / (ms / speed));
          const len = forward ? t * L : (1 - t) * L;
          const pt = edge.getPointAtLength(Math.max(0, Math.min(L, len)));

          // 원형 위치 업데이트 (회전 불필요)
          particle.attr('cx', pt.x).attr('cy', pt.y);

          if (t < 1) {
            requestAnimationFrame(loop);
          } else {
            particle.remove();
          }
        };
        requestAnimationFrame(loop);
      };

      edgeSel.nodes().forEach(edge => {
        if (!edge || !(edge instanceof SVGPathElement)) {
          return;
        }
        const edgeElement = edge as EdgeElement;
        // 방향 판단: 이전 단계에서 현재 단계로
        const source = edgeElement.__data__.source;
        const target = edgeElement.__data__.target;
        const sid = source.id;
        const tid = target.id;
        const forward =
          sid <= tid
            ? prev.componentId < cur.componentId
            : prev.componentId > cur.componentId;

        // 1개의 particle만 발사 (로그 레벨에 따른 색상)
        shootParticle(edgeElement, forward, particleColor, 900);
      });
    }
  }, [seq, flowData, speed]);

  // -----------------------------
  // 3) 렌더 (데이터 없을 때만 분기)
  // -----------------------------
  if (!flowData?.timeline?.length || !flowData?.components?.length) {
    return (
      <div className="rounded-2xl border bg-white p-8 text-center text-gray-500">
        흐름 데이터가 존재하지 않습니다.
      </div>
    );
  }

  const reset = () => setSeq(0);

  return (
    <div className="overflow-hidden rounded-2xl border bg-white shadow">
      <div className="flex items-center justify-between border-b bg-gray-50 px-4 py-3">
        <div className="text-primary">
          <div className="text-base font-semibold">요청 흐름 시뮬레이션</div>
          <div className="text-xs text-gray-600">
            {seq < flowData.timeline.length ? (
              <>
                단계 {seq + 1} / {flowData.timeline.length} →{' '}
                {flowData.timeline[seq].componentName}
              </>
            ) : (
              <>완료</>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={reset}
            className="bg-secondary flex h-9 w-9 items-center justify-center rounded-md border border-white text-white hover:opacity-90"
            title="처음으로"
          >
            <svg
              className="h-5 w-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
              />
            </svg>
          </button>
          <button
            onClick={prevStep}
            disabled={seq === 0}
            className="bg-secondary flex h-9 w-9 items-center justify-center rounded-md border border-white text-white hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            title="이전 단계 (← 또는 Backspace)"
          >
            <svg
              className="h-5 w-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 19l-7-7 7-7"
              />
            </svg>
          </button>
          <button
            onClick={nextStep}
            className="bg-secondary flex h-9 w-9 items-center justify-center rounded-md border border-white text-white hover:opacity-90"
            title="다음 단계 (→ 또는 Space) - 마지막에서 누르면 처음으로"
          >
            <svg
              className="h-5 w-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 5l7 7-7 7"
              />
            </svg>
          </button>
          {onClose && (
            <button
              onClick={onClose}
              className="ml-2 rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
            >
              닫기
            </button>
          )}
        </div>
      </div>
      <div className="relative h-[700px] bg-white">
        <svg
          ref={svgRef}
          viewBox={`0 0 ${width} ${height}`}
          className="h-full w-full"
        />
      </div>
    </div>
  );
};

export default FlowSimulation;
