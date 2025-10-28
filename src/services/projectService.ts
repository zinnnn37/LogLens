// src/services/projectService.ts

import { apiClient } from '@/services/apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  CreateProjectPayload,
  ProjectDTO,
  GetProjectsParams,
  PaginatedProjectResponse,
} from '@/types/project';
import { useProjectStore } from '@/stores/projectStore';

// 프로젝트 생성 함수
export const createProject = async (
  payload: CreateProjectPayload,
): Promise<ProjectDTO> => {
  try {
    const newProject = await apiClient.post<ProjectDTO>(
      API_PATH.PROJECT,
      payload,
    );

    useProjectStore.getState().addProject(newProject);

    return newProject;
  } catch (error) {
    console.error('프로젝트 생성 실패 ', error);
    throw error;
  }
};

/**
 * 프로젝트 목록 조회 함수 (GET /api/projects)
 * @param params - { page, size, sort, order }
 */
export const fetchProjects = async (
  params?: GetProjectsParams,
): Promise<PaginatedProjectResponse> => {
  try {
    const response = await apiClient.get<PaginatedProjectResponse>(
      API_PATH.PROJECT,
      params,
    );

    return response;
  } catch (error) {
    console.error('프로젝트 목록 조회 실패 ', error);
    throw error;
  }
};

// 멤버 초대 함수

// 프로젝트 상세 조회 함수
