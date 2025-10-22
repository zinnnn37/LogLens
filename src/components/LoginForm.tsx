import { useState } from 'react';
import { cn } from '@/lib/utils';
import { Mail, Lock, Eye, EyeOff } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { ROUTE_PATH } from '@/router/route-path';

export const LoginForm = ({
  className,
  ...props
}: React.ComponentProps<'form'>) => {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <form className={cn('flex flex-col', className)} {...props}>
      <FieldGroup className="font-yisunsin gap-4">
        <div className="flex flex-col items-center gap-1 text-center">
          <h1 className="font-pretendard text-2xl font-bold">로그인</h1>
          <p className="text-muted-foreground mt-2 mb-5 text-sm text-balance">
            로그인해서 프로젝트를 관리해보세요
          </p>
        </div>
        <Field>
          <FieldLabel htmlFor="email">
            <Mail size={18} /> 이메일
          </FieldLabel>
          <Input
            id="email"
            type="email"
            placeholder="ssafy@example.com"
            required
            className="rounded-[15px]"
          />
        </Field>
        <Field>
          <div className="flex items-center">
            <FieldLabel htmlFor="password">
              <Lock size={18} />
              비밀번호
            </FieldLabel>
          </div>
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              required
              className="rounded-[15px] pr-10"
            />
            <button
              type="button"
              onMouseDown={() => setShowPassword(true)}
              onMouseUp={() => setShowPassword(false)}
              onMouseLeave={() => setShowPassword(false)}
              onTouchStart={() => setShowPassword(true)}
              onTouchEnd={() => setShowPassword(false)}
              className="text-muted-foreground hover:text-foreground absolute top-1/2 right-3 -translate-y-1/2 transition-colors"
            >
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </Field>
        <Field>
          <Button
            type="submit"
            className="bg-secondary mt-3 rounded-[15px] py-5"
          >
            로그인
          </Button>
        </Field>
        <FieldSeparator></FieldSeparator>
        <Field>
          <FieldDescription className="text-center text-xs">
            계정이 없으신가요?{' '}
            <a
              href={ROUTE_PATH.SIGNUP}
              className="underline underline-offset-4"
            >
              회원가입
            </a>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </form>
  );
};
