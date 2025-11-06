// src/pages/LogsPage.tsx
<<<<<<< HEAD
import { useState, useCallback, useEffect } from 'react'; 
import { useParams } from 'react-router-dom'; // url 에서 projectUuid 가져오기위해
import { searchLogs } from '@/services/logService'; 
import type { LogData, LogSearchParams } from '@/types/log'; 

=======
import { useState, useCallback } from 'react'; // useCallback 추가
import { useParams } from 'react-router-dom';
>>>>>>> fe/develop
import DetailLogSearchBox, {
  type SearchCriteria,
} from '@/components/DetailLogSearchBox';
import LogTrendCard from '@/components/LogTrendCard';
import TrafficGraphCard from '@/components/TrafficGraphCard';
import FloatingChecklist from '@/components/FloatingChecklist';

import DetailLogSearchTable from '@/components/DetailLogSearchTable';

// --- 모달 컴포넌트 임포트 ---
import { Dialog, DialogContent } from '@/components/ui/dialog';
import LogDetailModal1 from '@/components/modal/LogDetailModal1';
import LogDetailModal2, {
  type JiraTicketFormData,
} from '@/components/modal/LogDetailModal2';

const TEMP_PROJECT_UUID = '48d96cd7-bf8d-38f5-891c-9c2f6430d871';

const LogsPage = () => {
  const { projectUuid: uuidFromParams } = useParams<{ projectUuid: string }>();
  const projectUuid = uuidFromParams || TEMP_PROJECT_UUID;

  const [logs, setLogs] = useState<LogData[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [cursor, setCursor] = useState<string | undefined>(undefined);

  const [criteria, setCriteria] = useState<SearchCriteria | null>(null);

  const fetchLogs = useCallback(
    async (
      isInitial: boolean,
      searchCriteria: SearchCriteria | null,
    ) => {
      if (!projectUuid) {return;}
      if (loading || (!isInitial && !hasMore)) {return;}

      setLoading(true);

      const params: LogSearchParams = {
        projectUuid,
        cursor: isInitial ? undefined : cursor,
        size: 50,
        logLevel: searchCriteria?.logLevel?.length
          ? searchCriteria.logLevel
          : undefined,
        sourceType: searchCriteria?.sourceType?.length
          ? searchCriteria.sourceType
          : undefined,
        traceId: searchCriteria?.traceId || undefined,
        keyword: searchCriteria?.keyword || undefined,
        startTime: searchCriteria?.startTime || undefined,
        endTime: searchCriteria?.endTime || undefined,
        sort: searchCriteria?.sort || 'TIMESTAMP,DESC',
      };

      try {
        const response = await searchLogs(params);
        const newLogs = response.logs; // 💡 LogData 원본 사용

        setLogs(prev => (isInitial ? newLogs : [...prev, ...newLogs]));
        setCursor(response.pagination.nextCursor || undefined);
        setHasMore(response.pagination.hasNext);
      } catch (error) {
        console.error('로그 조회 실패:', error);
      } finally {
        setLoading(false);
      }
    },
    [projectUuid, cursor, hasMore, loading],
  );

  // --- 최초 로드 ---
  useEffect(() => {
    if (projectUuid) {
      fetchLogs(true, null); 
    }
  }, [projectUuid]); 

  // 검색핸들러
  const handleSearch = (newCriteria: SearchCriteria) => {
    setCriteria(newCriteria); 
    fetchLogs(true, newCriteria); 
  };

  // 무한스크롤
  const handleLoadMore = () => {
    fetchLogs(false, criteria); 
  };

  const [selectedLog, setSelectedLog] = useState<LogData | null>(null); 
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalPage, setModalPage] = useState<'page1' | 'page2'>('page1');

  // 디테일 모달

 
  const handleRowClick = useCallback((log: LogData) => { 
    setSelectedLog(log);
    setIsModalOpen(true);
    setModalPage('page1'); 
  }, []);

  /**
   * 모달 닫기/열기 상태 변경 시
   */
  const handleModalOpenChange = (open: boolean) => {
    setIsModalOpen(open);
    if (!open) {
      setModalPage('page1');
    }
  };

  /**
   * 모달 1->2페이지
   */
  const handleGoToNextPage = () => {
    setModalPage('page2');
  };

  /**
   * 모달 2->1 페이지
   */
  const handleGoBack = () => {
    setModalPage('page1');
  };

  /**
   * 발행하기 버튼
   */
  const handleSubmitJira = (formData: JiraTicketFormData) => {
    console.log(
      'Jira Ticket Submitted:',
      formData,
      'for log:',
      selectedLog?.logId, 
    );
    // TODO: 실제 Jira 티켓 발행 API 호출
    alert('이쁜 alert 로 수정 예정입니다.');
    setIsModalOpen(false); 
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

      {/* 검색 결과 */}
      <div>
        <DetailLogSearchTable
          logs={logs}
          loading={loading}
          hasMore={hasMore}
          onLoadMore={handleLoadMore} 
          onRowClick={handleRowClick} 
        />
      </div>

      {/* 로그 상세 정보 모달 */}
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