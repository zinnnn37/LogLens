// tests/lightZone.test.ts

import { LightZone } from '../src/core/lightZone';

describe('LightZone', () => {
  beforeEach(() => {
    LightZone.reset();
  });

  afterEach(() => {
    LightZone.reset();
  });

  describe('초기화', () => {
    test('init 호출 시 패치 완료', () => {
      LightZone.init();
      expect(LightZone.isEnabled()).toBe(true);
    });

    test('중복 init 호출 시 경고', () => {
      const spy = jest.spyOn(console, 'warn').mockImplementation();

      LightZone.init();
      LightZone.init();

      expect(spy).toHaveBeenCalledWith('[LogLens] Already initialized');
      spy.mockRestore();
    });
  });

  describe('동기 실행', () => {
    test('컨텍스트 전파', () => {
      LightZone.init();

      const traceId = 'test-123';
      let captured: string | null = null;

      LightZone.run({ traceId }, () => {
        captured = LightZone.getTraceId();
      });

      expect(captured).toBe(traceId);
    });

    test('중첩 컨텍스트', () => {
      LightZone.init();

      const outer = 'outer-123';
      const inner = 'inner-456';
      const results: string[] = [];

      LightZone.run({ traceId: outer }, () => {
        results.push(LightZone.getTraceId()!);

        LightZone.run({ traceId: inner }, () => {
          results.push(LightZone.getTraceId()!);
        });

        results.push(LightZone.getTraceId()!);
      });

      expect(results).toEqual([outer, inner, outer]);
    });

    test('실행 완료 후 스택 정리', () => {
      LightZone.init();

      LightZone.run({ traceId: 'test' }, () => {
        expect(LightZone.getDepth()).toBe(1);
      });

      expect(LightZone.getDepth()).toBe(0);
    });

    test('예외 발생 시에도 스택 정리', () => {
      LightZone.init();

      expect(() => {
        LightZone.run({ traceId: 'test' }, () => {
          throw new Error('test error');
        });
      }).toThrow('test error');

      expect(LightZone.getDepth()).toBe(0);
    });
  });

  describe('비동기 실행', () => {
    test('Promise에서 컨텍스트 전파', async () => {
      LightZone.init();

      const traceId = 'async-123';
      let captured: string | null = null;

      await LightZone.runAsync({ traceId }, async () => {
        await new Promise((resolve) => setTimeout(resolve, 10));
        captured = LightZone.getTraceId();
      });

      expect(captured).toBe(traceId);
    });

    test('중첩 비동기 컨텍스트', async () => {
      LightZone.init();

      const outer = 'outer-async';
      const inner = 'inner-async';
      const results: string[] = [];

      await LightZone.runAsync({ traceId: outer }, async () => {
        results.push(LightZone.getTraceId()!);

        await LightZone.runAsync({ traceId: inner }, async () => {
          await new Promise((resolve) => setTimeout(resolve, 10));
          results.push(LightZone.getTraceId()!);
        });

        results.push(LightZone.getTraceId()!);
      });

      expect(results).toEqual([outer, inner, outer]);
    });

    test('Promise.all에서 컨텍스트 유지', async () => {
      LightZone.init();

      const traceId = 'parallel-123';
      const results: string[] = [];

      await LightZone.runAsync({ traceId }, async () => {
        await Promise.all([
          (async () => {
            await new Promise((resolve) => setTimeout(resolve, 10));
            results.push(LightZone.getTraceId()!);
          })(),
          (async () => {
            await new Promise((resolve) => setTimeout(resolve, 20));
            results.push(LightZone.getTraceId()!);
          })(),
        ]);
      });

      expect(results).toEqual([traceId, traceId]);
    });

    test('비동기 예외 발생 시 스택 정리', async () => {
      LightZone.init();

      await expect(
        LightZone.runAsync({ traceId: 'test' }, async () => {
          throw new Error('async error');
        }),
      ).rejects.toThrow('async error');

      expect(LightZone.getDepth()).toBe(0);
    });
  });

  describe('컨텍스트 조회', () => {
    test('Zone 밖에서는 null 반환', () => {
      LightZone.init();
      expect(LightZone.current()).toBeNull();
      expect(LightZone.getTraceId()).toBeNull();
    });

    test('Zone 안에서 current() 반환', () => {
      LightZone.init();

      const context = { traceId: 'test-123', userId: 'user-456' };

      LightZone.run(context, () => {
        const current = LightZone.current();
        expect(current).toEqual(context);
        expect(current?.userId).toBe('user-456');
      });
    });

    test('getDepth() 정확도', () => {
      LightZone.init();

      expect(LightZone.getDepth()).toBe(0);

      LightZone.run({ traceId: 'outer' }, () => {
        expect(LightZone.getDepth()).toBe(1);

        LightZone.run({ traceId: 'inner' }, () => {
          expect(LightZone.getDepth()).toBe(2);
        });

        expect(LightZone.getDepth()).toBe(1);
      });

      expect(LightZone.getDepth()).toBe(0);
    });
  });

  describe('상태 조회', () => {
    test('getStatus() 정보 확인', () => {
      LightZone.init();

      const status1 = LightZone.getStatus();
      expect(status1).toEqual({
        isPatched: true,
        stackDepth: 0,
        currentTraceId: null,
      });

      LightZone.run({ traceId: 'test-123' }, () => {
        const status2 = LightZone.getStatus();
        expect(status2).toEqual({
          isPatched: true,
          stackDepth: 1,
          currentTraceId: 'test-123',
        });
      });
    });
  });
});
