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
import { Loader2, ScanSearch, Link2 } from 'lucide-react'; // Link2 아이콘 추가
import { analyzeLogs } from '@/services/logService';
import { getJiraConnectionStatus } from '@/services/jiraService'; // Jira 서비스 추가
import type { LogData, LogAnalysisData } from '@/types/log';
import type { JiraConnectionStatusData } from '@/types/jira';

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

// react-markdown 컴포넌트 스타일
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
}

const LogDetailModal1 = ({
  open,
  onOpenChange,
  log,
  onGoToNextPage,
  onOpenJiraConnect,
}: LogDetailModalProps) => {
  const { projectUuid } = useParams<{ projectUuid: string }>();
  const [analysis, setAnalysis] = useState<LogAnalysisData | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isJiraConnected, setIsJiraConnected] = useState(false); // 💡 Jira 연동 상태

  useEffect(() => {
    if (open && log && projectUuid) {
      const fetchData = async () => {
        setIsLoading(true);
        setAnalysis(null);
        setError(null);
        if (log.logLevel !== 'ERROR') {
          setIsJiraConnected(false);
        }

        try {
          // 로그 분석 API 호출
          const analysisPromise = analyzeLogs({
            logId: log.logId,
            projectUuid: projectUuid,
          });

          // Jira 연동 상태 확인 (ERROR 레벨일 때만)
          let jiraStatusPromise: Promise<JiraConnectionStatusData | null> =
            Promise.resolve(null);
          if (log.logLevel === 'ERROR') {
            jiraStatusPromise = getJiraConnectionStatus(projectUuid);
          }

          // 두 API 병렬 호출
          const [analysisResponse, jiraStatusResponse] = await Promise.all([
            analysisPromise,
            jiraStatusPromise,
          ]);

          setAnalysis(analysisResponse.analysis);

          // Jira 연동 상태 업데이트
          if (jiraStatusResponse) {
            setIsJiraConnected(jiraStatusResponse.exists);
          }
        } catch (e) {
          console.error('API 호출 실패:', e);
          setError('데이터를 불러오는 데 실패했습니다.');
        } finally {
          setIsLoading(false);
        }
      };

      fetchData();
    }

    if (!open) {
      setAnalysis(null);
      setIsLoading(false);
      setError(null);
      setIsJiraConnected(false);
    }
  }, [open, log, projectUuid]);

  if (!log) {
    return null;
  }

  const isErrorLevel = log.logLevel === 'ERROR';

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
          </InfoSection>

          {isLoading && (
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
            </InfoSection>
          )}

          {analysis && !isLoading && (
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
                        {/* snake_case 주의: analysis.error_cause가 맞다면 그대로 유지 */}
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

          {/* Jira 버튼 분기 처리 */}
          {isErrorLevel &&
            (isJiraConnected ? (
              // 연동됨 -> 티켓 발행 버튼
              <Button
                onClick={onGoToNextPage}
                disabled={isLoading || !analysis}
                className="bg-[#0052CC] hover:bg-[#0747A6]"
              >
                Jira 티켓 발행
              </Button>
            ) : (
              // 연동 안 됨 -> 연동하기 버튼
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