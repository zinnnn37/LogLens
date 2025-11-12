import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import LogSearchBox from '@/components/LogSearchBox';
import FloatingChecklist from '@/components/FloatingChecklist';
import FlowSimulation from '@/components/FlowSimulation';
import { getTraceLogs, getTraceFlow } from '@/services/logService';
import type { TraceLogsResponse, TraceFlowResponse } from '@/types/log';

const RequestFlowPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  const [searchParams] = useSearchParams();

  const initialTraceId = searchParams.get('traceId');

  const [query, setQuery] = useState(initialTraceId ?? '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [traceData, setTraceData] = useState<TraceLogsResponse | null>(null);
  const [flowData, setFlowData] = useState<TraceFlowResponse | null>(null);

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
      const response = await getTraceLogs({
        projectUuid,
        traceId: traceId.trim(),
      });

      setTraceData(response);
      console.log('TraceId 검색 결과:', response);
    } catch (err) {
      console.error('TraceId 검색 에러:', err);
      setError('TraceId 검색에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  const handlePlayClick = async (traceId: string) => {
    if (!projectUuid) {
      setError('프로젝트 정보가 없습니다.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await getTraceFlow({
        projectUuid,
        traceId,
      });

      setFlowData(response);
      console.log('TraceId 흐름 조회 결과:', response);
    } catch (err) {
      console.error('TraceId 흐름 조회 에러:', err);
      setError('요청 흐름 조회에 실패했습니다. 다시 시도해주세요.');
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
          <div className="rounded-2xl bg-white p-6 shadow-sm">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-godoM text-lg text-gray-800">
                TraceId: {traceData.traceId}
              </h2>
              <button
                onClick={() => handlePlayClick(traceData.traceId)}
                disabled={loading}
                className="bg-secondary hover:bg-primary flex items-center gap-2 rounded-lg px-4 py-2 text-white transition-colors disabled:cursor-not-allowed disabled:opacity-50"
                title="요청 흐름 시뮬레이션 재생"
              >
                <svg
                  className="h-5 w-5"
                  fill="currentColor"
                  viewBox="0 0 20 20"
                >
                  <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z" />
                </svg>
                <span className="font-semibold">시뮬레이션 재생</span>
              </button>
            </div>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              <div>
                <p className="text-sm text-gray-500">전체 로그</p>
                <p className="text-xl font-semibold text-gray-800">
                  {traceData.logs.length}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">처리 시간</p>
                <p className="text-xl font-semibold text-gray-800">
                  {traceData.duration}ms
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">상태</p>
                <p
                  className={`text-xl font-semibold ${traceData.status === 'SUCCESS' ? 'text-green-600' : 'text-red-600'}`}
                >
                  {traceData.status}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">에러</p>
                <p className="text-xl font-semibold text-red-600">
                  {
                    traceData.logs.filter(log => log.logLevel === 'ERROR')
                      .length
                  }
                </p>
              </div>
            </div>

            {/* Request/Response 정보 */}
            <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="rounded-lg bg-blue-50 p-4">
                <h3 className="font-godoM mb-2 text-sm text-blue-900">
                  Request
                </h3>
                <p className="text-xs text-gray-600">
                  {traceData.request.message}
                </p>
                <p className="mt-1 text-xs text-gray-500">
                  {new Date(traceData.request.timestamp).toLocaleString()}
                </p>
                <p className="text-xs text-gray-500">
                  {traceData.request.componentName} -{' '}
                  {traceData.request.methodName}
                </p>
              </div>
              <div className="rounded-lg bg-green-50 p-4">
                <h3 className="font-godoM mb-2 text-sm text-green-900">
                  Response
                </h3>
                <p className="text-xs text-gray-600">
                  {traceData.response.message}
                </p>
                <p className="mt-1 text-xs text-gray-500">
                  {new Date(traceData.response.timestamp).toLocaleString()}
                </p>
                <p className="text-xs text-gray-500">
                  {traceData.response.componentName} -{' '}
                  {traceData.response.methodName}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Flow Simulation*/}
      {flowData && (
        <div className="mt-8">
          <FlowSimulation flowData={flowData} />
        </div>
      )}

      <FloatingChecklist />
    </div>
  );
};

export default RequestFlowPage;
