// AI 분석 문서 기능 테스트 페이지
import { useState } from 'react';
import { AnalysisDocumentViewer } from '@/components/AnalysisDocumentViewer';
import {
  generateProjectAnalysis,
  generateErrorAnalysis,
} from '@/services/analysisService';
import type { AnalysisDocumentResponse } from '@/types/analysis';
import { useAuthStore } from '@/stores/authStore';

const AnalysisTestPage = () => {
  const [document, setDocument] = useState<AnalysisDocumentResponse | null>(
    null,
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [projectUuid, setProjectUuid] = useState('');
  const [logId, setLogId] = useState('');
  const [manualToken, setManualToken] = useState('');
  const { accessToken, setAccessToken } = useAuthStore();

  const handleSetToken = () => {
    if (manualToken.trim()) {
      setAccessToken(manualToken.trim());
      setError(null);
      alert('토큰이 설정되었습니다.');
    } else {
      setError('토큰을 입력해주세요.');
    }
  };

  const handleProjectAnalysis = async () => {
    if (!projectUuid.trim()) {
      setError('프로젝트 UUID를 입력해주세요.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const now = new Date();
      const startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000); // 7일 전

      const response = await generateProjectAnalysis(projectUuid, {
        format: 'HTML',
        startTime: startTime.toISOString().split('.')[0], // 밀리초와 Z 제거
        endTime: now.toISOString().split('.')[0], // 밀리초와 Z 제거
        options: {
          includeCharts: true,
          darkMode: false,
        },
      });

      console.log('프로젝트 분석 응답:', response);
      setDocument(response);
    } catch (err) {
      console.error('프로젝트 분석 실패:', err);
      setError(
        err instanceof Error ? err.message : '프로젝트 분석에 실패했습니다.',
      );
    } finally {
      setLoading(false);
    }
  };

  const handleErrorAnalysis = async () => {
    if (!logId.trim()) {
      setError('로그 ID를 입력해주세요.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await generateErrorAnalysis(parseInt(logId, 10), {
        projectUuid: projectUuid || '',
        format: 'HTML',
        options: {
          includeRelatedLogs: true,
          includeImpactAnalysis: true,
          maxRelatedLogs: 10,
        },
      });

      console.log('에러 분석 응답:', response);
      setDocument(response);
    } catch (err) {
      console.error('에러 분석 실패:', err);
      setError(
        err instanceof Error ? err.message : '에러 분석에 실패했습니다.',
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="mx-auto max-w-4xl">
        <h1 className="mb-8 text-3xl font-bold">AI 분석 문서 테스트</h1>

        {/* 인증 설정 */}
        <div className="mb-6 rounded-lg border border-yellow-200 bg-yellow-50 p-6 shadow">
          <h2 className="mb-4 text-xl font-semibold">인증 설정</h2>
          <div className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                JWT 토큰 (수동 설정)
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={manualToken}
                  onChange={e => setManualToken(e.target.value)}
                  placeholder="eyJhbGciOiJIUzUxMiJ9..."
                  className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm focus:ring-2 focus:ring-yellow-500 focus:outline-none"
                />
                <button
                  onClick={handleSetToken}
                  className="rounded-md bg-yellow-500 px-4 py-2 text-white transition-colors hover:bg-yellow-600"
                >
                  설정
                </button>
              </div>
            </div>
            <div className="text-sm">
              <span className="font-medium">현재 토큰 상태: </span>
              {accessToken ? (
                <span className="text-green-600">
                  설정됨 ({accessToken.substring(0, 20)}...)
                </span>
              ) : (
                <span className="text-red-600">미설정</span>
              )}
            </div>
          </div>
        </div>

        {/* 입력 폼 */}
        <div className="mb-6 rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-xl font-semibold">테스트 설정</h2>

          <div className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                프로젝트 UUID
              </label>
              <input
                type="text"
                value={projectUuid}
                onChange={e => setProjectUuid(e.target.value)}
                placeholder="550e8400-e29b-41d4-a716-446655440000"
                className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                로그 ID (에러 분석용)
              </label>
              <input
                type="text"
                value={logId}
                onChange={e => setLogId(e.target.value)}
                placeholder="12345"
                className="w-full rounded-md border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>
        </div>

        {/* 버튼 */}
        <div className="mb-6 flex gap-4">
          <button
            onClick={handleProjectAnalysis}
            disabled={loading}
            className="flex-1 rounded-lg bg-blue-600 px-6 py-3 font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-400"
          >
            {loading ? '생성 중...' : '프로젝트 분석 문서 생성'}
          </button>

          <button
            onClick={handleErrorAnalysis}
            disabled={loading}
            className="flex-1 rounded-lg bg-red-600 px-6 py-3 font-medium text-white transition-colors hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-gray-400"
          >
            {loading ? '생성 중...' : '에러 분석 문서 생성'}
          </button>
        </div>

        {/* 에러 표시 */}
        {error && (
          <div className="mb-6 rounded border border-red-200 bg-red-50 px-4 py-3 text-red-700">
            <p className="font-medium">오류 발생:</p>
            <p>{error}</p>
          </div>
        )}

        {/* 안내 */}
        <div className="rounded border border-blue-200 bg-blue-50 px-4 py-3 text-blue-700">
          <p className="mb-2 font-medium">테스트 안내:</p>
          <ul className="list-inside list-disc space-y-1 text-sm">
            <li>로그인이 필요합니다 (JWT 토큰)</li>
            <li>프로젝트 UUID는 DB에 존재하는 값이어야 합니다</li>
            <li>dev 프로필에서는 H2 인메모리 DB를 사용합니다</li>
            <li>먼저 프로젝트를 생성해야 할 수 있습니다</li>
          </ul>
        </div>
      </div>

      {/* 문서 뷰어 */}
      {document && (
        <AnalysisDocumentViewer
          document={document}
          onClose={() => setDocument(null)}
        />
      )}
    </div>
  );
};

export default AnalysisTestPage;
