// vite.config.ts
import { resolve } from 'path';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';

export default defineConfig({
  build: {
    outDir: 'dist',
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'soo1Loglens',
      formats: ['es', 'umd'],
      fileName: (format) => `index.${format}.js`,
    },
    rollupOptions: {
      // 번들에서 react 제외
      external: ['react', 'react-dom', 'react/jsx-runtime'],
      output: {
        // 이렇게 하면 require() 사용 시 default 객체 없이 바로 접근 가능
        exports: 'named',
        globals: {
          react: 'React',
          'react-dom': 'ReactDOM',
          'react/jsx-runtime': 'jsxRuntime',
        },
      },
    },
  },
  plugins: [
    dts({
      // 타입 정의 파일도 'dist' 폴더에 생성되도록 명시
      outDir: 'dist',
      insertTypesEntry: true,
    }),
  ],
});
