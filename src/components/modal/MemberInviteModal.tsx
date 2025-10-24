// src/components/modal/MemberInviteModal.tsx
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
  { id: '2', name: '이종현', email: 'example1@gmail.com' },
  { id: '3', name: '이종현', email: 'example1@gmail.com' },
];

const MemberInviteModal = ({ open, onOpenChange }: MemberInviteModalProps) => {
  const seed = useMemo(() => DUMMY_MEMBERS, []);
  const [members, setMembers] = useState<Member[]>(seed);
  const [email, setEmail] = useState('');

  useEffect(() => {
    if (open) {
      setMembers(seed);
      setEmail('');
    }
  }, [open, seed]);

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
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle className="text-xl">프로젝트 멤버</DialogTitle>
        </DialogHeader>

        {/* 멤버 리스트 */}
        <div className="mt-3 space-y-3 mx-10">
          {members.map(m => (
            <div
              key={m.id}
              className="flex items-center justify-between rounded-2xl bg-[#D5E3F2]/40 px-6 py-2"
            >
              <div className="flex min-w-0 flex-1 items-center justify-between gap-6">
                <p className="truncate font-semibold">{m.name}</p>
                <p className="text-muted-foreground truncate">{m.email}</p>
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
            </div>
          ))}
        </div>

        {/* 초대 */}
        <div className="mt-6 mx-10 flex items-center gap-3">
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
      </DialogContent>
    </Dialog>
  );
};

export default MemberInviteModal;
