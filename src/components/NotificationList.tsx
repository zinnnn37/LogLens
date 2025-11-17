import { useEffect, useState } from 'react';
import { getAlertHistory, readAlert } from '@/services/alertService';
import type { AlertHistoryItem } from '@/types/alert';
import { Settings, BellRing } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { AlertConfigModal } from '@/components/modal/AlertConfigModal';
import { formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

interface NotificationListProps {
  projectUuid: string;
}

/**
 * 시간 포맷 유틸리티
 */
const formatTimeAgo = (isoString: string): string => {
  try {
    const date = new Date(isoString);
    return formatDistanceToNow(date, { addSuffix: true, locale: ko });
  } catch (error) {
    console.error('Invalid date format:', isoString, error);
    return isoString;
  }
};

const NotificationList = ({ projectUuid }: NotificationListProps) => {
  const [alerts, setAlerts] = useState<AlertHistoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);

  useEffect(() => {
    const fetchHistory = async () => {
      if (!projectUuid) {
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const response = await getAlertHistory({
          projectUuid,
          resolvedYN: 'N', 
        });

        console.log('getAlertHistory 응답:', response);

        setAlerts(response);
      } catch (err) {
        console.error('알림 이력 조회 실패:', err);
        setError(err instanceof Error ? err : new Error('An error occurred'));
      } finally {
        setIsLoading(false);
      }
    };

    fetchHistory();
  }, [projectUuid]);

  const handleReadAlert = async (alertId: number) => {
    try {
      // API 호출
      await readAlert({ alertId: alertId });
      console.log(`${alertId} 읽음 처리 성공`);

      setAlerts(prevAlerts => prevAlerts.filter(a => a.id !== alertId));
    } catch (error) {
      console.error('알림 읽음 처리 실패:', error);
    }
  };

  // 로딩중-
  if (isLoading) {
    return (
      <div className="flex h-40 items-center justify-center">
        <p className="text-sm text-gray-500">알림을 불러오는 중...</p>
      </div>
    );
  }

  // 에러
  if (error) {
    return (
      <div className="flex h-40 items-center justify-center p-4">
        <p className="text-center text-sm text-red-600">
          알림을 불러오는데 실패했습니다.
          <br />
          <span className="text-xs text-gray-500">{error.message}</span>
        </p>
      </div>
    );
  }

  // 데이터없음
  if (alerts.length === 0) {
    return (
      <>
        <div className="flex h-40 flex-col items-center justify-center gap-4 p-4">
          <p className="text-sm text-gray-500">표시할 알림이 없습니다.</p>
          <div className="flex items-center justify-center p-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsSettingsModalOpen(true)}
            >
              <Settings className="mr-2 h-4 w-4" />
              알림 설정
            </Button>
          </div>
        </div>
        <AlertConfigModal
          projectUuid={projectUuid}
          open={isSettingsModalOpen}
          onOpenChange={setIsSettingsModalOpen}
        />
      </>
    );
  }

  // 알림 목록
  return (
    <>
      <div className="max-h-96 overflow-y-auto">
        <ul className="divide-y divide-gray-100">
          {alerts.map(alert => (
            <li
              key={alert.id}
              className="cursor-pointer p-3 hover:bg-gray-50"
              onClick={() => handleReadAlert(alert.id)}
            >
              <div className="flex items-center gap-2">
                <span
                  className={
                    alert.resolvedYN === 'N' ? 'text-red-500' : 'text-gray-400'
                  }
                >
                  <BellRing className="h-4 w-4" />
                </span>
                <p className="flex-1 truncate text-sm font-semibold text-gray-800">
                  {alert.alertMessage}
                </p>
              </div>
              <p className="mt-1 pl-6 text-xs text-gray-400">
                {formatTimeAgo(alert.alertTime)}
              </p>
            </li>
          ))}
        </ul>

        <div className="flex items-center justify-between border-t p-2">
          <Button variant="link" size="sm" className="text-xs">
            모든 알림 보기
          </Button>

          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => setIsSettingsModalOpen(true)}
          >
            <Settings className="h-4 w-4" />
            <span className="sr-only">알림 설정 열기</span>
          </Button>
        </div>
      </div>

      <AlertConfigModal
        projectUuid={projectUuid}
        open={isSettingsModalOpen}
        onOpenChange={setIsSettingsModalOpen}
      />
    </>
  );
};

export default NotificationList;
