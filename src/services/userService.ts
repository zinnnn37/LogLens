import { apiClient } from '@/services/apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  SearchUserParams,
  PaginatedUserSearchResponse,
} from '@/types/user';

/**
 * 이름으로 멤버 검색 (GET /api/auth/users?name={name})
 * @param params - { name, page, size, sort, order }
 */
export const searchUsers = async (
  params: SearchUserParams,
): Promise<PaginatedUserSearchResponse> => {
  try {
    const response = await apiClient.get<PaginatedUserSearchResponse>(
      API_PATH.SEARCH_USERS,
      params,
    );
    return response;
  } catch (error) {
    console.error('멤버 검색 실패', error);
    throw error;
  }
};
