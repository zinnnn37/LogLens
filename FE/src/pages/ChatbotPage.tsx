import { useState, useRef, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import ChatMessage, { type Message } from '@/components/ChatMessage';
import InitialMessages from '@/components/InitialMessages';
import ChatInput from '@/components/ChatInput';
import { API_PATH } from '@/constants/api-path';
import { useAuthStore } from '@/stores/authStore';
import { FileDown } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { exportChatToPDF } from '@/utils/pdfExport';

const ChatbotPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();
  const { accessToken } = useAuthStore();

  const [messages, setMessages] = useState<Message[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 메시지 추가 시 자동 스크롤
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = async (content: string) => {
    if (!content.trim()) {
      return;
    }

    // 사용자 메시지 추가
    const userMessage: Message = { role: 'user', content };
    setMessages(prev => [...prev, userMessage]);
    setIsStreaming(true);

    try {
      // 대화 히스토리 생성 (현재 메시지 제외)
      const chatHistory = messages.map(msg => ({
        role: msg.role,
        content: msg.content,
      }));

      // API 요청
      const baseUrl = import.meta.env.VITE_API_AI_URL;
      if (!baseUrl) {
        throw new Error(
          'AI API URL이 설정되지 않았습니다. .env 파일을 확인해주세요.',
        );
      }

      const url = `${baseUrl}${API_PATH.CHATBOT_STREAM}`;

      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
        },
        credentials: 'include',
        body: JSON.stringify({
          question: content,
          project_uuid: projectUuid || 'testproject',
          chat_history: chatHistory,
        }),
      }).catch(err => {
        // 네트워크 에러를 더 자세히 표시
        console.error('네트워크 요청 실패:', err);
        throw new Error(
          `네트워크 연결 실패: AI 서버(${baseUrl})에 연결할 수 없습니다. 서버가 실행 중인지 확인해주세요.`,
        );
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error('서버 응답 에러:', response.status, errorText);
        throw new Error(
          `서버 응답 오류 (${response.status}): ${errorText || '알 수 없는 오류'}`,
        );
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('응답을 읽을 수 없습니다.');
      }

      const decoder = new TextDecoder();
      let buffer = '';
      let fullText = '';
      let hasStartedResponse = false;

      // SSE 스트리밍 처리
      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (!line.startsWith('data: ')) {
            continue;
          }

          const data = line.slice(6); // "data: " 이후의 내용

          // 완료 시그널
          if (data.trim() === '[DONE]') {
            setIsStreaming(false);
            if (hasStartedResponse) {
              setMessages(prev => {
                const updated = [...prev];
                updated[updated.length - 1] = {
                  ...updated[updated.length - 1],
                  isComplete: true,
                };
                return updated;
              });
            }
            continue;
          }

          // 에러 시그널
          if (data.trim() === '[ERROR]') {
            continue;
          }

          // JSON 에러 메시지 체크
          if (data.trim().startsWith('{')) {
            try {
              const parsed = JSON.parse(data.trim());
              if (parsed.error) {
                throw new Error(parsed.error);
              }
            } catch (e) {
              if (e instanceof SyntaxError) {
                // JSON 파싱 실패시 일반 텍스트로 처리
                fullText += data;
              } else {
                throw e;
              }
            }
            continue;
          }

          // 데이터 처리 - 각 청크를 누적
          fullText += data;

          // 이스케이프된 문자들을 실제 문자로 변환
          const displayText = fullText
            .replace(/\\n/g, '\n')
            .replace(/\\\*/g, '*')
            .replace(/\\_/g, '_')
            .replace(/\\`/g, '`')
            .replace(/\\\[/g, '[')
            .replace(/\\\]/g, ']');

          // 화면 업데이트 (누적 텍스트 표시)
          if (!hasStartedResponse && displayText.trim().length > 0) {
            hasStartedResponse = true;
            setMessages(prev => [
              ...prev,
              { role: 'assistant', content: displayText, isComplete: false },
            ]);
          } else if (hasStartedResponse) {
            setMessages(prev => {
              const updated = [...prev];
              updated[updated.length - 1] = {
                role: 'assistant',
                content: displayText,
                isComplete: false,
              };
              return updated;
            });
          }
        }
      }
    } catch (error) {
      console.error('챗봇 오류:', error);

      // 에러 메시지 표시
      setMessages(prev => [
        ...prev,
        {
          role: 'assistant',
          content:
            error instanceof Error
              ? `오류: ${error.message}`
              : '메시지 전송 중 오류가 발생했습니다.',
        },
      ]);
    } finally {
      setIsStreaming(false);
      // 스트리밍이 끝났으면 마지막 메시지를 완료 상태로 변경
      setMessages(prev => {
        const updated = [...prev];
        if (
          updated.length > 0 &&
          updated[updated.length - 1].role === 'assistant'
        ) {
          updated[updated.length - 1] = {
            ...updated[updated.length - 1],
            isComplete: true,
          };
        }
        return updated;
      });
    }
  };

  const handleQuestionClick = (question: string) => {
    handleSendMessage(question);
  };

  // PDF 다운로드 핸들러
  const handleDownloadPDF = async () => {
    await exportChatToPDF(messages);
  };

  // 첫 번째 AI 답변이 완료되었는지 확인
  const hasCompletedAIResponse = messages.some(
    msg => msg.role === 'assistant' && msg.isComplete,
  );

  return (
    <div className="relative flex h-full flex-col gap-4">
      {/* 채팅 영역 */}
      <div className="flex-1 overflow-hidden rounded-lg bg-sky-50 p-4">
        <div className="h-full overflow-y-auto">
          {messages.length === 0 ? (
            <InitialMessages onQuestionClick={handleQuestionClick} />
          ) : (
            <div className="flex flex-col gap-4">
              {messages.map((message, index) => (
                <ChatMessage key={index} message={message} />
              ))}

              {/* 로딩 표시 - 응답 대기 중일 때만 */}
              {isStreaming &&
                messages[messages.length - 1]?.role !== 'assistant' && (
                  <div className="flex gap-3 px-4">
                    <div className="bg-primary/10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
                      <div className="bg-primary h-2 w-2 animate-pulse rounded-full" />
                    </div>
                    <div className="text-muted-foreground flex items-center text-sm">
                      답변 생성 중...
                    </div>
                  </div>
                )}

              {/* 추천 질문 */}
              {!isStreaming && (
                <div className="mt-4">
                  <InitialMessages
                    onQuestionClick={handleQuestionClick}
                    showGreeting={false}
                  />
                </div>
              )}

              <div ref={messagesEndRef} />
            </div>
          )}
        </div>
      </div>

      {/* 입력 영역 */}
      <div className="shrink-0">
        <ChatInput onSendMessage={handleSendMessage} disabled={isStreaming} />
      </div>

      {/* 플로팅 PDF 다운로드 버튼 */}
      {hasCompletedAIResponse && (
        <div className="group fixed right-6 bottom-24">
          <Button
            onClick={handleDownloadPDF}
            className="h-14 w-14 rounded-full p-0 shadow-lg transition-all hover:scale-110 hover:shadow-xl"
          >
            <FileDown className="h-6 w-6" />
          </Button>
          {/* 툴팁 */}
          <div className="pointer-events-none absolute top-1/2 right-full mr-3 -translate-y-1/2 whitespace-nowrap opacity-0 transition-opacity group-hover:opacity-100">
            <div className="rounded-lg bg-gray-900 px-3 py-2 text-sm text-white shadow-lg">
              AI 답변 PDF로 저장
              <div className="absolute top-1/2 right-0 translate-x-full -translate-y-1/2">
                <div className="border-8 border-transparent border-l-gray-900"></div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ChatbotPage;
