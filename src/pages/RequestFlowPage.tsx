import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import LogSearchBox from '@/components/LogSearchBox';
import FloatingChecklist from '@/components/FloatingChecklist';
import { searchLogs } from '@/services/logService';
import type { TraceIdSearchResponse } from '@/types/log';

const RequestFlowPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  const [searchParams] = useSearchParams();

  const initialTraceId = searchParams.get('traceId');

  const [query, setQuery] = useState(initialTraceId ?? '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [traceData, setTraceData] = useState<TraceIdSearchResponse | null>(
    null,
  );

  const handleSearchSubmit = async (traceId: string) => {
    if (!projectUuid) {
      setError('프로젝트 정보가 없습니다.');
      return;
    }

    if (!traceId.trim()) {
      setError('TraceId를 입력해주세요.');
      return;
    }

    setLoading(true);
    setError(null);
    setTraceData(null);

    try {
      const response = await searchLogs({
        projectUuid,
        traceId: traceId.trim(),
      });

      // TraceId 검색 응답인지 확인
      if ('traceId' in response && 'summary' in response) {
        setTraceData(response as TraceIdSearchResponse);
        console.log('TraceId 검색 결과:', response);
      } else {
        setError('TraceId 검색 결과를 찾을 수 없습니다.');
      }
    } catch (err) {
      console.error('TraceId 검색 에러:', err);
      setError('TraceId 검색에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="">
      <h1 className="font-godoM pb-5 text-xl text-gray-700">
        요청 흐름 시뮬레이션
      </h1>
      <LogSearchBox
        value={query}
        onChange={setQuery}
        onSubmit={handleSearchSubmit}
        loading={loading}
        searchResults={traceData?.logs}
        showResults={Boolean(traceData)}
      />

      {/* 에러 메시지 */}
      {error && (
        <div className="mt-4 rounded-lg bg-red-50 p-4 text-red-700">
          {error}
        </div>
      )}

      {/* TraceId Summary 정보 */}
      {traceData && (
        <div className="mt-6">
          <div className="rounded-lg bg-white p-6 shadow-sm">
            <h2 className="font-godoM mb-4 text-lg text-gray-800">
              TraceId: {traceData.traceId}
            </h2>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              <div>
                <p className="text-sm text-gray-500">전체 로그</p>
                <p className="text-xl font-semibold text-gray-800">
                  {traceData.summary.totalLogs}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">처리 시간</p>
                <p className="text-xl font-semibold text-gray-800">
                  {traceData.summary.durationMs}ms
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">에러</p>
                <p className="text-xl font-semibold text-red-600">
                  {traceData.summary.errorCount}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">경고</p>
                <p className="text-xl font-semibold text-yellow-600">
                  {traceData.summary.warnCount}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      <FloatingChecklist />
    </div>
  );
};

export default RequestFlowPage;
