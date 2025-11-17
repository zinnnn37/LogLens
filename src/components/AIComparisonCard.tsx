import React from 'react';
import {
  CheckCircle,
  XCircle,
  Brain,
  Database,
  TrendingUp,
} from 'lucide-react';
import type { AIComparisonResponse } from '@/types/aiComparison';

interface AIComparisonCardProps {
  data: AIComparisonResponse;
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

const AIComparisonCard: React.FC<AIComparisonCardProps> = ({ data }) => {
  const {
    analysisPeriodHours,
    sampleSize,
    analyzedAt,
    dbStatistics,
    aiStatistics,
    accuracyMetrics,
    verdict,
    technicalHighlights,
  } = data;

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
          {verdict.canReplaceDb ? (
            <span className="flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700">
              <CheckCircle className="h-3.5 w-3.5" />
              DB 대체 가능
            </span>
          ) : (
            <span className="flex items-center gap-1 rounded-full border border-red-200 bg-red-50 px-3 py-1 text-xs font-medium text-red-700">
              <XCircle className="h-3.5 w-3.5" />
              추가 개선 필요
            </span>
          )}
        </div>
      </div>

      {/* 분석 정보 */}
      <div className="mb-4 rounded-md bg-slate-50 p-3 text-xs text-slate-600">
        <div className="flex items-center gap-4">
          <span>분석 기간: {analysisPeriodHours}시간</span>
          <span>샘플 크기: {formatNumber(sampleSize)}개</span>
          <span>분석 시점: {new Date(analyzedAt).toLocaleString('ko-KR')}</span>
        </div>
      </div>

      {/* 전체 정확도 게이지 */}
      <div className="mb-4">
        <div className="mb-2 flex items-center justify-between">
          <span className="text-sm font-medium text-slate-700">
            종합 정확도
          </span>
          <span
            className={`text-2xl font-bold ${getAccuracyColor(accuracyMetrics.overallAccuracy)}`}
          >
            {formatPercent(accuracyMetrics.overallAccuracy)}
          </span>
        </div>
        <div className="relative h-3 w-full overflow-hidden rounded-full bg-slate-200">
          <div
            className={`absolute h-full rounded-full transition-all duration-500 ${
              accuracyMetrics.overallAccuracy >= 95
                ? 'bg-emerald-500'
                : accuracyMetrics.overallAccuracy >= 90
                  ? 'bg-blue-500'
                  : accuracyMetrics.overallAccuracy >= 80
                    ? 'bg-sky-500'
                    : accuracyMetrics.overallAccuracy >= 70
                      ? 'bg-amber-500'
                      : 'bg-red-500'
            }`}
            style={{
              width: `${Math.min(accuracyMetrics.overallAccuracy, 100)}%`,
            }}
          />
        </div>
        <div className="mt-1 text-xs text-slate-500">
          AI 신뢰도: {accuracyMetrics.aiConfidence}점
        </div>
      </div>

      {/* 통계 비교 테이블 */}
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
                  {formatNumber(dbStatistics.totalLogs)}
                </td>
                <td className="px-3 py-2 text-right">
                  {formatNumber(aiStatistics.estimatedTotalLogs)}
                </td>
                <td
                  className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracyMetrics.totalLogsAccuracy)}`}
                >
                  {formatPercent(accuracyMetrics.totalLogsAccuracy)}
                </td>
              </tr>
              <tr className="bg-slate-25">
                <td className="px-3 py-2 font-medium text-slate-700">
                  ERROR 수
                </td>
                <td className="px-3 py-2 text-right text-red-600">
                  {formatNumber(dbStatistics.errorCount)}
                </td>
                <td className="px-3 py-2 text-right text-red-600">
                  {formatNumber(aiStatistics.estimatedErrorCount)}
                </td>
                <td
                  className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracyMetrics.errorCountAccuracy)}`}
                >
                  {formatPercent(accuracyMetrics.errorCountAccuracy)}
                </td>
              </tr>
              <tr>
                <td className="px-3 py-2 font-medium text-slate-700">
                  WARN 수
                </td>
                <td className="px-3 py-2 text-right text-amber-600">
                  {formatNumber(dbStatistics.warnCount)}
                </td>
                <td className="px-3 py-2 text-right text-amber-600">
                  {formatNumber(aiStatistics.estimatedWarnCount)}
                </td>
                <td
                  className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracyMetrics.warnCountAccuracy)}`}
                >
                  {formatPercent(accuracyMetrics.warnCountAccuracy)}
                </td>
              </tr>
              <tr className="bg-slate-25">
                <td className="px-3 py-2 font-medium text-slate-700">
                  INFO 수
                </td>
                <td className="px-3 py-2 text-right text-blue-600">
                  {formatNumber(dbStatistics.infoCount)}
                </td>
                <td className="px-3 py-2 text-right text-blue-600">
                  {formatNumber(aiStatistics.estimatedInfoCount)}
                </td>
                <td
                  className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracyMetrics.infoCountAccuracy)}`}
                >
                  {formatPercent(accuracyMetrics.infoCountAccuracy)}
                </td>
              </tr>
              <tr>
                <td className="px-3 py-2 font-medium text-slate-700">에러율</td>
                <td className="px-3 py-2 text-right">
                  {formatPercent(dbStatistics.errorRate)}
                </td>
                <td className="px-3 py-2 text-right">
                  {formatPercent(aiStatistics.estimatedErrorRate)}
                </td>
                <td
                  className={`px-3 py-2 text-right font-semibold ${getAccuracyColor(accuracyMetrics.errorRateAccuracy)}`}
                >
                  {formatPercent(accuracyMetrics.errorRateAccuracy)}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* AI 추론 근거 */}
      <div className="mb-4 rounded-md border border-purple-100 bg-purple-50 p-3">
        <p className="mb-1 text-xs font-medium text-purple-700">AI 추론 근거</p>
        <p className="text-xs text-purple-900">{aiStatistics.reasoning}</p>
      </div>

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
      {technicalHighlights.length > 0 && (
        <div className="rounded-md border border-blue-100 bg-blue-50 p-3">
          <p className="mb-2 text-xs font-medium text-blue-700">
            기술적 어필 포인트
          </p>
          <ul className="list-inside list-disc space-y-1 text-xs text-blue-900">
            {technicalHighlights.map((highlight, idx) => (
              <li key={idx}>{highlight}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default AIComparisonCard;
