import { useState } from 'react';
import { checkEmailAvailability } from '@/services/authApi';
import { ApiError } from '@/types/api';

export const useEmailCheck = () => {
  const [isEmailChecking, setIsEmailChecking] = useState(false);
  const [isEmailAvailable, setIsEmailAvailable] = useState<boolean | null>(null);
  const [emailCheckMessage, setEmailCheckMessage] = useState<string | null>(null);

  const checkEmail = async (email: string, emailError?: string) => {
    // 이메일 유효성 검사 먼저 확인
    if (!email || emailError) {
      setEmailCheckMessage('올바른 이메일을 입력해주세요.');
      return;
    }

    setIsEmailChecking(true);
    setEmailCheckMessage(null);

    try {
      const response = await checkEmailAvailability(email);
      setIsEmailAvailable(response.available);

      if (response.available) {
        setEmailCheckMessage('사용 가능한 이메일입니다.');
      } else {
        setEmailCheckMessage('이미 사용 중인 이메일입니다.');
      }
    } catch (error) {
      if (error instanceof ApiError) {
        setEmailCheckMessage(error.response?.message || '이메일 확인에 실패했습니다.');
      } else {
        setEmailCheckMessage('네트워크 오류가 발생했습니다.');
      }
      setIsEmailAvailable(null);
    } finally {
      setIsEmailChecking(false);
    }
  };

  const resetEmailCheck = () => {
    setIsEmailAvailable(null);
    setEmailCheckMessage(null);
  };

  return {
    isEmailChecking,
    isEmailAvailable,
    emailCheckMessage,
    checkEmail,
    resetEmailCheck,
  };
};
