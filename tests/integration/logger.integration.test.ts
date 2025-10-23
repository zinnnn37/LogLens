// tests/integration/logger.integration.test.ts

import { withLogLens } from '../../src/wrappers/trace';
import { LogCollector } from '../../src/core/logCollector';
import { loglens } from '../../src/core/logger';
import TraceContext from '../../src/core/traceContext';

describe('Logger Integration', () => {
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

  test('전체 워크플로우가 정상 동작한다', async () => {
    const fetchUser = withLogLens(
      async (userId: string) => {
        loglens.info(`Fetching user ${userId}`);
        await new Promise((resolve) => setTimeout(resolve, 10));
        return { id: userId, name: 'John' };
      },
      { name: 'API.fetchUser', includeResult: true },
    );

    await fetchUser('123');

    const logs = LogCollector.getLogs();
    expect(logs.length).toBeGreaterThan(0);

    const traceIds = logs.map((log) => log.traceId).filter((id) => id !== null);
    expect(new Set(traceIds).size).toBe(1); // 모두 같은 traceId
  });

  test('중첩된 함수 호출이 올바르게 추적된다', () => {
    const level3 = withLogLens(() => 'L3', { name: 'Level3' });
    const level2 = withLogLens(() => level3(), { name: 'Level2' });
    const level1 = withLogLens(() => level2(), { name: 'Level1' });

    level1();

    const logs = LogCollector.getLogs();
    expect(logs).toHaveLength(3);

    // 모두 동일한 traceId
    const traceId = logs[0].traceId;
    logs.forEach((log) => expect(log.traceId).toBe(traceId));
  });
});
