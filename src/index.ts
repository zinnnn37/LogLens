// src/index.ts

export { useLogger } from './react/useLogger';
import { LightZone } from './core/lightZone';
import { ErrorCapture } from './core/errorCapture';
import { loglens } from './core/logger';
import { withLogLens } from './wrappers/trace';
import type { CollectorConfig } from './types/logTypes.d';
import { LogCollector } from './core/logCollector';

export type InitLogLensConfig = {
  logCollector?: Partial<CollectorConfig> | null;
  captureErrors?: boolean;
};

const initLogLens = (config?: InitLogLensConfig): void => {
  // 컨텍스트 전파
  LightZone.init();

  // 로그 수집기 초기화
  LogCollector.init(config?.logCollector || null);

  // 에러 캡처링 활성화
  if (config?.captureErrors) {
    ErrorCapture.init();
  }
};

export { useLogLens } from './react/useLogLens';
export { initLogLens, withLogLens, LightZone, ErrorCapture, loglens };
