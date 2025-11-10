// src/pages/LogsPage.tsx

import { useState, useCallback, useEffect, useRef } from 'react';
import { searchLogs } from '@/services/logService';
import { createJiraIssue } from '@/services/jiraService';
import type { LogData, LogSearchParams } from '@/types/log';

import { useParams } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Download } from 'lucide-react';
import { toast } from 'sonner';

import DetailLogSearchBox, {
  type SearchCriteria,
} from '@/components/DetailLogSearchBox';
import LogTrendCard from '@/components/LogTrendCard';
import TrafficGraphCard from '@/components/TrafficGraphCard';
import FloatingChecklist from '@/components/FloatingChecklist';

import DetailLogSearchTable from '@/components/DetailLogSearchTable';

import { Dialog, DialogContent } from '@/components/ui/dialog';
import LogDetailModal1 from '@/components/modal/LogDetailModal1';
import LogDetailModal2, {
  type JiraTicketFormData,
} from '@/components/modal/LogDetailModal2';
import JiraIntegrationModal from '@/components/modal/JiraIntegrationModal'; // Jira 모달 import
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';

const LogsPage = () => {
  const { projectUuid: uuidFromParams } = useParams<{ projectUuid: string }>();
  const projectUuid = uuidFromParams;

  const [logs, setLogs] = useState<LogData[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [cursor, setCursor] = useState<string | undefined>(undefined);

  const [criteria, setCriteria] = useState<SearchCriteria | null>(null);

  // 모달 상태 관리
  const [selectedLog, setSelectedLog] = useState<LogData | null>(null);
  const [isLogDetailModalOpen, setIsLogDetailModalOpen] = useState(false);
  const [modalPage, setModalPage] = useState<'page1' | 'page2'>('page1');
  const [isJiraConnectModalOpen, setIsJiraConnectModalOpen] = useState(false); // Jira 연동 모달 상태

  const fetchLogs = useCallback(
    async (isInitial: boolean, searchCriteria: SearchCriteria | null) => {
      if (!projectUuid) {
        return;
      }
      if (loading || (!isInitial && !hasMore)) {
        return;
      }

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

        if ('pagination' in response) {
          const newLogs = response.logs;
          setLogs(prev => (isInitial ? newLogs : [...prev, ...newLogs]));
          setCursor(response.pagination.nextCursor || undefined);
          setHasMore(response.pagination.hasNext);
        } else if ('summary' in response) {
          const newLogs = response.logs;
          setLogs(prev => (isInitial ? newLogs : [...prev, ...newLogs]));
          setCursor(undefined);
          setHasMore(false);
        }
      } catch (error) {
        console.error('로그 조회 실패:', error);
        setHasMore(false);
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectUuid]);

  // --- 5초 자동 갱신 (Polling) 로직 ---
  const savedCriteria = useRef(criteria);
  useEffect(() => {
    savedCriteria.current = criteria;
  }, [criteria]);

  /* // 5초 자동 갱신 (서버 부하로 임시 주석 처리)
  useEffect(() => {
    const tick = async () => {
      if (loading || !projectUuid) {
        return;
      }

      const currentCriteria = savedCriteria.current;
      const params: LogSearchParams = {
        projectUuid,
        cursor: undefined,
        size: 50,
        logLevel: currentCriteria?.logLevel?.length
          ? currentCriteria.logLevel
          : undefined,
        sourceType: currentCriteria?.sourceType?.length
          ? currentCriteria.sourceType
          : undefined,
        traceId: currentCriteria?.traceId || undefined,
        keyword: currentCriteria?.keyword || undefined,
        startTime: currentCriteria?.startTime || undefined,
        endTime: currentCriteria?.endTime || undefined,
        sort: currentCriteria?.sort || 'TIMESTAMP,DESC',
      };

      try {
        const response = await searchLogs(params);
        if ('pagination' in response) {
          const newLogs = response.logs;

          setLogs(prevLogs => {
            const existingLogIds = new Set(prevLogs.map(log => log.logId));
            const actuallyNewLogs = newLogs.filter(
              log => !existingLogIds.has(log.logId),
            );

            if (actuallyNewLogs.length === 0) {
              return prevLogs;
            }
            return [...actuallyNewLogs, ...prevLogs];
          });
        }
      } catch (error) {
        console.error('5초 갱신 실패 (무시함):', error);
      }
    };

    const intervalId = setInterval(tick, 5000);
    return () => clearInterval(intervalId);
  }, [loading, projectUuid]);
  */

  // 검색핸들러
  const handleSearch = (newCriteria: SearchCriteria) => {
    setCriteria(newCriteria);
    fetchLogs(true, newCriteria);
  };

  // 무한스크롤
  const handleLoadMore = () => {
    fetchLogs(false, criteria);
  };

  // CSV 다운로드
  const handleDownloadCSV = () => {
    if (logs.length === 0) {
      toast.warning('다운로드할 로그 데이터가 없습니다.', {
        description:
          '현재 화면에 조회된 로그가 0개입니다. 로그 검색 후 시도해주세요.',
        icon: <Download className="h-4 w-4 text-orange-500" />,
        duration: 3000,
      });
      return;
    }

    const escapeCSV = (val: string | null | undefined): string => {
      if (val === null || val === undefined) {
        return '';
      }
      let str = String(val);
      if (str.match(/([",\n])/)) {
        str = `"${str.replace(/"/g, '""')}"`;
      }
      return str;
    };

    const headers = [
      'timestamp',
      'logLevel',
      'sourceType',
      'layer',
      'logger',
      'message',
      'traceId',
      'logId',
      'comment',
    ];

    const csvRows = logs.map(log => {
      return [
        escapeCSV(log.timestamp),
        escapeCSV(log.logLevel),
        escapeCSV(log.sourceType),
        escapeCSV(log.layer),
        escapeCSV(log.logger),
        escapeCSV(log.message),
        escapeCSV(log.traceId),
        log.logId,
        escapeCSV(log.comment),
      ].join(',');
    });

    // 한글 깨짐 방지
    const csvContent = [headers.join(','), ...csvRows].join('\n');
    const blob = new Blob(['\uFEFF' + csvContent], {
      type: 'text/csv;charset=utf-8;',
    });

    // 생성 및 다운로드
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    const dateStr = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-');
    link.setAttribute('href', url);
    link.setAttribute('download', `loglens_logs_${dateStr}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    // 다운로드 성공 알림 추가
    toast.success('로그 데이터 다운로드를 시작합니다.', {
      description: `${logs.length}개의 로그를 CSV 파일로 저장합니다.`,
      duration: 3000,
    });
  };

  // 디테일 모달 열기
  const handleRowClick = useCallback((log: LogData) => {
    setSelectedLog(log);
    setIsLogDetailModalOpen(true);
    setModalPage('page1');
  }, []);

  /**
   * 로그 상세 모달 닫기/열기 상태 변경
   */
  const handleLogDetailModalOpenChange = (open: boolean) => {
    setIsLogDetailModalOpen(open);
    if (!open) {
      setModalPage('page1');
    }
  };

  /**
   * Jira 연동 모달 열기 (로그 상세 모달은 닫음)
   */
  const handleOpenJiraConnect = () => {
    setIsLogDetailModalOpen(false);
    setIsJiraConnectModalOpen(true);
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
   * Jira 티켓 발행 버튼 (API 연동됨)
   */
  const handleSubmitJira = async (formData: JiraTicketFormData) => {
    if (!projectUuid || !selectedLog?.logId) {
      toast.error('프로젝트 정보나 로그 정보를 찾을 수 없습니다.');
      return;
    }

    try {
      await createJiraIssue({
        projectUuid: projectUuid,
        logId: selectedLog.logId,
        ...formData,
      });

      toast.success('Jira 이슈가 성공적으로 발행되었습니다!');
      setIsLogDetailModalOpen(false); // 모달 닫기
    } catch (error) {
      console.error('Jira 이슈 발행 실패:', error);
      toast.error('Jira 이슈 발행에 실패했습니다. 잠시 후 다시 시도해주세요.');
    }
  };

  return (
    <TooltipProvider>
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

        {/* 로그 상세 정보 모달 (1페이지) */}
        {modalPage === 'page1' && (
          <LogDetailModal1
            open={isLogDetailModalOpen}
            onOpenChange={handleLogDetailModalOpenChange}
            log={selectedLog}
            onGoToNextPage={handleGoToNextPage}
            onOpenJiraConnect={handleOpenJiraConnect} // Jira 연동 모달 열기 핸들러 전달
          />
        )}

        {/* 로그 상세 정보 모달 (2페이지 - Jira 발행) */}
        {modalPage === 'page2' && selectedLog && (
          <Dialog
            open={isLogDetailModalOpen}
            onOpenChange={handleLogDetailModalOpenChange}
          >
            <DialogContent className="sm:max-w-3xl">
              <LogDetailModal2
                log={selectedLog}
                onGoBack={handleGoBack}
                onSubmit={handleSubmitJira}
              />
            </DialogContent>
          </Dialog>
        )}

        {/* Jira 연동 모달 */}
        {projectUuid && (
          <JiraIntegrationModal
            open={isJiraConnectModalOpen}
            onOpenChange={setIsJiraConnectModalOpen}
            projectUuid={projectUuid}
          />
        )}

        {/* CSV 다운버튼 */}
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              onClick={handleDownloadCSV}
              className="fixed right-6 bottom-[72px] flex h-14 w-14 items-center justify-center rounded-full p-0 shadow-lg transition-all duration-300 hover:scale-110 hover:shadow-xl"
              aria-label="로그 데이터 CSV 파일 다운로드"
            >
              <Download className="h-6 w-6" />
            </Button>
          </TooltipTrigger>
          <TooltipContent className="bg-gray-800 text-white shadow-md">
            로그 데이터 CSV 파일 다운로드
          </TooltipContent>
        </Tooltip>

        <FloatingChecklist />
      </div>
    </TooltipProvider>
  );
};

export default LogsPage;