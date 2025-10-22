// src/react/useLogLens.ts

import { useMemo } from 'react';
import { withLogLens } from '../wrappers/trace';
import type { LogLensOptions } from '../core/types';

/**
 * 함수를 LogLens로 래핑하는 React Hook
 *
 * @example
 * const fetchUser = useLogLens(
 *   async (id: string) => {
 *     const res = await fetch(`/api/users/${id}`);
 *     return res.json();
 *   },
 *   { logger: 'API.fetchUser' },
 *   []
 * );
 *
 * await fetchUser('123');
 */
export function useLogLens<T extends (...args: any[]) => any>(
  fn: T,
  options?: LogLensOptions,
  deps: any[] = [],
): T {
  return useMemo(
    () => withLogLens(fn, options),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    deps,
  );
}
