import { useState } from 'react';
import { CheckCircle2, Circle, ListChecks, Minimize2 } from 'lucide-react';
import clsx from 'clsx';

interface ChecklistItem {
  id: string;
  label: string;
  completed: boolean;
}

const INITIAL_CHECKLIST: ChecklistItem[] = [
  {
    id: '1',
    label: 'Spring Boot 프로젝트에 LogLens SDK 설치',
    completed: false,
  },
  { id: '2', label: 'application.yml 설정 완료', completed: false },
  { id: '3', label: '프로젝트 생성 및 API Key 발급', completed: false },
  { id: '4', label: '로그 전송 확인', completed: false },
  { id: '5', label: '대시보드에서 로그 확인', completed: false },
];

const FloatingChecklist = () => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [checklist, setChecklist] =
    useState<ChecklistItem[]>(INITIAL_CHECKLIST);

  const toggleExpand = () => {
    setIsExpanded(!isExpanded);
  };

  const toggleCheckItem = (id: string) => {
    setChecklist(prev =>
      prev.map(item =>
        item.id === id ? { ...item, completed: !item.completed } : item,
      ),
    );
  };

  const completedCount = checklist.filter(item => item.completed).length;
  const totalCount = checklist.length;

  return (
    <div className="fixed right-6 bottom-6 z-50">
      {/* 접힌 상태: 동그란 플로팅 버튼 */}
      <div
        className={clsx(
          'absolute right-0 bottom-0 transition-all duration-300 ease-in-out',
          isExpanded ? 'scale-0 opacity-0' : 'scale-100 opacity-100',
        )}
      >
        <button
          onClick={toggleExpand}
          className="group bg-primary relative flex h-14 w-14 items-center justify-center rounded-full text-white shadow-lg transition-all duration-300 hover:scale-110 hover:shadow-xl"
        >
          <ListChecks className="h-6 w-6" />
          {/* 뱃지: 남은 개수 */}
          {completedCount < totalCount && (
            <span className="absolute -top-1 -right-1 flex h-6 w-6 items-center justify-center rounded-full bg-red-500 text-xs font-bold text-white shadow-md transition-all">
              {totalCount - completedCount}
            </span>
          )}
        </button>
      </div>

      {/* 펼친 상태: 체크리스트 카드 */}
      <div
        className={clsx(
          'absolute right-0 bottom-0 w-80 origin-bottom-right transform rounded-lg border border-gray-200 bg-white shadow-lg transition-all duration-300 ease-in-out',
          isExpanded
            ? 'scale-100 opacity-100'
            : 'pointer-events-none scale-95 opacity-0',
        )}
      >
        {/* Header */}
        <div className="bg-primary flex items-center justify-between rounded-t-lg px-4 py-3 text-white">
          <div className="flex items-center gap-2">
            <ListChecks className="h-5 w-5" />
            <h3 className="font-semibold">LogLens 시작 가이드</h3>
          </div>
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-white/20 px-2.5 py-0.5 text-xs font-semibold">
              {completedCount}/{totalCount}
            </span>
            <button
              onClick={toggleExpand}
              className="group/close rounded-full p-1 transition-all hover:bg-white/20"
              aria-label="체크리스트 접기"
            >
              <Minimize2 className="h-4 w-4 transition-transform group-hover/close:scale-110" />
            </button>
          </div>
        </div>

        {/* Checklist Items */}
        <div className="max-h-96 overflow-y-auto p-4">
          <ul className="space-y-3">
            {checklist.map((item, index) => (
              <li
                key={item.id}
                className="group flex cursor-pointer items-start gap-2 transition-transform hover:translate-x-1"
                onClick={() => toggleCheckItem(item.id)}
                style={{
                  animation: isExpanded
                    ? `slideIn 0.3s ease-out ${index * 0.05}s both`
                    : 'none',
                }}
              >
                <button className="mt-0.5 transition-transform group-hover:scale-110">
                  {item.completed ? (
                    <CheckCircle2 className="h-5 w-5 text-green-500" />
                  ) : (
                    <Circle className="h-5 w-5 text-gray-300 transition-colors group-hover:text-gray-400" />
                  )}
                </button>
                <span
                  className={clsx(
                    'flex-1 text-sm transition-all',
                    item.completed
                      ? 'text-gray-400 line-through'
                      : 'text-gray-700 group-hover:text-gray-900',
                  )}
                >
                  {item.label}
                </span>
              </li>
            ))}
          </ul>
        </div>

        {/* Progress Bar */}
        <div className="px-4 pb-4">
          <div className="h-2 overflow-hidden rounded-full bg-gray-200">
            <div
              className="bg-primary h-full transition-all duration-500 ease-out"
              style={{ width: `${(completedCount / totalCount) * 100}%` }}
            />
          </div>
          <p className="mt-2 text-center text-xs text-gray-500">
            {completedCount === totalCount
              ? '모든 단계를 완료했습니다! 🎉'
              : `${totalCount - completedCount}개 항목 남음`}
          </p>
        </div>
      </div>

      {/* 애니메이션 키프레임 */}
      <style>{`
        @keyframes slideIn {
          from {
            opacity: 0;
            transform: translateX(-10px);
          }
          to {
            opacity: 1;
            transform: translateX(0);
          }
        }
      `}</style>
    </div>
  );
};

export default FloatingChecklist;
