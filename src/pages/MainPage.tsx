// src/pages/MainPage.tsx
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import NoProjectIllust from '@/assets/images/NoProjectIllust.png';
import WithProject from '@/components/WithProject';
import ProjectCreateModal from '@/components/modal/ProjectCreateModal';

const MainPage = () => {
  const [projects, setProjects] = useState<string[]>([]);
  const [openCreate, setOpenCreate] = useState(false);

  // TODO : 더미용, 추후 실제 API 연결
  const handleComplete = (projectId: string) => {
    setProjects(prev => [...prev, projectId]);
  };

  // TODO : 더미데이터, 추후 API 를 통하여 실제 값 불러오기
  const handlePrepare = async (_payload: {
    name: string;
    description?: string;
  }) => {
    return {
      apiKey: 'll_live_****************************',
      installCmd:
        'curl -fsSL https://get.loglens.sh | bash -s -- --project=example',
      provisionId: 'provision-temp-123',
    };
  };

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-10">
      {/* 프로젝트가 없을 때 */}
      {projects.length === 0 ? (
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
        // 프로젝트 있을 때
        <WithProject />
      )}
      <Button onClick={() => setOpenCreate(true)} className="mt-2">
        + 새 프로젝트 생성
      </Button>

      {/* 모달 */}
      <ProjectCreateModal
        open={openCreate}
        onOpenChange={setOpenCreate}
        onCreate={async () => ({ projectId: `proj_${Date.now()}` })}
        onComplete={handleComplete}
        onPrepare={handlePrepare}
      />
    </main>
  );
};

export default MainPage;
