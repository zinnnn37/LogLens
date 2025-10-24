import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import clsx from 'clsx';

export interface LogSearchBoxProps {
  /** 상단 타이틀 (기본: "로그 검색") */
  title?: string;
  /** 입력 placeholder (기본: "TraceId로 검색...") */
  placeholder?: string;
  /** 외부 제어용 값 (controlled). 주지 않으면 내부 state로 동작 */
  value?: string;
  /** 값 변경 콜백 */
  onChange?: (val: string) => void;
  /** 제출 콜백 (버튼 클릭/Enter) */
  onSubmit?: (val: string) => void;
  /** 제출 중 상태 (로딩 시 버튼 disabled) */
  loading?: boolean;
  /** 루트 컨테이너 클래스 확장 */
  className?: string;
  /** 버튼 라벨 (기본: "검색") */
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
  // controlled/uncontrolled 겸용
  const [inner, setInner] = useState(value ?? '');
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
    onSubmit?.(inner.trim());
  };

  return (
    <form
      onSubmit={handleSubmit}
      className={clsx(
        'rounded-3xl border border-slate-200 bg-slate-50/70 p-5 md:p-6',
        'shadow-sm',
        className,
      )}
      role="search"
      aria-label={title}
    >
      {/* 헤더 */}
      <div className="font-godoM mb-4 flex items-center gap-2 text-slate-700">
        <Search className="size-5 shrink-0 text-slate-500" aria-hidden />
        <span className="text-base font-semibold">{title}</span>
      </div>

      {/* 입력 영역 */}
      <div className="flex items-center gap-3">
        <div
          className={clsx(
            'relative flex-1 rounded-2xl border border-slate-200 bg-white',
            'shadow-inner',
            'focus-within:border-slate-300 focus-within:ring-2 focus-within:ring-slate-200/60',
          )}
        >
          <input
            type="text"
            value={inner}
            onChange={e => handleInput(e.target.value)}
            placeholder={placeholder}
            aria-label={placeholder}
            className={clsx(
              'h-12 w-full rounded-2xl bg-transparent',
              'px-4 pr-10 text-[15px] outline-none',
              'placeholder:text-slate-400',
            )}
          />
          {/* 입력창 내부 돋보기 아이콘 (오른쪽) */}
          <Search
            className="pointer-events-none absolute top-1/2 right-3 size-5 -translate-y-1/2 text-slate-400"
            aria-hidden
          />
        </div>

        {/* 제출 버튼 */}
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
  );
};

export default LogSearchBox;
