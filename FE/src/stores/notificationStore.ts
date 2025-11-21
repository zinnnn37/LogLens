import { create } from 'zustand';

interface NotificationState {
  // projectUuid를 key로 하는 알림 상태 맵
  projectNotifications: Record<string, boolean>;

  // 특정 프로젝트에 알림 설정
  setProjectNotification: (
    projectUuid: string,
    hasNotification: boolean,
  ) => void;

  // 특정 프로젝트의 알림 상태 조회
  hasNotification: (projectUuid: string) => boolean;

  // 모든 알림 초기화
  clearAllNotifications: () => void;
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  projectNotifications: {},

  setProjectNotification: (projectUuid, hasNotification) =>
    set(state => ({
      projectNotifications: {
        ...state.projectNotifications,
        [projectUuid]: hasNotification,
      },
    })),

  hasNotification: projectUuid => {
    const state = get();
    return state.projectNotifications[projectUuid] ?? false;
  },

  clearAllNotifications: () => set({ projectNotifications: {} }),
}));
