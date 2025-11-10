import { apiClient } from '@/services/apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  DashboardOverviewParams,
  DashboardOverviewData,
} from '@/types/dashboard';

/**
 * 대시보드 통계 개요 조회 API
 */
export const getDashboardOverview = async (
  params: DashboardOverviewParams,
): Promise<DashboardOverviewData> => {
  const response = await apiClient.get<DashboardOverviewData>(
    API_PATH.STATISTICS_OVERVIEW,
    params,
  );
  return response;
};