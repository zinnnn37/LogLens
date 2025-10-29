import { LightZone } from './core/lightZone';
import { ErrorCapture } from './core/errorCapture';
import { loglens } from './core/logger';
import { withLogLens } from './wrappers/trace';
import { CollectorConfig } from './types/logTypes.d';
export { useLogger } from './react/useLogger';
export type InitLogLensConfig = {
    logCollector?: Partial<CollectorConfig> | null;
    captureErrors?: boolean;
};
declare const initLogLens: (config?: InitLogLensConfig) => void;
export { useLogLens } from './react/useLogLens';
export { initLogLens, withLogLens, LightZone, ErrorCapture, loglens };
