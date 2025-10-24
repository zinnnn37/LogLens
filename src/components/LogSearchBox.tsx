import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import clsx from 'clsx';
import LogResultsTable, { type LogRow } from '@/components/LogResultsTable';
import { DUMMY_LOGS } from '@/mocks/dummyLogs';

export interface LogSearchBoxProps {
  title?: string;
  placeholder?: string;
  value?: string;
  onChange?: (val: string) => void;
  onSubmit?: (val: string) => void;
  loading?: boolean;
  className?: string;
  buttonLabel?: string;
}

const LogSearchBox = ({
  title = '로그 검색',
  placeholder = 'TraceId로 검색...',
  value,
  onChange,
  onSubmit,
  loading = false,
  buttonLabel = '검색',
  className,
}: LogSearchBoxProps) => {
  const [inner, setInner] = useState(value ?? '');
  const [rows, setRows] = useState<LogRow[]>([]);
  const [showResults, setShowResults] = useState(false);

  // controlled/uncontrolled 겸용
  useEffect(() => {
    if (value !== undefined) {
      setInner(value);
    }
  }, [value]);

  const handleInput = (val: string) => {
    if (value === undefined) {
      setInner(val);
    }
    onChange?.(val);
  };

  const handleSubmit = (e?: FormEvent) => {
    e?.preventDefault();
    const q = inner.trim();
    onSubmit?.(q);

    if (!q) {
      // 검색어가 비어 있으면 결과 숨김
      setRows([]);
      setShowResults(false);
      return;
    }

    // 간단한 필터링 (TraceId기준)
    const lower = q.toLowerCase();
    const filtered = DUMMY_LOGS.filter(r => r.id.toLowerCase().includes(lower));

    setRows(filtered);
    setShowResults(true);
  };

  return (
    <section
      className={clsx(
        'rounded-3xl border border-slate-200 bg-slate-50/70 p-5 shadow-sm md:p-6',
        className,
      )}
    >
      {/* 검색 폼 */}
      <form onSubmit={handleSubmit} role="search" aria-label={title}>
        <div className="font-godoM mb-4 flex items-center gap-2 text-slate-700">
          <Search className="size-5 shrink-0 text-slate-500" aria-hidden />
          <span className="text-base font-semibold">{title}</span>
        </div>

        <div className="flex items-center gap-3">
          <div
            className={clsx(
              'relative flex-1 rounded-2xl border border-slate-200 bg-white',
              'shadow-inner focus-within:border-slate-300 focus-within:ring-2 focus-within:ring-slate-200/60',
            )}
          >
            <input
              type="text"
              value={inner}
              onChange={e => handleInput(e.target.value)}
              placeholder={placeholder}
              aria-label={placeholder}
              className="h-12 w-full rounded-2xl bg-transparent px-4 pr-10 text-[15px] outline-none placeholder:text-slate-400"
            />
            <Search
              className="pointer-events-none absolute top-1/2 right-3 size-5 -translate-y-1/2 text-slate-400"
              aria-hidden
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className={clsx(
              'text-md h-12 shrink-0 rounded-2xl px-6 font-semibold text-white',
              'bg-secondary hover:bg-primary active:bg-primary',
              'disabled:cursor-not-allowed disabled:opacity-70',
            )}
          >
            {loading ? '검색중…' : buttonLabel}
          </button>
        </div>
      </form>

      {/* 결과표: 검색했을 때만 렌더 */}
      {showResults && (
        <div className="mt-4">
          <LogResultsTable rows={rows} maxHeightClass="max-h-80" />
        </div>
      )}
    </section>
  );
};

export default LogSearchBox;
