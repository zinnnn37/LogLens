// src/core/logCollector.ts

import { LogFormatter } from '../utils/logFormatter';
import type { LogEntry, CollectorConfig } from '../types/logTypes';
import { loglens } from './logger';
import { getClientIp } from '../utils/ip';
import { DataMasker } from './dataMasker';

class LogCollector {
  private static masker: DataMasker;
  private static logs: LogEntry[] = [];
  private static config: CollectorConfig = {
    maxLogs: 1000,
    autoFlush: {
      enabled: true,
      interval: 30000,
      endpoint: '',
    },
    isProduction: false,
  };
  private static flushTimer: ReturnType<typeof setInterval> | null = null;

  private static isSending = false; // 전송 중 상태 플래그

  /**
   * 설정 초기화
   */
  static async init(config: Partial<CollectorConfig> | null): Promise<void> {
    this.masker = await DataMasker.initialize();

    if (config === null) {
      this.config = {
        maxLogs: 1000,
        autoFlush: {
          enabled: false,
          interval: 30000,
          endpoint: '',
        },
      };
    } else {
      this.config = { ...this.config, ...config };
    }

    if (this.config.autoFlush?.enabled) {
      this.startAutoFlush();
    }
  }

  /**
   * 로그 추가
   */
  static addLog(log: LogEntry): void {
    this.logs.push(log);

    // 개발 환경
    if (!this.config.isProduction) {
      console.log('[LogCollector] New log added:');
      const formatted = LogFormatter.toConsole(log);
      console.log(formatted);
    }
    // 에러 로그는 프로덕션이라도 콘솔에 출력
    else if (log.level === 'ERROR' && log.logger !== 'ErrorCapture') {
      console.error('[LogLens Error]', log.message);
      if (log.response) {
        console.error('Context:', log.response);
      }
    }

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
    return this.logs.filter((log) => log.traceId === traceId);
  }

  /**
   * 로그 전송
   */
  static async flush(): Promise<void> {
    if (this.logs.length === 0) {
      return;
    }

    if (!this.config.autoFlush?.enabled) {
      loglens.warn('[LogCollector] Auto flush is disabled');
      return;
    }

    const endpoint = this.config.autoFlush?.endpoint;
    if (!endpoint) {
      loglens.warn('[LogCollector] No endpoint configured for flush');
      return;
    }

    await this.sendLogs(endpoint);
  }

  /**
   * 수동 전송 (설정 무시하고 강제 전송)
   */
  static async send(): Promise<void> {
    if (this.logs.length === 0) {
      loglens.warn('[LogCollector] No logs to send');
      return;
    }

    const targetEndpoint = this.config.autoFlush?.endpoint;

    if (!targetEndpoint) {
      loglens.warn('[LogCollector] No endpoint provided for manual send');
      return;
    }

    await this.sendLogs(targetEndpoint);
  }

  /**
   * 로그 전송 로직
   */
  private static async sendLogs(endpoint: string): Promise<void> {
    // 전송 중이면 대기
    if (this.isSending) {
      if (!this.config.isProduction) {
        console.warn('[LogCollector] Already sending logs');
      }
      return;
    }

    this.isSending = true;
    const logsToSend = [...this.logs];
    this.logs = [];
    const maskedLogs = logsToSend.map((log) => this.masker.mask(log));
    const ipAddress = await getClientIp();

    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'LogLens-IP': ipAddress || '',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ logs: maskedLogs }),
      });

      if (!response.ok) {
        throw new Error(`Failed to send logs: ${response.status}`);
      }

      if (!this.config.isProduction) {
        console.log(
          `[LogCollector] Successfully sent ${logsToSend.length} logs`,
        );
      }
    } catch (error) {
      console.error('[LogCollector] Failed to send logs:', error);
      // 실패 시 앞에 다시 추가
      this.logs.unshift(...logsToSend);
    } finally {
      this.isSending = false;
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

    if (!this.config.isProduction) {
      console.log(
        `[LogCollector] Auto flush started (interval: ${interval}ms)`,
      );
    }
  }

  /**
   * 자동 전송 중지
   */
  static stopAutoFlush(): void {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
      this.flushTimer = null;
      if (!this.config.isProduction) {
        console.log('[LogCollector] Auto flush stopped');
      }
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

export { LogCollector };
