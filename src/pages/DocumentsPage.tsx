import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  FileText,
  Plus,
  Trash2,
  Eye,
  RefreshCw,
  AlertCircle,
  Download,
} from 'lucide-react';
import {
  generateProjectAnalysis,
  generateErrorAnalysis,
  getAnalysisDocuments,
  getAnalysisDocumentById,
  deleteAnalysisDocument,
} from '@/services/analysisService';
import type {
  AnalysisDocumentSummary,
  AnalysisDocumentDetailResponse,
  PageResponse,
  DocumentFormat,
} from '@/types/analysis';

const DocumentsPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // 상태 관리
  const [documents, setDocuments] = useState<AnalysisDocumentSummary[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 모달 상태
  const [selectedDocument, setSelectedDocument] =
    useState<AnalysisDocumentDetailResponse | null>(null);
  const [showViewer, setShowViewer] = useState(false);

  // 생성 옵션 모달
  const [showGenerateModal, setShowGenerateModal] = useState(false);
  const [generateType, setGenerateType] = useState<'project' | 'error'>(
    'project',
  );
  const [logIdInput, setLogIdInput] = useState('');

  // 문서 목록 조회
  const fetchDocuments = useCallback(async () => {
    if (!projectUuid) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response: PageResponse<AnalysisDocumentSummary> =
        await getAnalysisDocuments(projectUuid, currentPage, pageSize);
      setDocuments(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      console.error('문서 목록 조회 실패:', err);
      setError('문서 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, [projectUuid, currentPage, pageSize]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  // 프로젝트 분석 문서 생성
  const handleGenerateProjectAnalysis = async () => {
    if (!projectUuid) {
      return;
    }

    setGenerating(true);
    setError(null);

    try {
      const now = new Date();
      const startTime = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000); // 7일 전

      await generateProjectAnalysis(projectUuid, {
        format: 'HTML' as DocumentFormat,
        startTime: startTime.toISOString().split('.')[0],
        endTime: now.toISOString().split('.')[0],
        options: {
          includeComponents: true,
          includeAlerts: true,
          includeDependencies: true,
          includeCharts: true,
          darkMode: false,
        },
      });

      setShowGenerateModal(false);
      fetchDocuments();
    } catch (err) {
      console.error('프로젝트 분석 문서 생성 실패:', err);
      setError('프로젝트 분석 문서 생성에 실패했습니다.');
    } finally {
      setGenerating(false);
    }
  };

  // 에러 분석 문서 생성
  const handleGenerateErrorAnalysis = async () => {
    if (!projectUuid || !logIdInput) {
      return;
    }

    setGenerating(true);
    setError(null);

    try {
      await generateErrorAnalysis(parseInt(logIdInput, 10), {
        projectUuid,
        format: 'HTML' as DocumentFormat,
        options: {
          includeRelatedLogs: true,
          includeSimilarErrors: true,
          includeImpactAnalysis: true,
          includeCodeExamples: true,
          maxRelatedLogs: 10,
        },
      });

      setShowGenerateModal(false);
      setLogIdInput('');
      fetchDocuments();
    } catch (err) {
      console.error('에러 분석 문서 생성 실패:', err);
      setError('에러 분석 문서 생성에 실패했습니다.');
    } finally {
      setGenerating(false);
    }
  };

  // 문서 상세 조회
  const handleViewDocument = async (documentId: number) => {
    if (!projectUuid) {
      return;
    }

    try {
      const detail = await getAnalysisDocumentById(projectUuid, documentId);
      setSelectedDocument(detail);
      setShowViewer(true);
    } catch (err) {
      console.error('문서 상세 조회 실패:', err);
      setError('문서를 불러오는데 실패했습니다.');
    }
  };

  // 문서 삭제
  const handleDeleteDocument = async (documentId: number) => {
    if (!projectUuid) {
      return;
    }

    if (!confirm('정말로 이 문서를 삭제하시겠습니까?')) {
      return;
    }

    try {
      await deleteAnalysisDocument(projectUuid, documentId);
      fetchDocuments();
    } catch (err) {
      console.error('문서 삭제 실패:', err);
      setError('문서 삭제에 실패했습니다.');
    }
  };

  // 문서 다운로드
  const handleDownloadDocument = () => {
    if (!selectedDocument) {
      return;
    }

    const blob = new Blob([selectedDocument.content], { type: 'text/html' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${selectedDocument.title}.html`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // 날짜 포맷팅
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  // 문서 타입 한글화
  const getDocumentTypeLabel = (type: string) => {
    return type === 'PROJECT_ANALYSIS' ? '프로젝트 분석' : '에러 분석';
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mx-auto max-w-7xl">
        {/* 헤더 */}
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <FileText className="h-8 w-8 text-blue-600" />
            <div>
              <h1 className="text-2xl font-bold text-gray-900">문서 작성</h1>
              <p className="text-sm text-gray-500">
                AI 기반 프로젝트 분석 및 에러 분석 문서를 생성하고 관리합니다.
              </p>
            </div>
          </div>

          <div className="flex gap-3">
            <button
              onClick={fetchDocuments}
              className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`}
              />
              새로고침
            </button>
            <button
              onClick={() => setShowGenerateModal(true)}
              className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              disabled={generating}
            >
              <Plus className="h-4 w-4" />
              문서 생성
            </button>
          </div>
        </div>

        {/* 에러 메시지 */}
        {error && (
          <div className="mb-4 flex items-center gap-2 rounded-lg bg-red-50 p-4 text-red-700">
            <AlertCircle className="h-5 w-5" />
            {error}
          </div>
        )}

        {/* 통계 */}
        <div className="mb-6 grid grid-cols-3 gap-4">
          <div className="rounded-lg bg-white p-4 shadow-sm">
            <div className="text-2xl font-bold text-blue-600">
              {totalElements}
            </div>
            <div className="text-sm text-gray-500">총 문서 수</div>
          </div>
          <div className="rounded-lg bg-white p-4 shadow-sm">
            <div className="text-2xl font-bold text-green-600">
              {
                documents.filter(d => d.documentType === 'PROJECT_ANALYSIS')
                  .length
              }
            </div>
            <div className="text-sm text-gray-500">프로젝트 분석</div>
          </div>
          <div className="rounded-lg bg-white p-4 shadow-sm">
            <div className="text-2xl font-bold text-orange-600">
              {
                documents.filter(d => d.documentType === 'ERROR_ANALYSIS')
                  .length
              }
            </div>
            <div className="text-sm text-gray-500">에러 분석</div>
          </div>
        </div>

        {/* 문서 목록 테이블 */}
        <div className="overflow-hidden rounded-lg bg-white shadow-sm">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
                  ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
                  제목
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
                  타입
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
                  상태
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
                  건강 점수
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
                  생성일
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium tracking-wider text-gray-500 uppercase">
                  작업
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {loading ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-6 py-8 text-center text-gray-500"
                  >
                    <RefreshCw className="mx-auto h-8 w-8 animate-spin text-blue-600" />
                    <p className="mt-2">문서 목록을 불러오는 중...</p>
                  </td>
                </tr>
              ) : documents.length === 0 ? (
                <tr>
                  <td
                    colSpan={7}
                    className="px-6 py-8 text-center text-gray-500"
                  >
                    <FileText className="mx-auto h-12 w-12 text-gray-300" />
                    <p className="mt-2">생성된 문서가 없습니다.</p>
                    <p className="text-sm">
                      위의 &quot;문서 생성&quot; 버튼을 클릭하여 새 문서를
                      만들어보세요.
                    </p>
                  </td>
                </tr>
              ) : (
                documents.map(doc => (
                  <tr key={doc.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm whitespace-nowrap text-gray-900">
                      {doc.id}
                    </td>
                    <td className="px-6 py-4 text-sm font-medium text-gray-900">
                      {doc.title}
                    </td>
                    <td className="px-6 py-4 text-sm whitespace-nowrap">
                      <span
                        className={`inline-flex rounded-full px-2 text-xs leading-5 font-semibold ${
                          doc.documentType === 'PROJECT_ANALYSIS'
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-orange-100 text-orange-800'
                        }`}
                      >
                        {getDocumentTypeLabel(doc.documentType)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm whitespace-nowrap">
                      <span
                        className={`inline-flex rounded-full px-2 text-xs leading-5 font-semibold ${
                          doc.validationStatus === 'VALID'
                            ? 'bg-green-100 text-green-800'
                            : 'bg-yellow-100 text-yellow-800'
                        }`}
                      >
                        {doc.validationStatus}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm whitespace-nowrap text-gray-900">
                      {doc.healthScore ? `${doc.healthScore}/100` : '-'}
                    </td>
                    <td className="px-6 py-4 text-sm whitespace-nowrap text-gray-500">
                      {formatDate(doc.createdAt)}
                    </td>
                    <td className="px-6 py-4 text-sm whitespace-nowrap">
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleViewDocument(doc.id)}
                          className="text-blue-600 hover:text-blue-900"
                          title="보기"
                        >
                          <Eye className="h-5 w-5" />
                        </button>
                        <button
                          onClick={() => handleDeleteDocument(doc.id)}
                          className="text-red-600 hover:text-red-900"
                          title="삭제"
                        >
                          <Trash2 className="h-5 w-5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3">
              <div className="text-sm text-gray-700">
                총 {totalElements}개 중 {currentPage * pageSize + 1} -{' '}
                {Math.min((currentPage + 1) * pageSize, totalElements)}개 표시
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                  disabled={currentPage === 0}
                  className="rounded border border-gray-300 px-3 py-1 text-sm disabled:opacity-50"
                >
                  이전
                </button>
                <span className="px-3 py-1 text-sm">
                  {currentPage + 1} / {totalPages}
                </span>
                <button
                  onClick={() =>
                    setCurrentPage(p => Math.min(totalPages - 1, p + 1))
                  }
                  disabled={currentPage >= totalPages - 1}
                  className="rounded border border-gray-300 px-3 py-1 text-sm disabled:opacity-50"
                >
                  다음
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 문서 생성 모달 */}
      {showGenerateModal && (
        <div className="bg-opacity-50 fixed inset-0 z-50 flex items-center justify-center bg-black">
          <div className="w-full max-w-md rounded-lg bg-white p-6">
            <h3 className="mb-4 text-lg font-semibold">문서 생성</h3>

            <div className="mb-4">
              <label className="mb-2 block text-sm font-medium">
                문서 유형
              </label>
              <select
                value={generateType}
                onChange={e =>
                  setGenerateType(e.target.value as 'project' | 'error')
                }
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              >
                <option value="project">프로젝트 분석</option>
                <option value="error">에러 분석</option>
              </select>
            </div>

            {generateType === 'error' && (
              <div className="mb-4">
                <label className="mb-2 block text-sm font-medium">
                  로그 ID
                </label>
                <input
                  type="number"
                  value={logIdInput}
                  onChange={e => setLogIdInput(e.target.value)}
                  placeholder="에러 로그 ID를 입력하세요"
                  className="w-full rounded-lg border border-gray-300 px-3 py-2"
                />
              </div>
            )}

            <div className="flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowGenerateModal(false);
                  setLogIdInput('');
                }}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm"
                disabled={generating}
              >
                취소
              </button>
              <button
                onClick={
                  generateType === 'project'
                    ? handleGenerateProjectAnalysis
                    : handleGenerateErrorAnalysis
                }
                disabled={
                  generating || (generateType === 'error' && !logIdInput)
                }
                className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm text-white disabled:opacity-50"
              >
                {generating && <RefreshCw className="h-4 w-4 animate-spin" />}
                {generating ? '생성 중...' : '생성'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 문서 뷰어 모달 */}
      {showViewer && selectedDocument && (
        <div className="bg-opacity-50 fixed inset-0 z-50 flex items-center justify-center bg-black">
          <div className="flex h-[90vh] w-[90vw] flex-col rounded-lg bg-white">
            <div className="flex items-center justify-between border-b p-4">
              <h3 className="text-lg font-semibold">
                {selectedDocument.title}
              </h3>
              <div className="flex items-center gap-3">
                <button
                  onClick={handleDownloadDocument}
                  className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700"
                >
                  <Download className="h-4 w-4" />
                  다운로드
                </button>
                <button
                  onClick={() => {
                    setShowViewer(false);
                    setSelectedDocument(null);
                  }}
                  className="text-gray-500 hover:text-gray-700"
                >
                  닫기
                </button>
              </div>
            </div>
            <div className="flex-1 overflow-auto p-4">
              <iframe
                srcDoc={selectedDocument.content}
                className="h-full w-full border-0"
                title="Document Viewer"
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DocumentsPage;
