import type { LogRow } from '@/components/LogResultsTable';

export const DUMMY_LOGS: LogRow[] = Array.from({ length: 42 }).map((_, i) => {
  const levels: LogRow['level'][] = ['INFO', 'WARN', 'ERROR'];
  const layers: LogRow['layer'][] = ['FE', 'BE', 'INFRA'];
  const level = levels[i % 3];
  const layer = layers[i % 3];
  const ts = new Date(Date.now() - i * 1000 * 60 * 7).toISOString();
  const id = `trace-${(100000 + i).toString(16)}`;
  return {
    id,
    level,
    layer,
    date: ts,
    message:
      level === 'ERROR'
        ? `Unhandled exception on ${layer} (code=${500 + (i % 5)})`
        : level === 'WARN'
          ? `Latency spike detected on ${layer} gateway`
          : `Processed request successfully (#${i})`,
  };
});
