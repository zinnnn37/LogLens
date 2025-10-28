// src/core/lightZone.ts

/**
 * ZoneContext 타입 정의
 * 컨텍스트 저장 객체 타입
 * - traceId 필수
 * - 추가 동적 속성 허용
 */
type ZoneContext = {
  traceId: string;
  [key: string]: any; // 인덱스 시그니처
};

/**
 * 인덱스 시그니처
 * - ZoneContext 객체가 동적으로 여러 속성을 가질 수 있도록 허용
 *
 * ✅ 가능 - traceId만
 * const ctx1: ZoneContext = {
 *   traceId: 'abc-123'
 * };
 *
 * ✅ 가능 - 추가 필드들
 * const ctx2: ZoneContext = {
 *   traceId: 'abc-123',
 *   userId: 'user-456',
 *   requestId: 'req-789',
 *   sessionId: 'sess-999'
 * ;
 *
 * ✅ 가능 - 어떤 키든
 * const ctx3: ZoneContext = {
 *   traceId: 'abc-123',
 *   anything: 'value',
 *   whatever: 123,
 *   randomKey: true
 * };
 *
 * ❌ 불가능 - traceId 없음
 * const ctx4: ZoneContext = {
 *   userId: 'user-456'
 * };
 */

/**
 * LightZone 클래스
 * @description 비동기 컨텍스트 전파 및 관리
 * - Promise 기반 비동기 함수에서 traceId 등 컨텍스트 자동 전파
 * - Zone 스택 관리
 * - 앱 시작 시 init() 호출 필요
 *
 * @example
 * LightZone.init();
 *
 * const traceId = crypto.randomUUID();
 * LightZone.runAsync({ traceId }, async () => {
 *   비동기 작업 수행
 * });
 */
class LightZone {
  private static stack: ZoneContext[] = [];
  private static isPatched = false;

  /**
   * 초기화 - 앱 시작 시 한 번만 호출
   */
  static init(): void {
    if (this.isPatched) {
      console.warn('[LogLens] Already initialized');
      return;
    }

    this.patchPromise();
    this.isPatched = true;
    console.log('[LogLens] Initialized - Auto trace propagation enabled');
  }

  /**
   * 현재 활성화 여부
   */
  static isEnabled(): boolean {
    return this.isPatched;
  }

  /**
   * Zone 실행 (비동기)
   */
  static async runAsync<T>(
    context: ZoneContext,
    fn: () => Promise<T>,
  ): Promise<T> {
    this.stack.push(context);

    try {
      return await fn();
    } finally {
      this.stack.pop();
    }
  }

  /**
   * Zone 실행 (동기)
   */
  static run<T>(context: ZoneContext, fn: () => T): T {
    this.stack.push(context);

    try {
      return fn();
    } finally {
      this.stack.pop();
    }
  }

  /**
   * 현재 컨텍스트 조회
   */
  static current(): ZoneContext | null {
    return this.stack[this.stack.length - 1] || null;
  }

  /**
   * 현재 traceId만 조회
   */
  static getTraceId(): string | null {
    return this.current()?.traceId || null;
  }

  /**
   * 스택 깊이 조회
   */
  static getDepth(): number {
    return this.stack.length;
  }

  /**
   * 스택 초기화 (테스트용)
   */
  static reset(): void {
    this.stack = [];
  }

  /**
   * Promise 패치
   */
  private static patchPromise(): void {
    const OriginalPromise = Promise;
    const zone = this;

    // Promise 생성자 패치
    (window as any).Promise = class ZonedPromise<T> extends OriginalPromise<T> {
      constructor(
        executor: (
          resolve: (value: T | PromiseLike<T>) => void,
          reject: (reason?: any) => void,
        ) => void,
      ) {
        const capturedContext = zone.current();

        super((resolve, reject) => {
          const wrappedResolve = (value: T | PromiseLike<T>) => {
            if (capturedContext) {
              zone.stack.push(capturedContext);
            }
            try {
              resolve(value);
            } finally {
              if (capturedContext) {
                zone.stack.pop();
              }
            }
          };

          const wrappedReject = (reason?: any) => {
            if (capturedContext) {
              zone.stack.push(capturedContext);
            }
            try {
              reject(reason);
            } finally {
              if (capturedContext) {
                zone.stack.pop();
              }
            }
          };

          try {
            executor(wrappedResolve, wrappedReject);
          } catch (error) {
            wrappedReject(error);
          }
        });
      }
    };

    // Promise.resolve 패치
    (window as any).Promise.resolve = function <T>(
      value?: T | PromiseLike<T>,
    ): Promise<T> {
      return new (window as any).Promise((resolve: any) => resolve(value));
    };

    // Promise.reject 패치
    (window as any).Promise.reject = function <T = never>(
      reason?: any,
    ): Promise<T> {
      return new (window as any).Promise((_: any, reject: any) =>
        reject(reason),
      );
    };

    // Promise.all 패치 - 패치된 생성자를 거치게
    (window as any).Promise.all = function <T>(
      values: Iterable<T | PromiseLike<T>>,
    ): Promise<Awaited<T>[]> {
      return new (window as any).Promise((resolve: any, reject: any) => {
        OriginalPromise.all(values).then(resolve, reject);
      }) as Promise<Awaited<T>[]>;
    };

    // Promise.race 패치
    (window as any).Promise.race = function <T>(
      values: Iterable<T | PromiseLike<T>>,
    ): Promise<Awaited<T>> {
      return new (window as any).Promise((resolve: any, reject: any) => {
        OriginalPromise.race(values).then(resolve, reject);
      }) as Promise<Awaited<T>>;
    };

    // Promise.allSettled 패치
    (window as any).Promise.allSettled = function <T>(
      values: Iterable<T | PromiseLike<T>>,
    ): Promise<PromiseSettledResult<Awaited<T>>[]> {
      return new (window as any).Promise((resolve: any) => {
        OriginalPromise.allSettled(values).then(resolve);
      }) as Promise<PromiseSettledResult<Awaited<T>>[]>;
    };

    console.log('[LogLens] Promise patched - TraceId auto-propagation active');
  }

  /**
   * 상태 조회 (디버깅용)
   */
  static getStatus() {
    return {
      isPatched: this.isPatched,
      stackDepth: this.stack.length,
      currentTraceId: this.getTraceId(),
    };
  }
}

export { LightZone };
export type { ZoneContext };
