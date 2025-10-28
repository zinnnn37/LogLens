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
