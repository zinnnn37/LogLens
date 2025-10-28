// src/utils/logFormatter.ts

import type { LogEntry } from '../core/types';
import TraceContext from '../core/traceContext';

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
    const depth: number = Math.max(0, TraceContext.getDepth() - 1);
    const indent: string = '|'.repeat(depth);
    const arrow: string = '→';

    const color: string = this.COLORS[logEntry.logLevel];
    const reset: string = this.COLORS.RESET;
    const gray: string = this.COLORS.GRAY;

    // 로그 레벨 우측 정렬
    const level = `${color}[${logEntry.logLevel.padStart(
      this.LEVEL_WIDTH,
    )}]${reset}`;

    // Duration 색상
    const durationColor: string | null = this.getDurationColor(
      logEntry.executionTime,
    );
    const duration =
      logEntry.executionTime !== null
        ? `${durationColor}(${logEntry.executionTime
            .toString()
            .padStart(5)}ms)${reset}`
        : `${gray}(    - )${reset}`;

    // methodName이 없으면 comment 사용
    const displayName = logEntry.methodName || logEntry.comment || 'anonymous';
    const message = logEntry.comment
      ? `${displayName}: ${gray}${logEntry.comment}${reset}`
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
