// src/components/modals/project-create/steps/ProjectCreate2.tsx
import { useState } from 'react';
import type { MouseEvent } from 'react';
import { Button } from '@/components/ui/button';
import { Copy } from 'lucide-react';

interface ProjectCreate2Props {
  projectName: string;
  apiKey: string;
  onNext: () => void;
  onCopy?: (text: string) => Promise<void> | void;
  onPrev?: () => void;
}

const ProjectCreate2 = ({
  projectName,
  apiKey,
  onNext,
  onCopy,
  onPrev,
}: ProjectCreate2Props) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async (e: MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();

    try {
      if (onCopy) {
        await onCopy(apiKey);
      } else {
        await navigator.clipboard.writeText(apiKey);
      }
      setCopied(true);
      // 2~3초 후 상태 리셋
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      // 실패해도 UX 중단하지 않음(필요 시 토스트/에러 추가)
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

      {/* Step1. API 키 확인 */}
      <section className="space-y-3">
        <h3 className="text-base font-semibold">Step1. API 키 확인</h3>

        <div className="rounded-md bg-[#F7FAFC] p-4">
          <p className="text-sm">Fluent Bit 설치 시 필요한 인증 키 입니다.</p>

          <div className="bg-background mt-2 flex items-center justify-between gap-2 rounded-md border-1 p-3 text-sm">
            <span className="truncate" aria-live="polite" aria-atomic="true">
              [{apiKey}]
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
        <div>
          {onPrev ? (
            <Button type="button" variant="outline" onClick={onPrev}>
              이전
            </Button>
          ) : null}
        </div>

        <Button type="button" onClick={onNext}>
          다음
        </Button>
      </div>
    </div>
  );
};

export default ProjectCreate2;
