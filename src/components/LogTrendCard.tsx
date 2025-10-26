// src/components/LogTrendCard.tsx
import InfoIcon from '@/assets/images/InfoIcon.png';
import WarnIcon from '@/assets/images/WarnIcon.png';
import ErrorIcon from '@/assets/images/ErrorIcon.png';
import { DUMMY_LOGS } from '@/mocks/dummyLogs';

const LogTrendCard = () => {
    // DUMMY_LOGS에서 level별 카운트 집계
    const logCounts = DUMMY_LOGS.reduce(
        (acc, log) => {
            if (log.level === 'INFO') {
                acc.INFO++;
            } else if (log.level === 'WARN') {
                acc.WARN++;
            } else if (log.level === 'ERROR') {
                acc.ERROR++;
            }
            return acc;
        },
        { INFO: 0, WARN: 0, ERROR: 0 },
    );

    // UI 렌더링을 위한 데이터 배열
    const logLevels = [
        {
            level: 'INFO',
            label: 'info',
            icon: InfoIcon,
            count: logCounts.INFO,
        },
        {
            level: 'WARN',
            label: 'warn',
            icon: WarnIcon,
            count: logCounts.WARN,
        },
        {
            level: 'ERROR',
            label: 'error',
            icon: ErrorIcon,
            count: logCounts.ERROR,
        },
    ];

    return (
        <div className="rounded-lg border bg-white p-6 shadow-sm">
            {/* 카드 제목 */}
            <h2 className="mb-4 text-base font-semibold">로그 발생 추이</h2>

            {/* 카드 본문 */}
            <div className="flex flex-col space-y-6 sm:flex-row sm:space-x-6 sm:space-y-0">
                {/* 로그 카운트 리스트 */}
                <div className="space-y-4">
                    {logLevels.map((item) => (
                        <div key={item.level} className="flex items-center space-x-3">
                            <img
                                src={item.icon}
                                alt={item.label}
                                className="h-8 w-8 flex-shrink-0"
                            />
                            <div>
                                <p className="text-sm uppercase text-gray-500">{item.label}</p>
                                <p className="text-2xl font-bold">{item.count}</p>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="flex min-h-[250px] flex-1 items-center justify-center rounded-md bg-gray-50 text-sm text-gray-400">
                    그래프 영역
                </div>
            </div>
        </div>
    );
};

export default LogTrendCard;