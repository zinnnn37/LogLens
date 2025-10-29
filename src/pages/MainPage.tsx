import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import NoProjectIllust from '@/assets/images/NoProjectIllust.png';
import WithProject from '@/components/WithProject';
import ProjectCreateModal from '@/components/modal/ProjectCreateModal';
import { Loader2 } from 'lucide-react';

import { useProjectStore } from '@/stores/projectStore';
import { fetchProjects, createProject } from '@/services/projectService';

const MainPage = () => {
  const projects = useProjectStore(state => state.projects);
  const setProjectsInStore = useProjectStore(state => state.setProjects);

  const [openCreate, setOpenCreate] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [showEmptyMain, setShowEmptyMain] = useState(false);

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

  const handleDelete = async (id: number) => {
    console.warn(`[TODO] deleteProject(${id}) 서비스 및 스토어 액션 구현 필요`);
  };

  if (isLoading) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center">
        <Loader2 className="h-10 w-10 animate-spin text-muted-foreground" />
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-10">
      {showEmptyMain ? (
        <div className="flex flex-col items-center gap-6 text-center">
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
        </div>
      ) : (
        <WithProject
          projects={projects}
          onDelete={handleDelete}
          onEmptyAfterExit={() => setShowEmptyMain(true)}
        />
      )}

      <Button onClick={() => setOpenCreate(true)} className="mt-2">
        + 새 프로젝트 생성
      </Button>

      <ProjectCreateModal
        open={openCreate}
        onOpenChange={setOpenCreate}
        onCreate={createProject}
        onComplete={(newProject) => {
          useProjectStore.getState().addProject(newProject);
          setOpenCreate(false);

        }}
      />
    </main>
  );
};

export default MainPage;