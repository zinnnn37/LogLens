// 프로젝트 생성 요청
export interface CreateProjectPayload {
  projectName: string;
  description?: string;
}

// 프로젝트 생성 응답
export interface ProjectDTO {
  projectId: number;
  projectName: string;
  description: string;
  apiKey: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 프로젝트 목록 조회
 */

// 쿼리파라미터
export interface GetProjectsParams {
  page?: number;
  size?: number;
  sort?: 'CREATED_AT' | 'UPDATED_AT' | 'PROJECT_NAME';
  order?: 'ASC' | 'DESC';
}

export interface ProjectInfoDTO {
  projectId: number;
  projectName: string;
  description: string;
  apiKey: string;
  memberCount: number;
  logCount: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 'pageable'
 */
export interface Pageable {
  page: number;
  size: number;
  sort: 'CREATED_AT' | 'UPDATED_AT' | 'PROJECT_NAME';
  order: 'ASC' | 'DESC';
}

/**
 * 'data'
 */
export interface PaginatedProjectResponse {
  content: ProjectInfoDTO[];
  pageable: Pageable;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
