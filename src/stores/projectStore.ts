// src/stores/projectStore.ts

import { create } from 'zustand';
import type {
  ProjectDTO,
  ProjectInfoDTO,
  PaginatedProjectResponse,
  Pageable,
} from '@/types/project';

interface ProjectState {
  projects: ProjectInfoDTO[];
  currentProject: ProjectInfoDTO | null;

  // 페이지네이션 상태 추가
  pagination: Pageable | null;
  totalElements: number;
  totalPages: number;

  setProjects: (response: PaginatedProjectResponse) => void;

  addProject: (newProject: ProjectDTO) => void;

  setCurrentProject: (project: ProjectInfoDTO | null) => void;
}

export const useProjectStore = create<ProjectState>(set => ({
  // 초기 상태
  projects: [],
  currentProject: null,
  pagination: null,
  totalElements: 0,
  totalPages: 0,

  // 액션

  /**
   * (GET /api/projects)
   * 목록 조회 API의 응답(PaginatedProjectResponse)을 받아
   * 스토어 상태 전체를 업데이트합니다.
   */
  setProjects: response =>
    set({
      projects: response.content,
      pagination: response.pageable,
      totalElements: response.totalElements,
      totalPages: response.totalPages,
    }),

  /**
   * (POST /api/projects)
   * createProject가 반환한 ProjectDTO를 받아
   * ProjectInfoDTO로 변환한 뒤 목록 맨 앞에 추가합니다.
   */
  addProject: newProject =>
    set(state => {
      const newProjectInfo: ProjectInfoDTO = {
        ...newProject,
        memberCount: 1, // 초기 기본값
        logCount: 0, // 초기 기본값
      };

      return {
        projects: [newProjectInfo, ...state.projects],
        totalElements: state.totalElements + 1,
      };
    }),

  /**
   * (GET /api/projects/{id})
   * (향후 상세 조회 API 연결 시 사용)
   */
  setCurrentProject: project => set({ currentProject: project }),
}));
