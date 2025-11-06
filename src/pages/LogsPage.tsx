// src/pages/LogsPage.tsx
import { useState, useCallback } from 'react'; // useCallback 추가
import { useParams } from 'react-router-dom';
import DetailLogSearchBox, {
  type SearchCriteria,
} from '@/components/DetailLogSearchBox';
import LogTrendCard from '@/components/LogTrendCard';
import TrafficGraphCard from '@/components/TrafficGraphCard';
import FloatingChecklist from '@/components/FloatingChecklist';
import { DUMMY_LOG_SEARCH_RESULTS } from '@/mocks/dummyLogSearch';
import type { LogRow } from '@/components/LogResultsTable';
import DetailLogSearchTable from '@/components/DetailLogSearchTable';

// --- 모달 컴포넌트 임포트 ---
import { Dialog, DialogContent } from '@/components/ui/dialog';
import LogDetailModal1 from '@/components/modal/LogDetailModal1';
import LogDetailModal2, {
  type JiraTicketFormData,
} from '@/components/modal/LogDetailModal2';

const LogsPage = () => {
  const { projectUuid } = useParams<{ projectUuid: string }>();

  // TODO: projectUuid를 사용해서 실제 프로젝트 로그 데이터 가져오기
  console.log('Current project UUID:', projectUuid);

  // --- 기존 검색 상태 ---
  const [filteredLogs, setFilteredLogs] = useState<LogRow[]>(
    DUMMY_LOG_SEARCH_RESULTS,
  );

  // --- 모달 관리 상태 ---
  const [selectedLog, setSelectedLog] = useState<LogRow | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalPage, setModalPage] = useState<'page1' | 'page2'>('page1');

  /**
   * '검색' 버튼 클릭 핸들러 (기존 동기식 필터링)
   */
  const handleSearch = (criteria: SearchCriteria) => {
    const newFilteredLogs = DUMMY_LOG_SEARCH_RESULTS.filter(log => {
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

  // --- 모달 이벤트 핸들러 ---

  /**
   * 테이블 행 클릭 시
   */
  const handleRowClick = useCallback((log: LogRow) => {
    setSelectedLog(log);
    setIsModalOpen(true);
    setModalPage('page1'); // 항상 1페이지부터 시작
  }, []);

  /**
   * 모달 닫기/열기 상태 변경 시
   */
  const handleModalOpenChange = (open: boolean) => {
    setIsModalOpen(open);
    if (!open) {
      // 모달이 닫히면 항상 1페이지로 리셋
      setModalPage('page1');
    }
  };

  /**
   * (1페이지 -> 2페이지) 'Jira 티켓 발행' 버튼 클릭 시
   */
  const handleGoToNextPage = () => {
    setModalPage('page2');
  };

  /**
   * (2페이지 -> 1페이지) '이전' 버튼 클릭 시
   */
  const handleGoBack = () => {
    setModalPage('page1');
  };

  /**
   * (2페이지) '발행하기' 버튼 클릭 시
   */
  const handleSubmitJira = (formData: JiraTicketFormData) => {
    console.log(
      'Jira Ticket Submitted:',
      formData,
      'for log:',
      selectedLog?.id,
    );
    // TODO: 실제 Jira 티켓 발행 API 호출
    alert('이쁜 alert 로 수정 예정입니다.');
    setIsModalOpen(false); // 성공 시 모달 닫기
  };

  return (
    <div className="font-pretendard space-y-6 p-6 py-1">
      <h1 className="font-godoM text-lg">로그 내역</h1>

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
        {/* onRowClick 핸들러를 전달합니다. */}
        <DetailLogSearchTable logs={filteredLogs} onRowClick={handleRowClick} />
      </div>

      {/* --- 모달 렌더링 --- */}

      {/* 1페이지 모달 렌더링 */}
      {modalPage === 'page1' && (
        <LogDetailModal1
          open={isModalOpen}
          onOpenChange={handleModalOpenChange}
          log={selectedLog}
          onGoToNextPage={handleGoToNextPage}
        />
      )}

      {/* 2페이지 모달 렌더링 */}
      {modalPage === 'page2' && selectedLog && (
        <Dialog open={isModalOpen} onOpenChange={handleModalOpenChange}>
          <DialogContent className="sm:max-w-3xl">
            {/* log 데이터가 null이 아닐 때만 렌더링 */}
            <LogDetailModal2
              log={selectedLog}
              onGoBack={handleGoBack}
              onSubmit={handleSubmitJira}
            />
          </DialogContent>
        </Dialog>
      )}

      <FloatingChecklist />
    </div>
  );
};

export default LogsPage;
