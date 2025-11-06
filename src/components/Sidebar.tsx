// src/components/Sidebar.tsx
import { useEffect, useState } from 'react';
import type { ComponentProps } from 'react';
import { useNavigate, useLocation, useParams } from 'react-router-dom';
import {
  PlusSquare,
  MessageSquare,
  LogOut,
  BookOpen,
  Folder,
} from 'lucide-react';

import ProjectCreateModal from '@/components/modal/ProjectCreateModal';
import DocsTOC from '@/components/DocsTOC';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { logout } from '@/services/authApi';
import { useAuthStore } from '@/stores/authStore';
import { ROUTE_PATH, createProjectPath } from '@/router/route-path';

import { useProjectStore } from '@/stores/projectStore';
import { fetchProjects, createProject } from '@/services/projectService';

// --- Sidebar Props ---
type SidebarProps = ComponentProps<'aside'> & {
  // 필요 시 확장
};

// 공통 스타일: 링크/버튼 모양 일치
const itemBase =
  'text-[#6A6A6A] hover:bg-sidebar-accent hover:text-sidebar-accent-foreground flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left transition-all';

// 버튼 버전 (모달 트리거/액션 등)
const NavButton = ({
  icon: Icon,
  children,
  onClick,
}: {
  icon: React.ElementType;
  children: React.ReactNode;
  onClick?: () => void;
}) => {
  return (
    <button type="button" onClick={onClick} className={itemBase}>
      <Icon className="text-primary h-4 w-4" />
      {children}
    </button>
  );
};

// 섹션 헤딩
const NavHeading = ({ children }: { children: React.ReactNode }) => {
  return (
    <h2 className="text-muted-foreground mb-2 px-3 text-xs font-semibold tracking-wider uppercase">
      {children}
    </h2>
  );
};

