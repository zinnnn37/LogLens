// src/pages/DashboardPage.tsx
import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'sonner';
import {
  Loader2,
  AlertCircle,
  Brain,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

import DashboardStatsCards from '@/components/DashboardStatsCards';
import RecentAlertsCard from '@/components/RecentAlertsCard';
import LogHeatmapCard from '@/components/LogHeatmapCard';
import FrequentErrorsCard from '@/components/FrequentErrorsCard';
import FloatingChecklist from '@/components/FloatingChecklist';
import AIComparisonCard from '@/components/AIComparisonCard';

import {
  getDashboardOverview,
  getDashboardTopErrors,
  getLogHeatmap,
  getDashboardApiStats,
  getAIComparison,
  getErrorComparison,
} from '@/services/dashboardService';
import { getRecentAlerts } from '@/services/alertService';
import type {
  DashboardSummary,
  DashboardTopErrorsData,
  HeatmapResponse,
  DashboardApiStatsData,
} from '@/types/dashboard';
import type {
  AIComparisonResponse,
  ErrorComparisonResponse,
} from '@/types/aiComparison';
import type { Alert } from '@/types/alert';
import ApiStatsCard from '@/components/ApiStatsCard';

const DashboardPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // 통계 개요 상태
  const [stats, setStats] = useState<DashboardSummary | null>(null);
  const [statsLoading, setStatsLoading] = useState(true);
  const [statsError, setStatsError] = useState(false);

  // 자주 발생하는 에러 상태
  const [topErrors, setTopErrors] = useState<DashboardTopErrorsData | null>(
    null,
  );
  const [topErrorsLoading, setTopErrorsLoading] = useState(true);
  const [topErrorsError, setTopErrorsError] = useState(false);

  // 히트맵 상태
  const [heatmapData, setHeatmapData] = useState<HeatmapResponse | null>(null);
  const [heatmapLoading, setHeatmapLoading] = useState(true);
  const [heatmapError, setHeatmapError] = useState(false);

  // API 호출 통계 상태
  const [apiStats, setApiStats] = useState<DashboardApiStatsData | null>(null);
  const [apiStatsLoading, setApiStatsLoading] = useState(true);
  const [apiStatsError, setApiStatsError] = useState(false);

  // 최근 알림 상태
  const [recentAlerts, setRecentAlerts] = useState<Alert[]>([]);
  const [alertsLoading, setAlertsLoading] = useState(true);
  const [alertsError, setAlertsError] = useState(false);

  // AI vs DB 비교 상태
  const [showAIComparison, setShowAIComparison] = useState(false);
  const [aiComparison, setAIComparison] = useState<AIComparisonResponse | null>(
    null,
  );
  const [aiComparisonLoading, setAIComparisonLoading] = useState(false);
  const [aiComparisonError, setAIComparisonError] = useState(false);
  const [errorComparison, setErrorComparison] =
    useState<ErrorComparisonResponse | null>(null);

  // AI 비교 데이터 조회 함수
  const fetchAIComparison = async () => {
    if (!projectUuid) {
      return;
    }

    setAIComparisonLoading(true);
    setAIComparisonError(false);
    try {
      // 기존 전체 비교 API
      const response = await getAIComparison({
        projectUuid,
        timeHours: 24,
        sampleSize: 100,
      });
      setAIComparison(response);

      // ERROR 전용 비교 API 추가
      const errorResponse = await getErrorComparison({
        projectUuid,
        timeHours: 24,
        sampleSize: 100,
      });
      setErrorComparison(errorResponse);
    } catch (e) {
      console.error('AI 비교 데이터 조회 실패:', e);
      toast.error('AI 비교 데이터를 불러오지 못했습니다.');
      setAIComparisonError(true);
    } finally {
      setAIComparisonLoading(false);
    }
  };

  // AI 비교 토글 핸들러
  const handleToggleAIComparison = () => {
    if (!showAIComparison && !aiComparison && !aiComparisonLoading) {
      // 처음 열 때만 데이터 조회
      fetchAIComparison();
    }
    setShowAIComparison(!showAIComparison);
  };

  useEffect(() => {
    if (!projectUuid) {
      return;
    }

    // --- 1. 통계 개요 조회 ---
    const fetchOverview = async () => {
      setStatsLoading(true);
      setStatsError(false);
      try {
        const response = await getDashboardOverview({ projectUuid });
        setStats(response.summary);
      } catch (e) {
        console.error('대시보드 통계 조회 실패:', e);
        toast.error('대시보드 통계 정보를 불러오지 못했습니다.');
        setStatsError(true);
      } finally {
        setStatsLoading(false);
      }
    };

    // 자주 발행하는 에러
    const fetchTopErrors = async () => {
      setTopErrorsLoading(true);
      setTopErrorsError(false);
      try {
        // 기본값으로 10개 조회
        const response = await getDashboardTopErrors({
          projectUuid,
          limit: 10,
        });
        setTopErrors(response);
      } catch (e) {
        console.error('자주 발생하는 에러 조회 실패:', e);
        toast.error('자주 발생하는 에러 목록을 불러오지 못했습니다.');
        setTopErrorsError(true);
      } finally {
        setTopErrorsLoading(false);
      }
    };

    // 히트맵
    const fetchHeatmap = async () => {
      setHeatmapLoading(true);
      setHeatmapError(false);
      try {
        // 일단 디폴트로 호출중
        const now = new Date();
        const endTime = now.toISOString();
        const startTime = new Date(
          now.getTime() - 7 * 24 * 60 * 60 * 1000,
        ).toISOString();

        const response = await getLogHeatmap({
          projectUuid,
          startTime,
          endTime,
          logLevel: 'ALL',
        });
        setHeatmapData(response);
      } catch (e) {
        console.error('히트맵 데이터 조회 실패:', e);
        toast.error('로그 히트맵 정보를 불러오지 못했습니다.');
        setHeatmapError(true);
      } finally {
        setHeatmapLoading(false);
      }
    };

    const fetchApiStats = async () => {
      setApiStatsLoading(true);
      setApiStatsError(false);
      try {
        const response = await getDashboardApiStats({
          projectUuid,
        });
        setApiStats(response);
      } catch (e) {
        console.error('API 호출 통계 조회 실패:', e);
        toast.error('API 호출 통계 정보를 불러오지 못했습니다.');
        setApiStatsError(true);
      } finally {
        setApiStatsLoading(false);
      }
    };

    // 최근 알림 조회
    const fetchRecentAlerts = async () => {
      setAlertsLoading(true);
      setAlertsError(false);
      try {
        const alerts = await getRecentAlerts(projectUuid, 5);
        setRecentAlerts(alerts);
      } catch (e) {
        console.error('최근 알림 조회 실패:', e);
        toast.error('최근 알림 정보를 불러오지 못했습니다.');
        setAlertsError(true);
      } finally {
        setAlertsLoading(false);
      }
    };

    // API 동시 호출
    fetchOverview();
    fetchTopErrors();
    fetchHeatmap();
    fetchApiStats();
    fetchRecentAlerts();
  }, [projectUuid]);

  return (
    <div className="font-pretendard space-y-6 p-6 py-1">
      <div className="flex items-center justify-between">
        <h1 className="font-godoM text-lg">통계 요약</h1>
        <button
          onClick={handleToggleAIComparison}
          className="flex items-center gap-2 rounded-lg border border-purple-200 bg-purple-50 px-4 py-2 text-sm font-medium text-purple-700 transition-colors hover:bg-purple-100"
        >
          <Brain className="h-4 w-4" />
          AI 검증 보기
          {showAIComparison ? (
            <ChevronUp className="h-4 w-4" />
          ) : (
            <ChevronDown className="h-4 w-4" />
          )}
        </button>
      </div>

      {/* AI vs DB 비교 섹션 (토글) */}
      {showAIComparison && (
        <div className="animate-in slide-in-from-top-2 duration-300">
          {aiComparisonLoading ? (
            <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-purple-200 bg-purple-50 text-purple-600">
              <Loader2 className="mr-2 h-5 w-5 animate-spin" />
              AI 비교 분석 중... (최대 30초 소요)
            </div>
          ) : aiComparisonError ? (
            <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
              <AlertCircle className="mr-2 h-5 w-5" />
              AI 비교 데이터를 불러올 수 없습니다.
            </div>
          ) : aiComparison ? (
            <AIComparisonCard
              data={aiComparison}
              errorComparison={errorComparison}
            />
          ) : (
            <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
              AI 비교 데이터가 없습니다.
            </div>
          )}
        </div>
      )}

      {/* 대시보드 통계 개요 */}
      {statsLoading ? (
        <div className="flex min-h-[120px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          통계 정보를 불러오는 중...
        </div>
      ) : statsError ? (
        <div className="flex min-h-[120px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
          <AlertCircle className="mr-2 h-5 w-5" />
          통계 정보를 불러올 수 없습니다.
        </div>
      ) : stats ? (
        <DashboardStatsCards stats={stats} />
      ) : null}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {alertsLoading ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            최근 알림을 불러오는 중...
          </div>
        ) : alertsError ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
            <AlertCircle className="mr-2 h-5 w-5" />
            최근 알림을 불러올 수 없습니다.
          </div>
        ) : (
          <RecentAlertsCard alerts={recentAlerts} />
        )}

        {/* 히트맵 */}
        {heatmapLoading ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            히트맵을 불러오는 중...
          </div>
        ) : heatmapError ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
            <AlertCircle className="mr-2 h-5 w-5" />
            히트맵을 불러올 수 없습니다.
          </div>
        ) : heatmapData ? (
          <LogHeatmapCard data={heatmapData} />
        ) : (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            히트맵 데이터가 없습니다.
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* 자주 발생하는 에러 */}
        {topErrorsLoading ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            에러 목록을 불러오는 중...
          </div>
        ) : topErrorsError ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
            <AlertCircle className="mr-2 h-5 w-5" />
            에러 목록을 불러올 수 없습니다.
          </div>
        ) : topErrors ? (
          <FrequentErrorsCard data={topErrors} />
        ) : (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            데이터가 없습니다.
          </div>
        )}

        {/* API 호출 통계 카드 */}
        {apiStatsLoading ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            <Loader2 className="mr-2 h-5 w-5 animate-spin" />
            API 호출 통계를 불러오는 중...
          </div>
        ) : apiStatsError ? (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-red-200 bg-red-50 text-red-500">
            <AlertCircle className="mr-2 h-5 w-5" />
            API 호출 통계를 불러올 수 없습니다.
          </div>
        ) : apiStats ? (
          <ApiStatsCard data={apiStats} />
        ) : (
          <div className="flex min-h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200 bg-gray-50 text-gray-500">
            API 통계 데이터가 없습니다.
          </div>
        )}
      </div>
      <FloatingChecklist />
    </div>
  );
};

export default DashboardPage;
