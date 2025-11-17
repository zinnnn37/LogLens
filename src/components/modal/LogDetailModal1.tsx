// src/components/modal/LogDetailModal1.tsx
import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import ReactMarkdown, { type Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import {
  Loader2,
  ScanSearch,
  Link2,
  Wand2,
  Copy,
  FileText,
} from 'lucide-react';
import { analyzeLogs } from '@/services/logService';
import { getJiraConnectionStatus } from '@/services/jiraService';
import type { LogData, LogAnalysisData } from '@/types/log';
import { toast } from 'sonner';

const InfoSection = ({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) => (
  <div className="mb-6">
    <h3 className="mb-2 text-base font-semibold text-gray-900">{title}</h3>
    <div className="space-y-2 overflow-auto rounded-md border bg-gray-50 p-4">
      {children}
    </div>
  </div>
);

const InfoRow = ({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) => (
  <div className="grid grid-cols-1 gap-1 md:grid-cols-4 md:gap-4">
    <span className="text-sm font-medium text-gray-500">{label}</span>
    <span className="col-span-3 font-mono text-sm break-words text-gray-900">
      {value}
    </span>
  </div>
);

const markdownStyles: Components = {
  ul: ({ node, ...props }) => (
    <ul className="list-disc space-y-1 pl-5" {...props} />
  ),
  ol: ({ node, ...props }) => (
    <ol className="list-decimal space-y-1 pl-5" {...props} />
  ),
  li: ({ node, ...props }) => <li className="pl-1" {...props} />,
  h1: ({ node, ...props }) => (
    <h1 className="mt-4 mb-2 text-xl font-bold" {...props} />
  ),
  h2: ({ node, ...props }) => (
    <h2 className="mt-3 mb-2 text-lg font-bold" {...props} />
  ),
  h3: ({ node, ...props }) => (
    <h3 className="text-md mt-2 mb-1 font-bold" {...props} />
  ),
  blockquote: ({ node, ...props }) => (
    <blockquote
      className="border-l-4 border-gray-300 pl-4 text-gray-600 italic"
      {...props}
    />
  ),
  a: ({ node, ...props }) => (
    <a
      className="text-blue-600 hover:underline"
      target="_blank"
      rel="noopener noreferrer"
      {...props}
    />
  ),
  code: ({ className, children, node, ...props }) => {
    const isInline =
      !className && typeof children === 'string' && !children.includes('\n');

    if (isInline) {
      return (
        <code
          className="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-sm text-red-500"
          {...props}
        >
          {children}
        </code>
      );
    }

    return (
      <code
        className="my-2 block overflow-x-auto rounded-md bg-gray-800 p-3 font-mono text-xs text-gray-100"
        {...props}
      >
        {children}
      </code>
    );
  },
};

export interface LogDetailModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  log: LogData | null;
  onGoToNextPage: () => void;
  onOpenJiraConnect: () => void;
  onGenerateErrorDoc?: () => void;
  isGeneratingDoc?: boolean;
}

const LogDetailModal1 = ({
  open,
  onOpenChange,
  log,
  onGoToNextPage,
  onOpenJiraConnect,
  onGenerateErrorDoc,
  isGeneratingDoc = false,
}: LogDetailModalProps) => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  const [analysis, setAnalysis] = useState<LogAnalysisData | null>(null);
  const [isAnalysisLoading, setIsAnalysisLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [isJiraConnected, setIsJiraConnected] = useState(false);
  const [isJiraLoading, setIsJiraLoading] = useState(false);

  useEffect(() => {
    if (!open) {
      setAnalysis(null);
      setIsAnalysisLoading(false);
      setError(null);
      setIsJiraConnected(false);
      setIsJiraLoading(false);
      return;
    }

    if (log?.logLevel === 'ERROR' && projectUuid) {
      setIsJiraLoading(true);
      getJiraConnectionStatus(projectUuid)
        .then(response => {
          setIsJiraConnected(response.exists);
        })
        .catch(e => {
          console.error('Jira 연동 상태 확인 실패:', e);
          setIsJiraConnected(false);
        })
        .finally(() => {
          setIsJiraLoading(false);
        });
    }
  }, [open, log, projectUuid]);

  if (!log) {
    return null;
  }

  const isErrorLevel = log.logLevel === 'ERROR';

  // LogDetail 마크다운
  const buildLogDetailText = (): string => {
    if (!log.logDetails) {
      return '';
    }
    return JSON.stringify(log.logDetails, null, 2)
      .slice(1, -1)
      .replace(/"/g, '')
      .trim();
  };

  const handleCopyLogDetail = async () => {
    const text = buildLogDetailText();
    if (!text) {
      return;
    }

    try {
      await navigator.clipboard.writeText(text);
      toast.success('LogDetail 내용을 복사했습니다.');
    } catch (e) {
      console.error('클립보드 복사 실패:', e);
      toast.error('복사에 실패했습니다. 다시 시도해주세요.');
    }
  };

  const handleAnalyzeClick = async () => {
    if (!projectUuid || !log) {
      return;
    }

    setIsAnalysisLoading(true);
    setAnalysis(null);
    setError(null);

    try {
      const response = await analyzeLogs({
        logId: log.logId,
        projectUuid: projectUuid,
      });
      setAnalysis(response.analysis);
    } catch (e) {
      console.error('로그 분석 API 실패:', e);
      setError('AI 분석 데이터를 불러오는 데 실패했습니다.');
    } finally {
      setIsAnalysisLoading(false);
    }
  };

  const handleOpenRequestFlow = () => {
    if (!projectUuid || !log.traceId) {
      return;
    }

    const params = new URLSearchParams({ traceId: log.traceId });
    const url = `/project/${projectUuid}/request-flow?${params.toString()}`;
    window.open(url, '_blank');
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[90vh] flex-col sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>로그 상세정보 - {log.traceId}</DialogTitle>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto p-1 pr-4">
          <InfoSection title="로그 정보">
            <InfoRow label="Level" value={log.logLevel} />
            <InfoRow label="System" value={log.sourceType} />
            <InfoRow
              label="Date"
              value={new Date(log.timestamp).toLocaleString()}
            />
            <InfoRow
              label="Message"
              value={<pre className="whitespace-pre-wrap">{log.message}</pre>}
            />
            <InfoRow label="Logger" value={log.logger} />
            <InfoRow label="Layer" value={log.layer} />
            <InfoRow label="Service" value={log.serviceName} />
            <InfoRow label="Component" value={log.componentName} />
            <InfoRow label="Method" value={log.methodName || 'N/A'} />
            <InfoRow label="Thread" value={log.threadName} />
            <InfoRow label="IP" value={log.requesterIp} />
            <InfoRow
              label="Duration"
              value={log.duration !== null ? `${log.duration}ms` : 'N/A'}
            />

            {log.logDetails && (
              <InfoRow
                label="LogDetail"
                value={
                  <div className="relative rounded-md border bg-white p-3">
                    {/* 복사 버튼 */}
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="absolute top-2 right-2 h-7 w-7 border-none p-0 shadow-none outline-none hover:bg-transparent focus-visible:ring-0"
                      onClick={handleCopyLogDetail}
                    >
                      <Copy className="h-4 w-4 text-gray-600" />
                    </Button>

                    {/* 코드블록 */}
                    <pre className="mt-6 font-mono text-xs whitespace-pre-wrap text-gray-900">
                      {buildLogDetailText()}
                    </pre>
                  </div>
                }
              />
            )}
          </InfoSection>

          {!analysis && !isAnalysisLoading && !error && (
            <InfoSection title="AI 로그 분석">
              <Button onClick={handleAnalyzeClick} className="gap-2">
                <Wand2 className="h-4 w-4" />
                AI 분석 실행하기
              </Button>
            </InfoSection>
          )}

          {isAnalysisLoading && (
            <InfoSection title="AI 분석 중...">
              <div className="flex h-20 items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                <span className="ml-2 text-gray-500">
                  로그를 분석하고 있습니다...
                </span>
              </div>
            </InfoSection>
          )}

          {error && (
            <InfoSection title="분석 실패">
              <p className="text-sm text-red-500">{error}</p>
              <Button
                onClick={handleAnalyzeClick}
                className="mt-4 gap-2"
                variant="outline"
              >
                <Wand2 className="h-4 w-4" />
                재시도
              </Button>
            </InfoSection>
          )}

          {analysis && !isAnalysisLoading && (
            <>
              <InfoSection title="로그 요약">
                <div className="text-sm leading-relaxed text-gray-800">
                  <ReactMarkdown
                    remarkPlugins={[remarkGfm]}
                    components={markdownStyles}
                  >
                    {analysis.summary}
                  </ReactMarkdown>
                </div>
              </InfoSection>

              {isErrorLevel && (
                <>
                  <InfoSection title="에러 원인">
                    <div className="text-sm leading-relaxed text-gray-800">
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={markdownStyles}
                      >
                        {analysis.error_cause}
                      </ReactMarkdown>
                    </div>
                  </InfoSection>

                  <InfoSection title="해결 방안">
                    <div className="text-sm leading-relaxed text-gray-800">
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={markdownStyles}
                      >
                        {analysis.solution}
                      </ReactMarkdown>
                    </div>
                  </InfoSection>
                </>
              )}
            </>
          )}
        </div>

        <DialogFooter className="mt-4 gap-2 sm:justify-end">
          <Button
            variant="outline"
            onClick={handleOpenRequestFlow}
            className="gap-2"
          >
            <ScanSearch className="h-4 w-4" />
            요청 흐름 보기
          </Button>

          {isErrorLevel && onGenerateErrorDoc && (
            <Button
              variant="outline"
              onClick={onGenerateErrorDoc}
              disabled={isGeneratingDoc}
              className="gap-2 border-purple-600 text-purple-600 hover:bg-purple-50"
            >
              {isGeneratingDoc ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <FileText className="h-4 w-4" />
              )}
              {isGeneratingDoc ? '생성 중...' : '에러 분석 문서'}
            </Button>
          )}

          {isErrorLevel &&
            (isJiraLoading ? (
              <Button disabled variant="outline" className="gap-2 opacity-70">
                <Loader2 className="h-4 w-4 animate-spin" />
                확인 중...
              </Button>
            ) : isJiraConnected ? (
              <Button
                onClick={onGoToNextPage}
                className="bg-[#0052CC] hover:bg-[#0747A6]"
              >
                Jira 티켓 발행
              </Button>
            ) : (
              <Button
                onClick={onOpenJiraConnect}
                variant="outline"
                className="gap-2 border-[#0052CC] text-[#0052CC] hover:bg-[#DEEBFF]"
              >
                <Link2 className="h-4 w-4" />
                Jira 연동하기
              </Button>
            ))}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default LogDetailModal1;
