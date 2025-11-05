import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import NoProjectIllust from '@/assets/images/NoProjectIllust.png';
import WithProject from '@/components/WithProject';
import ProjectCreateModal from '@/components/modal/ProjectCreateModal';
import FloatingChecklist from '@/components/FloatingChecklist';
import { Loader2 } from 'lucide-react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { toast } from 'sonner';
import { useProjectStore } from '@/stores/projectStore';
import {
  fetchProjects,
  createProject,
  deleteProject,
} from '@/services/projectService';
import { ApiError } from '@/types/api';
import type { ProjectInfoDTO } from '@/types/project';

const MainPage = () => {
  const projects = useProjectStore(state => state.projects);
  const setProjectsInStore = useProjectStore(state => state.setProjects);

  const [openCreate, setOpenCreate] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [showEmptyMain, setShowEmptyMain] = useState(false);

  const [projectToConfirmDelete, setProjectToConfirmDelete] =
    useState<ProjectInfoDTO | null>(null);

  useEffect(() => {
    const loadProjects = async () => {
      setIsLoading(true);
      try {
        const response = await fetchProjects();
        setProjectsInStore(response);
      } catch (error) {
        console.error('프로젝트 목록 로드 실패', error);
      } finally {
        setIsLoading(false);
      }
    };
    loadProjects();
  }, [setProjectsInStore]);

  useEffect(() => {
    setShowEmptyMain(!isLoading && projects.length === 0);
  }, [isLoading, projects.length]);

  // 삭제버튼 눌렀을 때 alert-dialog
  const handleDeleteRequest = (projectUuid: string) => {
    const projectToConfirm = projects.find(p => p.projectUuid === projectUuid);
    if (projectToConfirm) {
      setProjectToConfirmDelete(projectToConfirm);
    } else {
      console.error('삭제할 프로젝트 정보를 찾을 수 없습니다.');
      toast.error('프로젝트 정보를 찾을   수 없어 삭제할 수 없습니다.');
    }
  };

  // alert-dialog 에서 삭제
  const executeDelete = async () => {
    if (!projectToConfirmDelete) {
      return;
    }

    try {
      // 프로젝트 삭제 API 호출
      await deleteProject({ projectUuid: projectToConfirmDelete.projectUuid });

      toast.success(
        `"${projectToConfirmDelete.projectName}" 프로젝트가 삭제되었습니다.`,
      );
    } catch (error) {
      console.error('프로젝트 삭제 실패', error);
      if (error instanceof ApiError && error.response) {
        toast.error(error.response.message);
      } else {
        toast.error('프로젝트 삭제에 실패했습니다.');
      }
    } finally {
      setProjectToConfirmDelete(null);
    }
  };

  if (isLoading) {
    return (
      <div className="font-pretendard flex h-full items-center justify-center space-y-6 p-6 py-1">
        <Loader2 className="text-muted-foreground h-10 w-10 animate-spin" />
      </div>
    );
  }

  return (
    <div className="font-pretendard relative space-y-6 p-6 py-1">
      {showEmptyMain ? (
        <div className="flex min-h-[60vh] flex-col items-center justify-center gap-6 text-center">
          <img
            src={NoProjectIllust}
            alt="프로젝트 없음"
            className="max-w-[280px] object-contain md:max-w-[400px]"
          />
          <div>
            <h2 className="text-foreground mb-1 text-lg font-semibold">
              프로젝트가 없습니다
            </h2>
            <p className="text-muted-foreground text-sm">
              로그 모니터링을 시작하려면
              <br />첫 프로젝트를 생성하세요.
            </p>
          </div>
          <Button onClick={() => setOpenCreate(true)} className="mt-2">
            + 새 프로젝트 생성
          </Button>
        </div>
      ) : (
        <>
          <WithProject
            projects={projects}
            onDelete={handleDeleteRequest}
            onEmptyAfterExit={() => setShowEmptyMain(true)}
          />
          {/* 플로팅 버튼 */}
          <Button
            onClick={() => setOpenCreate(true)}
            className="fixed bottom-[72px] right-6 flex h-14 w-14 items-center justify-center rounded-full p-0 text-2xl shadow-lg transition-all duration-300 hover:scale-110 hover:shadow-xl"
          >
            +
          </Button>
        </>
      )}

      <ProjectCreateModal
        open={openCreate}
        onOpenChange={setOpenCreate}
        onCreate={createProject}
        onComplete={() => {
          setOpenCreate(false);
        }}
      />

      <AlertDialog
        open={projectToConfirmDelete !== null}
        onOpenChange={open => {
          if (!open) {
            setProjectToConfirmDelete(null);
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              "{projectToConfirmDelete?.projectName}" 프로젝트를
              삭제하시겠습니까?
            </AlertDialogTitle>
            <AlertDialogDescription>
              이 작업은 되돌릴 수 없습니다. 프로젝트와 관련된 모든 데이터가
              영구적으로 삭제됩니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              onClick={executeDelete}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              삭제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
      <FloatingChecklist />
    </div>
  );
};

export default MainPage;
