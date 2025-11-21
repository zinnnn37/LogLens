// src/components/modals/project-create/ProjectCreateModal.tsx
import { useEffect, useMemo, useRef, useState } from 'react';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';

import type { CreateProjectPayload, ProjectDTO } from '@/types/project';

interface ProjectCreateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;

  onCreate: (payload: CreateProjectPayload) => Promise<ProjectDTO>;

  onComplete: (newProject: ProjectDTO) => void;
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
}: ProjectCreateModalProps) => {
  const [form, setForm] = useState<CreateProjectPayload>({
    projectName: '',
    description: '',
  });
  const [errors, setErrors] = useState<
    Partial<Record<keyof CreateProjectPayload, string>>
  >({});

  const [completing, setCompleting] = useState(false);

  const nameInputRef = useRef<HTMLInputElement | null>(null);

  // 모달 열릴 때 초기화
  useEffect(() => {
    if (open) {
      setForm({ projectName: '', description: '' });
      setErrors({});
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
    () => (payload: CreateProjectPayload) => {
      const next: Partial<Record<keyof CreateProjectPayload, string>> = {};
      const trimmed = payload.projectName.trim();

      if (trimmed.length < NAME_MIN) {
        next.projectName = `프로젝트명은 최소 ${NAME_MIN}자 이상입니다.`;
      } else if (trimmed.length > NAME_MAX) {
        next.projectName = `프로젝트명은 최대 ${NAME_MAX}자 이하로 입력하세요.`;
      }

      if ((payload.description ?? '').length > DESC_MAX) {
        next.description = `설명은 최대 ${DESC_MAX}자 이하로 입력하세요.`;
      }
      return next;
    },
    [],
  );

  const handleSubmit = async () => {
    const v = validate(form);
    if (Object.keys(v).length > 0) {
      setErrors(v);
      if (v.projectName) {
        nameInputRef.current?.focus();
      }
      return;
    }

    try {
      setCompleting(true);

      const payload: CreateProjectPayload = {
        projectName: form.projectName.trim(),
        description: form.description?.trim() || undefined,
      };

      const newProject = await onCreate(payload);

      toast.success('프로젝트가 성공적으로 생성되었습니다.');
      onComplete(newProject);
      onOpenChange(false);
    } catch (error) {
      console.error('프로젝트 생성 실패', error);
      toast.error('프로젝트 생성에 실패했습니다.', {
        description: '잠시 후 다시 시도해 주세요.',
      });
    } finally {
      setCompleting(false);
    }
  };

  const handleChange = (patch: Partial<CreateProjectPayload>) => {
    setForm(prev => ({ ...prev, ...patch }));
    if (patch.projectName && errors.projectName) {
      setErrors(prev => ({ ...prev, projectName: '' }));
    }
    if (patch.description && errors.description) {
      setErrors(prev => ({ ...prev, description: '' }));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="text-center">프로젝트 생성</DialogTitle>
          <DialogDescription className="sr-only">
            프로젝트 기본 정보를 입력하고 생성합니다.
          </DialogDescription>
        </DialogHeader>

        <div>
          <div className="space-y-6">
            <div className="grid gap-3">
              <Label htmlFor="project-name">프로젝트명</Label>
              <Input
                id="project-name"
                ref={nameInputRef}
                name="projectName"
                value={form.projectName}
                onChange={ev => handleChange({ projectName: ev.target.value })}
                placeholder="프로젝트명을 입력해 주세요"
                required
                minLength={NAME_MIN}
                maxLength={NAME_MAX}
                aria-required="true"
                aria-invalid={Boolean(errors?.projectName)}
                aria-describedby={
                  errors?.projectName ? 'project-name-error' : undefined
                }
                disabled={completing}
              />
              {errors?.projectName ? (
                <p id="project-name-error" className="text-destructive text-sm">
                  {errors.projectName}
                </p>
              ) : null}
            </div>

            <div className="grid gap-3">
              <Label htmlFor="project-desc">프로젝트 설명</Label>
              <Input
                id="project-desc"
                name="description"
                value={form.description ?? ''}
                onChange={ev => handleChange({ description: ev.target.value })}
                placeholder="프로젝트 설명을 입력해 주세요 (선택)"
                maxLength={DESC_MAX}
                aria-invalid={Boolean(errors?.description)}
                aria-describedby={
                  errors?.description ? 'project-desc-error' : undefined
                }
                disabled={completing}
              />
              {errors?.description ? (
                <p id="project-desc-error" className="text-destructive text-sm">
                  {errors.description}
                </p>
              ) : null}
            </div>
          </div>

          <div className="mt-8 flex justify-center">
            <Button
              type="button"
              onClick={handleSubmit}
              disabled={completing}
              className="w-32"
            >
              {completing ? (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              ) : null}
              {completing ? '생성 중...' : '생성하기'}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default ProjectCreateModal;
