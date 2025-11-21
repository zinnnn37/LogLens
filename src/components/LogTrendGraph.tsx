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
import type { LogDataPoint } from '@/types/log';

interface LogTrendGraphProps {
  dataPoints: LogDataPoint[];
}

type SeverityKey = 'INFO' | 'WARN' | 'ERROR';

const severityLegend = [
  {
    key: 'INFO',
    label: 'INFO',
    color: '#3b82f6',
  },
  {
    key: 'WARN',
    label: 'WARN',
    color: '#f97316',
  },
  {
    key: 'ERROR',
    label: 'ERROR',
    color: '#ef4444',
  },
] as const;

type NumericLogFields = 'infoCount' | 'warnCount' | 'errorCount';

const fieldBySeverity: Record<SeverityKey, NumericLogFields> = {
  INFO: 'infoCount',
  WARN: 'warnCount',
  ERROR: 'errorCount',
};

const severityMetaMap = severityLegend.reduce<
  Record<SeverityKey, (typeof severityLegend)[number]>
>(
  (acc, meta) => {
    acc[meta.key] = meta;
    return acc;
  },
  {} as Record<SeverityKey, (typeof severityLegend)[number]>,
);

const defaultVisibility: Record<SeverityKey, boolean> = {
  INFO: true,
  WARN: true,
  ERROR: true,
};

type ChartDatum = {
  hour: string;
} & Record<SeverityKey, number>;

const LogTrendGraph = ({ dataPoints }: LogTrendGraphProps) => {
  const gradientBaseId = useId();
  const [visibleSeries, setVisibleSeries] =
    useState<Record<SeverityKey, boolean>>(defaultVisibility);

  const chartData = useMemo<ChartDatum[]>(
    () =>
      dataPoints.map(point => ({
        hour: point.hour,
        INFO: point.infoCount,
        WARN: point.warnCount,
        ERROR: point.errorCount,
      })),
    [dataPoints],
  );

  const totals = useMemo(
    () =>
      severityLegend.reduce<Record<SeverityKey, number>>(
        (acc, { key }) => {
          acc[key] = dataPoints.reduce(
            (sum, point) => sum + point[fieldBySeverity[key]],
            0,
          );
          return acc;
        },
        {} as Record<SeverityKey, number>,
      ),
    [dataPoints],
  );

  const activeKeys = severityLegend
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

  const toggleSeries = (key: SeverityKey) => {
    setVisibleSeries(prev => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  const tooltipRenderer = (props: {
    active?: boolean;
    payload?: readonly { dataKey?: string | number; value?: number }[];
    label?: string | number;
  }) => {
    const { active, payload, label } = props;
    if (!active || !payload?.length) {
      return null;
    }

    return (
      <div className="min-w-[160px] rounded-2xl border border-slate-100 bg-white/95 p-3 text-xs shadow-2xl backdrop-blur">
        <p className="text-slate-400">{label}</p>
        <div className="mt-2 space-y-1">
          {payload.map(item => {
            if (!item?.dataKey) {
              return null;
            }

            const key = item.dataKey as SeverityKey;
            const meta = severityMetaMap[key];

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
          })}
        </div>
      </div>
    );
  };

  return (
    <div className="flex w-full flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        {severityLegend.map(({ key, label, color }) => {
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
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top,_rgba(59,130,246,0.15),_transparent_60%)]" />
        <div className="relative h-[320px] w-full sm:h-[360px]">
          {hasActiveSeries ? (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart
                data={chartData}
                margin={{ top: 12, right: 8, bottom: 12, left: 8 }}
              >
                <defs>
                  {severityLegend.map(({ key, color }) => (
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
                {severityLegend.map(({ key, color }) =>
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
              표시할 로그 레벨을 선택하세요.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default LogTrendGraph;
