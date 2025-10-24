import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

interface AuthState {
  accessToken: string | null;
  setAccessToken: (token: string | null) => void;
  clearAuth: () => void;
}

/**
 * 인증 상태 관리 스토어
 *
 * sessionStorage 사용 이유:
 * - 탭을 닫으면 자동으로 로그아웃
 * - 공용 PC에서 안전
 * - XSS 공격 시에도 브라우저 재시작하면 토큰 사라짐
 *
 * 저장 위치: sessionStorage['auth-storage']
 */
export const useAuthStore = create<AuthState>()(
  persist(
    set => ({
      accessToken: null,

      // 토큰 설정
      setAccessToken: token => set({ accessToken: token }),

      // 인증 정보 초기화 (명시적으로 sessionStorage도 정리)
      clearAuth: () => {
        set({ accessToken: null });
        // sessionStorage 명시적 삭제
        if (typeof window !== 'undefined') {
          sessionStorage.removeItem('auth-storage');
        }
      },
    }),
    {
      name: 'auth-storage',
      // sessionStorage 사용 (localStorage 대신)
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);
