import { useState, type FormEvent } from 'react';
import { Send } from 'lucide-react';

interface ChatInputProps {
  onSendMessage: (message: string) => void;
  disabled?: boolean;
}

const ChatInput = ({ onSendMessage, disabled = false }: ChatInputProps) => {
  const [inputValue, setInputValue] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    const trimmedValue = inputValue.trim();
    if (!trimmedValue || disabled) {
      return;
    }

    onSendMessage(trimmedValue);
    setInputValue('');
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex items-end gap-2">
      <textarea
        value={inputValue}
        onChange={e => setInputValue(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="메시지를 입력하세요... (Shift + Enter로 줄바꿈)"
        disabled={disabled}
        className="font-yisunsin bg-background focus:ring-primary max-h-32 min-h-[44px] flex-1 resize-none rounded-lg border px-4 py-3 text-sm focus:ring-2 focus:outline-none disabled:cursor-not-allowed disabled:opacity-50"
        rows={1}
      />
      <button
        type="submit"
        disabled={disabled || !inputValue.trim()}
        className="bg-primary text-primary-foreground hover:bg-primary/90 flex h-11 w-11 shrink-0 items-center justify-center rounded-lg transition-colors disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Send className="h-5 w-5" />
      </button>
    </form>
  );
};

export default ChatInput;
