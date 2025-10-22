// eslint.config.ts
import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';
import noRelativeImportPaths from 'eslint-plugin-no-relative-import-paths';
import reactPlugin from 'eslint-plugin-react';
import importPlugin from 'eslint-plugin-import';

export default tseslint.config([
  // === 글로벌 ignore: 빌드 산출물/외부만 무시 ===
  { ignores: ['dist', 'node_modules'] },

  {
    files: ['**/*.{ts,tsx}'],

    // 베이스 확장
    extends: [
      // JS 추천 규칙 (코어). TS와 중복될 수 있어 일부 코어 규칙은 아래에서 끕니다.
      js.configs.recommended,
      // TS 추천 규칙 (non type-checked)
      ...tseslint.configs.recommended,
    ],

    plugins: {
      'no-relative-import-paths': noRelativeImportPaths,
      'react': reactPlugin,
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
      'import': importPlugin,
    },

    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
      },
      parserOptions: {
        ecmaFeatures: { jsx: true },
        // 타입기반 규칙을 원하면 projectService 사용 (tseslint v8+)
        // projectService: true,
      },
    },

    settings: {
      'react': { version: 'detect' },
      // alias/TS 경로 인식을 위한 resolver
      // ※ eslint-import-resolver-typescript 설치 필요
      // 'import/resolver': {
      //   typescript: {
      //     project: true,
      //   },
      //   node: true,
      // },
    },

    rules: {
      // === 충돌/중복 방지 ===
      // TS 전용 규칙 사용을 위해 코어 no-unused-vars 비활성
      'no-unused-vars': 'off',

      // === TypeScript 컨벤션 ===
      '@typescript-eslint/consistent-type-imports': [
        'error',
        { prefer: 'type-imports' },
      ],
      '@typescript-eslint/array-type': ['error', { default: 'array' }],
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          ignoreRestSiblings: true,
        },
      ],
      '@typescript-eslint/consistent-type-definitions': ['error', 'interface'],
      '@typescript-eslint/no-empty-function': 'off',

      // === React 컨벤션 ===
      'react/react-in-jsx-scope': 'off', // React 17+ 자동 JSX 변환
      'react/function-component-definition': [
        'error',
        {
          namedComponents: 'arrow-function',
          unnamedComponents: 'arrow-function',
        },
      ],
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
      'react-refresh/only-export-components': 'warn',
      'react/jsx-pascal-case': ['error', { allowAllCaps: false }],

      // === Import/Export & 경로 ===
      'no-relative-import-paths/no-relative-import-paths': [
        'error',
        {
          allowSameFolder: true,
          rootDir: 'src',
          prefix: '@/',
        },
      ],
      'import/newline-after-import': ['error', { count: 1 }],
      'import/no-duplicates': 'error',

      // === 일반 품질 규칙 ===
      'prefer-const': 'error',
      'no-var': 'error',
      'curly': ['error', 'all'],
      'prefer-arrow-callback': 'error',
      'func-style': ['error', 'expression'],
      'no-implicit-coercion': 'error',

      // enum 금지 → TS AST 셀렉터 사용
      'no-restricted-syntax': [
        'error',
        {
          selector: 'TSEnumDeclaration',
          message: 'enum 대신 as const 객체를 사용하세요.',
        },
      ],

      // TODO/FIXME 관리
      'no-warning-comments': [
        'warn',
        { terms: ['todo', 'fixme', 'bug'], location: 'start' },
      ],
    },
  },

  // === Node 환경 파일 (Vite 설정/스크립트 등) ===
  {
    files: [
      'vite.config.*',
      'vitest.config.*',
      'eslint.config.*',
      'scripts/**/*.{ts,tsx,js,jsx}',
      '*.config.{ts,js,cjs,mjs}',
    ],
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
    rules: {
      // 브라우저 전용 규칙이 간섭하지 않도록 필요한 경우 완화
    },
  },

  // === 세부 override ===
  { files: ['**/*.types.ts'], rules: {} },
  { files: ['src/vite-env.d.ts'], rules: {} },

  // shadcn UI 컴포넌트는 검사 제외
  {
    files: ['src/components/ui/**/*.{ts,tsx}'],
    rules: {
      'func-style': 'off',
      'react/function-component-definition': 'off',
      'react-refresh/only-export-components': 'off',
    },
  },
]);
