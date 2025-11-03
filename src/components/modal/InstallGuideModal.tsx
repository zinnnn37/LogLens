// src/components/modals/InstallGuideModal.tsx
import { useEffect, useState } from 'react';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Loader2 } from 'lucide-react';

import type { ProjectDTO } from '@/types/project';

import InstallGuideStep1 from './InstallGuide1';
import InstallGuideStep2 from './InstallGuide2';

interface InstallGuideModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  project: ProjectDTO | null;
}

const InstallGuideModal = ({
  open,
  onOpenChange,
  project,
}: InstallGuideModalProps) => {
  // 0: API 키 확인, 1: 설치 명령어
  const [page, setPage] = useState<0 | 1>(0);

  // 모달 열릴 때 초기화
  useEffect(() => {
    if (open) {
      setPage(0); // 항상 첫 페이지(API 키)부터 시작
    }
  }, [open]);

  // 복사버튼
  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      // TODO: 실패 시 알림, 실패할 리가 있나?
    }
  };

  const handleClose = () => {
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <span aria-hidden>🛠️</span> Fluent Bit 설치 가이드
          </DialogTitle>
          <DialogDescription className="sr-only">
            {page === 0
              ? '설치 전 확인 사항과 API 키를 확인합니다.'
              : '설치 명령어를 확인하고 실행합니다.'}
          </DialogDescription>
        </DialogHeader>

        {/* 프로젝트 정보가 로드되지 않았으면 로더 표시 */}
        {!project ? (
          <div className="flex h-64 items-center justify-center">
            <Loader2 className="text-muted-foreground h-8 w-8 animate-spin" />
          </div>
        ) : null}

        {project && page === 0 ? (
          <InstallGuideStep1
            projectName={project.projectName}
            apiKey={project.apiKey}
            onNext={() => setPage(1)}
            onCopy={copyToClipboard}
          />
        ) : null}

        {project && page === 1 ? (
          <InstallGuideStep2
            projectName={project.projectName}
            onPrev={() => setPage(0)}
            onComplete={handleClose}
            onCopy={copyToClipboard}
          />
        ) : null}
      </DialogContent>
    </Dialog>
  );
};

export default InstallGuideModal;
