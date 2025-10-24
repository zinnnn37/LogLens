import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import NoProjectIllust from '@/assets/images/NoProjectIllust.png';
import type { Project } from '@/components/WithProject';
import WithProject from '@/components/WithProject';
import ProjectCreateModal from '@/components/modal/ProjectCreateModal';

const DEV_SEED_ON_FIRST_CREATE = true;

// 더미 목록 (WithProject의 더미와 톤만 맞춤)
const SEED_DUMMIES: Project[] = [
  { id: 'p1', name: '자율 프로젝트', memberCount: 2, todayLogCount: 1200 },
  { id: 'p2', name: '공통 프로젝트', memberCount: 2, todayLogCount: 1200 },
  { id: 'p3', name: '개인 프로젝트', memberCount: 2, todayLogCount: 1200 },
];

const MainPage = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [showEmptyMain, setShowEmptyMain] = useState(true);

  useEffect(() => {
    if (projects.length > 0) {
      setShowEmptyMain(false);
    }
  }, [projects.length]);

  const [openCreate, setOpenCreate] = useState(false);

  // 한번에 여러개 만들기 (더미용)
  const handleComplete = (projectId: string) => {
    if (DEV_SEED_ON_FIRST_CREATE && projects.length === 0) {
      const created: Project = {
        id: projectId,
        name: `새 프로젝트 1`,
        memberCount: 1,
        todayLogCount: 0,
      };
      setProjects([created, ...SEED_DUMMIES]);
      setShowEmptyMain(false);
      return;
    }

    setProjects(prev => [
      ...prev,
      {
        id: projectId,
        name: `새 프로젝트 ${prev.length + 1}`,
        memberCount: 1,
        todayLogCount: 0,
      },
    ]);
    setShowEmptyMain(false);
  };

  const handleDelete = (id: string) => {
    setProjects(prev => prev.filter(p => p.id !== id));
  };

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
        onCreate={async () => ({ projectId: `proj_${Date.now()}` })}
        onComplete={handleComplete}
      />
    </main>
  );
};

export default MainPage;
