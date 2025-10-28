// src/utils/auth.ts

/**
 * 환경변수에서 인증 토큰 가져오기
 * - Vite, Next.js, React, Vue, Node.js 등
 */
const getAuthToken = (): string | null => {
  // Vite 환경 (import.meta.env 체크 필요)
  if (typeof import.meta !== 'undefined' && import.meta.env?.VITE_AUTH_TOKEN) {
    return import.meta.env.VITE_AUTH_TOKEN;
  }

  // Node.js/브라우저 환경 (process 체크 필요)
  if (typeof process !== 'undefined' && process.env) {
    // React
    if (process.env.REACT_APP_AUTH_TOKEN) {
      return process.env.REACT_APP_AUTH_TOKEN;
    }
    // Vue
    if (process.env.VUE_APP_AUTH_TOKEN) {
      return process.env.VUE_APP_AUTH_TOKEN;
    }
    // Next.js
    if (process.env.NEXT_PUBLIC_AUTH_TOKEN) {
      return process.env.NEXT_PUBLIC_AUTH_TOKEN;
    }
    // Node.js
    if (process.env.AUTH_TOKEN) {
      return process.env.AUTH_TOKEN;
    }
  }

  return null;
};

export { getAuthToken };
