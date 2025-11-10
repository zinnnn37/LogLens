import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { Components } from 'react-markdown';

interface MarkdownContentProps {
  content: string;
}

const MarkdownContent = ({ content }: MarkdownContentProps) => {
  const markdownComponents: Components = {
    p: ({ children }) => <p className="my-2 leading-relaxed">{children}</p>,
    h1: ({ children }) => (
      <h1 className="mt-6 mb-3 text-2xl font-bold first:mt-0">{children}</h1>
    ),
    h2: ({ children }) => (
      <h2 className="mt-5 mb-2 text-xl font-bold first:mt-0">{children}</h2>
    ),
    h3: ({ children }) => (
      <h3 className="mt-4 mb-2 text-lg font-semibold first:mt-0">{children}</h3>
    ),
    ul: ({ children }) => (
      <ul className="my-2 ml-6 list-disc space-y-1">{children}</ul>
    ),
    ol: ({ children }) => (
      <ol className="my-2 ml-6 list-decimal space-y-1">{children}</ol>
    ),
    li: ({ children }) => <li className="leading-relaxed">{children}</li>,
    code: ({ inline, children, className }) => {
      if (inline) {
        return (
          <code className="rounded bg-red-100 px-1.5 py-0.5 font-mono text-[13px] font-medium text-red-700">
            {children}
          </code>
        );
      }
      return (
        <code className={`block font-mono text-[13px] ${className || ''}`}>
          {children}
        </code>
      );
    },
    pre: ({ children }) => (
      <pre className="my-3 overflow-x-auto rounded-md bg-gray-900 p-4 text-gray-100">
        {children}
      </pre>
    ),
    blockquote: ({ children }) => (
      <blockquote className="my-3 border-l-4 border-blue-500 pl-4 text-gray-700 italic">
        {children}
      </blockquote>
    ),
    a: ({ href, children }) => (
      <a
        href={href}
        className="text-blue-600 underline transition-colors hover:text-blue-700"
        target="_blank"
        rel="noopener noreferrer"
      >
        {children}
      </a>
    ),
    strong: ({ children }) => (
      <strong className="font-bold text-gray-900">{children}</strong>
    ),
    em: ({ children }) => <em className="text-gray-800 italic">{children}</em>,
    hr: () => <hr className="my-4 border-t-2 border-gray-300" />,
    table: ({ children }) => (
      <div className="my-3 overflow-x-auto">
        <table className="min-w-full border-collapse border border-gray-300">
          {children}
        </table>
      </div>
    ),
    thead: ({ children }) => <thead className="bg-gray-100">{children}</thead>,
    tbody: ({ children }) => <tbody>{children}</tbody>,
    tr: ({ children }) => (
      <tr className="border-b border-gray-300">{children}</tr>
    ),
    th: ({ children }) => (
      <th className="border border-gray-300 px-4 py-2 text-left font-semibold">
        {children}
      </th>
    ),
    td: ({ children }) => (
      <td className="border border-gray-300 px-4 py-2">{children}</td>
    ),
  };

  return (
    <div className="markdown-content text-sm">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={markdownComponents}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};

export default MarkdownContent;
