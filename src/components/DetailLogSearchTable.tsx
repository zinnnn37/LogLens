// src/components/DetailLogSearchTable.tsx

import { useEffect, useRef } from 'react';
import { Loader2 } from 'lucide-react';
import type { LogData } from '@/types/log';

const LevelBadge = ({ level }: { level: LogData['logLevel'] }) => {
  const cls =
    level === 'ERROR'
      ? 'bg-red-100 text-red-700'
      : level === 'WARN'
        ? 'bg-amber-100 text-amber-700'
        : 'bg-emerald-100 text-emerald-700';

  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${cls}`}
    >
      {level}
    </span>
  );
};

interface DetailLogSearchTableProps {
  onRowClick?: (log: LogData) => void;
  logs: LogData[];
  loading: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
}

const DetailLogSearchTable = ({
  onRowClick,
  logs,
  loading,
  hasMore,
  onLoadMore,
}: DetailLogSearchTableProps) => {
  const observerRef = useRef<IntersectionObserver | null>(null);
  const loadMoreRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (loading || !hasMore) {
      return;
    }

    observerRef.current = new IntersectionObserver(
      entries => {
        if (entries[0].isIntersecting) {
          onLoadMore();
        }
      },
      { threshold: 0.1 },
    );

    const currentLoadMoreRef = loadMoreRef.current;
    if (currentLoadMoreRef) {
      observerRef.current.observe(currentLoadMoreRef);
    }

    return () => {
      if (observerRef.current && currentLoadMoreRef) {
        observerRef.current.unobserve(currentLoadMoreRef);
      }
    };
  }, [loading, hasMore, onLoadMore]);

  return (
    <div className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-inner">
      {/* 스크롤 영역 */}
      <div className="max-h-[600px] overflow-y-auto [scrollbar-gutter:stable]">
        <table className="min-w-full table-fixed border-separate border-spacing-0">
          <thead className="sticky top-0 z-10 bg-white">
            <tr className="text-slate-500">
              <th className="text-md w-28 px-5 py-3 text-left font-semibold first:rounded-tl-2xl">
                Level
              </th>
              <th className="text-md w-28 px-5 py-3 text-left font-semibold">
                Layer
              </th>
              <th className="text-md px-5 py-3 text-left font-semibold">
                Message
              </th>
              <th className="text-md w-64 py-3 pr-5 text-left font-semibold whitespace-nowrap last:rounded-tr-2xl">
                Date
              </th>
            </tr>
          </thead>

          <tbody className="text-sm text-slate-700">
            {loading && logs.length === 0 && (
              <tr>
                <td
                  colSpan={4}
                  className="px-5 py-8 text-center text-slate-500"
                >
                  <div className="flex items-center justify-center gap-2">
                    <Loader2 className="h-5 w-5 animate-spin text-slate-500" />
                    <span>검색 중…</span>
                  </div>
                </td>
              </tr>
            )}

            {!loading && logs.length === 0 && (
              <tr>
                <td
                  colSpan={4}
                  className="px-5 py-8 text-center text-slate-500"
                >
                  검색 결과가 없습니다.
                </td>
              </tr>
            )}

            {logs.map(log => (
              <tr
                key={log.logId}
                className={`border-t border-slate-100 ${onRowClick ? 'cursor-pointer hover:bg-slate-50' : ''
                  }`}
                onClick={onRowClick ? () => onRowClick(log) : undefined}
              >
                <td className="px-5 py-3 align-top">
                  <LevelBadge level={log.logLevel} />
                </td>
                <td className="px-5 py-3 align-top">{log.sourceType}</td>
                <td className="px-5 py-3 align-top">
                  <div className="max-w-2xl truncate font-medium text-slate-800">
                    {log.message}
                  </div>
                  <div className="mt-1 max-w-2xl truncate text-xs text-slate-400">
                    TraceId: {log.traceId}
                  </div>
                </td>
                <td className="py-3 pr-5 text-left align-top whitespace-nowrap tabular-nums">
                  {new Date(log.timestamp.replace('Z', '')).toLocaleString()}
                </td>
              </tr>
            ))}

            {/* 무한 스크롤 로딩 */}
            {loading && logs.length > 0 && (
              <tr>
                <td colSpan={4} className="px-5 py-4 text-center">
                  <div className="flex items-center justify-center gap-2">
                    <Loader2 className="h-4 w-4 animate-spin text-slate-400" />
                    <span className="text-xs text-slate-400">
                      추가 로드 중...
                    </span>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>

        {/* 무한 스크롤 트리거 */}
        {hasMore && !loading && <div ref={loadMoreRef} className="h-4" />}
      </div>
    </div>
  );
};

export default DetailLogSearchTable;