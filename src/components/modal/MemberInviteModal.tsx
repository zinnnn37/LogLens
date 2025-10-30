import { useEffect, useState, useRef, useCallback, useMemo } from 'react';
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

import {
  getProjectDetail,
  inviteMember,
  deleteMember,
} from '@/services/projectService';
import { searchUsers } from '@/services/userService';
import { useProjectStore } from '@/stores/projectStore';
import type { ProjectMember } from '@/types/project';
import type { UserSearchResult } from '@/types/user';
import { ApiError } from '@/types/api';

interface MemberInviteModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: number;
}

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
  const currentProject = useProjectStore(state => state.currentProject);
  const currentMembers = useMemo(
    () => currentProject?.members ?? [],
    [currentProject?.members],
  );
  const [isLoadingMembers, setIsLoadingMembers] = useState(false);

  const [searchText, setSearchText] = useState('');
  const [searchResults, setSearchResults] = useState<UserSearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const debounceTimeout = useRef<NodeJS.Timeout | null>(null);

  const [invitingId, setInvitingId] = useState<number | null>(null);
  const [removingId, setRemovingId] = useState<number | null>(null);

  // 현재 프로젝트 내 멤버
  const loadCurrentMembers = useCallback(async () => {
    if (currentProject?.projectId !== projectId) {
      setIsLoadingMembers(true);
      try {
        await getProjectDetail(projectId);
      } catch (error) {
        console.error('멤버 목록 로드 실패', error);
      } finally {
        setIsLoadingMembers(false);
      }
    }
  }, [projectId, currentProject?.projectId]);

  useEffect(() => {
    if (open) {
      loadCurrentMembers();
      setSearchText('');
      setSearchResults([]);
      setIsSearching(false);
      setInvitingId(null);
      setRemovingId(null);
    } else {
      useProjectStore.getState().setCurrentProject(null);
    }
  }, [open, loadCurrentMembers]);

  useEffect(() => {
    if (debounceTimeout.current) {
      clearTimeout(debounceTimeout.current);
    }
    const trimmedSearch = searchText.trim();
    if (trimmedSearch.length < 2) {
      setSearchResults([]);
      setIsSearching(false);
      return;
    }
    setIsSearching(true);
    debounceTimeout.current = setTimeout(async () => {
      try {
        // searchUsers 서비스 함수 호출
        const response = await searchUsers({ name: trimmedSearch, size: 10 });

        // 현재 포함되어 있는 멤버는 제외
        const currentMemberEmails = new Set(currentMembers.map(m => m.email));
        const finalResults = response.content.filter(
          user => !currentMemberEmails.has(user.email),
        );

        setSearchResults(finalResults);
      } catch (error) {
        console.error('멤버 검색 실패', error);
      } finally {
        setIsSearching(false);
      }
    }, 500);

    return () => {
      if (debounceTimeout.current) {
        clearTimeout(debounceTimeout.current);
      }
    };
  }, [searchText, currentMembers]);

  // 멤버 초대
  const handleInvite = async (userToInvite: UserSearchResult) => {
    setInvitingId(userToInvite.userId);
    try {
      await inviteMember(projectId, { userId: userToInvite.userId });
      await loadCurrentMembers();
    } catch (err) {
      console.error('멤버 초대 실패', err);
      if (err instanceof ApiError && err.response) {
        // 에러문구 출력?
      }
    } finally {
      setInvitingId(null);
    }
  };

  // 멤버삭제 API
  const handleRemove = async (memberToRemove: ProjectMember) => {
    const ok = window.confirm(
      `'${memberToRemove.name}' 님을 프로젝트에서 삭제하시겠습니까?`,
    );
    if (!ok) { return; }

    setRemovingId(memberToRemove.userId);
    try {
      await deleteMember({
        projectId: projectId,
        memberId: memberToRemove.userId,
      });

      // TODO : 삭제되었다고 알려주기

    } catch (err) {
      console.error('멤버 삭제 실패', err);
      if (err instanceof ApiError && err.response) {
        // TODO : 삭제실패했다고 알려주기
        if (err.response.code === 'PJ403-4') {
          // 자기자신을 삭제할순없다
        }
      }
    } finally {
      setRemovingId(null);
    }
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
                            disabled={invitingId === user.userId}
                          >
                            {invitingId === user.userId ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              '초대'
                            )}
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
                        onClick={() => handleRemove(m)}
                        aria-label={`${m.name} 삭제`}
                        disabled={removingId === m.userId}
                      >
                        {removingId === m.userId ? (
                          <Loader2 className="h-5 w-5 animate-spin" />
                        ) : (
                          <Trash2 className="h-5 w-5" />
                        )}
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