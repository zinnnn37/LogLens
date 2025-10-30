// src/services/projectService.ts

import { apiClient } from '@/services/apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  CreateProjectPayload,
  ProjectDTO,
  GetProjectsParams,
  PaginatedProjectResponse,
  InviteMemberPayload,
  InviteMemberResponse,
  ProjectDetailDTO,
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

/**
 * 프로젝트에 멤버 초대 (POST /api/projects/{projectId}/members)
 * @param projectId - 초대할 프로젝트 ID
 * @param payload - { userId }
 */
export const inviteMember = async (
  projectId: number,
  payload: InviteMemberPayload,
): Promise<InviteMemberResponse> => {
  try {
    const response = await apiClient.post<InviteMemberResponse>(
      API_PATH.INVITE_MEMBERS(projectId),
      payload,
    );

    useProjectStore.getState().incrementMemberCount(projectId);

    return response;
  } catch (error) {
    console.error('멤버 초대 실패', error);
    throw error;
  }
};

// 프로젝트 상세 조회 함수
/**
 * 프로젝트 상세 조회 (GET /api/projects/{projectId})
 * @param projectId - 조회할 프로젝트 ID
 */
export const getProjectDetail = async (
  projectId: number,
): Promise<ProjectDetailDTO> => {
  try {
    const projectDetail = await apiClient.get<ProjectDetailDTO>(
      API_PATH.PROJECT_DETAIL(projectId),
    );

    useProjectStore.getState().setCurrentProject(projectDetail);

    return projectDetail;
  } catch (error) {
    console.error('프로젝트 상세 조회 실패', error);
    throw error;
  }
};

/**
 * 프로젝트 삭제 (DELETE /api/projects/{projectId})
 * @param projectId - 삭제할 프로젝트 ID
 */
export const deleteProject = async (projectId: number): Promise<void> => {
  try {
    await apiClient.delete<void>(API_PATH.DELETE_PROJECT(projectId));

    useProjectStore.getState().removeProject(projectId);

  } catch (error) {
    console.error('프로젝트 삭제 실패', error);
    throw error;
  }
};
