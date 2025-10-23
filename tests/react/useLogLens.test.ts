// tests/react/useLogLens.test.ts

/**
 * @jest-environment jsdom
 */
import { renderHook } from '@testing-library/react';
import { useLogLens } from '../../src/react/useLogLens';
import { LogCollector } from '../../src/core/logCollector';

describe('useLogLens', () => {
  beforeEach(() => {
    LogCollector.clear();
    jest.spyOn(console, 'log').mockImplementation();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('함수를 래핑한다', () => {
    const { result } = renderHook(() =>
      useLogLens((x: number) => x * 2, { logger: 'Double' }, []),
    );

    expect(result.current(5)).toBe(10);
  });

  test('deps가 변경되지 않으면 동일한 함수를 반환한다', () => {
    const { result, rerender } = renderHook(() =>
      useLogLens((x: number) => x * 2, { logger: 'Double' }, []),
    );

    const firstFunc = result.current;
    rerender();
    const secondFunc = result.current;

    expect(firstFunc).toBe(secondFunc);
  });

  test('deps가 변경되면 새로운 함수를 반환한다', () => {
    let count = 0;
    const { result, rerender } = renderHook(
      ({ deps }) =>
        useLogLens((x: number) => x * 2, { logger: 'Double' }, deps),
      { initialProps: { deps: [count] } },
    );

    const firstFunc = result.current;

    count = 1;
    rerender({ deps: [count] });
    const secondFunc = result.current;

    expect(firstFunc).not.toBe(secondFunc);
  });

  test('로그를 수집한다', () => {
    const { result } = renderHook(() =>
      useLogLens((x: number) => x + 1, { logger: 'Increment' }, []),
    );

    result.current(10);
    expect(LogCollector.getLogs()).toHaveLength(2);
  });
});
