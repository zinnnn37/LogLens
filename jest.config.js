// jest.config.js
module.exports = {
  preset: 'ts-jest',
  projects: [
    // Node 환경 테스트
    {
      displayName: 'node',
      testEnvironment: 'node',
      testMatch: [
        '<rootDir>/tests/*.test.ts',
        '<rootDir>/tests/core/**/*.test.ts',
        '<rootDir>/tests/wrappers/**/*.test.ts',
        '<rootDir>/tests/integration/**/*.test.ts',
      ],
      setupFiles: ['<rootDir>/tests/setup.ts'],
      transform: {
        '^.+\\.tsx?$': ['ts-jest', {
          tsconfig: {
            module: 'commonjs',
            esModuleInterop: true,
            allowSyntheticDefaultImports: true,
            moduleResolution: 'node',
            types: ['jest', 'node'],
            skipLibCheck: true,
            verbatimModuleSyntax: false,
            target: 'ES2020',
          }
        }]
      },
    },
    // React 환경 테스트 (jsdom)
    {
      displayName: 'react',
      testEnvironment: 'jsdom',
      testMatch: ['<rootDir>/tests/react/**/*.test.ts'],
      setupFiles: ['<rootDir>/tests/setup.ts'],
      transform: {
        '^.+\\.tsx?$': ['ts-jest', {
          tsconfig: {
            module: 'commonjs',
            esModuleInterop: true,
            allowSyntheticDefaultImports: true,
            moduleResolution: 'node',
            types: ['jest', 'node'],
            skipLibCheck: true,
            verbatimModuleSyntax: false,
            target: 'ES2020',
          }
        }]
      },
    },
  ],
};