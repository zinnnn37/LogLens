// src/components/TrafficGraph.tsx
import React, { useMemo, useState } from 'react';
import { ResponsiveLine } from '@nivo/line';
import type { TrafficDataPoint } from '@/types/log';

interface TrafficGraphProps {
  dataPoints: TrafficDataPoint[];
}

type SeriesKey = 'FE' | 'BE';

const chartTheme = {
  text: {
    fontSize: 11,
    fill: '#0f172a',
  },
  axis: {
    domain: {
      line: {
        stroke: '#e5e7eb',
        strokeWidth: 1,
      },
    },
    ticks: {
      line: {
        stroke: '#e5e7eb',
        strokeWidth: 1,
      },
      text: {
        fontSize: 11,
        fill: '#6b7280',
      },
    },
    legend: {
      text: {
        fontSize: 11,
        fill: '#4b5563',
      },
    },
  },
  grid: {
    line: {
      stroke: '#f3f4f6',
      strokeWidth: 1,
    },
  },
  tooltip: {
    container: {
      background: 'white',
      color: '#0f172a',
      fontSize: 11,
      borderRadius: 6,
      boxShadow: '0 10px 15px -3px rgb(15 23 42 / 0.15)',
      padding: '6px 8px',
    },
  },
  legends: {
    text: {
      fontSize: 11,
      fill: '#4b5563',
    },
  },
} as const;

const TrafficGraph: React.FC<TrafficGraphProps> = ({ dataPoints }) => {
  // FE / BE 표시 여부
  const [visible, setVisible] = useState<Record<SeriesKey, boolean>>({
    FE: true,
    BE: true,
  });

  const series = useMemo(() => {
    const base = [
      {
        id: 'FE' as const,
        data: dataPoints.map(p => ({ x: p.hour, y: p.feCount })),
      },
      {
        id: 'BE' as const,
        data: dataPoints.map(p => ({ x: p.hour, y: p.beCount })),
      },
    ];

    return base.filter(s => visible[s.id]);
  }, [dataPoints, visible]);

  const handleToggle = (key: SeriesKey) => {
    setVisible(prev => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  return (
    <div className="flex h-64 w-full flex-col">
      {/* 상단 토글 영역 */}
      <div className="mb-2 flex items-center gap-4 text-xs text-slate-600">
        <label className="flex cursor-pointer items-center gap-1">
          <input
            type="checkbox"
            className="h-3 w-3 rounded border-slate-300"
            checked={visible.FE}
            onChange={() => handleToggle('FE')}
          />
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-sky-500" />
            <span>FE</span>
          </span>
        </label>

        <label className="flex cursor-pointer items-center gap-1">
          <input
            type="checkbox"
            className="h-3 w-3 rounded border-slate-300"
            checked={visible.BE}
            onChange={() => handleToggle('BE')}
          />
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-orange-500" />
            <span>BE</span>
          </span>
        </label>
      </div>

      {/* 그래프 영역 */}
      <div className="flex-1">
        <ResponsiveLine
          data={series}
          margin={{ top: 20, right: 24, bottom: 40, left: 50 }}
          xScale={{ type: 'point' }}
          yScale={{
            type: 'linear',
            min: 0,
            max: 'auto',
            stacked: false,
            reverse: false,
          }}
          axisBottom={{
            tickSize: 0,
            tickPadding: 8,
            tickRotation: 0,
            legend: '',
            legendOffset: 32,
          }}
          axisLeft={{
            tickSize: 0,
            tickPadding: 6,
            tickRotation: 0,
            legend: '',
            legendOffset: -40,
          }}
          curve="monotoneX"
          enablePoints
          pointSize={4}
          pointBorderWidth={2}
          pointBorderColor={{ from: 'serieColor' }}
          useMesh
          enableGridX={false}
          enableGridY
          theme={chartTheme}
          // id 기준으로 색 고정 (체크 토글해도 안 꼬이게)
          colors={({ id }: { id: string }) => {
            const colorMap: Record<string, string> = {
              FE: '#0ea5e9', // sky-500
              BE: '#f97316', // orange-500
            };
            return colorMap[id] ?? '#6b7280';
          }}
        />
      </div>
    </div>
  );
};

export default TrafficGraph;
