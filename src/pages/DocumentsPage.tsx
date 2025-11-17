import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  FileText,
  Plus,
  XCircle,
  RefreshCw,
  AlertCircle,
  Download,
  Printer,
} from 'lucide-react';
import {
  generateProjectAnalysis,
  generateErrorAnalysis,
  getAnalysisDocuments,
  getAnalysisDocumentById,
  deleteAnalysisDocument,
} from '@/services/analysisService';
import { searchLogs } from '@/services/logService';
import type {
  AnalysisDocumentSummary,
  AnalysisDocumentDetailResponse,
  PageResponse,
  DocumentFormat,
} from '@/types/analysis';
import type { LogData } from '@/types/log';

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
  const [errorLogs, setErrorLogs] = useState<LogData[]>([]);
  const [loadingErrorLogs, setLoadingErrorLogs] = useState(false);

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

  // PDF로 인쇄
  const handlePrintAsPdf = () => {
    if (!selectedDocument) {
      return;
    }

    const printWindow = window.open('', '_blank');
    if (printWindow) {
      printWindow.document.write(selectedDocument.content);
      printWindow.document.close();
      printWindow.print();
    }
  };

  // 에러 로그 조회
  const fetchErrorLogs = useCallback(async () => {
    if (!projectUuid) {
      return;
    }

    setLoadingErrorLogs(true);
    try {
      const response = await searchLogs({
        projectUuid,
        logLevel: ['ERROR'],
        size: 20,
        sort: 'TIMESTAMP,DESC',
      });

      if ('logs' in response) {
        setErrorLogs(response.logs);
      }
    } catch (err) {
      console.error('에러 로그 조회 실패:', err);
    } finally {
      setLoadingErrorLogs(false);
    }
  }, [projectUuid]);

  // 모달이 열릴 때 에러 로그 조회
  useEffect(() => {
    if (showGenerateModal && generateType === 'error') {
      fetchErrorLogs();
    }
  }, [showGenerateModal, generateType, fetchErrorLogs]);

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
    <div className="font-pretendard space-y-6 p-6 py-1">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-godoM text-2xl font-bold text-gray-900">
            문서 작성
          </h1>
          <p className="mt-1 text-sm text-gray-600">
            AI 기반 프로젝트 분석 및 에러 분석 문서를 생성하고 관리합니다.
          </p>
        </div>

        <div className="flex gap-3">
          <button
            onClick={fetchDocuments}
            className="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:opacity-50"
            disabled={loading}
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            새로고침
          </button>
          <button
            onClick={() => setShowGenerateModal(true)}
            className="bg-primary hover:bg-primary/90 flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium text-white transition-colors disabled:opacity-50"
            disabled={generating}
          >
            <Plus className="h-4 w-4" />
            문서 생성
          </button>
        </div>
      </div>

      {/* 에러 메시지 */}
      {error && (
        <div className="flex items-center gap-2 rounded-lg bg-red-50 p-4 text-red-700">
          <AlertCircle className="h-5 w-5 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* 통계 카드 */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div className="rounded-lg border bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">총 문서 수</p>
              <p className="mt-2 text-3xl font-bold text-gray-900">
                {totalElements}
              </p>
            </div>
            <div className="bg-primary/10 flex h-12 w-12 items-center justify-center rounded-lg">
              <FileText className="text-primary h-6 w-6" />
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">프로젝트 분석</p>
              <p className="mt-2 text-3xl font-bold text-gray-900">
                {
                  documents.filter(d => d.documentType === 'PROJECT_ANALYSIS')
                    .length
                }
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-emerald-100">
              <FileText className="h-6 w-6 text-emerald-600" />
            </div>
          </div>
        </div>

        <div className="rounded-lg border bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">에러 분석</p>
              <p className="mt-2 text-3xl font-bold text-gray-900">
                {
                  documents.filter(d => d.documentType === 'ERROR_ANALYSIS')
                    .length
                }
              </p>
            </div>
            <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-amber-100">
              <AlertCircle className="h-6 w-6 text-amber-600" />
            </div>
          </div>
        </div>
      </div>

      {/* 문서 목록 테이블 */}
      <div className="overflow-hidden rounded-lg border bg-white shadow-sm">
        <div className="overflow-x-auto">
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
                  {/* 작업 */}
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
                  <tr
                    key={doc.id}
                    onClick={() => handleViewDocument(doc.id)}
                    className="cursor-pointer transition-colors hover:bg-gray-50"
                  >
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
                    <td
                      className="px-6 py-4 text-sm whitespace-nowrap"
                      onClick={e => e.stopPropagation()}
                    >
                      <div className="flex justify-center">
                        <button
                          onClick={() => handleDeleteDocument(doc.id)}
                          className="text-gray-400 transition-colors hover:text-red-500"
                          title="삭제"
                        >
                          <XCircle className="h-5 w-5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* 페이지네이션 */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-200 bg-white px-6 py-4">
            <div className="text-sm text-gray-700">
              총 {totalElements}개 중 {currentPage * pageSize + 1} -{' '}
              {Math.min((currentPage + 1) * pageSize, totalElements)}개 표시
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                disabled={currentPage === 0}
                className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                이전
              </button>
              <span className="flex items-center px-3 py-1.5 text-sm font-medium text-gray-700">
                {currentPage + 1} / {totalPages}
              </span>
              <button
                onClick={() =>
                  setCurrentPage(p => Math.min(totalPages - 1, p + 1))
                }
                disabled={currentPage >= totalPages - 1}
                className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                다음
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 문서 생성 모달 */}
      {showGenerateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="w-full max-w-md rounded-xl border bg-white p-6 shadow-2xl">
            <h3 className="font-godoM mb-6 text-xl font-bold text-gray-900">
              문서 생성
            </h3>

            <div className="mb-6">
              <label className="mb-2 block text-sm font-medium text-gray-700">
                문서 유형
              </label>
              <select
                value={generateType}
                onChange={e =>
                  setGenerateType(e.target.value as 'project' | 'error')
                }
                className="focus:border-primary focus:ring-primary/20 w-full appearance-none rounded-lg border border-gray-300 bg-white px-4 py-2.5 pr-10 text-sm transition-colors focus:ring-2 focus:outline-none"
                style={{
                  backgroundImage: `url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e")`,
                  backgroundPosition: 'right 0.75rem center',
                  backgroundRepeat: 'no-repeat',
                  backgroundSize: '1.25em 1.25em',
                }}
              >
                <option value="project">프로젝트 분석</option>
                <option value="error">에러 분석</option>
              </select>
            </div>

            {generateType === 'error' && (
              <div className="mb-6">
                <label className="mb-2 block text-sm font-medium text-gray-700">
                  에러 로그 선택
                </label>
                {loadingErrorLogs ? (
                  <div className="flex items-center justify-center rounded-lg border border-gray-200 py-8">
                    <RefreshCw className="text-primary h-5 w-5 animate-spin" />
                    <span className="ml-2 text-sm text-gray-600">
                      에러 로그 조회 중...
                    </span>
                  </div>
                ) : errorLogs.length === 0 ? (
                  <div className="rounded-lg border border-gray-200 bg-gray-50 py-8 text-center text-sm text-gray-500">
                    최근 에러 로그가 없습니다.
                  </div>
                ) : (
                  <div className="max-h-64 overflow-y-auto rounded-lg border border-gray-300">
                    {errorLogs.map(log => (
                      <button
                        key={log.logId}
                        type="button"
                        onClick={() => setLogIdInput(String(log.logId))}
                        className={`w-full border-b border-gray-200 p-4 text-left transition-colors last:border-b-0 hover:bg-gray-50 ${
                          logIdInput === String(log.logId)
                            ? 'bg-primary/10 border-primary/20'
                            : ''
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <span className="text-sm font-semibold text-gray-900">
                            #{log.logId}
                          </span>
                          <span className="rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800">
                            {log.logLevel}
                          </span>
                        </div>
                        <div className="mt-2 truncate text-sm text-gray-600">
                          {log.message}
                        </div>
                        <div className="mt-1 text-xs text-gray-400">
                          {new Date(log.timestamp).toLocaleString('ko-KR')}
                        </div>
                      </button>
                    ))}
                  </div>
                )}
                <p className="mt-3 text-xs text-gray-500">
                  최근 20개의 에러 로그를 표시합니다.
                </p>
              </div>
            )}

            <div className="flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowGenerateModal(false);
                  setLogIdInput('');
                }}
                className="rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:opacity-50"
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
                className="bg-primary hover:bg-primary/90 flex items-center gap-2 rounded-lg px-5 py-2.5 text-sm font-medium text-white transition-colors disabled:cursor-not-allowed disabled:opacity-50"
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm">
          <div className="flex h-[90vh] w-full max-w-7xl flex-col overflow-hidden rounded-xl border bg-white shadow-2xl">
            {/* 헤더 */}
            <div className="flex flex-shrink-0 items-center justify-between border-b bg-gray-50 px-6 py-4">
              <div className="min-w-0 flex-1">
                <h3 className="font-godoM truncate text-xl font-bold text-gray-900">
                  {selectedDocument.title}
                </h3>
                <p className="mt-1 text-sm text-gray-600">
                  {getDocumentTypeLabel(selectedDocument.documentType)}
                </p>
              </div>
              <div className="ml-6 flex flex-shrink-0 items-center gap-2">
                <button
                  onClick={handleDownloadDocument}
                  className="flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-emerald-700"
                >
                  <Download className="h-4 w-4" />
                  <span>HTML</span>
                </button>
                <button
                  onClick={handlePrintAsPdf}
                  className="flex items-center gap-2 rounded-lg bg-purple-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-purple-700"
                >
                  <Printer className="h-4 w-4" />
                  <span>PDF</span>
                </button>
                <button
                  onClick={() => {
                    setShowViewer(false);
                    setSelectedDocument(null);
                  }}
                  className="rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
                >
                  닫기
                </button>
              </div>
            </div>

            {/* 문서 내용 */}
            <div className="flex-1 overflow-hidden bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 p-8">
              <div className="relative h-full overflow-hidden rounded-2xl bg-white shadow-2xl">
                {/* 화려한 장식 요소 */}
                <div className="absolute -top-10 -right-10 h-40 w-40 rounded-full bg-gradient-to-br from-blue-400 to-purple-400 opacity-20 blur-2xl"></div>
                <div className="absolute -bottom-10 -left-10 h-40 w-40 rounded-full bg-gradient-to-tr from-emerald-400 to-blue-400 opacity-20 blur-2xl"></div>

                {/* iframe 컨테이너 */}
                <div className="relative z-10 h-full overflow-auto rounded-xl bg-white p-1">
                  <iframe
                    srcDoc={selectedDocument.content}
                    className="h-full w-full rounded-lg border-0"
                    title="Document Viewer"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DocumentsPage;
