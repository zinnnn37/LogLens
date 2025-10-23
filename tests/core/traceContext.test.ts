// tests/core/traceContext.test.ts

import TraceContext from '../../src/core/traceContext';

describe('TraceContext', () => {
  beforeEach(() => {
    TraceContext.reset();
  });

  describe('startTrace', () => {
    test('새로운 traceId를 생성한다', () => {
      const traceId = TraceContext.startTrace();
      expect(traceId).toBeDefined();
      expect(typeof traceId).toBe('string');
      expect(TraceContext.getCurrentTraceId()).toBe(traceId);
    });

    test('명시적 traceId를 사용한다', () => {
      const explicitId = 'custom-trace-id';
      const traceId = TraceContext.startTrace(explicitId);
      expect(traceId).toBe(explicitId);
    });

    test('중첩 호출 시 동일한 traceId를 재사용한다', () => {
      const traceId1 = TraceContext.startTrace();
      const traceId2 = TraceContext.startTrace();
      expect(traceId1).toBe(traceId2);
      expect(TraceContext.getDepth()).toBe(2);
    });
  });

  describe('endTrace', () => {
    test('스택에서 traceId를 제거한다', () => {
      TraceContext.startTrace();
      expect(TraceContext.getDepth()).toBe(1);
      TraceContext.endTrace();
      expect(TraceContext.getDepth()).toBe(0);
    });

    test('중첩 호출 후 올바르게 정리된다', () => {
      const traceId = TraceContext.startTrace();
      TraceContext.startTrace();
      TraceContext.endTrace();
      expect(TraceContext.getCurrentTraceId()).toBe(traceId);
      TraceContext.endTrace();
      expect(TraceContext.getCurrentTraceId()).toBeNull();
    });
  });

  describe('getCurrentTraceId', () => {
    test('스택이 비어있으면 null을 반환한다', () => {
      expect(TraceContext.getCurrentTraceId()).toBeNull();
    });

    test('최상단 traceId를 반환한다', () => {
      const traceId = TraceContext.startTrace();
      expect(TraceContext.getCurrentTraceId()).toBe(traceId);
    });
  });

  describe('reset', () => {
    test('모든 스택을 초기화한다', () => {
      TraceContext.startTrace();
      TraceContext.startTrace();
      TraceContext.reset();
      expect(TraceContext.getDepth()).toBe(0);
      expect(TraceContext.getCurrentTraceId()).toBeNull();
    });
  });
});
