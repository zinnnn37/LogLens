import { useEffect, useState, useRef, useCallback, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { toast } from 'sonner';
import { Trash2, Loader2 } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';

import {
  getProjectDetail,
  inviteMember,
  deleteMember,
} from '@/services/projectService';
import { searchUsers } from '@/services/userService';
import { useProjectStore } from '@/stores/projectStore';
import { useAuthStore } from '@/stores/authStore';
import type { ProjectMember } from '@/types/project';
import type { UserSearchResult } from '@/types/user';
import { ApiError } from '@/types/api';

interface MemberInviteModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectUuid: string;
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
  projectUuid,
}: MemberInviteModalProps) => {
  const currentProject = useProjectStore(state => state.currentProject);
  const projectName = currentProject?.projectName;
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

  const [userToConfirmInvite, setUserToConfirmInvite] =
    useState<UserSearchResult | null>(null);
  const [memberToConfirmDelete, setMemberToConfirmDelete] =
    useState<ProjectMember | null>(null);

  // 현재 로그인 사용자 id
  const myUserId = useAuthStore(s => s.user?.userId ?? null);

  // 현재 프로젝트 내 멤버
  const loadCurrentMembers = useCallback(async () => {
    if (currentProject?.projectUuid !== projectUuid) {
      setIsLoadingMembers(true);
      try {
        await getProjectDetail(projectUuid);
      } catch (error) {
        console.error('멤버 목록 로드 실패', error);
      } finally {
        setIsLoadingMembers(false);
      }
    }
  }, [projectUuid, currentProject?.projectUuid]);

  useEffect(() => {
    if (open) {
      loadCurrentMembers();
      setSearchText('');
      setSearchResults([]);
      setIsSearching(false);
      setInvitingId(null);
      setRemovingId(null);
      setUserToConfirmInvite(null);
      setMemberToConfirmDelete(null);
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

  // 멤버 초대 dialog
  const handleInviteRequest = (userToInvite: UserSearchResult) => {
    setUserToConfirmInvite(userToInvite);
  };

  // 멤버 초대 함수 호출
  const executeInvite = async () => {
    if (!userToConfirmInvite) {
      return;
    }

    setInvitingId(userToConfirmInvite.userId);
    try {
      await inviteMember(projectUuid, { userId: userToConfirmInvite.userId });
      await loadCurrentMembers();
      toast.success(`'${userToConfirmInvite.name}' 님을 초대했습니다.`);
    } catch (err) {
      console.error('멤버 초대 실패', err);
      if (err instanceof ApiError && err.response) {
        toast.error(err.response.message);
      } else {
        toast.error('멤버 초대에 실패했습니다.');
      }
    } finally {
      setInvitingId(null);
      setUserToConfirmInvite(null);
    }
  };

  // 멤버 삭제 dialog
  const handleRemoveRequest = (memberToRemove: ProjectMember) => {
    setMemberToConfirmDelete(memberToRemove);
  };

  // 삭제 API 호출
  const executeRemove = async () => {
    if (!memberToConfirmDelete) {
      return;
    }

    setRemovingId(memberToConfirmDelete.userId);
    try {
      await deleteMember({
        projectUuid: projectUuid,
        memberId: memberToConfirmDelete.userId,
      });

      await loadCurrentMembers();

      toast.success(`'${memberToConfirmDelete.name}' 님을 삭제했습니다.`);
    } catch (err) {
      console.error('멤버 삭제 실패', err);
      if (err instanceof ApiError && err.response) {
        if (err.response.code === 'PJ403-4') {
          toast.error('자기 자신은 삭제할 수 없습니다.');
        } else {
          toast.error(err.response.message);
        }
      } else {
        toast.error('멤버 삭제에 실패했습니다.');
      }
    } finally {
      setRemovingId(null);
      setMemberToConfirmDelete(null);
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
              <Loader2 className="text-muted-foreground absolute top-3 right-3 h-6 w-6 animate-spin" />
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
                  <h3 className="text-muted-foreground mb-3 px-2 text-sm font-semibold">
                    검색 결과
                  </h3>
                  <div className="space-y-3">
                    <AnimatePresence initial={false}>
                      {searchResults.map(user => {
                        const isMe =
                          myUserId != null && user.userId === myUserId;
                        return (
                          <motion.div
                            {...motionProps}
                            key={user.userId}
                            className="bg-muted/50 rounded-2xl px-6 py-2"
                          >
                            {/* 이름 / 이메일 / 액션(초대) - 액션 열 고정폭 */}
                            <div className="grid w-full grid-cols-[minmax(0,1fr)_minmax(0,1fr)_72px] items-center gap-6">
                              <p className="truncate font-semibold">
                                {user.name}
                                {isMe ? ' (나)' : ''}
                              </p>
                              <p className="text-muted-foreground truncate">
                                {user.email}
                              </p>
                              {/* 액션: 없을 때도 동일 폭 확보 */}
                              {!isMe ? (
                                <div className="flex justify-end">
                                  <Button
                                    variant="default"
                                    size="sm"
                                    className="rounded-lg px-4"
                                    onClick={() => handleInviteRequest(user)}
                                    disabled={invitingId === user.userId}
                                  >
                                    {invitingId === user.userId ? (
                                      <Loader2 className="h-4 w-4 animate-spin" />
                                    ) : (
                                      '초대'
                                    )}
                                  </Button>
                                </div>
                              ) : (
                                <div aria-hidden className="h-8 w-[72px]" />
                              )}
                            </div>
                          </motion.div>
                        );
                      })}
                      {!isSearching && searchResults.length === 0 && (
                        <p className="text-muted-foreground py-4 text-center text-sm">
                          검색 결과가 없습니다.
                        </p>
                      )}
                    </AnimatePresence>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>

            {/* 현재 멤버 */}
            <h3 className="text-muted-foreground mb-3 px-2 text-sm font-semibold">
              현재 멤버 ({currentMembers.length}명)
            </h3>
            <div className="space-y-3">
              <AnimatePresence initial={false}>
                {isLoadingMembers && (
                  <div className="flex justify-center py-8">
                    <Loader2 className="text-muted-foreground h-8 w-8 animate-spin" />
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
                  currentMembers.map(m => {
                    const isMe = myUserId != null && m.userId === myUserId;
                    return (
                      <motion.div
                        {...motionProps}
                        key={m.userId}
                        className="rounded-2xl bg-[#D5E3F2]/40 px-6 py-2"
                      >
                        {/* 이름 / 이메일 / 액션(삭제) - 액션 열 고정폭 */}
                        <div className="grid w-full grid-cols-[minmax(0,1fr)_minmax(0,1fr)_40px] items-center gap-6">
                          <p className="truncate font-semibold">
                            {m.name}
                            {isMe ? ' (나)' : ''}
                          </p>
                          <p className="text-muted-foreground truncate">
                            {m.email}
                          </p>
                          {!isMe ? (
                            <div className="flex justify-end">
                              <Button
                                variant="ghost"
                                size="icon"
                                className="ml-2"
                                onClick={() => handleRemoveRequest(m)}
                                aria-label={`${m.name} 삭제`}
                                disabled={removingId === m.userId}
                              >
                                {removingId === m.userId ? (
                                  <Loader2 className="h-5 w-5 animate-spin" />
                                ) : (
                                  <Trash2 className="h-5 w-5" />
                                )}
                              </Button>
                            </div>
                          ) : (
                            <div aria-hidden className="h-9 w-10" />
                          )}
                        </div>
                      </motion.div>
                    );
                  })}
              </AnimatePresence>
            </div>
          </div>
        </div>

        <AlertDialog
          open={userToConfirmInvite !== null}
          onOpenChange={open => {
            if (!open) {
              setUserToConfirmInvite(null);
            }
          }}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>멤버 초대</AlertDialogTitle>
              <AlertDialogDescription>
                {userToConfirmInvite?.name} ({userToConfirmInvite?.email}) 님을
                <br />
                {projectName} 프로젝트 멤버로 초대하시겠습니까?
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction onClick={executeInvite}>
                초대
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>

        <AlertDialog
          open={memberToConfirmDelete !== null}
          onOpenChange={open => {
            if (!open) {
              setMemberToConfirmDelete(null);
            }
          }}
        >
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>멤버 삭제</AlertDialogTitle>
              <AlertDialogDescription>
                {memberToConfirmDelete?.name} ({memberToConfirmDelete?.email})
                님을
                <br />
                {projectName} 프로젝트에서 탈퇴시키겠습니까?
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>취소</AlertDialogCancel>
              <AlertDialogAction
                onClick={executeRemove}
                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              >
                삭제
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </DialogContent>
    </Dialog>
  );
};

export default MemberInviteModal;
