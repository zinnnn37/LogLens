// src/react/types.ts

import type { LogLevel } from './logTypes';

export type UseLogLensOptions = {
  logger?: string;
  level?: LogLevel;
  includeResult?: boolean;
  context?: Record<string, any>;
};
