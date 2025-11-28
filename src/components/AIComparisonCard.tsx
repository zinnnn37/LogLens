import React from 'react';
import {
  CheckCircle,
  XCircle,
  Brain,
  Database,
  TrendingUp,
  AlertTriangle,
  AlertCircle,
} from 'lucide-react';
import type {
  AIComparisonResponse,
  ErrorComparisonResponse,
} from '@/types/aiComparison';

interface AIComparisonCardProps {
  data: AIComparisonResponse;
  errorComparison?: ErrorComparisonResponse | null;
}

const formatNumber = (value: number) => value.toLocaleString('ko-KR');

const formatPercent = (value: number, fractionDigits = 2) =>
  `${value.toFixed(fractionDigits)}%`;

const getGradeColor = (grade: string) => {
  const colors: Record<string, string> = {
    '매우 우수': 'bg-emerald-100 text-emerald-800 border-emerald-200',
    '우수': 'bg-blue-100 text-blue-800 border-blue-200',
    '양호': 'bg-sky-100 text-sky-800 border-sky-200',
    '보통': 'bg-amber-100 text-amber-800 border-amber-200',
    '미흡': 'bg-red-100 text-red-800 border-red-200',
  };
  return colors[grade] || 'bg-gray-100 text-gray-800 border-gray-200';
};

const getAccuracyColor = (accuracy: number) => {
  if (accuracy >= 95) {
    return 'text-emerald-600';
  }
  if (accuracy >= 90) {
    return 'text-blue-600';
  }
  if (accuracy >= 80) {
    return 'text-sky-600';
  }
  if (accuracy >= 70) {
    return 'text-amber-600';
  }
  return 'text-red-600';
};

