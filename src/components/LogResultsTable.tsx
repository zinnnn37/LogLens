import { memo } from 'react';

export interface LogRow {
  id: string; // TraceId
  level: 'INFO' | 'WARN' | 'ERROR';
  layer: 'FE' | 'BE' | 'INFRA';
  message: string;
  date: string; // ISO string
}

export interface LogResultsTableProps {
  rows: LogRow[];
  /** 결과 컨테이너 최대높이 (Tailwind 클래스). 기본: max-h-80 (20rem) */
  maxHeightClass?: string;
  /** 행 클릭 핸들러 (선택) */
  onRowClick?: (row: LogRow) => void;
  /** 로딩 상태 (선택) */
  loading?: boolean;
}

const LevelBadge = ({ level }: { level: LogRow['level'] }) => {
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

const LogResultsTable = memo(
  ({
    rows,
    maxHeightClass = 'max-h-80',
    onRowClick,
    loading,
  }: LogResultsTableProps) => {
    return (
      <div className="relative overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-inner">
        {/* 스크롤 영역 */}
        <div
          className={`overflow-y-auto ${maxHeightClass} [scrollbar-gutter:stable]`}
        >
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
              {loading && (
                <tr>
                  <td
                    colSpan={4}
                    className="px-5 py-8 text-center text-slate-500"
                  >
                    검색 중…
                  </td>
                </tr>
              )}

              {!loading && rows.length === 0 && (
                <tr>
                  <td
                    colSpan={4}
                    className="px-5 py-8 text-center text-slate-500"
                  >
                    검색 결과가 없습니다.
                  </td>
                </tr>
              )}

              {!loading &&
                rows.map((row, index) => (
                  <tr
                    key={`${row.id}-${row.date}-${index}`}
                    className={`border-t border-slate-100 ${onRowClick ? 'cursor-pointer hover:bg-slate-50' : ''}`}
                    onClick={onRowClick ? () => onRowClick(row) : undefined}
                  >
                    <td className="px-5 py-3 align-top">
                      <LevelBadge level={row.level} />
                    </td>
                    <td className="px-5 py-3 align-top">{row.layer}</td>
                    <td className="px-5 py-3 align-top">
                      <div className="truncate font-medium text-slate-800">
                        {row.message}
                      </div>
                      <div className="mt-1 text-xs text-slate-400">
                        TraceId: {row.id}
                      </div>
                    </td>
                    <td className="py-3 pr-5 text-left align-top whitespace-nowrap tabular-nums">
                      {new Date(row.date).toLocaleString('ko-KR', {
                        timeZone: 'UTC',
                      })}
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  },
);

export default LogResultsTable;
