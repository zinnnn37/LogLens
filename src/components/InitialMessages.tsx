import { Bot, Lightbulb } from 'lucide-react';

const SUGGESTED_QUESTIONS = [
  '최근 에러가 많이 발생한 서비스는?',
  '응답 시간이 가장 느린 API는?',
  '트래픽이 가장 많은 시간대는?',
  '시스템 전체 상태 요약해줘',
];

interface InitialMessagesProps {
  onQuestionClick?: (question: string) => void;
  showGreeting?: boolean;
}

const InitialMessages = ({
  onQuestionClick,
  showGreeting = true,
}: InitialMessagesProps) => {
  return (
    <div className="flex flex-col gap-4 p-6">
      {/* 첫 번째 말풍선: 인사말 (맨 처음에만 표시) */}
      {showGreeting && (
        <div className="flex gap-3">
          <div className="bg-primary/10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
            <Bot className="text-primary h-5 w-5" />
          </div>
          <div className="text-foreground max-w-[70%] rounded-lg bg-white px-4 py-2">
            <p className="font-yisunsin text-sm leading-relaxed">
              안녕하세요! AI 어시스턴트입니다.
              <br />
              시스템 로그의 성능 데이터를 분석하여 도움을 드릴게요.
            </p>
          </div>
        </div>
      )}

      {/* 추천 질문 */}
      <div className="flex gap-3">
        <div className="bg-primary/10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
          <Bot className="text-primary h-5 w-5" />
        </div>
        <div className="text-foreground max-w-[70%] rounded-lg bg-white px-4 py-3">
          <div className="mb-3 flex items-center gap-2">
            <Lightbulb className="h-4 w-4 text-yellow-500" />
            <span className="font-yisunsin text-sm font-semibold">
              추천 질문
            </span>
          </div>
          <div className="border-border border-t pt-3">
            <div className="flex flex-col gap-2">
              {SUGGESTED_QUESTIONS.map((question, index) => (
                <button
                  key={index}
                  onClick={() => onQuestionClick?.(question)}
                  className="font-yisunsin rounded-md bg-gray-100 px-3 py-2 text-left text-sm transition-colors hover:bg-gray-200"
                >
                  {question}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InitialMessages;
