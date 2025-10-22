// src/react/types.ts

import type { LogLevel } from '../core/types';

export type UseLogLensOptions = {
  logger?: string;
  level?: LogLevel;
  includeResult?: boolean;
  context?: Record<string, any>;
};
