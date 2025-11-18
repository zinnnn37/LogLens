export interface LogLensPluginOptions {
    include?: string | RegExp | (string | RegExp)[];
    exclude?: string | RegExp | (string | RegExp)[];
    sourcemap?: boolean;
}
export declare const unplugin: import("unplugin").UnpluginInstance<LogLensPluginOptions, boolean>;
export declare const vite: (options?: LogLensPluginOptions) => import("vite").Plugin<any> | import("vite").Plugin<any>[];
export declare const rollup: (options?: LogLensPluginOptions) => import("rollup").Plugin<any> | import("rollup").Plugin<any>[];
export declare const webpack: (options?: LogLensPluginOptions) => WebpackPluginInstance;
export declare const esbuild: (options?: LogLensPluginOptions) => import("esbuild").Plugin;
export default vite;
