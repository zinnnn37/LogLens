// tests/integration.test.ts

import { withLogLens } from '../src/wrappers/trace';
import { LightZone } from '../src/core/lightZone';
import { LogCollector } from '../src/core/logCollector';
import { loglens } from '../src/core/logger';

describe('통합 테스트', () => {
  beforeEach(() => {
    LightZone.init();
    LogCollector.init(null);
    LogCollector.clear();
  });

  afterEach(() => {
    LightZone.reset();
    LogCollector.clear();
  });

  test('실제 시나리오: 사용자 로그인', async () => {
    // 내부 함수들 (래핑 없음)
    const validateUser = async (username: string) => {
      await new Promise((resolve) => setTimeout(resolve, 10));
      loglens.info('User validated', { username });
      return true;
    };

    const createSession = async (userId: string) => {
      await new Promise((resolve) => setTimeout(resolve, 10));
      loglens.info('Session created', { userId });
      return 'session-token-123';
    };

    // 최상위 함수 (래핑)
    const handleLogin = withLogLens(
      async (username: string, _password: string) => {
        loglens.info('Login started', { username });

        const isValid = await validateUser(username);
        if (!isValid) throw new Error('Invalid user');

        const token = await createSession(username);

        loglens.info('Login completed', { token });
        return token;
      },
      { logger: 'handleLogin' },
    );

    // 실행
    const token = await handleLogin('testuser', 'password123');

    // 검증
    expect(token).toBe('session-token-123');

    const logs = LogCollector.getLogs();
    const traceIds = logs.map((log) => log.traceId).filter(Boolean);

    // 모든 로그가 같은 traceId를 가져야 함
    expect(new Set(traceIds).size).toBe(1);

    // 로그 순서 확인
    expect(logs.map((l) => l.message)).toEqual([
      'handleLogin called',
      'Login started',
      'User validated',
      'Session created',
      'Login completed',
      'handleLogin completed',
    ]);
  });

  test('실제 시나리오: 병렬 API 호출', async () => {
    const fetchUser = async (id: string) => {
      await new Promise((resolve) => setTimeout(resolve, 20));
      loglens.info('User fetched', { id });
      return { id, name: 'User' + id };
    };

    const fetchPosts = async (userId: string) => {
      await new Promise((resolve) => setTimeout(resolve, 15));
      loglens.info('Posts fetched', { userId });
      return [{ id: '1', title: 'Post 1' }];
    };

    const fetchComments = async (userId: string) => {
      await new Promise((resolve) => setTimeout(resolve, 10));
      loglens.info('Comments fetched', { userId });
      return [{ id: '1', text: 'Comment 1' }];
    };

    const loadUserDashboard = withLogLens(
      async (userId: string) => {
        const [user, posts, comments] = await Promise.all([
          fetchUser(userId),
          fetchPosts(userId),
          fetchComments(userId),
        ]);

        return { user, posts, comments };
      },
      { logger: 'loadUserDashboard' },
    );

    const result = await loadUserDashboard('user-123');

    expect(result.user.name).toBe('Useruser-123');
    expect(result.posts.length).toBe(1);
    expect(result.comments.length).toBe(1);

    const logs = LogCollector.getLogs();
    const traceIds = logs.map((log) => log.traceId).filter(Boolean);

    // 모든 병렬 호출이 같은 traceId 공유
    expect(new Set(traceIds).size).toBe(1);
  });

  test('실제 시나리오: 에러 처리 및 복구', async () => {
    let attempt = 0;

    const unreliableApi = async () => {
      attempt++;
      await new Promise((resolve) => setTimeout(resolve, 10));

      if (attempt < 3) {
        loglens.error('API failed', new Error(`Attempt ${attempt} failed`));
        throw new Error('API Error');
      }

      loglens.info('API succeeded');
      return 'success';
    };

    const retryWrapper = withLogLens(
      async (maxRetries: number) => {
        for (let i = 0; i < maxRetries; i++) {
          try {
            return await unreliableApi();
          } catch (error) {
            loglens.warn(`Retry ${i + 1}/${maxRetries}`);
            if (i === maxRetries - 1) throw error;
          }
        }
      },
      { logger: 'retryWrapper' },
    );

    const result = await retryWrapper(3);

    expect(result).toBe('success');

    const logs = LogCollector.getLogs();
    const errorLogs = logs.filter((log) => log.level === 'ERROR');
    const warnLogs = logs.filter((log) => log.level === 'WARN');

    expect(errorLogs.length).toBe(2); // 2번 실패
    expect(warnLogs.length).toBe(2); // 2번 재시도
  });
});
