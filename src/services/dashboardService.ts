import { apiClient } from '@/services/apiClient';
import { API_PATH } from '@/constants/api-path';
import type {
  DashboardOverviewParams,
  DashboardOverviewData,
  DashboardApiStatsData,
  DashboardApiStatsParams,
  DashboardTopErrorsData,
  DashboardTopErrorsParams,
  HeatmapParams,
  HeatmapResponse,
} from '@/types/dashboard';
import type {
  AIComparisonParams,
  AIComparisonResponse,
  ErrorComparisonParams,
  ErrorComparisonResponse,
} from '@/types/aiComparison';

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

/**
 * 히트맵
 */
export const getLogHeatmap = async (
  params: HeatmapParams,
): Promise<HeatmapResponse> => {
  const response = await apiClient.get<HeatmapResponse>(
    API_PATH.STATISTICS_HEATMAP,
    params,
  );
  return response;
};

/**
 * AI vs DB 통계 비교 조회 API
 * LLM의 DB 대체 역량을 검증하는 API
 */
export const getAIComparison = async (
  params: AIComparisonParams,
): Promise<AIComparisonResponse> => {
  const response = await apiClient.get<AIComparisonResponse>(
    API_PATH.AI_COMPARISON,
    params,
  );
  return response;
};

/**
 * ERROR 로그 전용 비교 API
 * Vector KNN 샘플링으로 ERROR 패턴 분석
 */
export const getErrorComparison = async (
  params: ErrorComparisonParams,
): Promise<ErrorComparisonResponse> => {
  const response = await apiClient.get<ErrorComparisonResponse>(
    API_PATH.ERROR_COMPARISON,
    params,
  );
  return response;
};
