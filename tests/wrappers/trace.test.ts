// tests/wrappers/trace.test.ts
import { withLogLens } from '../../src/wrappers/trace';
import { LogCollector } from '../../src/core/logCollector';
import TraceContext from '../../src/core/traceContext';

describe('withLogLens', () => {
  let consoleLogSpy: jest.SpyInstance;

  beforeEach(() => {
    LogCollector.clear();
    LogCollector.init({ maxLogs: 1000 });
    TraceContext.reset();
    consoleLogSpy = jest.spyOn(console, 'log').mockImplementation();
  });

  afterEach(() => {
    consoleLogSpy.mockRestore();
  });

  describe('동기 함수', () => {
    test('함수를 정상적으로 실행한다', () => {
      const add = withLogLens((a: number, b: number) => a + b, {
        name: 'Math.add',
      });

      const result = add(2, 3);
      expect(result).toBe(5);
    });

    test('로그를 수집한다', () => {
      const add = withLogLens((a: number, b: number) => a + b, {
        name: 'Math.add',
      });

      add(2, 3);
      const logs = LogCollector.getLogs();
      expect(logs.length).toBeGreaterThan(0);

      const completedLog = logs.find((log) =>
        log.comment?.includes('completed'),
      );
      expect(completedLog).toBeDefined();
      expect(completedLog?.methodName).toBe('Math.add');
    });

    test('에러를 올바르게 처리한다', () => {
      const throwError = withLogLens(
        () => {
          throw new Error('Test error');
        },
        { name: 'ErrorFunc' },
      );

      expect(() => throwError()).toThrow('Test error');

      const logs = LogCollector.getLogs();
      const errorLog = logs.find((log) => log.logLevel === 'ERROR');
      expect(errorLog).toBeDefined();
      expect(errorLog?.comment).toContain('failed');
      expect(errorLog?.additionalInfo).toHaveProperty('message', 'Test error');
    });

    test('실행 시간을 기록한다', () => {
      const slowFunc = withLogLens(
        () => {
          const start = Date.now();
          while (Date.now() - start < 10) {}
          return 'done';
        },
        { name: 'SlowFunc' },
      );

      slowFunc();
      const logs = LogCollector.getLogs();
      expect(logs[0].executionTime).toBeGreaterThanOrEqual(0);
    });
  });

  describe('비동기 함수', () => {
    test('Promise를 정상적으로 반환한다', async () => {
      const asyncFunc = withLogLens(
        async (x: number) => {
          return x * 2;
        },
        { name: 'AsyncFunc' },
      );

      const result = await asyncFunc(5);
      expect(result).toBe(10);
    });

    test('비동기 로그를 수집한다', async () => {
      const asyncFunc = withLogLens(
        async () => {
          await new Promise((resolve) => setTimeout(resolve, 10));
          return 'done';
        },
        { name: 'AsyncFunc' },
      );

      await asyncFunc();
      const logs = LogCollector.getLogs();
      expect(logs.length).toBeGreaterThan(0);

      const completedLog = logs.find((log) =>
        log.comment?.includes('completed'),
      );
      expect(completedLog?.methodName).toBe('AsyncFunc');
    });

    test('비동기 에러를 처리한다', async () => {
      const asyncError = withLogLens(
        async () => {
          throw new Error('Async error');
        },
        { name: 'AsyncError' },
      );

      await expect(asyncError()).rejects.toThrow('Async error');

      const logs = LogCollector.getLogs();
      const errorLog = logs.find((log) => log.logLevel === 'ERROR');
      expect(errorLog).toBeDefined();
      expect(errorLog?.comment).toContain('failed');
    });
  });

  describe('옵션', () => {
    test('includeArgs 옵션으로 인자를 로깅한다', () => {
      const func = withLogLens(
        (a: number, b: string) => {
          return `${a}-${b}`;
        },
        { name: 'Func', includeArgs: true },
      );

      func(42, 'test');
      const logs = LogCollector.getLogs();

      // includeArgs: true면 2개 로그 생성 (called + completed)
      expect(logs.length).toBe(2);

      const calledLog = logs.find((log) => log.comment?.includes('called'));
      expect(calledLog).toBeDefined();
      expect(Array.isArray(calledLog?.additionalInfo)).toBe(true);
      expect(calledLog?.additionalInfo).toEqual([42, 'test']);
    });

    test('includeResult 옵션으로 결과를 로깅한다', () => {
      const func = withLogLens(() => ({ value: 123 }), {
        name: 'Func',
        includeResult: true,
      });

      func();
      const logs = LogCollector.getLogs();
      const completedLog = logs.find((log) =>
        log.comment?.includes('completed'),
      );

      expect(completedLog).toBeDefined();
      expect(completedLog?.additionalInfo).toMatchObject({
        result: { value: 123 },
      });
    });

    test('skipLog 옵션으로 로깅을 건너뛴다', () => {
      const func = withLogLens(() => 'value', {
        name: 'Func',
        skipLog: true,
      });

      func();
      const logs = LogCollector.getLogs();
      expect(logs.length).toBe(0);
    });

    test('context 옵션으로 추가 정보를 로깅한다', () => {
      const func = withLogLens(() => 'done', {
        name: 'Func',
        context: { userId: '123', action: 'create' },
      });

      func();
      const logs = LogCollector.getLogs();
      const completedLog = logs.find((log) =>
        log.comment?.includes('completed'),
      );

      expect(completedLog?.additionalInfo).toMatchObject({
        userId: '123',
        action: 'create',
      });
    });

    test('includeResult와 context를 함께 사용한다', () => {
      const func = withLogLens(() => 'result', {
        name: 'Func',
        includeResult: true,
        context: { test: 'value' },
      });

      func();
      const logs = LogCollector.getLogs();
      const completedLog = logs.find((log) =>
        log.comment?.includes('completed'),
      );

      expect(completedLog?.additionalInfo).toMatchObject({
        result: 'result',
        test: 'value',
      });
    });
  });

  describe('TraceContext 통합', () => {
    test('TraceID를 생성하고 관리한다', () => {
      const func = withLogLens(() => 'done', { name: 'Func' });

      func();
      const logs = LogCollector.getLogs();
      expect(logs[0].traceId).toBeDefined();
      expect(typeof logs[0].traceId).toBe('string');
    });

    test('명시적 TraceID를 사용한다', () => {
      const func = withLogLens((_opts: any) => 'done', { name: 'Func' });

      func({ _traceId: 'custom-id' });

      const logs = LogCollector.getLogs();
      expect(logs[0].traceId).toBe('custom-id');
    });

    test('중첩 호출 시 동일한 TraceID를 사용한다', () => {
      const inner = withLogLens(() => 'inner', { name: 'Inner' });
      const outer = withLogLens(() => inner(), { name: 'Outer' });

      outer();
      const logs = LogCollector.getLogs();
      expect(logs.length).toBe(2);

      const traceId = logs[0].traceId;
      logs.forEach((log) => {
        expect(log.traceId).toBe(traceId);
      });
    });

    test('에러 발생 시에도 TraceContext를 정리한다', () => {
      const errorFunc = withLogLens(
        () => {
          throw new Error('Test');
        },
        { name: 'ErrorFunc' },
      );

      expect(() => errorFunc()).toThrow();
      expect(TraceContext.getDepth()).toBe(0);
    });

    test('비동기 에러 발생 시에도 TraceContext를 정리한다', async () => {
      const asyncError = withLogLens(
        async () => {
          throw new Error('Async Test');
        },
        { name: 'AsyncError' },
      );

      await expect(asyncError()).rejects.toThrow();
      expect(TraceContext.getDepth()).toBe(0);
    });
  });
});
