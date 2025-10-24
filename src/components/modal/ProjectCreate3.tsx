// src/components/modals/project-create/steps/ProjectCreate3.tsx
import { useState } from 'react';
import type { MouseEvent } from 'react';
import { Button } from '@/components/ui/button';
import { Copy } from 'lucide-react';

interface ProjectCreate3Props {
  projectName: string;
  installCmd: string;
  onPrev: () => void;
  onComplete: () => void;
  /**
   * 완료 처리 중 비활성화/레이블 변경을 위해 사용 (옵션)
   */
  completing?: boolean;
  /**
   * 상위에서 커스텀 복사 로직을 주입하고 싶을 때 사용.
   * 미제공 시 기본 clipboard API로 처리.
   */
  onCopy?: (text: string) => Promise<void> | void;
}

const ProjectCreate3 = ({
  projectName,
  installCmd,
  onPrev,
  onComplete,
  completing,
  onCopy,
}: ProjectCreate3Props) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async (e: MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    try {
      if (onCopy) {
        await onCopy(installCmd);
      } else {
        await navigator.clipboard.writeText(installCmd);
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
      <div className="text- m">
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

      {/* Step2. 설치 명령어 */}
      <section className="space-y-3">
        <h3 className="text-base font-semibold">
          Step2. 프로젝트 서버에서 설치
        </h3>

        <div className="rounded-md bg-[#F7FAFC] p-4">
          <p className="text-sm">아래 명령어를 서버에서 실행하세요.</p>

          <div className="bg-background mt-2 flex items-center justify-between gap-2 rounded-md border-1 p-3 text-sm">
            <span className="truncate" aria-live="polite" aria-atomic="true">
              {installCmd}
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
          disabled={Boolean(completing)}
        >
          이전
        </Button>
        <Button
          type="button"
          onClick={onComplete}
          disabled={Boolean(completing)}
        >
          {completing ? '생성 중...' : '완료'}
        </Button>
      </div>
    </div>
  );
};

export default ProjectCreate3;
