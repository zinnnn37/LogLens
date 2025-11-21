// tests/withLogLens.test.ts

import { withLogLens } from '../src/wrappers/trace';
import { LightZone } from '../src/core/lightZone';
import { LogCollector } from '../src/core/logCollector';

describe('withLogLens', () => {
  beforeEach(() => {
    LightZone.init();
    LogCollector.init(null);
    LogCollector.clear();
  });

  afterEach(() => {
    LightZone.reset();
    LogCollector.clear();
  });

  describe('동기 함수 래핑', () => {
    test('기본 동작', () => {
      const add = (a: number, b: number) => a + b;
      const wrapped = withLogLens(add, { logger: 'add' });

      const result = wrapped(1, 2);

      expect(result).toBe(3);
      expect(LogCollector.getLogs().length).toBeGreaterThan(0);
    });

    test('traceId 생성 및 전파', () => {
      const outer = withLogLens(
        () => {
          const inner = () => {
            return LightZone.getTraceId();
          };
          return inner();
        },
        { logger: 'outer' },
      );

      const traceId = outer();
      expect(traceId).toBeTruthy();
      expect(typeof traceId).toBe('string');
    });

    test('예외 발생 시 에러 로그', () => {
      const throwError = () => {
        throw new Error('test error');
      };
      const wrapped = withLogLens(throwError, { logger: 'throwError' });

      expect(() => wrapped()).toThrow('test error');

      const logs = LogCollector.getLogs();
      const errorLog = logs.find((log) => log.level === 'ERROR');
      expect(errorLog).toBeDefined();
      expect(errorLog?.message).toContain('test error');
    });
  });

  describe('비동기 함수 래핑', () => {
    test('Promise 반환 함수', async () => {
      const fetchData = async (id: string) => {
        await new Promise((resolve) => setTimeout(resolve, 10));
        return { id, name: 'test' };
      };
      const wrapped = withLogLens(fetchData, { logger: 'fetchData' });

      const result = await wrapped('123');

      expect(result).toEqual({ id: '123', name: 'test' });

      const logs = LogCollector.getLogs();
      expect(logs.some((log) => log.logger === 'fetchData')).toBe(true);
    });

    test('비동기 함수에서 traceId 전파', async () => {
      const outer = withLogLens(
        async () => {
          await new Promise((resolve) => setTimeout(resolve, 10));

          const inner = async () => {
            await new Promise((resolve) => setTimeout(resolve, 10));
            return LightZone.getTraceId();
          };

          return await inner();
        },
        { logger: 'outer' },
      );

      const traceId = await outer();
      expect(traceId).toBeTruthy();
    });

    test('비동기 예외 처리', async () => {
      const throwAsync = async () => {
        await new Promise((resolve) => setTimeout(resolve, 10));
        throw new Error('async error');
      };
      const wrapped = withLogLens(throwAsync, { logger: 'throwAsync' });

      await expect(wrapped()).rejects.toThrow('async error');

      const logs = LogCollector.getLogs();
      const errorLog = logs.find((log) => log.level === 'ERROR');
      expect(errorLog).toBeDefined();
    });
  });

  describe('로깅 옵션', () => {
    test('skipLog: true 시 로그 미출력', () => {
      const fn = (x: number) => x * 2;
      const wrapped = withLogLens(fn, { skipLog: true });

      wrapped(5);

      expect(LogCollector.getLogs().length).toBe(0);
    });

    test('logger 이름 지정', () => {
      const fn = () => 'test';
      const wrapped = withLogLens(fn, { logger: 'CustomLogger' });

      wrapped();

      const logs = LogCollector.getLogs();
      expect(logs.some((log) => log.logger === 'CustomLogger')).toBe(true);
    });

    test('함수명 자동 추출', () => {
      function namedFunction() {
        return 'test';
      }
      const wrapped = withLogLens(namedFunction);

      wrapped();

      const logs = LogCollector.getLogs();
      expect(logs.some((log) => log.logger === 'namedFunction')).toBe(true);
    });
  });

  describe('실행 시간 측정', () => {
    test('동기 함수 실행 시간', () => {
      const fn = () => {
        let sum = 0;
        for (let i = 0; i < 1000; i++) sum += i;
        return sum;
      };
      const wrapped = withLogLens(fn, { logger: 'computation' });

      wrapped();

      const logs = LogCollector.getLogs();
      const completedLog = logs.find((log) =>
        log.message.includes('completed'),
      );
      expect(completedLog?.executionTimeMs).toBeGreaterThanOrEqual(0);
    });

    test('비동기 함수 실행 시간', async () => {
      const fn = async () => {
        await new Promise((resolve) => setTimeout(resolve, 50));
        return 'done';
      };
      const wrapped = withLogLens(fn, { logger: 'delay' });

      await wrapped();

      const logs = LogCollector.getLogs();
      const completedLog = logs.find((log) =>
        log.message.includes('completed'),
      );
      expect(completedLog?.executionTimeMs).toBeGreaterThanOrEqual(50);
    });
  });

  describe('중첩 함수 호출', () => {
    test('같은 traceId 공유', async () => {
      const inner = async () => {
        await new Promise((resolve) => setTimeout(resolve, 10));
        return LightZone.getTraceId();
      };

      const outer = withLogLens(
        async () => {
          const outerTraceId = LightZone.getTraceId();
          const innerTraceId = await inner();
          return { outerTraceId, innerTraceId };
        },
        { logger: 'outer' },
      );

      const result = await outer();
      expect(result.outerTraceId).toBe(result.innerTraceId);
    });

    test('여러 함수 호출 시 같은 traceId', async () => {
      const traceIds: (string | null)[] = [];

      const step1 = async () => {
        await new Promise((resolve) => setTimeout(resolve, 10));
        traceIds.push(LightZone.getTraceId());
      };

      const step2 = async () => {
        await new Promise((resolve) => setTimeout(resolve, 10));
        traceIds.push(LightZone.getTraceId());
      };

      const main = withLogLens(
        async () => {
          traceIds.push(LightZone.getTraceId());
          await step1();
          await step2();
        },
        { logger: 'main' },
      );

      await main();

      expect(traceIds[0]).toBe(traceIds[1]);
      expect(traceIds[1]).toBe(traceIds[2]);
    });
  });
});
