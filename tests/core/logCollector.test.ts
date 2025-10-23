// tests/core/logCollector.test.ts
import { LogCollector } from '../../src/core/logCollector';
import type { LogEntry, CollectorConfig } from '../../src/core/types';

global.fetch = jest.fn();

describe('LogCollector', () => {
  let consoleLogSpy: jest.SpyInstance;
  let consoleWarnSpy: jest.SpyInstance;
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    LogCollector.clear();
    LogCollector.stopAutoFlush();
    LogCollector.init({ maxLogs: 1000 }); // 매번 초기화
    jest.clearAllMocks();

    consoleLogSpy = jest.spyOn(console, 'log').mockImplementation();
    consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
  });

  afterEach(() => {
    consoleLogSpy.mockRestore();
    consoleWarnSpy.mockRestore();
    consoleErrorSpy.mockRestore();
  });

  const createMockLog = (overrides?: Partial<LogEntry>): LogEntry => ({
    timestamp: new Date().toISOString(),
    traceId: 'test-trace-id',
    logLevel: 'INFO',
    sourceType: 'FRONT',
    comment: 'test message',
    methodName: null,
    className: null,
    stackTrace: null,
    requestData: null,
    responseData: null,
    executionTime: null,
    additionalInfo: null,
    ...overrides,
  });

  describe('init', () => {
    test('설정을 초기화한다', () => {
      const config: Partial<CollectorConfig> = {
        maxLogs: 500,
      };
      LogCollector.init(config);
      const status = LogCollector.getStatus();
      expect(status.maxLogs).toBe(500);
    });

    test('null로 초기화하면 기본값을 사용한다', () => {
      LogCollector.init(null);
      const status = LogCollector.getStatus();
      expect(status.maxLogs).toBe(1000);
      expect(status.autoFlushEnabled).toBe(false);
    });
  });

  describe('addLog', () => {
    test('로그를 추가한다', () => {
      const log = createMockLog();
      LogCollector.addLog(log);
      expect(LogCollector.getLogs()).toHaveLength(1);
      expect(LogCollector.getLogs()[0]).toEqual(log);
    });

    test('maxLogs를 초과하면 오래된 로그를 제거한다', () => {
      LogCollector.init({ maxLogs: 2 });
      LogCollector.addLog(createMockLog({ comment: 'log1' }));
      LogCollector.addLog(createMockLog({ comment: 'log2' }));
      LogCollector.addLog(createMockLog({ comment: 'log3' }));

      const logs = LogCollector.getLogs();
      expect(logs).toHaveLength(2);
      expect(logs[0].comment).toBe('log2');
      expect(logs[1].comment).toBe('log3');
    });

    test('콘솔에 로그를 출력한다', () => {
      LogCollector.addLog(createMockLog());
      expect(consoleLogSpy).toHaveBeenCalled();
    });
  });

  describe('getLogs', () => {
    test('모든 로그를 반환한다', () => {
      LogCollector.addLog(createMockLog({ comment: 'log1' }));
      LogCollector.addLog(createMockLog({ comment: 'log2' }));
      expect(LogCollector.getLogs()).toHaveLength(2);
    });

    test('원본 배열의 복사본을 반환한다', () => {
      LogCollector.addLog(createMockLog());
      const logs = LogCollector.getLogs();
      logs.push(createMockLog());
      expect(LogCollector.getLogs()).toHaveLength(1);
    });
  });

  describe('getLogsByTraceId', () => {
    test('특정 traceId의 로그만 반환한다', () => {
      LogCollector.addLog(createMockLog({ traceId: 'trace-1' }));
      LogCollector.addLog(createMockLog({ traceId: 'trace-2' }));
      LogCollector.addLog(createMockLog({ traceId: 'trace-1' }));

      const logs = LogCollector.getLogsByTraceId('trace-1');
      expect(logs).toHaveLength(2);
      logs.forEach((log) => expect(log.traceId).toBe('trace-1'));
    });

    test('해당하는 로그가 없으면 빈 배열을 반환한다', () => {
      const logs = LogCollector.getLogsByTraceId('non-existent');
      expect(logs).toEqual([]);
    });
  });

  describe('flush', () => {
    test('로그가 없으면 전송하지 않는다', async () => {
      await LogCollector.flush();
      expect(global.fetch).not.toHaveBeenCalled();
    });

    test('endpoint가 없으면 경고를 출력한다', async () => {
      LogCollector.addLog(createMockLog());
      await LogCollector.flush();
      expect(consoleWarnSpy).toHaveBeenCalledWith(
        expect.stringContaining('No endpoint configured'),
      );
    });

    test('로그를 서버로 전송한다', async () => {
      LogCollector.init({
        autoFlush: {
          enabled: false,
          interval: 60000,
          endpoint: 'https://api.example.com/logs',
        },
      });

      (global.fetch as jest.Mock).mockResolvedValue({
        ok: true,
        status: 200,
      });

      LogCollector.addLog(createMockLog());
      await LogCollector.flush();

      expect(global.fetch).toHaveBeenCalledWith(
        'https://api.example.com/logs',
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    });

    test('전송 실패 시 로그를 복원한다', async () => {
      LogCollector.init({
        autoFlush: {
          enabled: false,
          interval: 60000,
          endpoint: 'https://api.example.com/logs',
        },
      });

      (global.fetch as jest.Mock).mockRejectedValue(new Error('Network error'));

      LogCollector.addLog(createMockLog());
      await LogCollector.flush();

      expect(LogCollector.getLogs()).toHaveLength(1);
    });
  });

  describe('clear', () => {
    test('모든 로그를 제거한다', () => {
      LogCollector.addLog(createMockLog());
      LogCollector.addLog(createMockLog());
      LogCollector.clear();
      expect(LogCollector.getLogs()).toHaveLength(0);
    });
  });

  describe('autoFlush', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    test('자동 전송이 활성화되면 주기적으로 flush를 호출한다', () => {
      const flushSpy = jest.spyOn(LogCollector, 'flush').mockImplementation();

      LogCollector.init({
        autoFlush: {
          enabled: true,
          interval: 1000,
          endpoint: 'https://api.example.com/logs',
        },
      });

      jest.advanceTimersByTime(1000);
      expect(flushSpy).toHaveBeenCalledTimes(1);

      jest.advanceTimersByTime(1000);
      expect(flushSpy).toHaveBeenCalledTimes(2);

      flushSpy.mockRestore();
    });

    test('stopAutoFlush 호출 시 자동 전송을 중지한다', () => {
      const flushSpy = jest.spyOn(LogCollector, 'flush').mockImplementation();

      LogCollector.init({
        autoFlush: {
          enabled: true,
          interval: 1000,
          endpoint: 'https://api.example.com/logs',
        },
      });

      LogCollector.stopAutoFlush();
      jest.advanceTimersByTime(2000);

      expect(flushSpy).not.toHaveBeenCalled();
      flushSpy.mockRestore();
    });
  });
});
