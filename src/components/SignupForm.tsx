import { useState } from 'react';
import { cn } from '@/lib/utils';
import {
  validateEmail,
  validatePassword,
  validatePasswordMatch,
  validateName,
} from '@/lib/authValidation';
import { useFormValidation } from '@/hooks/useFormValidation';
import { Button } from '@/components/ui/button';
import { User, Mail, Lock, Eye, EyeOff } from 'lucide-react';
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
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

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
    const fieldId = e.target.id;
    setFieldValue(fieldId, e.target.value);
    // 이미 터치된 필드이거나, 값이 있는 경우 즉시 유효성 검사
    if (touched[fieldId] || e.target.value) {
      setFieldTouched(fieldId);
    }
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
      <FieldGroup className="font-yisunsin gap-4">
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
            className={cn(
              'rounded-[15px]',
              touched.name && errors.name && 'bg-destructive/10',
            )}
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
            className={cn(
              'rounded-[15px]',
              touched.email && errors.email && 'bg-destructive/10',
            )}
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
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              value={values.password}
              onChange={handleChange}
              onBlur={handleBlur}
              className={cn(
                'rounded-[15px] pr-10',
                touched.password && errors.password && 'bg-destructive/10',
              )}
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
          <FieldLabel htmlFor="confirm-password">
            <Lock size={18} />
            비밀번호 확인
            {touched['confirm-password'] && errors['confirm-password'] && (
              <p className="text-destructive text-xs">
                {errors['confirm-password']}
              </p>
            )}
          </FieldLabel>
          <div className="relative">
            <Input
              id="confirm-password"
              type={showConfirmPassword ? 'text' : 'password'}
              value={values['confirm-password']}
              onChange={handleChange}
              onBlur={handleBlur}
              className={cn(
                'rounded-[15px] pr-10',
                touched['confirm-password'] &&
                  errors['confirm-password'] &&
                  'bg-destructive/10',
              )}
            />
            <button
              type="button"
              onMouseDown={() => setShowConfirmPassword(true)}
              onMouseUp={() => setShowConfirmPassword(false)}
              onMouseLeave={() => setShowConfirmPassword(false)}
              onTouchStart={() => setShowConfirmPassword(true)}
              onTouchEnd={() => setShowConfirmPassword(false)}
              className="text-muted-foreground hover:text-foreground absolute top-1/2 right-3 -translate-y-1/2 transition-colors"
            >
              {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
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
