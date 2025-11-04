// src/stores/authStore.ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export interface AuthUser {
  userId: number;
}

interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;

  setAuth: (payload: {
    accessToken: string | null;
    user: AuthUser | null;
  }) => void;

  setAccessToken: (token: string | null) => void;

  setUser: (user: AuthUser | null) => void;

  clearAuth: () => void;
}

/**
 * 인증 상태 관리 스토어
 *
 * sessionStorage 사용 이유:
 * - 탭 닫으면 자동 로그아웃
 * - 공용 PC에서 상대적으로 안전
 * - 브라우저 재시작 시 토큰 휘발성
 *
 * 저장 위치: sessionStorage['auth-storage']
 */
export const useAuthStore = create<AuthState>()(
  persist(
    set => ({
      accessToken: null,
      user: null,

      setAuth: ({ accessToken, user }) => {
        set({ accessToken, user });
      },

      setAccessToken: token => {
        set({ accessToken: token });
      },

      setUser: user => {
        set({ user });
      },

      clearAuth: () => {
        set({ accessToken: null, user: null });
        if (typeof window !== 'undefined') {
          sessionStorage.removeItem('auth-storage');
        }
      },
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => sessionStorage),
      partialize: state => ({
        accessToken: state.accessToken,
        user: state.user,
      }),
    },
  ),
);
