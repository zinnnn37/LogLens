// src/components/ChatMessage.tsx
import { useState } from 'react';
import { Bot, User, Copy, Check } from 'lucide-react';
import clsx from 'clsx';
import MarkdownContent from './MarkdownContent';

export type MessageRole = 'user' | 'assistant';

export interface Message {
  role: MessageRole;
  content: string;
  isComplete?: boolean; // 스트리밍 완료 여부
}

interface ChatMessageProps {
  message: Message;
}

const ChatMessage = ({ message }: ChatMessageProps) => {
  const isUser = message.role === 'user';
  // 스트리밍 완료된 assistant 메시지만 마크다운 렌더링
  const shouldRenderMarkdown = !isUser && message.isComplete !== false;

  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch (error) {
      console.error('클립보드 복사 실패:', error);
    }
  };

  return (
    <div
      className={clsx(
        'flex gap-3 p-4',
        isUser ? 'justify-end' : 'justify-start',
      )}
    >
      {/* AI 답변 */}
      {!isUser && (
        <div className="bg-primary/10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
          <Bot className="text-primary h-5 w-5" />
        </div>
      )}

      {/* 말풍선 */}
      <div
        className={clsx(
          'relative max-w-[70%] rounded-lg px-4 py-2',
          isUser
            ? 'bg-primary text-primary-foreground'
            : 'text-foreground bg-white',
        )}
      >
        {/* 복사 버튼 */}
        {message.content.trim().length > 0 && (
          <button
            type="button"
            onClick={handleCopy}
            className={clsx(
              "absolute top-2 right-2 inline-flex h-6 w-6 items-center justify-center rounded-md text-xs transition focus:outline-none",
              isUser
                ? "text-white hover:bg-white/20 focus:ring-white/40"
                : "text-muted-foreground hover:bg-black/5 focus:ring-primary/40"
            )}
            aria-label="메시지 복사"
          >
            {copied ? (
              <Check className="h-3 w-3" />
            ) : (
              <Copy className="h-3 w-3" />
            )}
          </button>
        )}

        {/* 내용 */}
        <div className="pr-6">
          {shouldRenderMarkdown ? (
            <MarkdownContent content={message.content} />
          ) : (
            <p className="font-yisunsin text-sm leading-relaxed whitespace-pre-wrap">
              {message.content}
            </p>
          )}
        </div>
      </div>
      {/* 유저 */}
      {isUser && (
        <div className="bg-secondary/10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
          <User className="text-secondary h-5 w-5" />
        </div>
      )}
    </div>
  );
};

export default ChatMessage;
