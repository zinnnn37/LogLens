import React, { useMemo, useState } from 'react';
import { ResponsiveLine } from '@nivo/line';
import type { LogDataPoint } from '@/types/log';

interface LogTrendGraphProps {
  dataPoints: LogDataPoint[];
}

type SeriesKey = 'INFO' | 'WARN' | 'ERROR';

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

const LogTrendGraph: React.FC<LogTrendGraphProps> = ({ dataPoints }) => {
  // 어떤 시리즈를 보여줄지 토글 상태
  const [visible, setVisible] = useState<Record<SeriesKey, boolean>>({
    INFO: true,
    WARN: true,
    ERROR: true,
  });

  const series = useMemo(() => {
    const base = [
      {
        id: 'INFO' as const,
        data: dataPoints.map(p => ({ x: p.hour, y: p.infoCount })),
      },
      {
        id: 'WARN' as const,
        data: dataPoints.map(p => ({ x: p.hour, y: p.warnCount })),
      },
      {
        id: 'ERROR' as const,
        data: dataPoints.map(p => ({ x: p.hour, y: p.errorCount })),
      },
    ];

    // 체크된 시리즈만 필터링
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
      {/* 상단 체크박스 영역 */}
      <div className="mb-2 flex items-center gap-4 text-xs text-slate-600">
        <label className="flex cursor-pointer items-center gap-1">
          <input
            type="checkbox"
            className="h-3 w-3 rounded border-slate-300"
            checked={visible.INFO}
            onChange={() => handleToggle('INFO')}
          />
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-emerald-500" />
            <span>INFO</span>
          </span>
        </label>

        <label className="flex cursor-pointer items-center gap-1">
          <input
            type="checkbox"
            className="h-3 w-3 rounded border-slate-300"
            checked={visible.WARN}
            onChange={() => handleToggle('WARN')}
          />
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-amber-500" />
            <span>WARN</span>
          </span>
        </label>

        <label className="flex cursor-pointer items-center gap-1">
          <input
            type="checkbox"
            className="h-3 w-3 rounded border-slate-300"
            checked={visible.ERROR}
            onChange={() => handleToggle('ERROR')}
          />
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-red-500" />
            <span>ERROR</span>
          </span>
        </label>
      </div>

      {/* 실제 그래프 영역 */}
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
          colors={({ id }: { id: string }) => {
            const colorMap: Record<string, string> = {
              INFO: '#16a34a', // 초록
              WARN: '#f59e0b', // 노랑
              ERROR: '#ef4444', // 빨강
            };
            return colorMap[id] ?? '#6b7280';
          }}
        />
      </div>
    </div>
  );
};

export default LogTrendGraph;
