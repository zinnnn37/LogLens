// src/components/WithProject.tsx
import { Button } from '@/components/ui/button';
import { UserPlus2, Trash2, Link2, Link } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import MemberInviteModal from './modal/MemberInviteModal';
import { AnimatePresence, motion } from 'framer-motion';
import { JiraIntegrationModal } from '@/components/modal/JiraIntegrationModal';
import type { ProjectInfoDTO } from '@/types/project';

export interface WithProjectProps {
  projects?: ProjectInfoDTO[];
  onSelect?: (projectUuid: string) => void;
  onDelete?: (projectUuid: string) => void;
  onEmptyAfterExit?: () => void;
}

const WithProject = ({
  projects,
  onSelect,
  onDelete,
  onEmptyAfterExit,
}: WithProjectProps) => {
  const list = projects ?? [];

  const [invitingProjectId, setInvitingProjectId] = useState<string | null>(
    null,
  );
  const [jiraProjectId, setJiraProjectId] = useState<string | null>(null);

  const becameEmptyRef = useRef(false);
  const prevLenRef = useRef(list.length);

  useEffect(() => {
    const prev = prevLenRef.current;
    const curr = list.length;
    becameEmptyRef.current = prev > 0 && curr === 0;
    prevLenRef.current = curr;
  }, [list.length]);

  return (
    <div className="flex h-full w-full flex-col">
      <section className="min-w-0 flex-1">
        <div className="space-y-6 px-6 py-1">
          <h1 className="font-godoM text-lg">프로젝트 목록</h1>
          <div className="">
            <div className="">
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
                      key={p.projectUuid}
                      layout
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{
                        opacity: 0,
                        x: 50,
                        transition: { duration: 0.25, ease: 'easeIn' },
                      }}
                      transition={{
                        layout: {
                          duration: 0.25,
                          ease: [0.4, 0, 0.2, 1],
                        },
                      }}
                      role="button"
                      tabIndex={0}
                      onClick={() => onSelect?.(p.projectUuid)}
                      onKeyDown={e => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          onSelect?.(p.projectUuid);
                        }
                      }}
                      className="focus:ring-ring/40 cursor-pointer rounded-xl bg-white px-5 py-4 shadow-sm transition [will-change:transform,opacity] hover:shadow-md focus:ring-2 focus:outline-none"
                    >
                      <div className="flex items-center justify-between gap-4">
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <p className="text-foreground truncate font-semibold">
                              {p.projectName}
                            </p>
                            {p.jiraConnectionExist && (
                              <Link
                                className="h-4 w-4 shrink-0 text-blue-600"
                                aria-label="Jira 연결됨"
                              />
                            )}
                          </div>
                          <p className="text-muted-foreground text-sm">
                            멤버 {p.memberCount}명
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
                              setJiraProjectId(p.projectUuid);
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
                              setInvitingProjectId(p.projectUuid);
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
                                onDelete?.(p.projectUuid);
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
          projectUuid={invitingProjectId}
        />
      )}

      {/* Jira 연동 모달 */}
      {jiraProjectId !== null && (
        <JiraIntegrationModal
          open={true}
          onOpenChange={() => setJiraProjectId(null)}
          projectUuid={jiraProjectId}
        />
      )}
    </div>
  );
};

export default WithProject;
