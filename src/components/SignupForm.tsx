import { cn } from '@/lib/utils';
import {
  validateEmail,
  validatePassword,
  validatePasswordMatch,
  validateName,
} from '@/lib/authValidation';
import { useFormValidation } from '@/hooks/useFormValidation';
import { Button } from '@/components/ui/button';
import { User, Mail, Lock } from 'lucide-react';
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
  FieldSeparator,
} from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { ROUTE_PATH } from '@/router/route-path';

export const SignupForm = ({
  className,
  ...props
}: React.ComponentProps<'form'>) => {
  const { values, errors, touched, isValid, setFieldValue, setFieldTouched } =
    useFormValidation(
      {
        'name': '',
        'email': '',
        'password': '',
        'confirm-password': '',
      },
      {
        'name': validateName,
        'email': validateEmail,
        'password': validatePassword,
        'confirm-password': value =>
          validatePasswordMatch(values.password, value),
      },
    );

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (isValid) {
      console.log('Form submitted:', values);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFieldValue(e.target.id, e.target.value);
  };

  const handleBlur = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFieldTouched(e.target.id);
  };

  return (
    <form
      className={cn('flex flex-col', className)}
      onSubmit={handleSubmit}
      {...props}
    >
      <FieldGroup className="gap-4 font-[YiSunShin]">
        <div className="flex flex-col items-center gap-1 text-center">
          <h1 className="text-2xl font-bold">회원가입</h1>
          <p className="text-muted-foreground mt-2 mb-5 text-sm text-balance">
            로그인해서 프로젝트를 관리해보세요
          </p>
        </div>
        <Field>
          <FieldLabel htmlFor="name">
            <User size={18} />
            이름
            {touched.name && errors.name && (
              <p className="text-destructive text-xs">{errors.name}</p>
            )}
          </FieldLabel>
          <Input
            id="name"
            type="text"
            placeholder="김싸피"
            value={values.name}
            onChange={handleChange}
            onBlur={handleBlur}
            className="rounded-[15px]"
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="email">
            <Mail size={18} />
            이메일
            {touched.email && errors.email && (
              <p className="text-destructive text-xs">{errors.email}</p>
            )}
          </FieldLabel>
          <Input
            id="email"
            type="email"
            placeholder="ssafy@example.com"
            value={values.email}
            onChange={handleChange}
            onBlur={handleBlur}
            className="rounded-[15px]"
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="password">
            <Lock size={18} />
            비밀번호
            {touched.password && errors.password && (
              <p className="text-destructive text-xs">{errors.password}</p>
            )}
          </FieldLabel>
          <Input
            id="password"
            type="password"
            value={values.password}
            onChange={handleChange}
            onBlur={handleBlur}
            className="rounded-[15px]"
          />
        </Field>
        <Field>
          <FieldLabel htmlFor="confirm-password">
            <Lock size={18} />
            비밀번호 확인
            {touched['confirm-password'] && errors['confirm-password'] && (
              <p className="text-destructive text-xs">
                {errors['confirm-password']}
              </p>
            )}
          </FieldLabel>
          <Input
            id="confirm-password"
            type="password"
            value={values['confirm-password']}
            onChange={handleChange}
            onBlur={handleBlur}
            className="rounded-[15px]"
          />
        </Field>
        <Field>
          <Button
            type="submit"
            disabled={!isValid}
            className="bg-secondary mt-3 rounded-[15px] py-5"
          >
            회원가입
          </Button>
        </Field>
        <FieldSeparator></FieldSeparator>
        <Field>
          <FieldDescription className="px-6 text-center text-xs">
            이미 계정이 있으신가요? <a href={ROUTE_PATH.LOGIN}>로그인</a>
          </FieldDescription>
        </Field>
      </FieldGroup>
    </form>
  );
};
