// src/core/traceContext.ts

class TraceContext {
  // 중첩 호출인 경우 동일한 traceId를 사용하게 하기 위함
  private static stack: string[] = [];

  /**
   * Trace 시작
   * - stack이 비어있으면 새 TraceID 생성
   * - stack에 값이 있으면 최상단 TraceID 재사용 - 중첩 호출 핸들링
   */
  static startTrace(explicitId?: string): string {
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
