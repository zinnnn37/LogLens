import { Button } from '@/components/ui/button';
import { UserPlus2, Trash2 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import MemberInviteModal from './modal/MemberInviteModal';
import { AnimatePresence, motion } from 'framer-motion';

export interface Project {
  id: string;
  name: string;
  memberCount: number;
  todayLogCount: number;
}

export interface WithProjectProps {
  projects?: Project[];
  onSelect?: (id: string) => void;
  onDelete?: (id: string) => Promise<void> | void;
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

  const [openInvite, setOpenInvite] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const becameEmptyRef = useRef(false);
  const prevLenRef = useRef(list.length);
  useEffect(() => {
    const prev = prevLenRef.current;
    const curr = list.length;
    becameEmptyRef.current = prev > 0 && curr === 0;
    prevLenRef.current = curr;
  }, [list.length]);

  const handleDelete = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    const ok = window.confirm('정말 이 프로젝트를 삭제하시겠습니까?');
    if (!ok) {
      return;
    }
    try {
      setDeletingId(id);
      await onDelete?.(id);
    } finally {
      setDeletingId(prev => (prev === id ? null : prev));
    }
  };

  return (
    <div className="flex h-full w-full">
      <section className="min-w-0 flex-1">
        <div className="px-6 py-8">
          {/* 멤버 초대 처럼 일정 영역 할당하고 그 안에서 내부 스크롤 생성 */}
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
                      key={p.id}
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
                      onClick={() => onSelect?.(p.id)}
                      onKeyDown={e => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          onSelect?.(p.id);
                        }
                      }}
                      className="focus:ring-ring/40 cursor-pointer rounded-xl bg-white px-5 py-4 shadow-sm transition [will-change:transform,opacity] hover:shadow-md focus:ring-2 focus:outline-none"
                    >
                      <div className="flex items-center justify-between gap-4">
                        <div className="min-w-0">
                          <p className="text-foreground truncate font-semibold">
                            {p.name}
                          </p>
                          <p className="text-muted-foreground text-sm">
                            멤버 {p.memberCount}명{DOT}오늘 로그{' '}
                            {formatK(p.todayLogCount)}건
                          </p>
                        </div>

                        <div className="flex items-center gap-2">
                          <Button
                            variant="secondary"
                            className="gap-2"
                            onClick={e => {
                              e.stopPropagation();
                              setOpenInvite(true);
                            }}
                          >
                            <UserPlus2 className="h-4 w-4" />
                            초대
                          </Button>

                          <Button
                            asChild
                            className="gap-2 bg-[#ff6347] text-white hover:bg-[#ff6347]/90 disabled:opacity-60"
                          >
                            <motion.button
                              whileTap={{ scale: 0.96 }}
                              aria-label={`${p.name} 프로젝트 삭제`}
                              disabled={deletingId === p.id}
                              onClick={e => handleDelete(e, p.id)}
                            >
                              <Trash2 className="h-4 w-4" />
                              {deletingId === p.id
                                ? '삭제 중…'
                                : '프로젝트 삭제'}
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

      <MemberInviteModal open={openInvite} onOpenChange={setOpenInvite} />
    </div>
  );
};

export default WithProject;
