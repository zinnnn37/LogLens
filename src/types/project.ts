// 프로젝트 생성 요청
export interface CreateProjectPayload {
  projectName: string;
  description?: string;
}

// 프로젝트 생성 응답
export interface ProjectDTO {
  projectId: number;
  projectName: string;
  projectUuid: string;
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

// 프로젝트 멤버 초대
/**
 * 멤버 초대 API (POST /api/projects/{projectId}/members) 요청 Body
 */
export interface InviteMemberPayload {
  userId: number;
}

/**
 * 멤버 초대 API 응답 (data.member)의 멤버 정보
 */
export interface InvitedMemberInfo {
  userId: number;
  username: string;
  email: string;
  joinedAt: string;
}

/**
 * 멤버 초대 API (POST /api/projects/{projectId}/members)의
 * `data` 필드 전체 응답 타입
 */
export interface InviteMemberResponse {
  projectId: number;
  member: InvitedMemberInfo;
}

// 프로젝트 상세조회
/**
 * 프로젝트 상세 조회 (data.members)에 포함된
 * 개별 멤버 정보 타입
 */
export interface ProjectMember {
  userId: number;
  name: string;
  email: string;
  joinedAt: string;
}

/**
 * 프로젝트 상세 조회 API (GET /api/projects/{projectId})의
 * `data` 필드 전체 응답 타입
 */
export interface ProjectDetailDTO {
  projectId: number;
  projectName: string;
  description: string;
  apiKey: string;
  members: ProjectMember[];
  createdAt: string;
  updatedAt: string;
}

// 프로젝트 삭제 API 요청 파라미터
export interface DeleteProjectParams {
  projectId: number;
}

// 프로젝트 내 멤버 삭제 요청 파라미터
export interface DeleteMemberParams {
  projectId: number;
  memberId: number;
}
