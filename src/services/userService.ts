import { apiClient } from '@/services/apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  SearchUserParams,
  PaginatedUserSearchResponse,
} from '@/types/user';

/**
 * 이름으로 멤버 검색 (GET /api/auth/users?name={name})
 */
export const searchUsers = async (
  params: SearchUserParams,
): Promise<PaginatedUserSearchResponse> => {
  try {
    // GET의 두 번째 인자는 config 객체 → params는 반드시 이렇게 감싸야 함
    const res = await apiClient.get<PaginatedUserSearchResponse>(
      API_PATH.SEARCH_USERS,
      { params },
    );
    return res;
  } catch (error) {
    console.error('멤버 검색 실패', error);
    throw error;
  }
};
