package S13P31A306.loglens.domain.auth.validator;

import static S13P31A306.loglens.global.constants.GlobalErrorCode.EMAIL_DUPLICATED;

import S13P31A306.loglens.domain.auth.constants.AuthErrorCode;
import S13P31A306.loglens.domain.auth.constants.UserErrorCode;
import S13P31A306.loglens.domain.auth.constants.UserSortField;
import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserValidator {

    private static final String LOG_PREFIX = "[UserValidator]";
    private static final int MIN_PAGE = 0;
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 50;
    private static final String NAME_REGEX = "^[가-힣]*$";

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
            throw new BusinessException(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        log.debug("{}비밀번호 확인 검증 성공", LOG_PREFIX);
    }

    /**
     * 사용자 이름 검색 파라미터 유효성 검증
     *
     * @param name 검색할 이름
     * @param page 페이지 번호
     * @param size 페이지 크기
     */
    public void validateFindUsersByName(String name, int page, int size) {
        log.debug("{} 사용자 검색 파라미터 검증 시작: name={}, page={}, size={}", LOG_PREFIX, name, page, size);

        if (Objects.isNull(name) || name.isBlank()) {
            log.warn("{} 이름 누락 또는 공백", LOG_PREFIX);
            throw new BusinessException(UserErrorCode.NAME_REQUIRED);
        }

        if (name.length() > MAX_NAME_LENGTH) {
            log.warn("{} 이름 길이 초과: {}", LOG_PREFIX, name.length());
            throw new BusinessException(UserErrorCode.NAME_LENGTH_INVALID);
        }

        if (!name.matches(NAME_REGEX)) {
            log.warn("{} 이름 형식 오류: {}", LOG_PREFIX, name);
            throw new BusinessException(UserErrorCode.NAME_FORMAT_INVALID);
        }

        if (page < MIN_PAGE) {
            log.warn("{} 페이지 번호 유효성 실패: {}", LOG_PREFIX, page);
            throw new BusinessException(UserErrorCode.PAGE_INVALID);
        }

        if (size < MIN_SIZE || size > MAX_SIZE) {
            log.warn("{} 페이지 크기 유효성 실패: {}", LOG_PREFIX, size);
            throw new BusinessException(UserErrorCode.SIZE_INVALID);
        }

        log.debug("{} 사용자 검색 파라미터 검증 완료", LOG_PREFIX);
    }

    public Sort validateSortAndOrder(String sort, String order) {
        String sortField = validateSortField(sort);
        Sort.Direction direction = validateSortDirection(order);
        return Sort.by(direction, sortField);
    }

    private String validateSortField(String sort) {
        try {
            return UserSortField.valueOf(sort.toUpperCase()).getFieldName();
        } catch (IllegalArgumentException e) {
            log.warn("{} 유효하지 않은 정렬 필드: {}", LOG_PREFIX, sort);
            throw new BusinessException(UserErrorCode.SORT_INVALID);
        }
    }

    private Sort.Direction validateSortDirection(String order) {
        try {
            return Sort.Direction.fromString(order.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("{} 유효하지 않은 정렬 방향: {}", LOG_PREFIX, order);
            throw new BusinessException(UserErrorCode.ORDER_INVALID);
        }
    }
}
