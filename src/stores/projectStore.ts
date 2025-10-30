// src/stores/projectStore.ts

import { create } from 'zustand';
import type {
  ProjectDTO,
  ProjectInfoDTO,
  PaginatedProjectResponse,
  Pageable,
  ProjectDetailDTO,
} from '@/types/project';

interface ProjectState {
  projects: ProjectInfoDTO[]; // 프로젝트

  // 프로젝트 상세 조회
  currentProject: ProjectDetailDTO | null;

  // 페이지네이션 상태
  pagination: Pageable | null;
  totalElements: number;
  totalPages: number;

  setProjects: (response: PaginatedProjectResponse) => void;
  addProject: (newProject: ProjectDTO) => void;

  setCurrentProject: (project: ProjectDetailDTO | null) => void;

  incrementMemberCount: (projectId: number) => void;

  removeProject: (projectId: number) => void;
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
   * 'getProjectDetail' 서비스가 호출하는 액션.
   * API 응답(ProjectDetailDTO)을 받아 'currentProject' 상태에 저장합니다.
   */
  setCurrentProject: project => set({ currentProject: project }),

  /**
   * (POST /api/projects/{projectId}/members)
   * 멤버 초대가 성공했을 때, 해당 프로젝트의 memberCount를 1 증가시킵니다.
   */
  incrementMemberCount: projectId =>
    set(state => ({
      // 멤버 증가
      projects: state.projects.map(project =>
        project.projectId === projectId
          ? { ...project, memberCount: project.memberCount + 1 }
          : project,
      ),

      // 멤버 추가 후 다시 조회
      currentProject:
        state.currentProject?.projectId === projectId
          ? null
          : state.currentProject,
    })),

  /**
   * (DELETE /api/projects/{id})
   * 'deleteProject' 서비스가 호출하는 액션.
   * 목록(projects)에서 해당 프로젝트를 제거합니다.
   */
  removeProject: projectId =>
    set(state => ({
      projects: state.projects.filter(
        project => project.projectId !== projectId,
      ),
      totalElements: state.totalElements - 1,
    })),
}));
