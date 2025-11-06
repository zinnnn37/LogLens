import type { ComponentProps } from 'react';
import { NavLink, useParams } from 'react-router-dom';
import { History, Bot, Workflow, Blocks, ChartColumnBig } from 'lucide-react';
import clsx from 'clsx';
import { createProjectPath } from '@/router/route-path';

type HeaderProps = ComponentProps<'header'>;

interface NavItem {
  label: string;
  icon: React.ElementType;
  page: 'logs' | 'dashboard' | 'dependency-graph' | 'request-flow' | 'chatbot';
}

const NAV_ITEMS: NavItem[] = [
  { label: '로그 내역', icon: History, page: 'logs' },
  { label: '대시보드', icon: Blocks, page: 'dashboard' },
  {
    label: '의존성 그래프',
    icon: ChartColumnBig,
    page: 'dependency-graph',
  },
  { label: '요청 흐름', icon: Workflow, page: 'request-flow' },
  { label: 'AI Chat', icon: Bot, page: 'chatbot' },
];

/** 개별 네비게이션 링크 */
const HeaderLink = ({
  page,
  icon: Icon,
  label,
  projectUuid,
}: NavItem & { projectUuid?: string }) => {
  // projectUuid가 없으면 비활성 상태로 렌더
  if (!projectUuid) {
    return (
      <span
        className="flex cursor-not-allowed items-center gap-1.5 text-sm text-gray-400 opacity-60"
        title="프로젝트를 선택해주세요"
        aria-disabled="true"
      >
        <Icon className="h-4 w-4" />
        {label}
      </span>
    );
  }

  const to = createProjectPath(projectUuid, page);

  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        clsx(
          'flex items-center gap-1.5 text-sm transition-colors',
          isActive
            ? 'text-primary font-semibold'
            : 'hover:text-secondary text-gray-600',
        )
      }
      end
    >
      <Icon className="h-4 w-4" />
      {label}
    </NavLink>
  );
};

const Header = ({ className, ...props }: HeaderProps) => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // 메인 페이지(프로젝트 선택 전)에서는 Header를 숨김
  if (!projectUuid) {
    return null;
  }

  return (
    <header
      className={clsx(
        'bg-card flex h-16 items-center justify-end border-b px-6',
        className,
      )}
      {...props}
    >
      <nav>
        <ul className="font-godoM flex items-center gap-6">
          {NAV_ITEMS.map(item => (
            <li key={item.label}>
              <HeaderLink {...item} projectUuid={projectUuid} />
            </li>
          ))}
        </ul>
      </nav>
    </header>
  );
};

export default Header;
