// AI 분석 문서 뷰어 컴포넌트

import React, { useState, useRef, useEffect, useMemo } from 'react';
import { X, Download, Printer } from 'lucide-react';
import { marked } from 'marked';
import { exportAnalysisDocumentToPDF } from '@/utils/analysisDocumentPdfExport';
import type { AnalysisDocumentResponse } from '@/types/analysis';

interface AnalysisDocumentViewerProps {
  /**
   * 분석 문서 응답 데이터
   */
  document: AnalysisDocumentResponse;

  /**
   * 닫기 콜백
   */
  onClose: () => void;

  /**
   * 전체 화면 모드 (기본: true)
   */
  fullScreen?: boolean;
}

/**
 * AI 분석 문서를 HTML로 표시하고 PDF 다운로드 기능을 제공하는 컴포넌트
 *
 * @example
 * ```typescript
 * const [document, setDocument] = useState<AnalysisDocumentResponse | null>(null);
 *
 * const handleGenerateAnalysis = async () => {
 *   const response = await generateProjectAnalysis(projectUuid, {
 *     format: 'HTML',
 *     startTime: '2025-01-01T00:00:00',
 *     endTime: '2025-01-16T23:59:59'
 *   });
 *   setDocument(response);
 * };
 *
 * return (
 *   <>
 *     <button onClick={handleGenerateAnalysis}>분석 보기</button>
 *     {document && (
 *       <AnalysisDocumentViewer
 *         document={document}
 *         onClose={() => setDocument(null)}
 *       />
 *     )}
 *   </>
 * );
 * ```
 */
// Markdown을 HTML로 변환하는 헬퍼 함수
const processMarkdownInHtml = (htmlContent: string): string => {
  // marked 옵션 설정
  marked.setOptions({
    breaks: true, // 줄바꿈을 <br>로 변환
    gfm: true, // GitHub Flavored Markdown 지원
  });

  // HTML 파서를 사용하여 텍스트 노드에서 Markdown 변환
  const parser = new DOMParser();
  const doc = parser.parseFromString(htmlContent, 'text/html');

  // Markdown 패턴을 포함할 수 있는 텍스트 노드 찾기
  const textWalker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT, null);

  const nodesToProcess: { node: Text; parent: Element }[] = [];

  let textNode: Text | null;
  while ((textNode = textWalker.nextNode() as Text | null)) {
    const text = textNode.textContent || '';
    // Markdown 패턴 감지: **, ###, - [ ], 등
    const hasMarkdown =
      /\*\*[^*]+\*\*|###\s|##\s|#\s|- \[ \]|- \[x\]|\n- /.test(text);
    if (hasMarkdown && textNode.parentElement) {
      nodesToProcess.push({ node: textNode, parent: textNode.parentElement });
    }
  }

  // Markdown을 HTML로 변환
  nodesToProcess.forEach(({ node, parent }) => {
    const markdownText = node.textContent || '';
    const htmlResult = marked.parse(markdownText) as string;

    // 새 HTML 요소 생성
    const wrapper = doc.createElement('div');
    wrapper.innerHTML = htmlResult;

    // 기존 텍스트 노드를 새 HTML로 대체
    const fragment = doc.createDocumentFragment();
    while (wrapper.firstChild) {
      fragment.appendChild(wrapper.firstChild);
    }
    parent.replaceChild(fragment, node);
  });

  return doc.documentElement.outerHTML;
};

