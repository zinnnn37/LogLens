import { useId, useMemo, useState } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import type { TrafficDataPoint } from '@/types/log';

interface TrafficGraphProps {
  dataPoints: TrafficDataPoint[];
}

type ChannelKey = 'FE' | 'BE';

const channelLegend = [
  {
    key: 'FE',
    label: 'Front-end',
    color: '#0ea5e9',
  },
  {
    key: 'BE',
    label: 'Back-end',
    color: '#f97316',
  },
] as const;

type NumericTrafficFields = 'feCount' | 'beCount';

const fieldByChannel: Record<ChannelKey, NumericTrafficFields> = {
  FE: 'feCount',
  BE: 'beCount',
};

const channelMetaMap = channelLegend.reduce<
  Record<ChannelKey, (typeof channelLegend)[number]>
>(
  (acc, meta) => {
    acc[meta.key] = meta;
    return acc;
  },
  {} as Record<ChannelKey, (typeof channelLegend)[number]>,
);

const defaultVisibility: Record<ChannelKey, boolean> = {
  FE: true,
  BE: true,
};

type ChartDatum = {
  hour: string;
} & Record<ChannelKey, number>;

const TrafficGraph = ({ dataPoints }: TrafficGraphProps) => {
  const gradientBaseId = useId();
  const [visibleSeries, setVisibleSeries] =
    useState<Record<ChannelKey, boolean>>(defaultVisibility);

  const chartData = useMemo<ChartDatum[]>(
    () =>
      dataPoints.map(point => ({
        hour: point.hour,
        FE: point.feCount,
        BE: point.beCount,
      })),
    [dataPoints],
  );

  const totals = useMemo(
    () =>
      channelLegend.reduce<Record<ChannelKey, number>>(
        (acc, { key }) => {
          acc[key] = dataPoints.reduce(
            (sum, point) => sum + point[fieldByChannel[key]],
            0,
          );
          return acc;
        },
        {} as Record<ChannelKey, number>,
      ),
    [dataPoints],
  );

  const activeKeys = channelLegend
    .filter(({ key }) => visibleSeries[key])
    .map(({ key }) => key);

  const hasActiveSeries = activeKeys.length > 0 && chartData.length > 0;

  const maxValue = useMemo(
    () =>
      chartData.reduce((max, entry) => {
        const entryMax = activeKeys.reduce(
          (innerMax, key) => Math.max(innerMax, entry[key]),
          0,
        );
        return Math.max(max, entryMax);
      }, 0),
    [chartData, activeKeys],
  );

  const toggleSeries = (key: ChannelKey) => {
    setVisibleSeries(prev => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const tooltipRenderer = (props: any) => {
    const { active, payload, label } = props;
    if (!active || !payload?.length) {
      return null;
    }

    return (
      <div className="min-w-[160px] rounded-2xl border border-slate-100 bg-white/95 p-3 text-xs shadow-2xl backdrop-blur">
        <p className="text-slate-400">{label}</p>
        <div className="mt-2 space-y-1">
          {payload.map(
            (item: {
              dataKey?: string | number;
              value?: number;
              color?: string;
            }) => {
              if (!item?.dataKey) {
                return null;
              }

              const key = item.dataKey as ChannelKey;
              const meta = channelMetaMap[key];

              return (
                <div
                  key={item.dataKey}
                  className="flex items-center justify-between text-sm text-slate-600"
                >
                  <span className="flex items-center gap-2">
                    <span
                      className="h-2 w-2 rounded-full"
                      style={{ backgroundColor: meta?.color }}
                    />
                    <span>{meta?.label ?? key}</span>
                  </span>
                  <span className="font-semibold text-slate-900">
                    {Number(item.value ?? 0).toLocaleString()}건
                  </span>
                </div>
              );
            },
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="flex w-full flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        {channelLegend.map(({ key, label, color }) => {
          const isActive = visibleSeries[key];
          return (
            <Button
              key={key}
              type="button"
              variant="ghost"
              size="sm"
              aria-pressed={isActive}
              onClick={() => toggleSeries(key)}
              className={cn(
                'h-8 rounded-full border px-3 text-[11px] font-semibold tracking-wide transition',
                isActive
                  ? 'border-slate-900/30 bg-white text-slate-900 shadow-[0_8px_20px_rgba(15,23,42,0.15)]'
                  : 'border-slate-200 bg-white/80 text-slate-500 hover:text-slate-900',
              )}
            >
              <span className="flex items-center gap-2">
                <span
                  className="h-2.5 w-2.5 rounded-full"
                  style={{ backgroundColor: color }}
                />
                <span>{label}</span>
              </span>
              <Badge
                variant="outline"
                className={cn(
                  'ml-1.5 rounded-full border-none px-1.5 py-0 text-[10px] font-semibold transition-colors',
                  isActive
                    ? 'bg-slate-900 text-white shadow-sm'
                    : 'bg-slate-100 text-slate-500',
                )}
              >
                {totals[key]?.toLocaleString() ?? 0}
              </Badge>
            </Button>
          );
        })}
      </div>

      <div className="relative flex-1 overflow-hidden rounded-[1.75rem] border border-slate-100 bg-gradient-to-b from-white via-slate-50 to-white p-4 shadow-[inset_0_-1px_0_rgba(15,23,42,0.05)]">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(14,165,233,0.12),_transparent_60%)]" />
        <div className="relative h-[300px] w-full sm:h-[340px]">
          {hasActiveSeries ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart
                data={chartData}
                margin={{ top: 12, right: 8, bottom: 12, left: 8 }}
              >
                <defs>
                  {channelLegend.map(({ key, color }) => (
                    <linearGradient
                      key={key}
                      id={`${gradientBaseId}-${key}`}
                      x1="0"
                      y1="0"
                      x2="0"
                      y2="1"
                    >
                      <stop offset="5%" stopColor={color} stopOpacity={0.35} />
                      <stop offset="95%" stopColor={color} stopOpacity={0.05} />
                    </linearGradient>
                  ))}
                </defs>

                <CartesianGrid
                  stroke="rgba(148,163,184,0.15)"
                  vertical={false}
                  strokeDasharray="6 8"
                />
                <XAxis dataKey="hour" hide />
                <YAxis
                  hide
                  domain={
                    maxValue > 0 ? [0, Math.ceil(maxValue * 1.2)] : [0, 'auto']
                  }
                />
                <Tooltip
                  cursor={{ stroke: 'rgba(15,23,42,0.15)' }}
                  content={tooltipRenderer}
                />
                {channelLegend.map(({ key, color }) =>
                  visibleSeries[key] ? (
                    <Area
                      key={key}
                      type="monotone"
                      dataKey={key}
                      stroke={color}
                      strokeWidth={3}
                      fill={`url(#${gradientBaseId}-${key})`}
                      fillOpacity={1}
                      activeDot={{
                        r: 6,
                        style: {
                          filter: 'drop-shadow(0_6px_20px_rgba(15,23,42,0.25))',
                        },
                      }}
                      isAnimationActive
                    />
                  ) : null,
                )}
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex h-full items-center justify-center text-sm text-slate-400">
              표시할 트래픽 채널을 선택하세요.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default TrafficGraph;
