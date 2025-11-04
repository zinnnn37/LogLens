// src/components/WithProject.tsx
import { Button } from '@/components/ui/button';
import { UserPlus2, Trash2, Link2 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import MemberInviteModal from './modal/MemberInviteModal';
import { AnimatePresence, motion } from 'framer-motion';
import { JiraIntegrationModal } from '@/components/modal/JiraIntegrationModal';
import type { ProjectInfoDTO } from '@/types/project';

export interface WithProjectProps {
  projects?: ProjectInfoDTO[];
  onSelect?: (id: number) => void;
  onDelete?: (id: number) => void;
  onEmptyAfterExit?: () => void;
}

const formatK = (n: number) => {
  if (n < 1000) {
    return `${n}`;
  }
  const k = n / 1000;
  return `${Number.isInteger(k) ? k.toFixed(0) : k.toFixed(1)}K`;
};

const DOT = ' • ';

const WithProject = ({
  projects,
  onSelect,
  onDelete,
  onEmptyAfterExit,
}: WithProjectProps) => {
  const list = projects ?? [];

  const [invitingProjectId, setInvitingProjectId] = useState<number | null>(
    null,
  );
  const [jiraProjectId, setJiraProjectId] = useState<number | null>(null);

  const becameEmptyRef = useRef(false);
  const prevLenRef = useRef(list.length);

  useEffect(() => {
    const prev = prevLenRef.current;
    const curr = list.length;
    becameEmptyRef.current = prev > 0 && curr === 0;
    prevLenRef.current = curr;
  }, [list.length]);

  return (
    <div className="flex h-full w-full">
      <section className="min-w-0 flex-1">
        <div className="px-6 py-8">
          <div className="h-[60vh] max-h-[640px] min-h-[360px]">
            <div className="h-full overflow-y-auto overscroll-contain pr-2 [scrollbar-gutter:stable]">
              <motion.div layout className="flex flex-col gap-4">
                <AnimatePresence
                  initial={false}
                  onExitComplete={() => {
                    if (becameEmptyRef.current) {
                      becameEmptyRef.current = false;
                      onEmptyAfterExit?.();
                    }
                  }}
                >
                  {list.map(p => (
                    <motion.div
                      key={p.projectId}
                      layout="position"
                      initial={{ opacity: 0, y: 8, scale: 0.99 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: -10, scale: 0.98 }}
                      transition={{
                        duration: 0.18,
                        ease: 'easeOut',
                        layout: { duration: 0.18, ease: 'easeOut' },
                      }}
                      role="button"
                      tabIndex={0}
                      onClick={() => onSelect?.(p.projectId)}
                      onKeyDown={e => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          onSelect?.(p.projectId);
                        }
                      }}
                      className="focus:ring-ring/40 cursor-pointer rounded-xl bg-white px-5 py-4 shadow-sm transition [will-change:transform,opacity] hover:shadow-md focus:ring-2 focus:outline-none"
                    >
                      <div className="flex items-center justify-between gap-4">
                        <div className="min-w-0">
                          <p className="text-foreground truncate font-semibold">
                            {p.projectName}
                          </p>
                          <p className="text-muted-foreground text-sm">
                            멤버 {p.memberCount}명{DOT}로그{' '}
                            {formatK(p.logCount)}건 멤버 {p.memberCount}명{DOT}
                            로그 {formatK(p.logCount)}건
                          </p>
                        </div>

                        <div className="flex items-center gap-2">
                          {/* TODO : 연결상태 확인할 수 있는지 체크 후 조건부 렌더링 추가 */}
                          {/* Jira 연결 버튼 */}
                          <Button
                            variant="outline"
                            className="gap-2"
                            onClick={e => {
                              e.stopPropagation();
                              setJiraProjectId(p.projectId);
                            }}
                          >
                            <Link2 className="h-4 w-4" />
                            Jira 연결
                          </Button>

                          {/* 멤버 초대 */}
                          <Button
                            variant="secondary"
                            className="gap-2"
                            onClick={e => {
                              e.stopPropagation();
                              setInvitingProjectId(p.projectId);
                            }}
                          >
                            <UserPlus2 className="h-4 w-4" />
                            초대
                          </Button>

                          {/* 프로젝트 삭제 */}
                          <Button
                            asChild
                            className="gap-2 bg-[#ff6347] text-white hover:bg-[#ff6347]/90"
                          >
                            <motion.button
                              whileTap={{ scale: 0.96 }}
                              aria-label={`${p.projectName} 프로젝트 삭제`}
                              onClick={e => {
                                e.stopPropagation();
                                onDelete?.(p.projectId); // 부모가 AlertDialog로 확인/삭제 처리
                              }}
                            >
                              <Trash2 className="h-4 w-4" />
                              프로젝트 삭제
                            </motion.button>
                          </Button>
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>
              </motion.div>
            </div>
          </div>
        </div>
      </section>

      {/* 멤버 초대 모달 */}

      {invitingProjectId !== null && (
        <MemberInviteModal
          open={true}
          onOpenChange={() => setInvitingProjectId(null)}
          projectId={invitingProjectId}
        />
      )}

      {/* Jira 연동 모달 */}
      {jiraProjectId !== null && (
        <JiraIntegrationModal
          open={true}
          onOpenChange={() => setJiraProjectId(null)}
          projectId={jiraProjectId}
        />
      )}

      {/* Jira 연동 모달 */}
      {jiraProjectId !== null && (
        <JiraIntegrationModal
          open={true}
          onOpenChange={() => setJiraProjectId(null)}
          projectId={jiraProjectId}
        />
      )}
    </div>
  );
};

export default WithProject;
