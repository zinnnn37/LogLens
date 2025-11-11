import { Bot, User } from 'lucide-react';
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

  return (
    <div
      className={clsx(
        'flex gap-3 p-4',
        isUser ? 'justify-end' : 'justify-start',
      )}
    >
      {!isUser && (
        <div className="bg-primary/10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
          <Bot className="text-primary h-5 w-5" />
        </div>
      )}

      <div
        className={clsx(
          'max-w-[70%] rounded-lg px-4 py-2',
          isUser
            ? 'bg-primary text-primary-foreground'
            : 'text-foreground bg-white',
        )}
      >
        {shouldRenderMarkdown ? (
          <MarkdownContent content={message.content} />
        ) : (
          <p className="font-yisunsin text-sm leading-relaxed whitespace-pre-wrap">
            {message.content}
          </p>
        )}
      </div>

      {isUser && (
        <div className="bg-secondary/10 flex h-8 w-8 shrink-0 items-center justify-center rounded-full">
          <User className="text-secondary h-5 w-5" />
        </div>
      )}
    </div>
  );
};

export default ChatMessage;
