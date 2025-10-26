// src/pages/LogsPage.tsx
import { useState } from 'react';
import DetailLogSearchBox, {
  type SearchCriteria,
} from '@/components/DetailLogSearchBox';
import LogTrendCard from '@/components/LogTrendCard';
import TrafficGraphCard from '@/components/TrafficGraphCard';
import { DUMMY_LOG_SEARCH_RESULTS } from '@/mocks/dummyLogSearch';
import type { LogRow } from '@/components/LogResultsTable';
import DetailLogSearchTable from '@/components/DetailLogSearchTable';

const LogsPage = () => {
  const [filteredLogs, setFilteredLogs] = useState<LogRow[]>(
    DUMMY_LOG_SEARCH_RESULTS,
  );

  const handleSearch = (criteria: SearchCriteria) => {
    const newFilteredLogs = DUMMY_LOG_SEARCH_RESULTS.filter((log) => {
      // TraceID 필터
      if (
        criteria.traceId &&
        !log.id.toLowerCase().includes(criteria.traceId.toLowerCase())
      ) {
        return false;
      }

      // 시스템 필터
      if (criteria.system !== 'all' && log.layer !== criteria.system) {
        return false;
      }

      // 레벨 필터
      if (criteria.level !== 'all' && log.level !== criteria.level) {
        return false;
      }

      return true;
    });

    setFilteredLogs(newFilteredLogs);
  };

  return (
    <div className="font-pretendard space-y-6 p-6 py-1">
      <h1 className="font-godoM text-lg">통계 요약</h1>

      {/* 로그 발생 추이 */}
      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <div className="xl:col-span-1">
          <LogTrendCard />
        </div>

        {/* 트래픽 그래프 */}
        <div className="xl:col-span-1">
          <TrafficGraphCard />
        </div>
      </div>

      {/* 검색창 */}
      <div>
        <DetailLogSearchBox onSearch={handleSearch} />
      </div>

      {/* 검색 결과 표 */}
      <div>
        <DetailLogSearchTable logs={filteredLogs} />
      </div>
    </div>
  );
};

export default LogsPage;