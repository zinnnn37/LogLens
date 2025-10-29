import { useState } from 'react';
import { Copy, Check } from 'lucide-react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

interface CodeBlockProps {
  code: string;
  language?: string;
  variant?: 'default' | 'error';
}

const CodeBlock = ({
  code,
  language = 'java',
  variant = 'default',
}: CodeBlockProps) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const customStyle = {
    margin: 0,
    padding: '1.25rem',
    borderRadius: '0.5rem',
    fontSize: '0.875rem',
    background:
      variant === 'error'
        ? 'linear-gradient(to bottom right, rgb(69 10 10), rgb(127 29 29))'
        : 'linear-gradient(to bottom right, rgb(15 23 42), rgb(30 41 59))',
  };

  const borderClass =
    variant === 'error' ? 'border-red-800' : 'border-slate-700';

  return (
    <div className="group relative">
      <button
        onClick={handleCopy}
        className="absolute top-3 right-3 z-10 rounded-md bg-white/10 p-2 opacity-0 transition-colors group-hover:opacity-100 hover:bg-white/20"
        title="Copy code"
      >
        {copied ? (
          <Check className="h-4 w-4 text-green-400" />
        ) : (
          <Copy className="h-4 w-4 text-gray-300" />
        )}
      </button>
      <div
        className={`rounded-lg border ${borderClass} overflow-hidden shadow-lg`}
      >
        <SyntaxHighlighter
          language={language}
          style={vscDarkPlus}
          customStyle={customStyle}
          wrapLongLines
        >
          {code}
        </SyntaxHighlighter>
      </div>
    </div>
  );
};

export default CodeBlock;
