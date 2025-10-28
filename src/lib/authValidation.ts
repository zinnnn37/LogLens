/**
 * 이메일 형식이 유효한지 검증합니다.
 * @param email - 검증할 이메일 주소
 * @returns 유효한 이메일이면 true, 아니면 false
 */
export const isValidEmail = (email: string): boolean => {
  // RFC 5322 기반의 이메일 정규식
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return emailRegex.test(email);
};

/**
 * 이메일 형식 검증 및 에러 메시지 반환
 * @param email - 검증할 이메일 주소
 * @returns 유효하면 null, 유효하지 않으면 에러 메시지
 */
export const validateEmail = (email: string): string | null => {
  if (!email) {
    return '이메일을 입력해주세요.';
  }

  if (!isValidEmail(email)) {
    return '올바른 이메일 형식이 아닙니다.';
  }

  return null;
};

/**
 * 비밀번호 유효성 검증
 * @param password - 검증할 비밀번호
 * @returns 유효하면 null, 유효하지 않으면 에러 메시지
 */
export const validatePassword = (password: string): string | null => {
  if (!password) {
    return '비밀번호를 입력해주세요.';
  }

  if (password.length < 8) {
    return '비밀번호는 8자 이상이어야 합니다.';
  }

  if (!/(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*])/.test(password)) {
    return '비밀번호는 영문 대소문자, 숫자, 특수문자를 포함해야 합니다.';
  }

  return null;
};

/**
 * 비밀번호 확인 검증
 * @param password - 원본 비밀번호
 * @param confirmPassword - 확인 비밀번호
 * @returns 일치하면 null, 일치하지 않으면 에러 메시지
 */
export const validatePasswordMatch = (
  password: string,
  confirmPassword: string,
): string | null => {
  if (!confirmPassword) {
    return '비밀번호 확인을 입력해주세요.';
  }

  if (password !== confirmPassword) {
    return '비밀번호가 일치하지 않습니다.';
  }

  return null;
};

/**
 * 이름 유효성 검증
 * @param name - 검증할 이름
 * @returns 유효하면 null, 유효하지 않으면 에러 메시지
 */
export const validateName = (name: string): string | null => {
  if (!name) {
    return '이름을 입력해주세요.';
  }

  if (name.length < 2) {
    return '이름은 2자 이상이어야 합니다.';
  }

  if (name.length > 15) {
    return '이름은 15자 이하여야 합니다.';
  }

  return null;
};
