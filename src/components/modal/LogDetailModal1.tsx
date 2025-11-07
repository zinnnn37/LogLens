// src/components/modal/LogDetailModal1.tsx
import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Loader2, Workflow } from 'lucide-react';
import { analyzeLogs } from '@/services/logService';
import type { LogData, LogAnalysisData } from '@/types/log';

const InfoSection = ({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) => (
  <div className="mb-6">
    <h3 className="mb-2 text-base font-semibold text-gray-900">{title}</h3>
    <div className="space-y-2 rounded-md border bg-gray-50 p-4">{children}</div>
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

export interface LogDetailModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  log: LogData | null;
  onGoToNextPage: () => void;
}

/**
 * 로그 상세 정보 모달
 */
const LogDetailModal1 = ({
  open,
  onOpenChange,
  log,
  onGoToNextPage,
}: LogDetailModalProps) => {
  const { projectUuid } = useParams<{ projectUuid: string }>();
  const [analysis, setAnalysis] = useState<LogAnalysisData | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open && log && projectUuid) {
      const fetchAnalysis = async () => {
        setIsLoading(true);
        setAnalysis(null);
        setError(null);

        try {
          const params = {
            logId: log.logId,
            project_uuid: projectUuid,
          };
          const response = await analyzeLogs(params);
          setAnalysis(response.analysis);
        } catch (e) {
          console.error('로그 분석 API 실패:', e);
          setError('AI 분석 데이터를 불러오는 데 실패했습니다.');
        } finally {
          setIsLoading(false);
        }
      };

      fetchAnalysis();
    }

    if (!open) {
      setAnalysis(null);
      setIsLoading(false);
      setError(null);
    }
  }, [open, log, projectUuid]);

  if (!log) {
    return null;
  }

  const isErrorLevel = log.logLevel === 'ERROR';

  // 요청 흐름 보기 새 탭 열기 핸들러
  const handleOpenRequestFlow = () => {
    if (!projectUuid || !log.traceId) { return; }

    const params = new URLSearchParams({ traceId: log.traceId });
    const url = `/project/${projectUuid}/request-flow?${params.toString()}`;
    window.open(url, '_blank');
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>로그 상세정보 - {log.traceId}</DialogTitle>
        </DialogHeader>

        <div className="max-h-[60vh] overflow-y-auto p-1 pr-4">
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

          {/* 로딩 중 */}
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

          {/* 에러 발생 */}
          {error && (
            <InfoSection title="분석 실패">
              <p className="text-sm text-red-500">{error}</p>
            </InfoSection>
          )}

          {/* 성공 */}
          {analysis && !isLoading && (
            <>
              <InfoSection title="로그 요약">
                <p className="text-sm whitespace-pre-wrap text-gray-700">
                  {analysis.summary}
                </p>
              </InfoSection>

              {isErrorLevel && (
                <>
                  <InfoSection title="에러 원인">
                    <p className="text-sm whitespace-pre-wrap text-gray-700">
                      {analysis.error_cause}
                    </p>
                  </InfoSection>

                  <InfoSection title="해결 방안">
                    <p className="text-sm whitespace-pre-wrap text-gray-700">
                      {analysis.solution}
                    </p>
                  </InfoSection>
                </>
              )}
            </>
          )}
        </div>

        <DialogFooter className="gap-2 sm:justify-end">
          {/* 요청 흐름 버튼 */}
          <Button
            variant="outline"
            onClick={handleOpenRequestFlow}
            className="gap-2"
          >
            <Workflow className="h-4 w-4" />
            요청 흐름 보기
          </Button>

          {/* Jira 버튼 */}
          {isErrorLevel && (
            <Button
              onClick={onGoToNextPage}
              disabled={isLoading || !analysis}
              className="bg-[#0052CC] hover:bg-[#0747A6]" 
            >
              Jira 티켓 발행
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default LogDetailModal1;