import type { ComponentProps } from 'react';
import { PlusSquare, Settings, MessageSquare, LogOut } from 'lucide-react';

/**
 * 사이드바 Props
 * @extends ComponentProps<'aside'> - className, id 등 'aside' 태그의 모든 속성 상속
 *
 */
type SidebarProps = ComponentProps<'aside'> & {
  // 현재는 커스텀 prop이 없지만, 필요시 여기에 추가
};

const NavLink = ({
  icon: Icon,
  children,
}: {
  icon: React.ElementType;
  children: React.ReactNode;
}) => (
  <a
    href="#"
    className="text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground flex items-center gap-3 rounded-lg px-3 py-2 transition-all"
  >
    <Icon className="h-4 w-4" />
    {children}
  </a>
);

/**
 * 섹션 제목을 위한 내부 컴포넌트
 */
const NavHeading = ({ children }: { children: React.ReactNode }) => (
  <h2 className="text-muted-foreground mb-2 px-3 text-xs font-semibold tracking-wider uppercase">
    {children}
  </h2>
);

/**
 * 메인 사이드바 컴포넌트
 */
const Sidebar = ({ className, ...props }: SidebarProps) => {
  return (
    <aside
      className={`border-sidebar-border flex h-screen w-60 flex-col justify-between border-r p-2 ${
        className || ''
      }`}
      {...props}
    >
      {/* 1. 상단 그룹 (로고 + 메뉴) */}
      <div>
        {/* 로고 */}
        <div className="mb-3 flex h-14 items-center px-2">
          <a href="/" className="text-4xl font-bold">
            <span className="text-[#1C1C1C]">Log</span>
            <span className="text-[#6A91BE]">Lens</span>
          </a>
        </div>

        {/* 네비게이션 메뉴 */}
        <nav className="flex flex-col gap-4">
          {/* Projects 섹션 */}
          <section>
            <NavHeading>Projects</NavHeading>
            <ul className="flex flex-col gap-1 text-[#6A6A6A]">
              <li>
                <NavLink icon={PlusSquare}>새 프로젝트 생성</NavLink>
              </li>
              {/* 추후에 프로젝트 조회한 결과 나열해주기 */}
            </ul>
          </section>

          {/* Support 섹션 */}
          <section>
            <NavHeading>Support</NavHeading>
            <ul className="flex flex-col gap-1">
              <li>
                <NavLink icon={Settings}>Jira API 연결</NavLink>
              </li>
            </ul>
          </section>
        </nav>
      </div>

      {/* 2. 하단 그룹 (AI Chat + Logout) */}
      <div>
        {/* 구분선 */}
        <hr className="border-sidebar-border my-4" />

        <nav className="flex flex-col gap-1">
          <NavLink icon={MessageSquare}>AI Chat</NavLink>
          <NavLink icon={LogOut}>Log out</NavLink>
        </nav>
      </div>
    </aside>
  );
};

export default Sidebar;
