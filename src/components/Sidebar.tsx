// src/components/Sidebar.tsx
import { useState } from 'react';
import type { ComponentProps } from 'react';
import { PlusSquare, Settings, MessageSquare, LogOut } from 'lucide-react';

import ProjectCreateModal from '@/components/modal/ProjectCreateModal';

// --- Sidebar Props ---
type SidebarProps = ComponentProps<'aside'> & {
  // 필요 시 확장
};

// 공통 스타일: 링크/버튼 모양 일치
const itemBase =
  'text-[#6A6A6A] hover:bg-sidebar-accent hover:text-sidebar-accent-foreground flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left transition-all';

// a 태그 버전 (라우팅/외부 링크 등)
const NavLink = ({
  icon: Icon,
  children,
  href = '#',
}: {
  icon: React.ElementType;
  children: React.ReactNode;
  href?: string;
}) => {
  return (
    <a href={href} className={itemBase}>
      <Icon className="text-primary h-4 w-4" />
      {children}
    </a>
  );
};

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
  // 프로젝트 생성 모달 Open 상태 관리
  const [openCreate, setOpenCreate] = useState(false);

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

  return (
    <aside
      className={`border-sidebar-border flex h-[100dvh] w-60 flex-col justify-between border-r p-2 ${className || ''}`}
      {...props}
    >
      {/* 1) 상단 그룹 */}
      <div className="font-godoM">
        {/* 로고 */}
        <div className="mb-3 flex h-14 items-center px-2">
          <a href="/" className="font-pretendard text-4xl font-bold">
            <span className="text-[#1C1C1C]">Log</span>
            <span className="text-[#6A91BE]">Lens</span>
          </a>
        </div>

        {/* 네비게이션 */}
        <nav className="flex flex-col gap-4">
          {/* Projects */}
          <section>
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
          <section>
            <NavHeading>
              <p className="font-pretendard">Support</p>
            </NavHeading>
            <ul className="flex flex-col gap-1">
              <li>
                <NavLink icon={Settings}>Jira API 연결</NavLink>
              </li>
            </ul>
          </section>
        </nav>
      </div>

      {/* 2) 하단 그룹 */}
      <div>
        <hr className="border-sidebar-border my-4" />
        <nav className="font-godoM flex flex-col gap-1">
          <NavLink icon={MessageSquare}>AI Chat</NavLink>
          <NavLink icon={LogOut}>Log out</NavLink>
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
    </aside>
  );
};

export default Sidebar;
