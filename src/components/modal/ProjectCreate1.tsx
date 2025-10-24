// src/components/modals/project-create/steps/ProjectCreate1.tsx
import type { Ref } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

interface CreatePayload {
  name: string;
  description?: string;
}

interface ProjectCreate1Props {
  value: CreatePayload;
  errors?: Partial<Record<keyof CreatePayload, string>>;
  onChange: (patch: Partial<CreatePayload>) => void;
  onNext: () => void;
  nameInputRef?: Ref<HTMLInputElement>;
}

const NAME_MIN = 2;
const NAME_MAX = 40;
const DESC_MAX = 500;

const ProjectCreate1 = ({
  value,
  errors,
  onChange,
  onNext,
  nameInputRef,
}: ProjectCreate1Props) => {
  return (
    <div>
      <div className="space-y-6">
        <div className="grid gap-3">
          <Label htmlFor="project-name">프로젝트명</Label>
          <Input
            id="project-name"
            ref={nameInputRef}
            name="name"
            value={value.name}
            onChange={ev => onChange({ name: ev.target.value })}
            placeholder="프로젝트명을 입력해 주세요"
            required
            minLength={NAME_MIN}
            maxLength={NAME_MAX}
            aria-required="true"
            aria-invalid={Boolean(errors?.name)}
            aria-describedby={errors?.name ? 'project-name-error' : undefined}
          />
          {errors?.name ? (
            <p id="project-name-error" className="text-destructive text-sm">
              {errors.name}
            </p>
          ) : null}
        </div>

        <div className="grid gap-3">
          <Label htmlFor="project-desc">프로젝트 설명</Label>
          <Input
            id="project-desc"
            name="description"
            value={value.description ?? ''}
            onChange={ev => onChange({ description: ev.target.value })}
            placeholder="프로젝트 설명을 입력해 주세요 (선택)"
            maxLength={DESC_MAX}
            aria-invalid={Boolean(errors?.description)}
            aria-describedby={
              errors?.description ? 'project-desc-error' : undefined
            }
          />
          {errors?.description ? (
            <p id="project-desc-error" className="text-destructive text-sm">
              {errors.description}
            </p>
          ) : null}
        </div>
      </div>

      <div className="mt-8 flex justify-center">
        <Button type="button" onClick={onNext}>
          다음
        </Button>
      </div>
    </div>
  );
};

export default ProjectCreate1;
