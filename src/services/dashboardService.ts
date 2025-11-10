import { apiClient } from '@/services/apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  DashboardOverviewParams,
  DashboardOverviewData,
  DashboardApiStatsData,
  DashboardApiStatsParams,
  DashboardTopErrorsData,
  DashboardTopErrorsParams,
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

/**
 * API 호출 통계 조회 API
 */
export const getDashboardApiStats = async (
  params: DashboardApiStatsParams,
): Promise<DashboardApiStatsData> => {
  const response = await apiClient.get<DashboardApiStatsData>(
    API_PATH.STATISTICS_API_CALLS,
    params,
  );
  return response;
};

/**
 * 자주 발생하는 에러 TOP N 조회 API
 */
export const getDashboardTopErrors = async (
  params: DashboardTopErrorsParams,
): Promise<DashboardTopErrorsData> => {
  const response = await apiClient.get<DashboardTopErrorsData>(
    API_PATH.STATISTICS_ERROS_TOP, // 또는 STATISTICS_TOP_ERRORS (이름 변경 시)
    params,
  );
  return response;
};
