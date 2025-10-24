import { useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Trash2 } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';

interface Member {
  id: string;
  name: string;
  email: string;
}

interface MemberInviteModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

// TODO : 현재는 더미멤버, 추후 API 연결
const DUMMY_MEMBERS: Member[] = [
  { id: '1', name: '이종현', email: 'example1@gmail.com' },
  { id: '2', name: '이종현', email: 'example2@gmail.com' },
  { id: '3', name: '이종현', email: 'example3@gmail.com' },
  { id: '4', name: '이종현', email: 'example4@gmail.com' },
  { id: '5', name: '이종현', email: 'example5@gmail.com' },
  { id: '6', name: '이종현', email: 'example6@gmail.com' },
  { id: '7', name: '이종현', email: 'example7@gmail.com' },
  { id: '8', name: '이종현', email: 'example8@gmail.com' },
];

const MemberInviteModal = ({ open, onOpenChange }: MemberInviteModalProps) => {
  const seed = useMemo(() => DUMMY_MEMBERS, []);
  const [members, setMembers] = useState<Member[]>(seed);
  const [email, setEmail] = useState('');
  const [showEmpty, setShowEmpty] = useState(false); // ✅ 빈상태 문구 노출 시점 제어

  useEffect(() => {
    if (open) {
      setMembers(seed);
      setEmail('');
      setShowEmpty(seed.length === 0);
    }
  }, [open, seed]);

  useEffect(() => {
    if (members.length > 0) {
      setShowEmpty(false);
    }
  }, [members.length]);

  const handleInvite = () => {
    const trimmed = email.trim();
    if (!trimmed) {
      return;
    }
    setMembers(prev => [
      ...prev,
      {
        id: `m_${Date.now()}`,
        name: trimmed.split('@')[0] || '신규멤버',
        email: trimmed,
      },
    ]);
    setEmail('');
  };

  const handleRemove = (id: string) => {
    setMembers(prev => prev.filter(m => m.id !== id));
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {/* ✨ DialogContent의 round는 여기서 제어됨 */}
      <DialogContent className="h-[450px] w-[600px] rounded-2xl sm:max-w-2xl">
        <div className="flex h-full flex-col gap-4 overflow-hidden">
          <DialogHeader className="shrink-0">
            <DialogTitle className="text-xl">프로젝트 멤버</DialogTitle>
          </DialogHeader>

          {/* 스크롤 영역 */}
          <div className="flex-1 overflow-y-auto overscroll-contain pr-4 [scrollbar-gutter:stable]">
            <div className="space-y-3">
              <AnimatePresence
                initial={false}
                onExitComplete={() => {
                  if (members.length === 0) {
                    setShowEmpty(true);
                  }
                }}
              >
                {members.map(m => (
                  <motion.div
                    key={m.id}
                    layout
                    initial={{
                      opacity: 0,
                      y: 8,
                      scale: 0.98,
                      filter: 'blur(2px)',
                    }}
                    animate={{
                      opacity: 1,
                      y: 0,
                      scale: 1,
                      filter: 'blur(0px)',
                    }}
                    exit={{
                      opacity: 0,
                      y: -10,
                      scale: 0.97,
                      filter: 'blur(2px)',
                    }}
                    transition={{ duration: 0.18, ease: 'easeOut' }}
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
                      onClick={() => handleRemove(m.id)}
                      aria-label={`${m.name} 삭제`}
                    >
                      <Trash2 className="h-5 w-5" />
                    </Button>
                  </motion.div>
                ))}
              </AnimatePresence>

              {showEmpty && (
                <motion.div
                  key="empty"
                  initial={{ opacity: 0, scale: 0.98, y: 4 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  transition={{ duration: 0.25, ease: 'easeOut' }}
                  className="text-muted-foreground px-2 py-8 text-center text-sm select-none"
                >
                  멤버가 없습니다.
                </motion.div>
              )}
            </div>
          </div>

          {/* 하단 입력 영역 */}
          <div className="mt-6 flex shrink-0 items-center gap-3">
            <Input
              type="email"
              placeholder="초대할 이메일을 입력해주세요"
              value={email}
              onChange={e => setEmail(e.target.value)}
              onKeyDown={e => {
                if (e.key === 'Enter') {
                  handleInvite();
                }
              }}
              className="h-12 rounded-2xl"
            />
            <Button
              className="h-12 rounded-2xl px-6"
              onClick={handleInvite}
              disabled={!email.trim()}
            >
              초대
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default MemberInviteModal;
