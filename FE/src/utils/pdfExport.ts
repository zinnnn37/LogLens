import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';
import { marked } from 'marked';

// Message 타입 정의
export interface Message {
  role: 'user' | 'assistant';
  content: string;
  isComplete?: boolean;
}

// marked 옵션 설정 초기화
const initializeMarked = () => {
  marked.setOptions({
    gfm: true,
    breaks: true,
  });
};

initializeMarked();

// PDF 스타일 정의 - Tailwind oklch 변수를 HEX로 오버라이드
const PDF_EXPORT_STYLES = `
  <style>
    :root {
      --background: #ffffff;
      --foreground: #0f172a;
      --card: #ffffff;
      --card-foreground: #0f172a;
      --popover: #ffffff;
      --popover-foreground: #0f172a;
      --primary: #1d4ed8;
      --primary-foreground: #f8fafc;
      --secondary: #0ea5e9;
      --secondary-foreground: #f8fafc;
      --muted: #f1f5f9;
      --muted-foreground: #64748b;
      --accent: #f1f5f9;
      --accent-foreground: #334155;
      --destructive: #dc2626;
      --destructive-foreground: #f8fafc;
      --border: #e2e8f0;
      --input: #e2e8f0;
      --ring: #1d4ed8;
      --chart-1: #f59e0b;
      --chart-2: #10b981;
      --chart-3: #3b82f6;
      --chart-4: #8b5cf6;
      --chart-5: #ef4444;
      --sidebar: #f8fafc;
      --sidebar-foreground: #0f172a;
      --sidebar-primary: #1d4ed8;
      --sidebar-primary-foreground: #f8fafc;
      --sidebar-accent: #f1f5f9;
      --sidebar-accent-foreground: #334155;
      --sidebar-border: #e2e8f0;
      --sidebar-ring: #1d4ed8;
    }

    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
      text-decoration: none;
    }

    body {
      font-family: 'Pretendard', 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
      font-size: 14px;
      line-height: 1.6;
      color: #0f172a;
      background: #ffffff;
      padding: 20px;
    }

    h1, h2, h3, h4, h5, h6 {
      margin-top: 24px;
      margin-bottom: 16px;
      font-weight: 600;
      line-height: 1.25;
      color: #0f172a;
      text-decoration: none;
    }
    h1 { font-size: 32px; border-bottom: 1px solid #e2e8f0; padding-bottom: 8px; text-decoration: none; }
    h2 { font-size: 24px; border-bottom: 1px solid #e2e8f0; padding-bottom: 8px; text-decoration: none; }
    h3 { font-size: 20px; text-decoration: none; }
    h4 { font-size: 16px; text-decoration: none; }
    h5 { font-size: 14px; text-decoration: none; }
    h6 { font-size: 13px; color: #64748b; text-decoration: none; }

    p {
      margin-bottom: 16px;
      color: #0f172a;
    }

    ul, ol {
      margin-bottom: 16px;
      padding-left: 32px;
    }

    li {
      margin-bottom: 4px;
    }

    code {
      background-color: #f1f5f9;
      padding: 2px 6px;
      border-radius: 3px;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      font-size: 85%;
      color: #0f172a;
    }

    pre {
      background-color: #f1f5f9;
      padding: 16px;
      border-radius: 6px;
      overflow-x: auto;
      margin-bottom: 16px;
    }

    pre code {
      background-color: transparent;
      padding: 0;
      font-size: 12px;
      line-height: 1.45;
    }

    blockquote {
      padding: 0 16px;
      border-left: 4px solid #e2e8f0;
      color: #64748b;
      margin-bottom: 16px;
    }

    table {
      border-collapse: collapse;
      width: 100%;
      margin-bottom: 16px;
    }

    table th,
    table td {
      border: 1px solid #e2e8f0;
      padding: 6px 13px;
      color: #0f172a;
    }

    table th {
      background-color: #f1f5f9;
      font-weight: 600;
    }

    table tr:nth-child(2n) {
      background-color: #f8fafc;
    }

    strong {
      font-weight: 600;
    }

    em {
      font-style: italic;
    }

    a {
      color: #1d4ed8;
      text-decoration: none;
    }

    hr {
      border: none;
      border-top: 1px solid #e2e8f0;
      margin: 24px 0;
    }

    .answer-section {
      margin-bottom: 40px;
      page-break-inside: avoid;
    }

    .answer-header {
      font-size: 18px;
      font-weight: 600;
      color: #1d4ed8;
      margin-bottom: 12px;
      padding-bottom: 8px;
      border-bottom: 2px solid #1d4ed8;
    }
  </style>
`;

/**
 * AI 채팅 히스토리를 PDF로 다운로드
 * @param messages - 전체 메시지 배열
 */
export const exportChatToPDF = async (messages: Message[]): Promise<void> => {
  // AI 답변만 필터링
  const aiMessages = messages.filter(msg => msg.role === 'assistant');

  if (aiMessages.length === 0) {
    alert('저장할 AI 답변이 없습니다.');
    return;
  }

  try {
    // AI 답변들을 마크다운에서 HTML로 변환
    const answersHtml = aiMessages
      .map((msg, index) => {
        const htmlContent = marked.parse(msg.content) as string;
        return `
          <div class="answer-section">
            <div class="answer-header">답변 ${index + 1}</div>
            <div>${htmlContent}</div>
          </div>
        `;
      })
      .join('');

    // 완성된 HTML 문서 생성 (스타일 포함)
    const htmlDocument = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        ${PDF_EXPORT_STYLES}
      </head>
      <body>
        <h1>LogLens AI Chat History</h1>
        <p style="color: #64748b; margin-bottom: 32px;">생성일: ${new Date().toLocaleString('ko-KR')}</p>
        <div class="answers-container">
          ${answersHtml}
        </div>
      </body>
      </html>
    `;

    // iframe을 사용하여 완전히 격리된 환경에서 렌더링
    const iframe = document.createElement('iframe');
    iframe.style.cssText = `
      position: fixed;
      left: -10000px;
      top: -10000px;
      width: 800px;
      height: 600px;
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
    iframeDoc.write(htmlDocument);
    iframeDoc.close();

    // iframe 내용이 로드될 때까지 대기
    await new Promise(resolve => setTimeout(resolve, 500));

    // jsPDF 인스턴스 생성
    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'pt',
      format: 'a4',
    });

    // iframe의 body를 캡처
    const targetElement = iframeDoc.body;

    // html2canvas로 DOM 캡처
    const canvas = await html2canvas(targetElement, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      windowWidth: 800,
      windowHeight: targetElement.scrollHeight,
      onclone: (clonedDoc: Document) => {
        // 복제된 문서의 모든 요소를 찾아서 oklch 제거
        const allElements = clonedDoc.querySelectorAll('*');
        allElements.forEach((el: Element) => {
          if (el instanceof HTMLElement) {
            // 모든 스타일 속성을 안전한 값으로 덮어쓰기
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

    // PDF 크기 계산
    const pdfWidth = doc.internal.pageSize.getWidth();
    const pdfHeight = doc.internal.pageSize.getHeight();
    const imgWidth = pdfWidth - 40; // 좌우 마진 20씩
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
    const fileName = `loglens-ai-chat-${new Date()
      .toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      })
      .replace(/\. /g, '-')
      .replace('.', '')}.pdf`;
    doc.save(fileName);
  } catch (error) {
    console.error('PDF 생성 오류:', error);
    alert('PDF 생성 중 오류가 발생했습니다.');
  }
};
