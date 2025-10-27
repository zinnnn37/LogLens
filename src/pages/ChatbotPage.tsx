import { useState, useRef, useEffect } from 'react';
import ChatMessage, { type Message } from '@/components/ChatMessage';
import InitialMessages from '@/components/InitialMessages';
import ChatInput from '@/components/ChatInput';

const ChatbotPage = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 메시지 추가 시 자동 스크롤
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = async (content: string) => {
    // 사용자 메시지 추가
    const userMessage: Message = { role: 'user', content };
    setMessages(prev => [...prev, userMessage]);
    setIsLoading(true);

    try {
      // TODO: 실제 API 호출로 교체
      // 임시 응답
      await new Promise(resolve => setTimeout(resolve, 1000));

      const assistantMessage: Message = {
        role: 'assistant',
        content:
          '죄송합니다. 아직 AI 응답 기능이 구현되지 않았습니다.\n실제 API 연동이 필요합니다.',
      };

      setMessages(prev => [...prev, assistantMessage]);
    } catch (error) {
      console.error('메시지 전송 실패:', error);
      const errorMessage: Message = {
        role: 'assistant',
        content: '메시지 전송 중 오류가 발생했습니다. 다시 시도해주세요.',
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuestionClick = (question: string) => {
    handleSendMessage(question);
  };

  return (
    <div className="flex h-full flex-col gap-4">
      {/* 채팅 기록 영역 - 네모 박스 */}
      <div className="flex-1 overflow-hidden rounded-lg bg-sky-50 p-4">
        <div className="h-full overflow-y-auto">
          {messages.length === 0 ? (
            <InitialMessages onQuestionClick={handleQuestionClick} />
          ) : (
            <div className="flex flex-col">
              {messages.map((message, index) => (
                <ChatMessage key={index} message={message} />
              ))}
              {isLoading && (
                <div className="flex gap-3 p-4">
                  <div className="bg-primary/10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
                    <div className="bg-primary h-5 w-5 animate-pulse rounded-full" />
                  </div>
                  <div className="bg-muted rounded-lg px-4 py-2">
                    <p className="text-muted-foreground text-sm">
                      답변을 생성하고 있습니다...
                    </p>
                  </div>
                </div>
              )}
              {!isLoading && (
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

      {/* 입력 영역 - 박스 아래 */}
      <div className="shrink-0">
        <ChatInput onSendMessage={handleSendMessage} disabled={isLoading} />
      </div>
    </div>
  );
};

export default ChatbotPage;
