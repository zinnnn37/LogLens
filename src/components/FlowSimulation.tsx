// src/components/FlowSimulation.tsx
import { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';

interface FlowData {
  summary: {
    totalDuration: number;
    status: string;
    startTime: string;
    endTime: string;
  };
  timeline: {
    sequence: number;
    componentId: number;
    componentName: string;
    layer: 'CONTROLLER' | 'SERVICE' | 'REPOSITORY' | string;
    startTime: string;
    endTime: string;
    duration: number;
    logs: { logLevel: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR' | string }[];
  }[];
  components: { id: number; name: string; layer: string }[];
  graph: { edges: { from: number; to: number }[] };
}

interface Props {
  flowData: FlowData; // 호출부: flowData={response.data.data}
  height?: number;
  width?: number;
  autoPlay?: boolean;
  speed?: 0.5 | 1 | 2 | 4;
}

interface D3Node extends d3.SimulationNodeDatum {
  id: number;
  name: string;
  layer: string;
}
interface D3Link extends d3.SimulationLinkDatum<D3Node> {
  source: number | D3Node;
  target: number | D3Node;
}

const getLayerColor = (layer: string) => {
  switch (layer.toUpperCase()) {
    case 'CONTROLLER':
      return { glow: 'rgba(96,165,250,.22)' };
    case 'SERVICE':
      return { glow: 'rgba(52,211,153,.22)' };
    case 'REPOSITORY':
      return { glow: 'rgba(167,139,250,.22)' };
    default:
      return { glow: 'rgba(148,163,184,.18)' };
  }
};

const FlowSimulation = ({
  flowData,
  width = 1200,
  height = 680,
  autoPlay = true,
  speed = 1,
}: Props) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const [seq, setSeq] = useState(0);
  const [paused, setPaused] = useState(!autoPlay);

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

    // 노드
    const nodes: D3Node[] = components.map(c => ({
      id: c.id,
      name: c.name,
      layer: c.layer ?? 'UNKNOWN',
    }));
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
          .distance(190)
          .strength(0.12),
      )
      .force('charge', d3.forceManyBody().strength(-220))
      .force('collide', d3.forceCollide().radius(46))
      .force('x', d3.forceX<D3Node>(d => laneX(d.layer)).strength(0.22))
      .force('y', d3.forceY(height / 2).strength(0.06));

    // 레이어 배경 (아주 옅게)
    const lanes = [
      { x: width * 0.22, key: 'CONTROLLER' },
      { x: width * 0.5, key: 'SERVICE' },
      { x: width * 0.78, key: 'REPOSITORY' },
    ];
    g.append('g')
      .selectAll('rect')
      .data(lanes)
      .join('rect')
      .attr('x', d => d.x - 200)
      .attr('y', 40)
      .attr('width', 400)
      .attr('height', height - 80)
      .attr('fill', '#0f172a06');

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

    nodeSel
      .append('circle')
      .attr('r', 50)
      .attr('fill', d => getLayerColor(d.layer).glow)
      .attr('opacity', 0.18);

    nodeSel
      .append('circle')
      .attr('r', 42)
      .attr('fill', '#0b122012')
      .attr('stroke', '#64748b')
      .attr('stroke-width', 2.2)
      .attr('filter', 'url(#node-glow)');

    nodeSel
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('dy', '0.35em')
      .style('fontWeight', 600)
      .style('fontSize', '11px')
      .style('fill', '#0f172a')
      .text(d => d.name);

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
      linkSel.attr('d', (l: any) =>
        qcurve(l.source.x, l.source.y, l.target.x, l.target.y),
      );
      nodeSel.attr('transform', d => `translate(${d.x},${d.y})`);
    });

    // 저장: 애니메이션에서 사용
    (svg.node() as unknown as Record<string, unknown>).__linkSel__ = linkSel;

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

    // 현재 노드만 살짝 강조(outer glow만)
    const cur = timeline[seq];
    svg.selectAll('.nodes g circle:first-of-type').attr('opacity', 0.18);
    svg
      .selectAll('.nodes g')
      .filter((d: any) => d.id === cur.componentId)
      .select('circle:first-of-type')
      .transition()
      .duration(180 / speed)
      .attr('opacity', 0.32);

    // 링크 selection
    const linkSel: d3.Selection<SVGPathElement, any, any, any> = (
      svg.node() as unknown as Record<string, unknown>
    ).__linkSel__ as d3.Selection<SVGPathElement, any, any, any>;

    // 한 스텝 ahead가 있다면 해당 간선 위로 particle 이동
    if (seq < timeline.length - 1) {
      const nxt = timeline[seq + 1];

      // 무방향 매칭: (a,b) 또는 (b,a)
      const edgeSel = linkSel.filter((d: any) => {
        const sid = typeof d.source === 'object' ? d.source.id : d.source;
        const tid = typeof d.target === 'object' ? d.target.id : d.target;
        return (
          (sid === cur.componentId && tid === nxt.componentId) ||
          (sid === nxt.componentId && tid === cur.componentId)
        );
      });

      // particle(회색 라인 그대로 유지, 점만 이동)
      const shootParticle = (
        edge: SVGPathElement,
        forward: boolean,
        ms = 900,
      ) => {
        const L = edge.getTotalLength();
        const p = d3
          .select(edge.parentNode as SVGGElement)
          .append('circle')
          .attr('r', 3)
          .attr('fill', '#0ea5e9') // 파란 점 (시인성)
          .attr('opacity', 0.95)
          .style('filter', 'drop-shadow(0 0 6px rgba(14,165,233,.9))');

        const t0 = performance.now();
        const loop = (now: number) => {
          if (paused) {
            requestAnimationFrame(loop);
            return;
          }
          const t = Math.min(1, (now - t0) / (ms / speed));
          const len = forward ? t * L : (1 - t) * L; // 방향 제어
          const pt = edge.getPointAtLength(len);
          p.attr('cx', pt.x).attr('cy', pt.y);
          if (t < 1) {
            requestAnimationFrame(loop);
          } else {
            p.remove();
          }
        };
        requestAnimationFrame(loop);
      };

      edgeSel.nodes().forEach((edge: any) => {
        // 방향 판단: 저장된 path는 (minId -> maxId)
        const sid = (edge.__data__.source as any).id ?? edge.__data__.source;
        const tid = (edge.__data__.target as any).id ?? edge.__data__.target;
        const forward =
          sid <= tid
            ? cur.componentId <= nxt.componentId
            : cur.componentId >= nxt.componentId;

        // 여러 발 쏴서 흐름감↑
        setTimeout(
          () => shootParticle(edge as SVGPathElement, forward, 900),
          30,
        );
        setTimeout(
          () => shootParticle(edge as SVGPathElement, forward, 900),
          140,
        );
        setTimeout(
          () => shootParticle(edge as SVGPathElement, forward, 900),
          250,
        );
      });
    }

    const timer = setTimeout(() => {
      if (!paused) {
        setSeq(s => s + 1);
      }
    }, 1100 / speed);

    return () => {
      clearTimeout(timer);
    };
  }, [seq, flowData, paused, speed]);

  // -----------------------------
  // 3) 렌더 (데이터 없을 때만 분기)
  // -----------------------------
  if (!flowData?.timeline?.length || !flowData?.components?.length) {
    return (
      <div className="rounded-2xl border bg-white p-8 text-center text-gray-500">
        흐름 데이터를 불러오지 못했습니다.
      </div>
    );
  }

  const reset = () => setSeq(0);
  const toggle = () => setPaused(p => !p);

  return (
    <div className="rounded-2xl border bg-white shadow">
      <div className="flex items-center justify-between border-b bg-slate-950 px-4 py-3">
        <div className="text-white">
          <div className="text-base font-semibold">요청 흐름 시뮬레이션</div>
          <div className="text-xs text-slate-300">
            Step {Math.min(seq + 1, flowData.timeline.length)} /{' '}
            {flowData.timeline.length}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={toggle}
            className="rounded-md border border-slate-600 bg-slate-800 px-3 py-1.5 text-sm text-white"
          >
            {paused ? '재생' : '일시정지'}
          </button>
          <button
            onClick={reset}
            className="rounded-md border border-slate-600 bg-slate-800 px-3 py-1.5 text-sm text-white"
          >
            다시보기
          </button>
        </div>
      </div>
      <div className="relative h-[680px] bg-gradient-to-b from-slate-50 to-indigo-50/30">
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