export const AnalysisDocumentViewer: React.FC<AnalysisDocumentViewerProps> = ({
  document,
  onClose,
  fullScreen = true,
}) => {
  const [isDownloading, setIsDownloading] = useState(false);
  const iframeRef = useRef<HTMLIFrameElement>(null);

  // Markdown을 HTML로 변환한 콘텐츠
  const processedContent = useMemo(() => {
    if (!document.content) {
      return '';
    }
    try {
      return processMarkdownInHtml(document.content);
    } catch (error) {
      console.error('Markdown 변환 실패:', error);
      return document.content; // 실패 시 원본 반환
    }
  }, [document.content]);

  // ESC 키로 닫기
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [onClose]);

  // PDF로 다운로드
  const handleDownloadPDF = async () => {
    if (!processedContent) {
      alert('다운로드할 문서 내용이 없습니다.');
      return;
    }

    setIsDownloading(true);
    try {
      const fileName = `${document.documentMetadata.title.replace(/\s+/g, '-')}-${new Date().toISOString().split('T')[0]}.pdf`;
      await exportAnalysisDocumentToPDF(processedContent, fileName);
    } catch (error) {
      console.error('PDF 다운로드 실패:', error);
      alert('PDF 다운로드 중 오류가 발생했습니다.');
    } finally {
      setIsDownloading(false);
    }
  };

  // 인쇄
  const handlePrint = () => {
    if (iframeRef.current?.contentWindow) {
      iframeRef.current.contentWindow.print();
    }
  };

  // 백드롭 클릭 시 닫기
  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div
      className={`fixed inset-0 z-50 flex items-center justify-center bg-black/50 ${fullScreen ? '' : 'p-4'}`}
      onClick={handleBackdropClick}
    >
      <div
        className={`flex flex-col rounded-lg bg-white shadow-xl ${fullScreen ? 'h-full w-full' : 'h-[90vh] w-[90vw] max-w-7xl'}`}
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between border-b px-6 py-4">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">
              {document.documentMetadata.title}
            </h2>
            <p className="mt-1 text-sm text-gray-500">
              생성 시간:{' '}
              {new Date(document.documentMetadata.generatedAt).toLocaleString(
                'ko-KR',
              )}
              {document.documentMetadata.estimatedReadingTime && (
                <span className="ml-4">
                  예상 읽기 시간:{' '}
                  {document.documentMetadata.estimatedReadingTime}
                </span>
              )}
            </p>
          </div>

          <div className="flex items-center gap-2">
            {/* PDF 다운로드 버튼 */}
            <button
              onClick={handleDownloadPDF}
              disabled={isDownloading}
              className="flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-400"
              title="PDF로 저장"
            >
              <Download className="h-4 w-4" />
              {isDownloading ? '변환 중...' : 'PDF 저장'}
            </button>

            {/* 인쇄 버튼 */}
            <button
              onClick={handlePrint}
              className="flex items-center gap-2 rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
              title="인쇄"
            >
              <Printer className="h-4 w-4" />
              인쇄
            </button>

            {/* 닫기 버튼 */}
            <button
              onClick={onClose}
              className="p-2 text-gray-400 transition-colors hover:text-gray-600"
              title="닫기 (ESC)"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        {/* HTML 콘텐츠 영역 */}
        <div className="flex-1 overflow-hidden">
          {processedContent ? (
            <iframe
              ref={iframeRef}
              srcDoc={processedContent}
              className="h-full w-full border-0"
              title="분석 문서"
              sandbox="allow-same-origin allow-scripts"
            />
          ) : (
            <div className="flex h-full items-center justify-center">
              <p className="text-gray-500">문서 내용을 불러올 수 없습니다.</p>
            </div>
          )}
        </div>

        {/* 푸터 (메타데이터 표시) */}
        {document.documentMetadata.summary && (
          <div className="border-t bg-gray-50 px-6 py-3">
            <div className="flex items-center gap-6 text-sm text-gray-600">
              {document.documentMetadata.summary.healthScore !== undefined && (
                <span>
                  건강 점수:{' '}
                  <strong
                    className={
                      document.documentMetadata.summary.healthScore >= 80
                        ? 'text-green-600'
                        : document.documentMetadata.summary.healthScore >= 60
                          ? 'text-yellow-600'
                          : 'text-red-600'
                    }
                  >
                    {document.documentMetadata.summary.healthScore}/100
                  </strong>
                </span>
              )}
              {document.documentMetadata.summary.criticalIssues !==
                undefined && (
                <span>
                  심각한 이슈:{' '}
                  <strong>
                    {document.documentMetadata.summary.criticalIssues}건
                  </strong>
                </span>
              )}
              {document.documentMetadata.summary.totalIssues !== undefined && (
                <span>
                  전체 이슈:{' '}
                  <strong>
                    {document.documentMetadata.summary.totalIssues}건
                  </strong>
                </span>
              )}
              {document.documentMetadata.summary.severity && (
                <span>
                  심각도:{' '}
                  <strong
                    className={
                      document.documentMetadata.summary.severity ===
                        'CRITICAL' ||
                      document.documentMetadata.summary.severity === 'HIGH'
                        ? 'text-red-600'
                        : document.documentMetadata.summary.severity ===
                            'MEDIUM'
                          ? 'text-yellow-600'
                          : 'text-blue-600'
                    }
                  >
                    {document.documentMetadata.summary.severity}
                  </strong>
                </span>
              )}
              {document.documentMetadata.wordCount && (
                <span className="ml-auto">
                  총 {document.documentMetadata.wordCount.toLocaleString()}자
                </span>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AnalysisDocumentViewer;
