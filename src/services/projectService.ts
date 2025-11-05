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
  DeleteMemberParams,
  DeleteProjectParams,
} from '@/types/project';
import type { ArchitectureData } from '@/types/architecture';
import type { ComponentListData } from '@/types/component';
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
  projectUuid: string,
  payload: InviteMemberPayload,
): Promise<InviteMemberResponse> => {
  try {
    const response = await apiClient.post<InviteMemberResponse>(
      API_PATH.INVITE_MEMBERS(projectUuid),
      payload,
    );

    useProjectStore.getState().incrementMemberCount(projectUuid);

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
  projectUuid: string,
): Promise<ProjectDetailDTO> => {
  try {
    const projectDetail = await apiClient.get<ProjectDetailDTO>(
      API_PATH.PROJECT_DETAIL(projectUuid),
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
export const deleteProject = async ({
  projectUuid,
}: DeleteProjectParams): Promise<void> => {
  try {
    await apiClient.delete<void>(API_PATH.DELETE_PROJECT(projectUuid));

    useProjectStore.getState().removeProject(projectUuid);
  } catch (error) {
    console.error('프로젝트 삭제 실패', error);
    throw error;
  }
};

// 프로젝트 내 멤버 삭제(DELETE /api/projects/{projectId}/members/{memberId})
export const deleteMember = async ({
  projectUuid,
  memberId,
}: DeleteMemberParams): Promise<void> => {
  try {
    await apiClient.delete<void>(API_PATH.DELETE_MEMBER(projectUuid, memberId));

    useProjectStore.getState().removeMember(projectUuid, memberId);
  } catch (error) {
    console.log('멤버 삭제 실패', error);
    throw error;
  }
};

/**
 * 프로젝트 아키텍처 흐름 조회 (GET /api/projects/{projectId}/architecture)
 * @param projectId - 조회할 프로젝트 ID
 * @param params - { startDate, endDate } 기간 파라미터 (옵션)
 */
export const getArchitecture = async (
  projectId: number,
  params?: { startDate?: string; endDate?: string },
): Promise<ArchitectureData> => {
  try {
    const architecture = await apiClient.get<ArchitectureData>(
      API_PATH.ARCHITECTURE(String(projectId)),
      params,
    );

    return architecture;
  } catch (error) {
    console.error('아키텍처 조회 실패', error);
    throw error;
  }
};

/**
 * 프로젝트 컴포넌트 목록 조회 (GET /api/projects/{projectId}/components)
 * @param projectId - 조회할 프로젝트 ID
 * @param params - { limit, offset } 페이지네이션 파라미터 (옵션)
 */
export const getComponents = async (
  projectId: number,
  params?: { limit?: number; offset?: number },
): Promise<ComponentListData> => {
  try {
    const components = await apiClient.get<ComponentListData>(
      API_PATH.COMPONENTS(String(projectId)),
      params,
    );

    return components;
  } catch (error) {
    console.error('컴포넌트 목록 조회 실패', error);
    throw error;
  }
};
