// AI 분석 문서를 PDF로 변환하는 유틸리티

import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';

/**
 * AI 분석 문서 HTML을 PDF로 변환하여 다운로드
 *
 * @param htmlContent - HTML 문자열
 * @param fileName - 저장할 파일명 (기본: 'analysis-document.pdf')
 *
 * @example
 * ```typescript
 * const response = await generateProjectAnalysis(projectUuid, {
 *   format: 'HTML',
 *   // ...
 * });
 *
 * await exportAnalysisDocumentToPDF(
 *   response.content,
 *   'project-analysis-2025-01-16.pdf'
 * );
 * ```
 */
export const exportAnalysisDocumentToPDF = async (
  htmlContent: string,
  fileName: string = 'analysis-document.pdf',
): Promise<void> => {
  try {
    // iframe을 사용하여 격리된 환경에서 HTML 렌더링
    const iframe = document.createElement('iframe');
    iframe.style.cssText = `
      position: fixed;
      left: -10000px;
      top: -10000px;
      width: 1200px;
      height: 800px;
      border: none;
      visibility: hidden;
      pointer-events: none;
    `;
    document.body.appendChild(iframe);

    // iframe 내부에 HTML 문서 작성
    const iframeDoc = iframe.contentDocument || iframe.contentWindow?.document;
    if (!iframeDoc) {
      document.body.removeChild(iframe);
      throw new Error('iframe 문서를 생성할 수 없습니다.');
    }

    iframeDoc.open();
    iframeDoc.write(htmlContent);
    iframeDoc.close();

    // iframe 내용이 로드될 때까지 대기 (이미지, 폰트 등)
    await new Promise(resolve => setTimeout(resolve, 1000));

    // jsPDF 인스턴스 생성
    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'pt',
      format: 'a4',
    });

    const pdfWidth = doc.internal.pageSize.getWidth();
    const pdfHeight = doc.internal.pageSize.getHeight();

    // iframe의 body를 캡처
    const targetElement = iframeDoc.body;

    // html2canvas로 DOM 캡처
    const canvas = await html2canvas(targetElement, {
      scale: 2, // 고해상도
      useCORS: true,
      backgroundColor: '#ffffff',
      windowWidth: 1200,
      windowHeight: targetElement.scrollHeight,
      onclone: (clonedDoc: Document) => {
        // 복제된 문서의 스타일 정리
        const allElements = clonedDoc.querySelectorAll('*');
        allElements.forEach((el: Element) => {
          if (el instanceof HTMLElement) {
            // oklch 등의 CSS 변수를 안전한 값으로 변환
            el.style.setProperty(
              'background-color',
              el.style.backgroundColor || 'transparent',
              'important',
            );
            el.style.setProperty(
              'color',
              el.style.color || '#000000',
              'important',
            );
            el.style.setProperty(
              'border-color',
              el.style.borderColor || '#cccccc',
              'important',
            );
            el.style.setProperty('text-decoration', 'none', 'important');
          }
        });
      },
    });

    // 캔버스를 이미지로 변환
    const imgData = canvas.toDataURL('image/png');

    // PDF 크기 계산 (좌우 마진 20pt)
    const imgWidth = pdfWidth - 40;
    const imgHeight = (canvas.height * imgWidth) / canvas.width;

    let heightLeft = imgHeight;
    let position = 20; // 상단 마진

    // 첫 페이지에 이미지 추가
    doc.addImage(imgData, 'PNG', 20, position, imgWidth, imgHeight);
    heightLeft -= pdfHeight - 40;

    // 필요한 경우 추가 페이지 생성
    while (heightLeft > 0) {
      position = heightLeft - imgHeight + 20;
      doc.addPage();
      doc.addImage(imgData, 'PNG', 20, position, imgWidth, imgHeight);
      heightLeft -= pdfHeight - 40;
    }

    // iframe 제거
    if (document.body.contains(iframe)) {
      document.body.removeChild(iframe);
    }

    // PDF 저장
    doc.save(fileName);
  } catch (error) {
    console.error('PDF 생성 오류:', error);
    throw error;
  }
};

/**
 * 브라우저의 인쇄 기능을 사용하여 PDF 생성
 * (더 간단하고 빠른 대안)
 *
 * @param htmlContent - HTML 문자열
 *
 * @example
 * ```typescript
 * printAnalysisDocument(response.content);
 * ```
 */
export const printAnalysisDocument = (htmlContent: string): void => {
  // 새 창에서 인쇄 대화상자 열기
  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    alert('팝업이 차단되었습니다. 팝업 허용 후 다시 시도해주세요.');
    return;
  }

  printWindow.document.write(htmlContent);
  printWindow.document.close();

  // 문서 로드 후 인쇄
  printWindow.onload = () => {
    printWindow.print();
  };
};
