/**
 * 이름으로 멤버 검색 API (GET /api/auth/users) 요청 쿼리 파라미터
 */
export interface SearchUserParams {
  /**
   * 검색할 사용자 이름 (필수)
   */
  name: string;
  page?: number;
  size?: number;
  sort?: 'CREATED_AT' | 'NAME' | 'EMAIL';
  order?: 'ASC' | 'DESC';
}

/**
 * 멤버 검색 응답 (data.content)의 개별 유저 정보
 */
export interface UserSearchResult {
  userId: number;
  name: string;
  email: string;
}

/**
 * 멤버 검색 응답에 포함된 'pageable' 객체 타입
 */
export interface UserPageable {
  page: number;
  size: number;
  sort: 'CREATED_AT' | 'NAME' | 'EMAIL';
  order: 'ASC' | 'DESC';
}

/**
 * 멤버 검색 API (GET /api/auth/users)의
 * `data` 필드 전체 응답 타입 (페이지네이션 래퍼)
 */
export interface PaginatedUserSearchResponse {
  content: UserSearchResult[];
  pageable: UserPageable;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