const AIComparisonCard: React.FC<AIComparisonCardProps> = ({
  data,
  errorComparison,
}) => {
  const {
    analysis_period_hours,
    sample_size,
    analyzed_at,
    db_statistics,
    ai_statistics,
    accuracy_metrics,
    verdict,
    technical_highlights,
  } = data;

  // 불완전한 응답 체크
  const isIncompleteResponse =
    !db_statistics || !ai_statistics || !accuracy_metrics;

  return (
    <div className="flex flex-col rounded-lg border bg-white p-6 shadow-sm">
      {/* 헤더 */}
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Brain className="h-5 w-5 text-purple-500" />
          <h2 className="text-base font-semibold">AI vs DB 통계 비교</h2>
        </div>
        <div className="flex items-center gap-2">
          <span
            className={`rounded-full border px-3 py-1 text-xs font-medium ${getGradeColor(verdict.grade)}`}
          >
            {verdict.grade}
          </span>
          {verdict.can_replace_db === true ? (
            <span className="flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700">
              <CheckCircle className="h-3.5 w-3.5" />
              DB 대체 가능
            </span>
          ) : verdict.can_replace_db === false ? (
            <span className="flex items-center gap-1 rounded-full border border-red-200 bg-red-50 px-3 py-1 text-xs font-medium text-red-700">
              <XCircle className="h-3.5 w-3.5" />
              추가 개선 필요
            </span>
          ) : (
            <span className="flex items-center gap-1 rounded-full border border-gray-200 bg-gray-50 px-3 py-1 text-xs font-medium text-gray-700">
              <AlertTriangle className="h-3.5 w-3.5" />
              평가 불가
            </span>
          )}
        </div>
      </div>

      {/* 분석 정보 */}
      <div className="mb-4 rounded-md bg-slate-50 p-3 text-xs text-slate-600">
        <div className="flex items-center gap-4">
          <span>분석 기간: {analysis_period_hours}시간</span>
          <span>샘플 크기: {formatNumber(sample_size)}개</span>
          <span>
            분석 시점: {new Date(analyzed_at).toLocaleString('ko-KR')}
          </span>
        </div>
      </div>

      {/* 불완전한 응답 경고 */}
      {isIncompleteResponse && (
        <div className="mb-4 rounded-md border border-amber-200 bg-amber-50 p-3">
          <div className="flex items-center gap-2 text-amber-800">
            <AlertTriangle className="h-4 w-4" />
            <span className="text-sm font-medium">데이터 수집 실패</span>
          </div>
          <p className="mt-1 text-xs text-amber-700">
            AI 서비스에서 완전한 통계 데이터를 받지 못했습니다. 잠시 후 다시
            시도해주세요.
          </p>
        </div>
      )}

      {/* 전체 정확도 게이지 */}
      {accuracy_metrics && (
        <div className="mb-4">
          <div className="mb-2 flex items-center justify-between">
            <span className="text-sm font-medium text-slate-700">
              종합 정확도
            </span>
            <span
              className={`text-2xl font-bold ${getAccuracyColor(accuracy_metrics.overall_accuracy)}`}
            >
              {formatPercent(accuracy_metrics.overall_accuracy)}
            </span>
          </div>
          <div className="relative h-3 w-full overflow-hidden rounded-full bg-slate-200">
            <div
              className={`absolute h-full rounded-full transition-all duration-500 ${
                accuracy_metrics.overall_accuracy >= 95
                  ? 'bg-emerald-500'
                  : accuracy_metrics.overall_accuracy >= 90
                    ? 'bg-blue-500'
                    : accuracy_metrics.overall_accuracy >= 80
                      ? 'bg-sky-500'
                      : accuracy_metrics.overall_accuracy >= 70
                        ? 'bg-amber-500'
                        : 'bg-red-500'
              }`}
              style={{
                width: `${Math.min(accuracy_metrics.overall_accuracy, 100)}%`,
              }}
            />
          </div>
          <div className="mt-1 text-xs text-slate-500">
            AI 신뢰도: {accuracy_metrics.ai_confidence}점
          </div>
        </div>
      )}

      {/* 통계 비교 테이블 */}
      {db_statistics && ai_statistics && accuracy_metrics && (
        <div className="mb-4">
          <p className="mb-2 text-xs font-medium text-slate-500">
            DB vs AI 통계 비교
          </p>
          <div className="overflow-hidden rounded-md border border-slate-200">
            <table className="min-w-full divide-y divide-slate-100 text-xs">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-3 py-2 text-left font-medium text-slate-600">
                    항목
                  </th>
                  <th className="px-3 py-2 text-right font-medium text-slate-600">
                    <div className="flex items-center justify-end gap-1">
                      <Database className="h-3.5 w-3.5" />
                      DB 실제값
                    </div>
                  </th>
                  <th className="px-3 py-2 text-right font-medium text-slate-600">
                    <div className="flex items-center justify-end gap-1">
                      <Brain className="h-3.5 w-3.5" />
                      AI 추론값
                    </div>
                  </th>
                  <th className="px-3 py-2 text-right font-medium text-slate-600">
                    <div className="flex items-center justify-end gap-1">
                      <TrendingUp className="h-3.5 w-3.5" />
                      정확도
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50 bg-white">
                <tr>
                  <td className="px-3 py-2 font-medium text-slate-700">
                    총 로그 수
                  </td>
                  <td className="px-3 py-2 text-right">
                    {formatNumber(db_statistics.total_logs)}
                  </td>
                  <td className="px-3 py-2 text-right">
                    {formatNumber(ai_statistics.estimated_total_logs)}
                  </td>
                  <td
                    className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracy_metrics.total_logs_accuracy)}`}
                  >
                    {formatPercent(accuracy_metrics.total_logs_accuracy)}
                  </td>
                </tr>
                <tr className="bg-slate-25">
                  <td className="px-3 py-2 font-medium text-slate-700">
                    ERROR 수
                  </td>
                  <td className="px-3 py-2 text-right text-red-600">
                    {formatNumber(db_statistics.error_count)}
                  </td>
                  <td className="px-3 py-2 text-right text-red-600">
                    {formatNumber(ai_statistics.estimated_error_count)}
                  </td>
                  <td
                    className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracy_metrics.error_count_accuracy)}`}
                  >
                    {formatPercent(accuracy_metrics.error_count_accuracy)}
                  </td>
                </tr>
                <tr>
                  <td className="px-3 py-2 font-medium text-slate-700">
                    WARN 수
                  </td>
                  <td className="px-3 py-2 text-right text-amber-600">
                    {formatNumber(db_statistics.warn_count)}
                  </td>
                  <td className="px-3 py-2 text-right text-amber-600">
                    {formatNumber(ai_statistics.estimated_warn_count)}
                  </td>
                  <td
                    className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracy_metrics.warn_count_accuracy)}`}
                  >
                    {formatPercent(accuracy_metrics.warn_count_accuracy)}
                  </td>
                </tr>
                <tr className="bg-slate-25">
                  <td className="px-3 py-2 font-medium text-slate-700">
                    INFO 수
                  </td>
                  <td className="px-3 py-2 text-right text-blue-600">
                    {formatNumber(db_statistics.info_count)}
                  </td>
                  <td className="px-3 py-2 text-right text-blue-600">
                    {formatNumber(ai_statistics.estimated_info_count)}
                  </td>
                  <td
                    className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracy_metrics.info_count_accuracy)}`}
                  >
                    {formatPercent(accuracy_metrics.info_count_accuracy)}
                  </td>
                </tr>
                <tr>
                  <td className="px-3 py-2 font-medium text-slate-700">
                    에러율
                  </td>
                  <td className="px-3 py-2 text-right">
                    {formatPercent(db_statistics.error_rate)}
                  </td>
                  <td className="px-3 py-2 text-right">
                    {formatPercent(ai_statistics.estimated_error_rate)}
                  </td>
                  <td
                    className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracy_metrics.error_rate_accuracy)}`}
                  >
                    {formatPercent(accuracy_metrics.error_rate_accuracy)}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* AI 추론 근거 */}
      {ai_statistics && (
        <div className="mb-4 rounded-md border border-purple-100 bg-purple-50 p-3">
          <p className="mb-1 text-xs font-medium text-purple-700">
            AI 추론 근거
          </p>
          <p className="text-xs text-purple-900">{ai_statistics.reasoning}</p>
        </div>
      )}

      {/* 판정 결론 */}
      <div className="mb-4 rounded-md bg-slate-50 p-3">
        <p className="mb-1 text-xs font-medium text-slate-700">판정 결론</p>
        <p className="text-sm text-slate-800">{verdict.explanation}</p>
        {verdict.recommendations.length > 0 && (
          <div className="mt-2">
            <p className="mb-1 text-xs font-medium text-slate-600">
              권장 사항:
            </p>
            <ul className="list-inside list-disc space-y-0.5 text-xs text-slate-600">
              {verdict.recommendations.map((rec, idx) => (
                <li key={idx}>{rec}</li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* 기술적 하이라이트 */}
      {technical_highlights && technical_highlights.length > 0 && (
        <div className="rounded-md border border-blue-100 bg-blue-50 p-3">
          <p className="mb-2 text-xs font-medium text-blue-700">
            기술적 어필 포인트
          </p>
          <ul className="list-inside list-disc space-y-1 text-xs text-blue-900">
            {technical_highlights.map((highlight, idx) => (
              <li key={idx}>{highlight}</li>
            ))}
          </ul>
        </div>
      )}

      {/* ERROR 상세 분석 섹션 */}
      {errorComparison && (
        <div className="mt-6 border-t border-slate-200 pt-6">
          <h4 className="mb-4 flex items-center gap-2 text-sm font-semibold text-slate-900">
            <AlertCircle className="h-4 w-4 text-red-500" />
            ERROR 로그 상세 분석 (Vector DB 활용)
          </h4>

          {/* ERROR 비교 통계 */}
          <div className="mb-4 grid grid-cols-2 gap-4">
            {/* DB ERROR */}
            <div className="rounded-md bg-slate-50 p-4">
              <div className="mb-1 text-xs text-slate-600">DB 실제 ERROR</div>
              <div className="text-2xl font-bold text-slate-900">
                {formatNumber(errorComparison.db_error_stats.total_errors)}
              </div>
              <div className="mt-1 text-xs text-slate-500">
                비율: {formatPercent(errorComparison.db_error_stats.error_rate)}
              </div>
            </div>

            {/* AI ERROR 추정 */}
            <div className="rounded-md bg-blue-50 p-4">
              <div className="mb-1 text-xs text-blue-600">AI 추정 ERROR</div>
              <div className="text-2xl font-bold text-blue-900">
                {formatNumber(
                  errorComparison.ai_error_stats.estimated_total_errors,
                )}
              </div>
              <div className="mt-1 text-xs text-blue-500">
                비율:{' '}
                {formatPercent(
                  errorComparison.ai_error_stats.estimated_error_rate,
                )}
              </div>
            </div>
          </div>

          {/* ERROR 정확도 */}
          <div className="mb-4 rounded-md bg-gradient-to-r from-red-50 to-orange-50 p-4">
            <div className="mb-2 flex items-center justify-between">
              <span className="text-sm font-medium text-slate-700">
                ERROR 추정 정확도
              </span>
              <span
                className={`text-2xl font-bold ${getAccuracyColor(errorComparison.accuracy_metrics.overall_accuracy)}`}
              >
                {formatPercent(
                  errorComparison.accuracy_metrics.overall_accuracy,
                  1,
                )}
              </span>
            </div>
            <div className="h-2 w-full rounded-full bg-slate-200">
              <div
                className="h-2 rounded-full bg-red-500 transition-all duration-500"
                style={{
                  width: `${errorComparison.accuracy_metrics.overall_accuracy}%`,
                }}
              />
            </div>
          </div>

          {/* Vector 샘플링 정보 */}
          {errorComparison.vector_analysis && (
            <div className="mb-4 rounded-md bg-purple-50 p-4">
              <div className="mb-2 text-xs font-semibold text-purple-800">
                Vector 샘플링 정보
              </div>
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div>
                  <span className="text-slate-600">벡터화된 ERROR:</span>
                  <span className="ml-2 font-medium text-slate-900">
                    {formatNumber(
                      errorComparison.vector_analysis.vectorized_error_count,
                    )}
                    개
                  </span>
                </div>
                <div>
                  <span className="text-slate-600">벡터화율:</span>
                  <span className="ml-2 font-medium text-slate-900">
                    {formatPercent(
                      errorComparison.vector_analysis.vectorization_rate,
                      1,
                    )}
                  </span>
                </div>
                <div className="col-span-2">
                  <span className="text-slate-600">샘플링 방법:</span>
                  <span
                    className={`ml-2 font-medium ${
                      errorComparison.vector_analysis.sampling_method ===
                      'vector_knn'
                        ? 'text-green-600'
                        : 'text-amber-600'
                    }`}
                  >
                    {errorComparison.vector_analysis.sampling_method ===
                    'vector_knn'
                      ? 'Vector KNN (유사도 기반)'
                      : 'Random (폴백)'}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* AI 추론 근거 */}
          <div className="rounded-md bg-slate-50 p-4 text-sm text-slate-700">
            <div className="mb-1 font-medium text-slate-900">AI 추론 근거:</div>
            {errorComparison.ai_error_stats.reasoning}
          </div>
        </div>
      )}
    </div>
  );
};

export default AIComparisonCard;
