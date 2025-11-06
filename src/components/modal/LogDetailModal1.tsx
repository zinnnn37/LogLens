// src/components/modal/LogDetailModal1.tsx
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import type { LogData } from '@/types/log';

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
  if (!log) {
    return null;
  }

  const isErrorLevel = log.logLevel === 'ERROR';

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

          {/* 로그 레벨 ERROR 인 경우에 */}
          {isErrorLevel && (
            <InfoSection title="에러 원인 (Sample)">
              <p className="text-sm text-gray-700">
                [AI가 분석한 에러 원인 샘플 텍스트입니다.]
                <br />
                <code>ResourceNotFoundException</code>이(가) BE 레이어의{' '}
                <code>/api/vendor/phpunit/...</code> 경로에서 발생했습니다.
                <br />
                해당 엔드포인트가 존재하지 않거나, 서버 라우팅 설정에 문제가
                있을 수 있습니다.
              </p>
            </InfoSection>
          )}

          {/* 해결 방안, 레벨이 ERROR 인 경우에만 */}
          {isErrorLevel && (
            <InfoSection title="해결 방안 (Sample)">
              <ul className="list-disc space-y-1 pl-5 text-sm text-gray-700">
                <li>
                  요청한 URL 경로(<code>/api/vendor/phpunit/...</code>)가
                  올바른지 확인합니다.
                </li>
                <li>
                  BE 서버의 컨트롤러 및 라우팅 설정에 해당 엔드포인트가
                  정상적으로 매핑되어 있는지 확인합니다.
                </li>
              </ul>
            </InfoSection>
          )}
        </div>

        {isErrorLevel && (
          <DialogFooter>
            <Button onClick={onGoToNextPage}>Jira 티켓 발행</Button>
          </DialogFooter>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default LogDetailModal1;
