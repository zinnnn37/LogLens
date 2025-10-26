package S13P31A306.loglens.domain.auth.validator;

import static S13P31A306.loglens.global.constants.GlobalErrorCode.EMAIL_DUPLICATED;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.PASSWORD_CONFIRMATION_MISMATCH;

import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserValidator {

    private static final String LOG_PREFIX = "[UserValidator]";

    private final UserRepository userRepository;

    /**
     * 이메일 사용 가능 여부 확인 (예외를 던지지 않음)
     *
     * @param email 확인할 이메일
     * @return true: 사용 가능, false: 이미 사용 중
     */
    public boolean isEmailAvailable(final String email) {
        log.debug("{}이메일 사용 가능 여부 확인: {}", LOG_PREFIX, email);
        boolean exists = userRepository.existsByEmail(email);
        boolean available = !exists;

        if (available) {
            log.debug("{}이메일 {}은(는) 사용 가능합니다.", LOG_PREFIX, email);
        } else {
            log.debug("{}이메일 {}은(는) 이미 사용 중입니다.", LOG_PREFIX, email);
        }

        return available;
    }

    /**
     * 이메일 중복 검증
     */
    public void validateDuplicateEmail(final String email) {
        log.debug("{}이메일 중복 확인: {}", LOG_PREFIX, email);
        if (userRepository.existsByEmail(email)) {
            log.warn("{}이메일 {}은(는) 이미 사용 중입니다.", LOG_PREFIX, email);
            throw new BusinessException(EMAIL_DUPLICATED);
        }
        log.debug("{}이메일 {}은(는) 사용 가능합니다.", LOG_PREFIX, email);
    }

    /**
     * 비밀번호 일치 검증
     */
    public void validatePasswordConfirmation(final String password, final String passwordConfirm) {
        log.debug("{}비밀번호 확인 검증 시작", LOG_PREFIX);
        if (!password.equals(passwordConfirm)) {
            log.warn("{}비밀번호 불일치", LOG_PREFIX);
            throw new BusinessException(PASSWORD_CONFIRMATION_MISMATCH);
        }
        log.debug("{}비밀번호 확인 검증 성공", LOG_PREFIX);
    }
}
