import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import NoProjectIllust from '@/assets/images/NoProjectIllust.png';
import WithProject from '@/components/WithProject';
import ProjectCreateModal from '@/components/modal/ProjectCreateModal';
import FloatingChecklist from '@/components/FloatingChecklist';
import { Loader2, AlertTriangle } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { toast } from 'sonner';
import { useProjectStore } from '@/stores/projectStore';
import { useNotificationStore } from '@/stores/notificationStore';
import {
  fetchProjects,
  createProject,
  deleteProject,
} from '@/services/projectService';
import { getUnreadAlertCount } from '@/services/alertService';
import { ApiError } from '@/types/api';
import type { ProjectInfoDTO } from '@/types/project';

const MainPage = () => {
  const projects = useProjectStore(state => state.projects);
  const setProjectsInStore = useProjectStore(state => state.setProjects);
  const setProjectNotification = useNotificationStore(
    state => state.setProjectNotification,
  );

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

        // 각 프로젝트의 알림 상태 확인
        response.content.forEach(async project => {
          try {
            const alertResponse = await getUnreadAlertCount({
              projectUuid: project.projectUuid,
            });
            setProjectNotification(
              project.projectUuid,
              (alertResponse.unreadCount || 0) > 0,
            );
          } catch (error) {
            console.error(
              `프로젝트 ${project.projectName} 알림 조회 실패:`,
              error,
            );
          }
        });
      } catch (error) {
        console.error('프로젝트 목록 로드 실패', error);
      } finally {
        setIsLoading(false);
      }
    };
    loadProjects();
  }, [setProjectsInStore, setProjectNotification]);

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
            className="mt-10 max-w-[280px] object-contain md:max-w-[400px]"
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
            className="fixed right-6 bottom-[72px] flex h-14 w-14 items-center justify-center rounded-full p-0 text-2xl shadow-lg transition-all duration-300 hover:scale-110 hover:shadow-xl"
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

      <Dialog
        open={projectToConfirmDelete !== null}
        onOpenChange={() => setProjectToConfirmDelete(null)}
      >
        <DialogContent className="gap-6 p-10 pb-7 sm:max-w-[440px]">
          {/* 경고 아이콘 */}
          <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-red-100 ring-8 ring-red-50">
            <AlertTriangle className="h-10 w-10 text-red-600" />
          </div>

          <DialogHeader className="space-y-3 text-center">
            <DialogTitle className="text-center text-2xl font-bold">
              프로젝트 삭제
            </DialogTitle>
            <DialogDescription className="space-y-2 text-center text-base leading-relaxed">
              <div>
                <span className="text-foreground text-xl font-semibold">
                  {projectToConfirmDelete?.projectName}
                </span>
              </div>
              <div className="text-muted-foreground">
                프로젝트를 정말 삭제하시겠습니까?
              </div>
            </DialogDescription>
          </DialogHeader>

          {/* 경고 메시지 */}
          <div className="rounded-xl border-2 border-red-200 bg-red-50 p-4 text-center">
            <div className="space-y-1 text-sm text-red-900">
              <p className="font-semibold">
                ⚠️ 이 작업은 되돌릴 수 없습니다 ⚠️
              </p>
              <p className="text-red-700">
                프로젝트와 관련된 모든 데이터가 영구적으로 삭제됩니다.
              </p>
            </div>
          </div>

          <DialogFooter className="flex-col items-center gap-2 sm:flex-row sm:justify-center sm:gap-3">
            <Button
              variant="outline"
              onClick={() => setProjectToConfirmDelete(null)}
              className="m-0 w-full text-center sm:w-32"
            >
              취소
            </Button>
            <Button
              onClick={executeDelete}
              className="m-0 w-full bg-red-600 text-center text-white hover:bg-red-700 sm:w-32"
            >
              삭제
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <FloatingChecklist />
    </div>
  );
};

export default MainPage;
