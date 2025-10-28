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
// src/core/lightZone.ts - 완전 재작성

class LightZone {
  private static stack: ZoneContext[] = [];
  private static isPatched = false;
  private static OriginalPromise: PromiseConstructor | null = null;

  static init(): void {
    if (this.isPatched) {
      console.warn('[LogLens] Already initialized');
      return;
    }

    this.OriginalPromise = Promise;
    this.patchPromise();
    this.isPatched = true;
    console.log('[LogLens] Initialized - Auto trace propagation enabled');
  }

  static isEnabled(): boolean {
    return this.isPatched;
  }

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

  static run<T>(context: ZoneContext, fn: () => T): T {
    this.stack.push(context);

    try {
      return fn();
    } finally {
      this.stack.pop();
    }
  }

  static current(): ZoneContext | null {
    return this.stack[this.stack.length - 1] || null;
  }

  static getTraceId(): string | null {
    return this.current()?.traceId || null;
  }

  static getDepth(): number {
    return this.stack.length;
  }

  static reset(): void {
    this.stack = [];
    this.isPatched = false;
  }

  private static patchPromise(): void {
    if (!this.OriginalPromise) {
      throw new Error('[LogLens] OriginalPromise not initialized');
    }

    const OriginalPromise = this.OriginalPromise!;
    const zone = this;

    // Promise 생성자 패치
    class ZonedPromise<T> extends OriginalPromise<T> {
      constructor(
        executor: (
          resolve: (value: T | PromiseLike<T>) => void,
          reject: (reason?: any) => void,
        ) => void,
      ) {
        const capturedContext = zone.current();

        super((resolve, reject) => {
          executor(
            (value: T | PromiseLike<T>) => {
              // 컨텍스트 복원
              if (capturedContext && zone.current() !== capturedContext) {
                zone.stack.push(capturedContext);
                try {
                  resolve(value);
                } finally {
                  zone.stack.pop();
                }
              } else {
                resolve(value);
              }
            },
            (reason?: any) => {
              // 컨텍스트 복원
              if (capturedContext && zone.current() !== capturedContext) {
                zone.stack.push(capturedContext);
                try {
                  reject(reason);
                } finally {
                  zone.stack.pop();
                }
              } else {
                reject(reason);
              }
            },
          );
        });
      }

      then<TResult1 = T, TResult2 = never>(
        onfulfilled?:
          | ((value: T) => TResult1 | PromiseLike<TResult1>)
          | null
          | undefined,
        onrejected?:
          | ((reason: any) => TResult2 | PromiseLike<TResult2>)
          | null
          | undefined,
      ): Promise<TResult1 | TResult2> {
        const capturedContext = zone.current();

        return super.then(
          onfulfilled
            ? (value: T) => {
                // 컨텍스트 복원
                if (capturedContext && zone.current() !== capturedContext) {
                  zone.stack.push(capturedContext);
                  try {
                    return onfulfilled(value);
                  } finally {
                    zone.stack.pop();
                  }
                }
                return onfulfilled(value);
              }
            : undefined,
          onrejected
            ? (reason: any) => {
                // 컨텍스트 복원
                if (capturedContext && zone.current() !== capturedContext) {
                  zone.stack.push(capturedContext);
                  try {
                    return onrejected(reason);
                  } finally {
                    zone.stack.pop();
                  }
                }
                return onrejected(reason);
              }
            : undefined,
        ) as Promise<TResult1 | TResult2>;
      }
    }

    // 전역 Promise 교체
    (globalThis as any).Promise = ZonedPromise;

    // 정적 메서드들
    ZonedPromise.resolve = function <T>(
      value?: T | PromiseLike<T>,
    ): Promise<T> {
      return new ZonedPromise((resolve) => resolve(value as T));
    };

    ZonedPromise.reject = function <T = never>(reason?: any): Promise<T> {
      return new ZonedPromise((_, reject) => reject(reason));
    };

    ZonedPromise.all = function <T>(
      values: Iterable<T | PromiseLike<T>>,
    ): Promise<Awaited<T>[]> {
      return new ZonedPromise((resolve, reject) => {
        OriginalPromise.all(values).then(resolve, reject);
      }) as Promise<Awaited<T>[]>;
    };

    ZonedPromise.race = function <T>(
      values: Iterable<T | PromiseLike<T>>,
    ): Promise<Awaited<T>> {
      return new ZonedPromise((resolve, reject) => {
        OriginalPromise.race(values).then(resolve, reject);
      }) as Promise<Awaited<T>>;
    };

    ZonedPromise.allSettled = function <T>(
      values: Iterable<T | PromiseLike<T>>,
    ): Promise<PromiseSettledResult<Awaited<T>>[]> {
      return new ZonedPromise((resolve) => {
        OriginalPromise.allSettled(values).then(resolve);
      }) as Promise<PromiseSettledResult<Awaited<T>>[]>;
    };

    if ((OriginalPromise as any).any) {
      (ZonedPromise as any).any = function <T>(
        values: Iterable<T | PromiseLike<T>>,
      ): Promise<Awaited<T>> {
        return new ZonedPromise((resolve, reject) => {
          (OriginalPromise as any).any(values).then(resolve, reject);
        });
      };
    }

    console.log('[LogLens] Promise patched - TraceId auto-propagation active');
  }

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
