// src/components/WithProject.tsx
import { Button } from '@/components/ui/button';
import { UserPlus2, Trash2 } from 'lucide-react';
import { useState } from 'react';
import MemberInviteModal from './modal/MemberInviteModal';

export interface Project {
  id: string;
  name: string;
  memberCount: number;
  todayLogCount: number;
}

export interface WithProjectProps {
  projects?: Project[];

  // 해당 프로젝트 상세 페이지로 이동
  onSelect?: (id: string) => void;
}

// TODO : 추후 실제 프로젝트 조회 API 로 대체
const DUMMY_PROJECTS: Project[] = [
  { id: 'p1', name: '자율 프로젝트', memberCount: 2, todayLogCount: 1200 },
  { id: 'p2', name: '공통 프로젝트', memberCount: 2, todayLogCount: 1200 },
  { id: 'p3', name: '개인 프로젝트', memberCount: 2, todayLogCount: 1200 },
];

// kilo 아래로 자르기
const formatK = (n: number) => {
  if (n < 1000) {
    return `${n}`;
  }
  const k = n / 1000;
  return `${Number.isInteger(k) ? k.toFixed(0) : k.toFixed(1)}K`;
};

const DOT = ' • ';

const WithProject = ({ projects, onSelect }: WithProjectProps) => {
  const list = projects && projects.length > 0 ? projects : DUMMY_PROJECTS;

  const [openInvite, setOpenInvite] = useState(false);

  return (
    <div className="flex h-full w-full">
      {/* 본문 */}
      <section className="min-w-0 flex-1">
        {/* 프로젝트 리스트 */}
        <div className="px-6 py-8">
          <div className="space-y-4">
            {list.map(p => (
              <div
                key={p.id}
                role="button"
                tabIndex={0}
                onClick={() => onSelect?.(p.id)}
                onKeyDown={e => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    onSelect?.(p.id);
                  }
                }}
                className="focus:ring-ring/40 cursor-pointer rounded-xl bg-white px-5 py-4 shadow-sm transition hover:shadow-md focus:ring-2 focus:outline-none"
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
                      variant="destructive"
                      className="gap-2"
                      onClick={e => {
                        e.stopPropagation();
                      }}
                    >
                      <Trash2 className="h-4 w-4" />
                      프로젝트 삭제
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>
      <MemberInviteModal open={openInvite} onOpenChange={setOpenInvite} />
    </div>
  );
};

export default WithProject;
