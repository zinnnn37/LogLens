"use strict";
// unplugin/index.ts
Object.defineProperty(exports, "__esModule", { value: true });
exports.esbuild = exports.webpack = exports.rollup = exports.vite = exports.unplugin = void 0;
var unplugin_1 = require("unplugin");
var babel = require("@babel/core");
var index_1 = require("../babel/index");
exports.unplugin = (0, unplugin_1.createUnplugin)(function (options) {
    if (options === void 0) { options = {}; }
    return ({
        name: 'unplugin-loglens',
        transformInclude: function (id) {
            // node_modules 제외
            if (id.includes('node_modules'))
                return false;
            // .ts, .tsx, .js, .jsx 파일만 처리
            if (!/\.[jt]sx?$/.test(id))
                return false;
            // 사용자 정의 exclude
            if (options.exclude) {
                var excludePatterns = Array.isArray(options.exclude)
                    ? options.exclude
                    : [options.exclude];
                for (var _i = 0, excludePatterns_1 = excludePatterns; _i < excludePatterns_1.length; _i++) {
                    var pattern = excludePatterns_1[_i];
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
                var includePatterns = Array.isArray(options.include)
                    ? options.include
                    : [options.include];
                for (var _a = 0, includePatterns_1 = includePatterns; _a < includePatterns_1.length; _a++) {
                    var pattern = includePatterns_1[_a];
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
        transform: function (code, id) {
            try {
                var result = babel.transformSync(code, {
                    filename: id,
                    plugins: [index_1.default],
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
            }
            catch (error) {
                console.error("[LogLens] Failed to transform ".concat(id, ":"), error);
                return null;
            }
        },
    });
});
// 각 번들러용 export
exports.vite = exports.unplugin.vite;
exports.rollup = exports.unplugin.rollup;
exports.webpack = exports.unplugin.webpack;
exports.esbuild = exports.unplugin.esbuild;
// default export (vite)
exports.default = exports.vite;
