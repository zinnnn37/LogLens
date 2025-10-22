// src/core/logCollector.ts

import type { LogEntry, CollectorConfig } from './types';

class LogCollector {
  private static logs: LogEntry[] = [];
  private static config: CollectorConfig = {
    maxLogs: 1000,
    autoFlush: {
      enabled: false,
      interval: 60000,
      endpoint: '',
    },
  };
  private static flushTimer: ReturnType<typeof setInterval> | null = null;

  /**
   * 설정 초기화
   */
  static init(config: Partial<CollectorConfig>): void {
    this.config = { ...this.config, ...config };

    if (this.config.autoFlush?.enabled) {
      this.startAutoFlush();
    }
  }

  /**
   * 로그 추가
   */
  static addLog(log: LogEntry): void {
    this.logs.push(log);

    if (this.logs.length > (this.config.maxLogs || 1000)) {
      this.logs.shift();
    }
  }

  /**
   * 모든 로그 조회
   */
  static getLogs(): LogEntry[] {
    return [...this.logs];
  }

  /**
   * 특정 TraceID의 로그만 조회
   */
  static getLogsByTraceId(traceId: string): LogEntry[] {
    return this.logs.filter((log) => log.trace_id === traceId);
  }

  /**
   * 로그 전송
   */
  static async flush(): Promise<void> {
    if (this.logs.length === 0) {
      return;
    }

    const endpoint = this.config.autoFlush?.endpoint;
    if (!endpoint) {
      console.warn('[LogCollector] No endpoint configured for flush');
      return;
    }

    const logsToSend = [...this.logs];
    this.logs = [];

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ logs: logsToSend }),
      });

      if (!response.ok) {
        throw new Error(`Failed to send logs: ${response.status}`);
      }

      console.log(`[LogCollector] Successfully sent ${logsToSend.length} logs`);
    } catch (error) {
      console.error('[LogCollector] Failed to flush logs:', error);
      this.logs.unshift(...logsToSend);
    }
  }

  /**
   * 로그 초기화
   */
  static clear(): void {
    this.logs = [];
  }

  /**
   * 자동 전송 시작
   */
  private static startAutoFlush(): void {
    if (this.flushTimer) {
      return;
    }

    const interval = this.config.autoFlush?.interval || 60000;
    this.flushTimer = setInterval(() => {
      this.flush();
    }, interval);

    console.log(`[LogCollector] Auto flush started (interval: ${interval}ms)`);
  }

  /**
   * 자동 전송 중지
   */
  static stopAutoFlush(): void {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
      console.log('[LogCollector] Auto flush stopped');
    }
  }

  /**
   * 현재 상태 조회 (디버깅용)
   */
  static getStatus() {
    return {
      logCount: this.logs.length,
      maxLogs: this.config.maxLogs,
      autoFlushEnabled: this.config.autoFlush?.enabled || false,
      endpoint: this.config.autoFlush?.endpoint || 'not configured',
    };
  }
}

export default LogCollector;
