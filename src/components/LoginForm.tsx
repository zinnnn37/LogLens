import { cn } from '@/lib/utils';
import { Mail, Lock } from 'lucide-react';
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
  return (
    <form className={cn('flex flex-col', className)} {...props}>
      <FieldGroup className="gap-4 font-[YiSunShin]">
        <div className="flex flex-col items-center gap-1 text-center">
          <h1 className="text-2xl font-bold">로그인</h1>
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
          <Input
            id="password"
            type="password"
            required
            className="rounded-[15px]"
          />
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
