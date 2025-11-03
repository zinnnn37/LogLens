// src/components/Sidebar.tsx
import { useState } from 'react';
import type { ComponentProps } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  PlusSquare,
  Settings,
  MessageSquare,
  LogOut,
  BookOpen,
} from 'lucide-react';

import ProjectCreateModal from '@/components/modal/ProjectCreateModal';
import { JiraIntegrationModal } from '@/components/modal/JiraIntegrationModal';
import DocsTOC from '@/components/DocsTOC';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion';
import { logout } from '@/services/authApi';
import { useAuthStore } from '@/stores/authStore';
import { ROUTE_PATH } from '@/router/route-path';

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
  const { clearAuth } = useAuthStore();

  const isDocsPage = location.pathname === ROUTE_PATH.DOCS;

  // 프로젝트 생성 모달 Open 상태 관리
  const [openCreate, setOpenCreate] = useState(false);
  // Jira API 연동 모달 Open 상태 관리
  const [openJira, setOpenJira] = useState(false);

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

  // TODO : 프로젝트 생성 API 함수 정의하기.
  const handleCreate = async (_args: {
    payload: { name: string; description?: string };
    provisionId?: string;
  }) => {
    return { projectId: 'proj_temp_123' };
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
      {/* 1) 상단 그룹 */}
      <div className="font-godoM flex min-h-0 flex-1 flex-col">
        {/* 로고 */}
        <div className="mb-3 flex h-14 flex-shrink-0 items-center px-2">
          <a href="/" className="font-pretendard text-4xl font-bold">
            <span className="text-[#1C1C1C]">Log</span>
            <span className="text-[#6A91BE]">Lens</span>
          </a>
        </div>

        {/* 네비게이션 */}
        <nav className="flex min-h-0 flex-1 flex-col gap-4">
          {/* Projects */}
          <section className="flex-shrink-0">
            <NavHeading>
              <p className="font-pretendard">projects</p>
            </NavHeading>
            <ul className="flex flex-col gap-1 text-[#6A6A6A]">
              <li>
                <NavButton
                  icon={PlusSquare}
                  onClick={() => setOpenCreate(true)}
                >
                  새 프로젝트 생성
                </NavButton>
              </li>
              {/* TODO: 프로젝트 목록 API 통해 불러와서 보여주기 */}
            </ul>
          </section>

          {/* Support */}
          <section className="flex min-h-0 flex-1 flex-col">
            <NavHeading>
              <p className="font-pretendard">Support</p>
            </NavHeading>
            <div className="flex min-h-0 flex-1 flex-col gap-1">
              <NavButton icon={Settings} onClick={() => setOpenJira(true)}>
                Jira API 연결
              </NavButton>

              <Accordion
                type="single"
                collapsible
                className="min-h-0 flex-1"
                defaultValue={isDocsPage ? 'docs' : undefined}
              >
                <AccordionItem
                  value="docs"
                  className="flex min-h-0 flex-1 flex-col border-none"
                >
                  <AccordionTrigger
                    className={`${itemBase} flex-shrink-0 py-2 text-base font-normal hover:no-underline`}
                  >
                    <div className="flex items-center gap-3">
                      <BookOpen className="text-primary h-4 w-4" />
                      <span
                        onClick={e => {
                          e.stopPropagation();
                          navigate(ROUTE_PATH.DOCS);
                        }}
                        className="cursor-pointer"
                      >
                        사용자 가이드
                      </span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="flex min-h-0 flex-1 flex-col pb-0">
                    <DocsTOC />
                  </AccordionContent>
                </AccordionItem>
              </Accordion>
            </div>
          </section>
        </nav>
      </div>

      {/* 2) 하단 그룹 */}
      <div className="flex-shrink-0">
        <hr className="border-sidebar-border my-4" />
        <nav className="font-godoM flex flex-col gap-1">
          <NavButton
            icon={MessageSquare}
            onClick={() => navigate(ROUTE_PATH.AI_CHAT)}
          >
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
        onPrepare={handlePrepare}
        onCreate={handleCreate}
        onComplete={() => {
          // TODO :생성 완료 시 사용자에게 알려줄 것 정의하기.
        }}
      />

      {/* Jira API 연동 모달 */}
      <JiraIntegrationModal open={openJira} onOpenChange={setOpenJira} />
    </aside>
  );
};

export default Sidebar;
