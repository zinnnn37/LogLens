import { type ComponentProps, useEffect, useState, useCallback } from 'react';
import { NavLink, useParams } from 'react-router-dom';
import {
  History,
  Bot,
  Workflow,
  Blocks,
  ChartColumnBig,
  Bell,
} from 'lucide-react';
import clsx from 'clsx';
import { createProjectPath } from '@/router/route-path';
import { useAuthStore } from '@/stores/authStore';
import {
  getUnreadAlertCount,
  connectAlertStream,
} from '@/services/alertService';
import type { UnreadAlertCountResponse } from '@/types/alert';

import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import NotificationList from '@/components/NotificationList';

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
  const { accessToken } = useAuthStore();

  // --- 알림 상태 관리 ---
  const [unreadCount, setUnreadCount] = useState(0);
  const [isAlertPopoverOpen, setIsAlertPopoverOpen] = useState(false);
  const hasNewNotification = unreadCount > 0;

  // 안 읽은 알림 조회
  const fetchUnreadCount = useCallback(async () => {
    if (!projectUuid) {
      return;
    }
    try {
      const response: UnreadAlertCountResponse = await getUnreadAlertCount({
        projectUuid,
      });
      console.log('getUnreadAlertCount 응답:', response);
      setUnreadCount(response.unreadCount || 0);
    } catch (error) {
      console.error('안 읽은 알림 개수 조회 실패:', error);
    }
  }, [projectUuid]);

  // 페이지 진입 시 안읽은 알림 조회
  useEffect(() => {
    fetchUnreadCount();
  }, [fetchUnreadCount]);

  // 알림 SSE
  useEffect(() => {
    if (!projectUuid || !accessToken) {
      return;
    }

    console.log('🔔 알림 SSE 연결 시작...');
    const eventSource = connectAlertStream({ projectUuid }, accessToken);

    eventSource.onopen = () => {
      console.log('🔔 알림 SSE 연결 성공 🟢');
    };

    eventSource.addEventListener('alert-update', (event: MessageEvent) => {
      console.log('🔔 실시간 알림 수신:', event.data);
      setTimeout(() => fetchUnreadCount(), 500);
    });

    eventSource.onerror = err => {
      console.error('🔔 알림 SSE 연결 에러 🔴', err);
      eventSource.close();
    };

    return () => {
      console.log('🔔 알림 SSE 연결 종료');
      eventSource.close();
    };
  }, [projectUuid, accessToken, fetchUnreadCount]);

  // 알림버튼 클릭시 팝오버 핸들러
  const handlePopoverOpenChange = (open: boolean) => {
    setIsAlertPopoverOpen(open);
    if (open) {
      setUnreadCount(0);
    } else {
      fetchUnreadCount();
    }
  };

  // 메인 페이지(프로젝트 선택 전)에서는 Header를 숨김
  if (!projectUuid) {
    return null;
  }

  return (
    <header
      className={clsx(
        'bg-card flex h-16 items-center justify-end gap-6 border-b px-6',
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
      
      {/* 알림 버튼 누르면 나오는 영역 */}
      <Popover open={isAlertPopoverOpen} onOpenChange={handlePopoverOpenChange}>
        <PopoverTrigger asChild>
          <button
            type="button"
            className="hover:text-secondary relative text-gray-600 transition-colors"
            aria-label="알림 열기"
          >
            <Bell className="h-5 w-5" />
            {hasNewNotification && (
              <span className="absolute -top-1 -right-1 block h-2.5 w-2.5 rounded-full bg-red-500 ring-2 ring-card" />
            )}
          </button>
        </PopoverTrigger>
        <PopoverContent className="w-80 p-0" align="end">
          <NotificationList projectUuid={projectUuid} />
        </PopoverContent>
      </Popover>
    </header>
  );
};

export default Header;