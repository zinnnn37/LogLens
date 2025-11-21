import { useEffect, useState } from 'react';
import { Outlet, useParams, useLocation } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import Sidebar from '@/components/Sidebar';
import Header from '@/components/Header';
import ProjectNotConnectedPage from '@/pages/ProjectNotConnectedPage';
import { checkProjectConnection } from '@/services/projectService';
import { useProjectStore } from '@/stores/projectStore';

const Layout = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();
  const location = useLocation();
  const projects = useProjectStore(state => state.projects);

  const [isConnected, setIsConnected] = useState<boolean | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // 프로젝트 UUID가 있는 경로인지 확인
  const isProjectRoute = projectUuid !== undefined;

  // 프로젝트 이름 가져오기
  const currentProject = projects.find(p => p.projectUuid === projectUuid);
  const projectName = currentProject?.projectName;

  useEffect(() => {
    // 프로젝트 라우트가 아니거나 Docs 페이지면 연결 확인 스킵
    if (!isProjectRoute || location.pathname === '/docs') {
      setIsConnected(null);
      return;
    }

    const checkConnection = async () => {
      setIsLoading(true);
      try {
        const status = await checkProjectConnection(projectUuid);
        setIsConnected(status.isConnected);
      } catch (error) {
        console.error('연결 상태 확인 실패:', error);
        // 에러 발생 시 일단 연결되지 않은 것으로 간주
        setIsConnected(false);
      } finally {
        setIsLoading(false);
      }
    };

    checkConnection();
  }, [projectUuid, isProjectRoute, location.pathname]);

  return (
    <div className="bg-background flex h-screen">
      {/* 사이드바 */}
      <Sidebar />

      <div className="flex flex-1 flex-col">
        {/* 헤더 */}
        <Header />

        {/* 메인 콘텐츠 영역 */}
        <main
          className={`flex-1 bg-[#F9F8FD] ${
            isProjectRoute && isConnected === false
              ? 'overflow-hidden'
              : 'overflow-y-auto p-6'
          }`}
        >
          {/* 로딩 중 */}
          {isLoading ? (
            <div className="flex h-full items-center justify-center">
              <Loader2 className="text-primary h-10 w-10 animate-spin" />
            </div>
          ) : /* 프로젝트 라우트이고 연결되지 않은 경우 */
          isProjectRoute && isConnected === false ? (
            <ProjectNotConnectedPage
              projectName={projectName}
              projectUuid={projectUuid}
            />
          ) : (
            /* 정상적인 페이지 렌더링 */
            <Outlet />
          )}
        </main>
      </div>
    </div>
  );
};

export default Layout;
