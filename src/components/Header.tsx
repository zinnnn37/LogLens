import type { ComponentProps } from 'react';
import type { To } from 'react-router-dom';
import { NavLink } from 'react-router-dom';
import { History, Bot, Workflow, Blocks, ChartColumnBig } from 'lucide-react';
import clsx from 'clsx';
import { ROUTE_PATH } from '@/router/route-path';

type HeaderProps = ComponentProps<'header'>;

interface NavItem {
  label: string;
  icon: React.ElementType;
  to?: To;
}

const NAV_ITEMS = [
  { label: '로그 내역', icon: History, to: ROUTE_PATH.LOGS },
  { label: '대시보드', icon: Blocks, to: ROUTE_PATH.DASHBOARD },
  {
    label: '의존성 그래프',
    icon: ChartColumnBig,
    to: ROUTE_PATH.DEPENDENCY_GRAPH,
  },
  { label: '요청 흐름', icon: Workflow, to: ROUTE_PATH.REQUEST_FLOW },
  { label: 'AI Chat', icon: Bot, to: ROUTE_PATH.AI_CHAT },
] satisfies NavItem[];

/** 개별 네비게이션 링크 */
const HeaderLink = ({ to, icon: Icon, label }: NavItem) => {
  // to가 undefined/null이면 비활성 상태로 렌더
  if (to == null) {
    return (
      <span
        className="flex cursor-not-allowed items-center gap-1.5 text-sm text-gray-400 opacity-60"
        title="Not available yet"
        aria-disabled="true"
      >
        <Icon className="h-4 w-4" />
        {label}
      </span>
    );
  }

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
              <HeaderLink {...item} />
            </li>
          ))}
        </ul>
      </nav>
    </header>
  );
};

export default Header;
