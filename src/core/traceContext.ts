// src/core/traceContext.ts

class TraceContext {
  // 중첩 호출인 경우 동일한 traceId를 사용하게 하기 위함
  private static stack: string[] = [];
  private static isInitialized = false;

  /**
   * 자동 정리 초기화 (브라우저 환경에서만)
   */
  private static initialize(): void {
    if (this.isInitialized) return;
    if (typeof window === 'undefined') return;

    // 뒤로가기/앞으로가기
    window.addEventListener('popstate', () => {
      console.log('[TraceContext] Navigation detected - resetting stack');
      this.reset();
    });

    // pushState 감지 (SPA 페이지 이동)
    const originalPushState = history.pushState;
    history.pushState = function (...args) {
      TraceContext.reset();
      return originalPushState.apply(this, args);
    };

    // replaceState 감지
    const originalReplaceState = history.replaceState;
    history.replaceState = function (...args) {
      TraceContext.reset();
      return originalReplaceState.apply(this, args);
    };

    // 페이지 언로드
    window.addEventListener('beforeunload', () => {
      this.reset();
    });

    this.isInitialized = true;
    console.log('[TraceContext] Navigation tracking initialized');
  }

  /**
   * Trace 시작
   * - stack이 비어있으면 새 TraceID 생성
   * - stack에 값이 있으면 최상단 TraceID 재사용 - 중첩 호출 핸들링
   */
  static startTrace(explicitId?: string): string {
    // 첫 사용 시 자동 초기화
    if (!this.isInitialized) {
      this.initialize();
    }

    if (explicitId) {
      this.stack.push(explicitId);
      return explicitId;
    }

    if (this.stack.length === 0) {
      const traceId = this.generateTraceId();
      this.stack.push(traceId);
      return traceId;
    }

    const traceId = this.stack[this.stack.length - 1];
    this.stack.push(traceId);
    return traceId;
  }

  /**
   * Trace 종료
   */
  static endTrace(): void {
    this.stack.pop();
  }

  /**
   * 현재 활성 TraceID 조회
   */
  static getCurrentTraceId(): string | null {
    if (this.stack.length === 0) return null;
    return this.stack[this.stack.length - 1];
  }

  /**
   * 모든 Trace 초기화
   */
  static reset(): void {
    this.stack = [];
  }

  /**
   * 현재 스택 깊이 조회
   */
  static getDepth(): number {
    return this.stack.length;
  }

  /**
   * TraceID 생성
   */
  private static generateTraceId(): string {
    return crypto.randomUUID();
  }
}

export default TraceContext;
