import { useEffect, useState, useRef, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Trash2, Loader2 } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';

// TODO : 실제 API 연결 시 활성화
// import { getProjectDetail, inviteMember } from '@/services/projectService';
// import { searchUsers } from '@/services/userService';
import type { ProjectMember } from '@/types/project';
import type { UserSearchResult } from '@/types/user';
// import { ApiError } from '@/types/api';

interface MemberInviteModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: number;
}

// TODO : 더미 추후 제거
const DUMMY_CURRENT_MEMBERS: ProjectMember[] = [
  { userId: 1, name: '홍길동 ', email: 'hong@example.com', joinedAt: '' },
  { userId: 2, name: '김철수 ', email: 'kim@example.com', joinedAt: '' },
];

const DUMMY_ALL_USERS: UserSearchResult[] = [
  { userId: 1, username: '홍길동', email: 'hong@example.com' },
  { userId: 2, username: '김철수', email: 'kim@example.com' },
  { userId: 3, username: '이영희', email: 'lee@example.com' },
  { userId: 4, username: '박지성', email: 'park@example.com' },
  { userId: 5, username: '이종현', email: 'lee.jong@example.com' },
  { userId: 6, username: '이종헌', email: 'lee.heon@example.com' },
];

const motionProps = {
  initial: { opacity: 0, y: 8, scale: 0.98, filter: 'blur(2px)' },
  animate: { opacity: 1, y: 0, scale: 1, filter: 'blur(0px)' },
  exit: { opacity: 0, y: -10, scale: 0.97, filter: 'blur(2px)' },
  transition: { duration: 0.18, ease: 'easeOut' },
  layout: true,
} as const;

