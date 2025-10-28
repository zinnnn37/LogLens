// src/utils/logFormatter.ts

import type { LogEntry } from '../core/types';
import { LightZone } from '../core/lightZone';

class LogFormatter {
  private static readonly COLORS = {
    INFO: '\x1b[30m', // black
    WARN: '\x1b[33m', // yellow
    ERROR: '\x1b[91m', // red
    RESET: '\x1b[0m',
    GRAY: '\x1b[90m',
    GREEN: '\x1b[32m',
    BOLD: '\x1b[1m',
  };

  private static readonly LEVEL_WIDTH = 5;

  static toConsole(logEntry: LogEntry): string {
    const depth: number = Math.max(0, LightZone.getDepth() - 1);
    const indent: string = '|'.repeat(depth);
    const arrow: string = '→';

    const color: string = this.COLORS[logEntry.level];
    const reset: string = this.COLORS.RESET;
    const gray: string = this.COLORS.GRAY;

    // 로그 레벨 우측 정렬
    const level = `${color}[${logEntry.level.padStart(
      this.LEVEL_WIDTH,
    )}]${reset}`;

    // Duration 색상
    const durationColor: string | null = this.getDurationColor(
      logEntry.executionTimeMs,
    );
    const duration =
      logEntry.executionTimeMs !== null
        ? `${durationColor}(${logEntry.executionTimeMs
            .toString()
            .padStart(5)}ms)${reset}`
        : '';

    // methodName이 없으면 comment 사용
    const displayName = logEntry.logger || logEntry.message || 'anonymous';
    const message = logEntry.message
      ? `${displayName}: ${gray}${logEntry.message}${reset}`
      : displayName;

    return `${level} ${indent}${arrow} ${message} ${duration}`;
  }

  /**
   * Duration 기반 색상 선택
   */
  private static getDurationColor(ms: number | null): string {
    if (ms === null) return this.COLORS.GRAY;
    if (ms > 1000) return this.COLORS.ERROR;
    if (ms > 100) return this.COLORS.WARN;
    return this.COLORS.GREEN;
  }
}

export { LogFormatter };
