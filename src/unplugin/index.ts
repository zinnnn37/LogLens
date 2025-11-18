// unplugin/index.ts

import { createUnplugin } from 'unplugin';
import * as babel from '@babel/core';
import babelPluginLoglens from '../babel/index';

export interface LogLensPluginOptions {
  include?: string | RegExp | (string | RegExp)[];
  exclude?: string | RegExp | (string | RegExp)[];
  sourcemap?: boolean;
}

export const unplugin = createUnplugin(
  (options: LogLensPluginOptions = {}) => ({
    name: 'unplugin-loglens',

    transformInclude(id) {
      // node_modules 제외
      if (id.includes('node_modules')) return false;

      // .ts, .tsx, .js, .jsx 파일만 처리
      if (!/\.[jt]sx?$/.test(id)) return false;

      // 사용자 정의 exclude
      if (options.exclude) {
        const excludePatterns = Array.isArray(options.exclude)
          ? options.exclude
          : [options.exclude];

        for (const pattern of excludePatterns) {
          if (typeof pattern === 'string' && id.includes(pattern)) {
            return false;
          }
          if (pattern instanceof RegExp && pattern.test(id)) {
            return false;
          }
        }
      }

      // 사용자 정의 include (지정된 경우)
      if (options.include) {
        const includePatterns = Array.isArray(options.include)
          ? options.include
          : [options.include];

        for (const pattern of includePatterns) {
          if (typeof pattern === 'string' && id.includes(pattern)) {
            return true;
          }
          if (pattern instanceof RegExp && pattern.test(id)) {
            return true;
          }
        }
        return false;
      }

      return true;
    },

    transform(code, id) {
      try {
        const result = babel.transformSync(code, {
          filename: id,
          plugins: [babelPluginLoglens],
          sourceMaps: options.sourcemap !== false,
          configFile: false,
          babelrc: false,
        });

        if (!result || !result.code) {
          return null;
        }

        return {
          code: result.code,
          map: result.map || null,
        };
      } catch (error) {
        console.error(`[LogLens] Failed to transform ${id}:`, error);
        return null;
      }
    },
  }),
);

// 각 번들러용 export
export const vite = unplugin.vite;
export const rollup = unplugin.rollup;
export const webpack = unplugin.webpack;
export const esbuild = unplugin.esbuild;

// default export (vite)
export default vite;