const Sidebar = ({ className, ...props }: SidebarProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { projectUuid } = useParams<{ projectUuid: string }>();
  const { clearAuth } = useAuthStore();

  // 프로젝트 생성 모달 Open 상태 관리
  const [openCreate, setOpenCreate] = useState(false);

  // 프로젝트 목록(store)
  const projects = useProjectStore(state => state.projects);
  const setProjectsInStore = useProjectStore(state => state.setProjects);

  // 초기 프로젝트 목록 로드 (메인페이지 패턴과 동일)
  useEffect(() => {
    const loadProjects = async () => {
      try {
        const response = await fetchProjects();
        setProjectsInStore(response);
      } catch (error) {
        // 목록 로드 실패 시 콘솔만 (UX 정책은 추후)
        console.error('프로젝트 목록 로드 실패', error);
      }
    };
    loadProjects();
  }, [setProjectsInStore]);

  // 프로젝트 선택 핸들러
  const handleProjectSelect = (selectedProjectUuid: string) => {
    navigate(createProjectPath(selectedProjectUuid, 'dashboard'));
  };

  // AI Chat 버튼 핸들러
  const handleAIChatClick = () => {
    if (projectUuid) {
      // 현재 프로젝트가 선택되어 있으면 해당 프로젝트의 챗봇으로 이동
      navigate(createProjectPath(projectUuid, 'chatbot'));
    } else if (projects.length > 0) {
      // 프로젝트가 선택되지 않았지만 프로젝트 목록이 있으면 첫 번째 프로젝트의 챗봇으로 이동
      navigate(createProjectPath(projects[0].projectUuid, 'chatbot'));
    } else {
      // 프로젝트가 없으면 메인 페이지로 이동
      navigate(ROUTE_PATH.MAIN);
    }
  };

  // 로그아웃 핸들러
  const handleLogout = async () => {
    try {
      await logout(); // 서버에 로그아웃 요청
      clearAuth(); // 로컬 토큰 삭제
      navigate(ROUTE_PATH.LOGIN); // 로그인 페이지로 이동
    } catch (error) {
      console.error('로그아웃 실패:', error);
      // 에러가 발생해도 로컬 토큰은 삭제하고 로그인 페이지로 이동
      clearAuth();
      navigate(ROUTE_PATH.LOGIN);
    }
  };

  return (
    <aside
      className={`border-sidebar-border flex h-[100dvh] w-56 flex-col border-r p-2 ${className || ''}`}
      {...props}
    >
      {/* 로고 - 고정 */}
      <div className="mb-3 flex h-14 flex-shrink-0 items-center px-2">
        <a href="/" className="font-pretendard text-4xl font-bold">
          <span className="text-[#1C1C1C]">Log</span>
          <span className="text-[#6A91BE]">Lens</span>
        </a>
      </div>

      {/* 네비게이션 */}
      <div className="font-godoM flex min-h-0 flex-1 flex-col overflow-y-auto [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        <nav className="flex flex-col gap-4">
          {/* Projects */}
          <section className="flex-shrink-0">
            <NavHeading>
              <p className="font-pretendard">projects</p>
            </NavHeading>
            <div className="flex flex-col gap-1">
              <NavButton icon={PlusSquare} onClick={() => setOpenCreate(true)}>
                새 프로젝트 생성
              </NavButton>

              <Accordion type="single" collapsible defaultValue="project-list">
                <AccordionItem value="project-list" className="border-none">
                  <AccordionTrigger
                    className={`${itemBase} py-2 text-base font-normal hover:no-underline`}
                  >
                    <div className="flex items-center gap-3">
                      <Folder className="text-primary h-4 w-4" />
                      <span>프로젝트 목록</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="pb-0">
                    {/* 프로젝트 목록 - 스크롤 영역 */}
                    <ul className="flex flex-col gap-1 pl-6 text-[#6A6A6A]">
                      {projects.map(p => (
                        <li key={p.projectUuid}>
                          <button
                            type="button"
                            onClick={() => handleProjectSelect(p.projectUuid)}
                            className={`${itemBase} text-sm ${
                              projectUuid === p.projectUuid
                                ? 'bg-sidebar-accent text-sidebar-accent-foreground font-semibold'
                                : ''
                            }`}
                          >
                            {/* 프로젝트 이름*/}
                            <span className="truncate">{p.projectName}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  </AccordionContent>
                </AccordionItem>
              </Accordion>
            </div>
          </section>

          {/* Support - 목차만 스크롤 */}
          <section className="flex-shrink-0">
            <NavHeading>
              <p className="font-pretendard">Support</p>
            </NavHeading>
            <div className="flex flex-col gap-1">
              <Accordion type="single" collapsible defaultValue="user-guide">
                <AccordionItem value="user-guide" className="border-none">
                  <AccordionTrigger
                    className={`${itemBase} py-2 text-base font-normal hover:no-underline`}
                  >
                    <div className="flex items-center gap-3">
                      <BookOpen className="text-primary h-4 w-4" />
                      <span>사용자 가이드</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="pb-0">
                    {/* Docs 목차 - 스크롤 영역 */}
                    <DocsTOC />
                  </AccordionContent>
                </AccordionItem>
              </Accordion>
            </div>
          </section>
        </nav>
      </div>

      {/* 하단 그룹 */}
      <div className="flex-shrink-0">
        <hr className="border-sidebar-border my-4" />
        <nav className="font-godoM flex flex-col gap-1">
          <NavButton icon={MessageSquare} onClick={handleAIChatClick}>
            AI Chat
          </NavButton>
          <NavButton icon={LogOut} onClick={handleLogout}>
            Log out
          </NavButton>
        </nav>
      </div>

      {/* 프로젝트 생성 모달 */}
      <ProjectCreateModal
        open={openCreate}
        onOpenChange={setOpenCreate}
        onCreate={createProject}
        onComplete={() => {
          setOpenCreate(false);
          // 현재 페이지가 메인이 아니면 메인으로 리다이렉트
          if (location.pathname !== ROUTE_PATH.MAIN) {
            navigate(ROUTE_PATH.MAIN);
          }
        }}
      />
    </aside>
  );
};

export default Sidebar;
