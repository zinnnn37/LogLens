// src/components/modals/project-create/ProjectCreateModal.tsx
import { useEffect, useMemo, useRef, useState } from 'react';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

import ProjectCreate1 from './ProjectCreate1';
import ProjectCreate2 from './ProjectCreate2';
import ProjectCreate3 from './ProjectCreate3';

// ==2= 타입 ====
interface CreatePayload {
  name: string;
  description?: string;
}

interface PrepareResult {
  apiKey: string;
  installCmd: string;
  // 백엔드가 사전발급/예약 개념을 쓴다면 유지용 ID (선택)
  provisionId?: string;
}

interface CreateResult {
  projectId: string;
}

interface ProjectCreateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;

  /**
   * 최종 완료 후 상위에 알림 (예: 목록 갱신)
   */
  onComplete?: (projectId: string) => void;

  /**
   * 3페이지 "완료" 시 실제 프로젝트 생성
   * - 필요한 경우 1→2 단계에서 받은 provisionId를 함께 전달
   */
  onCreate: (args: {
    payload: CreatePayload;
    provisionId?: string;
  }) => Promise<CreateResult>;

  /**
   * (선택) 1→2 이동 시, UI에 보여줄 키/명령을 준비하는 API
   * - 실제 생성은 아님. 사용하지 않으면 플레이스홀더로 표시됨
   */
  onPrepare?: (payload: CreatePayload) => Promise<PrepareResult>;
}

// ==== 상수/검증 ====
const NAME_MIN = 2;
const NAME_MAX = 40;
const DESC_MAX = 500;

const ProjectCreateModal = ({
  open,
  onOpenChange,
  onCreate,
  onComplete,
  onPrepare,
}: ProjectCreateModalProps) => {
  // 0: 기본 정보, 1: API 키 확인, 2: 설치 명령어
  const [page, setPage] = useState<0 | 1 | 2>(0);

  const [form, setForm] = useState<CreatePayload>({
    name: '',
    description: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  // 1→2 진입을 위한 준비값 (실제 생성 전 미리보기 용)
  const [prepared, setPrepared] = useState<PrepareResult | null>(null);

  // 로딩 플래그
  const [preparing, setPreparing] = useState(false); // 1→2 준비 중
  const [completing, setCompleting] = useState(false); // 3에서 실제 생성 중

  // 포커스
  const nameInputRef = useRef<HTMLInputElement | null>(null);

  // 모달 열릴 때 초기화
  useEffect(() => {
    if (open) {
      setPage(0);
      setErrors({});
      setPrepared(null);
      setPreparing(false);
      setCompleting(false);

      const id = requestAnimationFrame(() => {
        nameInputRef.current?.focus();
      });
      return () => cancelAnimationFrame(id);
    }
    return undefined;
  }, [open]);

  // 입력폼 검증
  const validate = useMemo(
    () => (payload: CreatePayload) => {
      const next: Record<string, string> = {};
      const trimmed = payload.name.trim();

      if (trimmed.length < NAME_MIN) {
        next.name = `프로젝트명은 최소 ${NAME_MIN}자 이상입니다.`;
      } else if (trimmed.length > NAME_MAX) {
        next.name = `프로젝트명은 최대 ${NAME_MAX}자 이하로 입력하세요.`;
      }

      if ((payload.description ?? '').length > DESC_MAX) {
        next.description = `설명은 최대 ${DESC_MAX}자 이하로 입력하세요.`;
      }
      return next;
    },
    [],
  );

  // Step1 → Step2
  const goStep2 = async () => {
    const v = validate(form);
    if (Object.keys(v).length > 0) {
      setErrors(v);
      if (v.name) {
        nameInputRef.current?.focus();
      }
      return;
    }

    // 준비 API가 있다면 호출해서 미리보기 데이터 확보
    if (onPrepare) {
      try {
        setPreparing(true);
        const res = await onPrepare({
          name: form.name.trim(),
          description: form.description?.trim() || undefined,
        });
        setPrepared(res);
      } catch {
        // 실패해도 UX 중단은 금물 — 필요 시 토스트/알림 처리
      } finally {
        setPreparing(false);
      }
    } else {
      // 준비 API가 없으면 플레이스홀더
      setPrepared({
        apiKey: '발급은 완료 시 표시됩니다',
        installCmd: '완료 후 생성되는 설치 명령',
      });
    }

    setPage(1);
  };

  // Step2 → Step3
  const goStep3 = () => {
    setPage(2);
  };

  // Step3 “완료” → 실제 생성
  const handleComplete = async () => {
    try {
      setCompleting(true);
      const res = await onCreate({
        payload: {
          name: form.name.trim(),
          description: form.description?.trim() || undefined,
        },
        provisionId: prepared?.provisionId,
      });
      onComplete?.(res.projectId);
      onOpenChange(false);
    } catch {
      // 실패했다고 알려주기
    } finally {
      setCompleting(false);
    }
  };

  // Step1 인풋 변경 핸들러
  const handleChange = (patch: Partial<CreatePayload>) => {
    setForm(prev => ({ ...prev, ...patch }));
    if (patch.name && errors.name) {
      setErrors(prev => ({ ...prev, name: '' }));
    }
    if (patch.description && errors.description) {
      setErrors(prev => ({ ...prev, description: '' }));
    }
  };

  // 복사
  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      // 무시 or 토스트
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          {/* 타이틀/설명은 페이지별 */}
          {page === 0 && (
            <>
              <DialogTitle className="text-center">프로젝트 생성</DialogTitle>
              <DialogDescription className="sr-only">
                프로젝트 기본 정보를 입력하고 다음 단계로 이동합니다.
              </DialogDescription>
            </>
          )}
          {page === 1 && (
            <>
              <DialogTitle className="flex items-center gap-2">
                <span aria-hidden>🛠️</span> Fluent Bit 설치 가이드
              </DialogTitle>
              <DialogDescription className="sr-only">
                설치 전 확인 사항과 API 키를 확인합니다.
              </DialogDescription>
            </>
          )}
          {page === 2 && (
            <>
              <DialogTitle className="flex items-center gap-2">
                <span aria-hidden>🛠️</span> Fluent Bit 설치 가이드
              </DialogTitle>
              <DialogDescription className="sr-only">
                설치 명령어를 확인하고 실행합니다.
              </DialogDescription>
            </>
          )}
        </DialogHeader>

        {/* 페이지 0 */}
        {page === 0 ? (
          <ProjectCreate1
            value={form}
            errors={errors}
            onChange={handleChange}
            onNext={goStep2}
            nameInputRef={nameInputRef}
          />
        ) : null}

        {/* 페이지 1 */}
        {page === 1 ? (
          <ProjectCreate2
            projectName={form.name}
            apiKey={
              prepared?.apiKey ??
              (preparing ? '발급 중…' : '발급은 완료 시 표시됩니다')
            }
            onNext={goStep3}
            onPrev={() => setPage(0)}
            onCopy={copyToClipboard}
          />
        ) : null}

        {/* 페이지 2 */}
        {page === 2 ? (
          <ProjectCreate3
            projectName={form.name}
            installCmd={
              prepared?.installCmd ??
              (preparing ? '생성 중…' : '완료 후 생성됩니다')
            }
            onPrev={() => setPage(1)}
            onComplete={handleComplete}
            completing={completing}
            onCopy={copyToClipboard}
          />
        ) : null}
      </DialogContent>
    </Dialog>
  );
};

export default ProjectCreateModal;
