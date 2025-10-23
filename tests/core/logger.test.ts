// tests/core/logger.test.ts

import { loglens } from '../../src/core/logger';
import { LogCollector } from '../../src/core/logCollector';
import TraceContext from '../../src/core/traceContext';

describe('loglens', () => {
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

  describe('info', () => {
    test('INFO 레벨 로그를 생성한다', () => {
      loglens.info('테스트 메시지');

      const logs = LogCollector.getLogs();
      expect(logs).toHaveLength(1);
      expect(logs[0].logLevel).toBe('INFO');
      expect(logs[0].comment).toBe('테스트 메시지');
      expect(logs[0].sourceType).toBe('FRONT');
    });

    test('context를 추가 정보로 기록한다', () => {
      const context = { userId: '123', action: 'login' };
      loglens.info('로그인 시도', context);

      const logs = LogCollector.getLogs();
      expect(logs[0].additionalInfo).toEqual(context);
    });

    test('현재 TraceID를 사용한다', () => {
      const traceId = TraceContext.startTrace();
      loglens.info('메시지');

      const logs = LogCollector.getLogs();
      expect(logs[0].traceId).toBe(traceId);

      TraceContext.endTrace();
    });

    test('TraceID가 없으면 null로 기록한다', () => {
      loglens.info('메시지');

      const logs = LogCollector.getLogs();
      expect(logs[0].traceId).toBeNull();
    });

    test('timestamp를 ISO 형식으로 기록한다', () => {
      loglens.info('메시지');

      const logs = LogCollector.getLogs();
      expect(logs[0].timestamp).toMatch(
        /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/,
      );
    });
  });

  describe('warn', () => {
    test('WARN 레벨 로그를 생성한다', () => {
      loglens.warn('경고 메시지');

      const logs = LogCollector.getLogs();
      expect(logs).toHaveLength(1);
      expect(logs[0].logLevel).toBe('WARN');
      expect(logs[0].comment).toBe('경고 메시지');
      expect(logs[0].sourceType).toBe('FRONT');
    });

    test('context를 추가 정보로 기록한다', () => {
      const context = { code: 'DEPRECATED_API' };
      loglens.warn('API 경고', context);

      const logs = LogCollector.getLogs();
      expect(logs[0].additionalInfo).toEqual(context);
    });

    test('현재 TraceID를 사용한다', () => {
      const traceId = TraceContext.startTrace();
      loglens.warn('경고');

      const logs = LogCollector.getLogs();
      expect(logs[0].traceId).toBe(traceId);

      TraceContext.endTrace();
    });
  });

  describe('error', () => {
    test('ERROR 레벨 로그를 생성한다', () => {
      loglens.error('에러 메시지');

      const logs = LogCollector.getLogs();
      expect(logs).toHaveLength(1);
      expect(logs[0].logLevel).toBe('ERROR');
      expect(logs[0].comment).toBe('에러 메시지');
      expect(logs[0].sourceType).toBe('FRONT');
    });

    test('Error 객체의 stack trace를 기록한다', () => {
      const error = new Error('테스트 에러');
      loglens.error('에러 발생', error);

      const logs = LogCollector.getLogs();
      expect(logs[0].stackTrace).toBeDefined();
      expect(logs[0].stackTrace).toContain('Error: 테스트 에러');
    });

    test('Error 없이도 로그를 생성한다', () => {
      loglens.error('에러 메시지');

      const logs = LogCollector.getLogs();
      expect(logs[0].stackTrace).toBeNull();
    });

    test('현재 TraceID를 사용한다', () => {
      const traceId = TraceContext.startTrace();
      loglens.error('에러');

      const logs = LogCollector.getLogs();
      expect(logs[0].traceId).toBe(traceId);

      TraceContext.endTrace();
    });
  });

  describe('모든 로그 타입', () => {
    test('여러 로그를 순서대로 기록한다', () => {
      loglens.info('첫 번째');
      loglens.warn('두 번째');
      loglens.error('세 번째');

      const logs = LogCollector.getLogs();
      expect(logs).toHaveLength(3);
      expect(logs[0].comment).toBe('첫 번째');
      expect(logs[1].comment).toBe('두 번째');
      expect(logs[2].comment).toBe('세 번째');
    });

    test('모든 로그의 필수 필드가 null로 초기화된다', () => {
      loglens.info('테스트');

      const logs = LogCollector.getLogs();
      expect(logs[0].methodName).toBeNull();
      expect(logs[0].className).toBeNull();
      expect(logs[0].requestData).toBeNull();
      expect(logs[0].responseData).toBeNull();
      expect(logs[0].executionTime).toBeNull();
    });
  });
});
