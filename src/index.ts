// src/index.ts

import { LightZone } from './core/lightZone';
import { ErrorCapture } from './core/errorCapture';
import { loglens } from './core/logger';
import { withLogLens } from './wrappers/trace';
import { LogCollector } from './core/logCollector';

export { useLogger } from './react/useLogger';
export { useLogLens } from './react/useLogLens';
export {
  initLogLens,
  withLogLens,
  // LightZone,
  // ErrorCapture,
  loglens,
  // LogCollector,
};

type InitLogLensConfig = {
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

  const trimmedDomain = finalConfig.domain.trim();
  const domain = trimmedDomain.endsWith('/')
    ? trimmedDomain.slice(0, -1)
    : trimmedDomain;

  const endpoint = `${domain}/api/logs/frontend`;

  console.log('[LogLens] Log collection enabled. Endpoint:', endpoint);

  LogCollector.init({
    maxLogs: finalConfig.maxLogs,
    autoFlush: {
      enabled: finalConfig.autoFlushEnabled,
      interval: finalConfig.autoFlushInterval,
      endpoint: endpoint,
    },
  });

  if (finalConfig.captureErrors) {
    ErrorCapture.init();
  }
};
