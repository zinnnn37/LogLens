// tests/setup.ts

declare const global: any;

global.crypto = {
  randomUUID: () => {
    return 'test-uuid-' + Math.random().toString(36).substring(2, 15);
  },
};
