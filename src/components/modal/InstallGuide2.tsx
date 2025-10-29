// src/components/modals/project-create/steps/InstallGuideStep2.tsx
import { useState } from 'react';
import type { MouseEvent } from 'react';
import { Button } from '@/components/ui/button';
import { Copy } from 'lucide-react';

export interface InstallGuideStep2Props {
  projectName: string;
  onPrev: () => void;
  onComplete: () => void;
  onCopy?: (text: string) => Promise<void> | void;
}

const FLUENT_BIT_INSTALL_COMMAND =
  'curl -sL https://example.com/install.sh | sudo bash -s -- --api-key YOUR_API_KEY';

const InstallGuideStep2 = ({
  projectName,
  onPrev,
  onComplete,
  onCopy,
}: InstallGuideStep2Props) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async (e: MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    try {
      const cmdToCopy = FLUENT_BIT_INSTALL_COMMAND;
      if (onCopy) {
        await onCopy(cmdToCopy);
      } else {
        await navigator.clipboard.writeText(cmdToCopy);
      }
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* 프로젝트명 표시 */}
      <div className="text-m">
        <span className="font-semibold">프로젝트명 :</span>{' '}
        <span>{projectName}</span>
      </div>

      {/* 설치 전 확인 박스 */}
      <div className="rounded-md bg-[#FFF8E3] p-4">
        <div className="mb-2 flex items-center gap-2 font-semibold">
          <span aria-hidden>⚠️</span>
          <span>설치 전 확인 사항</span>
        </div>
        <ul className="text-muted-foreground list-disc pl-5 text-sm">
          <li>서버 당 하나의 Fluent Bit만 설치하세요.</li>
          <li>중복 설치 시 로그가 여러 번 전송될 수 있습니다.</li>
        </ul>
      </div>

      <section className="space-y-3">
        <h3 className="text-base font-semibold">
          Step2. 프로젝트 서버에서 설치
        </h3>

        <div className="rounded-md bg-[#F7FAFC] p-4">
          <p className="text-sm">아래 명령어를 서버에서 실행하세요.</p>

          <div className="bg-background mt-2 flex items-center justify-between gap-2 rounded-md border-1 p-3 text-sm">
            <span className="truncate" aria-live="polite" aria-atomic="true">
              {FLUENT_BIT_INSTALL_COMMAND}
            </span>

            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={handleCopy}
            >
              <Copy className="h-4 w-4" />
              <span className="sr-only">{copied ? '복사됨' : '복사'}</span>
            </Button>
          </div>
        </div>
      </section>

      {/* 하단 내비게이션 */}
      <div className="flex items-center justify-center gap-8">
        <Button
          type="button"
          variant="outline"
          onClick={onPrev}
        >
          이전
        </Button>
        <Button
          type="button"
          onClick={onComplete}
        >
          완료
        </Button>
      </div>
    </div>
  );
};

export default InstallGuideStep2;