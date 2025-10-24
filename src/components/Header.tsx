import type { ComponentProps } from 'react';
import { History, Bot, Workflow, Blocks, ChartColumnBig } from 'lucide-react';

/**
 * 헤더 Props
 */
type HeaderProps = ComponentProps<'header'> & {
  // 커스텀 Props가 필요하면 여기에 추가
};

const HeaderLink = ({
  icon: Icon,
  children,
}: {
  icon: React.ElementType;
  children: React.ReactNode;
}) => (
  <a
    href="#"
    className="hover:text-primary flex items-center gap-1.5 text-sm text-gray-600 transition-colors"
  >
    <Icon className="h-4 w-4" />
    {children}
  </a>
);

/**
 * 메인 헤더 컴포넌트
 */
const Header = ({ className, ...props }: HeaderProps) => {
  return (
    <header
      className={`bg-card flex h-16 items-center justify-end border-b px-6 ${
        className || ''
      }`}
      {...props}
    >
      <nav>
        {/* 메뉴 항목 리스트 */}
        <ul className="font-godoM flex items-center gap-6">
          <li>
            <HeaderLink icon={History}>로그 내역</HeaderLink>
          </li>
          <li>
            <HeaderLink icon={Blocks}>대시보드</HeaderLink>
          </li>
          <li>
            <HeaderLink icon={ChartColumnBig}>의존성 그래프</HeaderLink>
          </li>
          <li>
            <HeaderLink icon={Workflow}>요청 흐름</HeaderLink>
          </li>
          <li>
            <HeaderLink icon={Bot}>AI Chat</HeaderLink>
          </li>
        </ul>
      </nav>
    </header>
  );
};

export default Header;
