import { create } from 'zustand';
import type { ProjectDTO } from '@/types/project';

interface ProjectState {
  projects: ProjectDTO[];
  currentProject: ProjectDTO | null;

  setProjects: (projects: ProjectDTO[]) => void;

  addProject: (newProject: ProjectDTO) => void;

  setCurrentProject: (project: ProjectDTO | null) => void;
}

export const useProjectStore = create<ProjectState>(set => ({
  // 초기 상태
  projects: [],
  currentProject: null,

  // 상태 변경
  // (GET /api/projects)
  setProjects: projects => set({ projects: projects }),

  // 프로젝트 생성
  addProject: newProject =>
    set(state => ({
      // 새 프로젝트를 목록 맨 앞에 추가
      projects: [newProject, ...state.projects],
    })),

  // (GET /api/projects/{id})
  setCurrentProject: project => set({ currentProject: project }),
}));