const MemberInviteModal = ({
  open,
  onOpenChange,
  projectId,
}: MemberInviteModalProps) => {
  const [currentMembers, setCurrentMembers] = useState<ProjectMember[]>([]);
  const [isLoadingMembers, setIsLoadingMembers] = useState(false);

  const [searchText, setSearchText] = useState('');
  const [searchResults, setSearchResults] = useState<UserSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const debounceTimeout = useRef<NodeJS.Timeout | null>(null);

  // TODO : 일단 더미 추후 실제로 교체
  const loadCurrentMembers = useCallback(() => {
    setIsLoadingMembers(true);
    setCurrentMembers(DUMMY_CURRENT_MEMBERS);
    setIsLoadingMembers(false);
  }, []);

  useEffect(() => {
    if (open) {
      loadCurrentMembers();
      setSearchText('');
      setSearchResults([]);
      setIsSearching(false);
    }
  }, [open, projectId, loadCurrentMembers]);

  useEffect(() => {
    if (debounceTimeout.current) {
      clearTimeout(debounceTimeout.current);
    }
    const trimmedSearch = searchText.trim().toLowerCase();
    if (trimmedSearch.length === 0) {
      setSearchResults([]);
      setIsSearching(false);
      return;
    }
    setIsSearching(true);
    debounceTimeout.current = setTimeout(() => {
      const filtered = DUMMY_ALL_USERS.filter(
        user =>
          user.username.toLowerCase().includes(trimmedSearch) ||
          user.email.toLowerCase().includes(trimmedSearch),
      );

      // 현재 포함되어 있는 멤버는 제외
      const currentMemberEmails = new Set(currentMembers.map(m => m.email));
      const finalResults = filtered.filter(
        user => !currentMemberEmails.has(user.email),
      );

      setSearchResults(finalResults);
      setIsSearching(false);
    }, 500);

    return () => {
      if (debounceTimeout.current) {
        clearTimeout(debounceTimeout.current);
      }
    };
  }, [searchText, currentMembers]);


  const handleInvite = (userToInvite: UserSearchResult) => {
    // 1. 검색 결과에서 제거
    setSearchResults(prev =>
      prev.filter(user => user.userId !== userToInvite.userId),
    );
    // 2. 현재 멤버 목록에 추가 (타입 변환)
    setCurrentMembers(prev => [
      ...prev,
      {
        userId: userToInvite.userId,
        name: userToInvite.username,
        email: userToInvite.email,
        joinedAt: new Date().toISOString(),
      },
    ]);
  };

  // TODO : 멤버삭제 API 연결
  const handleRemove = (id: number) => {
    setCurrentMembers(prev => prev.filter(m => m.userId !== id));
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="h-[600px] w-[600px] rounded-2xl sm:max-w-2xl">
        <div className="flex h-full flex-col gap-4 overflow-hidden px-4">
          <DialogHeader className="shrink-0">
            <DialogTitle className="text-xl">프로젝트 멤버</DialogTitle>
          </DialogHeader>

          {/* 검색창 */}
          <div className="relative shrink-0">
            <Input
              type="text"
              placeholder="초대할 멤버의 이름을 입력하세요."
              value={searchText}
              onChange={e => setSearchText(e.target.value)}
              className="h-12 rounded-2xl pr-10"
            />
            {isSearching && (
              <Loader2 className="absolute right-3 top-3 h-6 w-6 animate-spin text-muted-foreground" />
            )}
          </div>

          {/* 검색 결과 */}
          <div className="flex-1 overflow-y-auto overscroll-contain pr-4 [scrollbar-gutter:stable]">
            <AnimatePresence>
              {searchText.length > 0 && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="mb-6"
                >
                  <h3 className="mb-3 px-2 text-sm font-semibold text-muted-foreground">
                    검색 결과
                  </h3>
                  <div className="space-y-3">
                    <AnimatePresence initial={false}>
                      {searchResults.map(user => (
                        <motion.div
                          {...motionProps}
                          key={user.userId}
                          className="flex items-center justify-between rounded-2xl bg-muted/50 px-6 py-2"
                        >
                          <div className="flex min-w-0 flex-1 items-center justify-between gap-6">
                            <p className="truncate font-semibold">
                              {user.username}
                            </p>
                            <p className="text-muted-foreground truncate">
                              {user.email}
                            </p>
                          </div>
                          <Button
                            variant="default"
                            size="sm"
                            className="ml-4 rounded-lg px-4"
                            onClick={() => handleInvite(user)}
                          >
                            초대
                          </Button>
                        </motion.div>
                      ))}
                      {!isSearching && searchResults.length === 0 && (
                        <p className="py-4 text-center text-sm text-muted-foreground">
                          검색 결과가 없습니다.
                        </p>
                      )}
                    </AnimatePresence>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* 현재 멤버 */}
            <h3 className="mb-3 px-2 text-sm font-semibold text-muted-foreground">
              현재 멤버 ({currentMembers.length}명)
            </h3>
            <div className="space-y-3">
              <AnimatePresence initial={false}>
                {isLoadingMembers && (
                  <div className="flex justify-center py-8">
                    <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                  </div>
                )}
                {!isLoadingMembers && currentMembers.length === 0 && (
                  <motion.div
                    key="empty"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="text-muted-foreground px-2 py-8 text-center text-sm select-none"
                  >
                    멤버가 없습니다.
                  </motion.div>
                )}
                {!isLoadingMembers &&
                  currentMembers.map(m => (
                    <motion.div
                      {...motionProps}
                      key={m.userId}
                      className="flex items-center justify-between rounded-2xl bg-[#D5E3F2]/40 px-6 py-2"
                    >
                      <div className="flex min-w-0 flex-1 items-center justify-between gap-6">
                        <p className="truncate font-semibold">{m.name}</p>
                        <p className="text-muted-foreground truncate">
                          {m.email}
                        </p>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="ml-2"
                        onClick={() => handleRemove(m.userId)}
                        aria-label={`${m.name} 삭제`}
                      >
                        <Trash2 className="h-5 w-5" />
                      </Button>
                    </motion.div>
                  ))}
              </AnimatePresence>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default MemberInviteModal;