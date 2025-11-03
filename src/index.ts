// src/index.ts

export { useLogger } from './react/useLogger';
import { LightZone } from './core/lightZone';
import { ErrorCapture } from './core/errorCapture';
import { loglens } from './core/logger';
import { withLogLens } from './wrappers/trace';
import { LogCollector } from './core/logCollector';

export type InitLogLensConfig = {
  domain: string;
  maxLogs?: number;
  autoFlushInterval?: number;
  autoFlushEnabled?: boolean; // 수집 여부
  captureErrors?: boolean;
};

const DEFAULT_CONFIG = {
  maxLogs: 1000,
  autoFlushInterval: 60000,
  autoFlushEnabled: true,
  captureErrors: false,
};

const initLogLens = (config: InitLogLensConfig): void => {
  const finalConfig = {
    ...DEFAULT_CONFIG,
    ...config,
  };

  LightZone.init();

  // autoFlushEnabled가 false면 수집 안 함
  if (finalConfig.autoFlushEnabled) {
    let domain = finalConfig.domain.trim();
    if (domain.endsWith('/')) {
      domain = domain.slice(0, -1);
    }

    const endpoint = `${domain}/api/logs/frontend`;

    LogCollector.init({
      maxLogs: finalConfig.maxLogs,
      autoFlush: {
        enabled: true,
        interval: finalConfig.autoFlushInterval,
        endpoint: endpoint,
      },
    });
  } else {
    LogCollector.init(null); // 수집 비활성화
  }

  if (finalConfig.captureErrors) {
    ErrorCapture.init();
  }
};

export { useLogLens } from './react/useLogLens';
export { initLogLens, withLogLens, LightZone, ErrorCapture, loglens };
