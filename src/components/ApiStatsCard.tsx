// src/components/ApiStatsCard.tsx
import React from 'react';
import type {
    DashboardApiStatsData,
    ApiEndpointStats,
} from '@/types/dashboard';

interface ApiStatsCardProps {
    data: DashboardApiStatsData;
}

const formatNumber = (value: number | undefined) =>
    typeof value === 'number' ? value.toLocaleString('ko-KR') : '-';

const formatPercent = (value: number | undefined, fractionDigits = 1) =>
    typeof value === 'number' ? `${value.toFixed(fractionDigits)}%` : '-';

const formatMs = (value: number | undefined) =>
    typeof value === 'number' ? `${value.toFixed(0)} ms` : '-';

const ApiStatsCard: React.FC<ApiStatsCardProps> = ({ data }) => {
    const { summary, endpoints } = data;

    return (
        <div className="flex h-full flex-col rounded-lg border bg-white p-6 shadow-sm">
            <div className="mb-2 flex items-baseline justify-between">
                <h2 className="text-base font-semibold">API 호출 통계</h2>
            </div>

            {/* 상단 요약 영역 */}
            <div className="mb-4 grid grid-cols-2 gap-4 text-sm">
                <div className="rounded-md bg-slate-50 p-3">
                    <p className="text-[11px] text-slate-500">총 엔드포인트 수</p>
                    <p className="mt-1 text-lg font-semibold text-slate-900">
                        {formatNumber(summary.totalEndpoints)}
                    </p>
                </div>
                <div className="rounded-md bg-slate-50 p-3">
                    <p className="text-[11px] text-slate-500">총 요청 수</p>
                    <p className="mt-1 text-lg font-semibold text-slate-900">
                        {formatNumber(summary.totalRequests)}
                    </p>
                </div>
                <div className="rounded-md bg-slate-50 p-3">
                    <p className="text-[11px] text-slate-500">총 에러 수</p>
                    <p className="mt-1 text-lg font-semibold text-red-500">
                        {formatNumber(summary.totalErrors)}
                    </p>
                </div>
                <div className="rounded-md bg-slate-50 p-3">
                    <p className="text-[11px] text-slate-500">전체 에러율</p>
                    <p className="mt-1 text-lg font-semibold text-red-500">
                        {formatPercent(summary.overallErrorRate)}
                    </p>
                </div>
                <div className="rounded-md bg-slate-50 p-3">
                    <p className="text-[11px] text-slate-500">평균 응답 시간</p>
                    <p className="mt-1 text-lg font-semibold text-slate-900">
                        {formatMs(summary.avgResponseTime)}
                    </p>
                </div>
                <div className="rounded-md bg-slate-50 p-3">
                    <p className="text-[11px] text-slate-500">주의 엔드포인트</p>
                    <p className="mt-1 text-lg font-semibold text-amber-600">
                        {formatNumber(summary.criticalEndpoints)}
                    </p>
                </div>
            </div>

            {/* 하단 Top 엔드포인트 목록 */}
            <div className="mt-2 flex-1">
                <p className="mb-2 text-xs font-medium text-slate-500">
                    엔드포인트별 호출 통계 (상위 {Math.min(endpoints.length, 10)}개)
                </p>

                {endpoints.length > 0 ? (
                    <div className="max-h-60 overflow-y-auto rounded-md border border-slate-100">
                        <table className="min-w-full divide-y divide-slate-100 text-xs">
                            <thead className="bg-slate-50">
                                <tr>
                                    <th className="px-3 py-2 text-left font-medium text-slate-500">
                                        메서드
                                    </th>
                                    <th className="px-3 py-2 text-left font-medium text-slate-500">
                                        엔드포인트
                                    </th>
                                    <th className="px-3 py-2 text-right font-medium text-slate-500">
                                        요청 수
                                    </th>
                                    <th className="px-3 py-2 text-right font-medium text-slate-500">
                                        에러 수
                                    </th>
                                    <th className="px-3 py-2 text-right font-medium text-slate-500">
                                        에러율
                                    </th>
                                    <th className="px-3 py-2 text-right font-medium text-slate-500">
                                        평균 응답
                                    </th>
                                    <th className="px-3 py-2 text-right font-medium text-slate-500">
                                        이상 탐지
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-50 bg-white">
                                {endpoints.slice(0, 10).map((endpoint: ApiEndpointStats) => (
                                    <tr key={endpoint.id}>
                                        <td className="px-3 py-2 text-[11px] font-semibold text-slate-700">
                                            {endpoint.httpMethod}
                                        </td>
                                        <td className="max-w-[180px] px-3 py-2 text-[11px] text-slate-600">
                                            <span className="truncate">{endpoint.endpointPath}</span>
                                        </td>
                                        <td className="px-3 py-2 text-right text-[11px] text-slate-700">
                                            {formatNumber(endpoint.totalRequests)}
                                        </td>
                                        <td className="px-3 py-2 text-right text-[11px] text-red-500">
                                            {formatNumber(endpoint.errorCount)}
                                        </td>
                                        <td className="px-3 py-2 text-right text-[11px] text-slate-700">
                                            {formatPercent(endpoint.errorRate)}
                                        </td>
                                        <td className="px-3 py-2 text-right text-[11px] text-slate-700">
                                            {formatMs(endpoint.avgResponseTime)}
                                        </td>
                                        <td className="px-3 py-2 text-right text-[11px] text-amber-600">
                                            {formatNumber(endpoint.anomalyCount)}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="flex h-32 items-center justify-center rounded-md border border-dashed border-slate-200 bg-slate-50 text-[11px] text-slate-400">
                        표시할 API 통계가 없습니다.
                    </div>
                )}
            </div>
        </div>
    );
};

export default ApiStatsCard;
