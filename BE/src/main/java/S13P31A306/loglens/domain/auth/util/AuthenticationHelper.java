package S13P31A306.loglens.domain.auth.util;

import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.auth.model.CustomUserDetails;
import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security 인증 정보 관련 헬퍼 클래스 인증된 사용자 정보 조회 및 검증 기능 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationHelper {

    private static final String LOG_PREFIX = "[AuthenticationHelper]";

    private final UserRepository userRepository;

    /**
     * 현재 인증된 사용자의 ID를 반환
     *
     * @return 현재 인증된 사용자 ID
     * @throws BusinessException 인증 정보가 없거나 잘못된 경우
     */
    public Integer getCurrentUserId() {
        log.debug("{} 현재 사용자 ID 조회", LOG_PREFIX);
        CustomUserDetails userDetails = getCurrentUserDetails();
        Integer userId = userDetails.getUserId();
        log.debug("{} 현재 사용자 ID: {}", LOG_PREFIX, userId);
        return userId;
    }

    /**
     * 현재 인증된 사용자의 CustomUserDetails를 반환
     *
     * @return CustomUserDetails 객체
     * @throws BusinessException 인증 정보가 없거나 잘못된 경우
     */
    public CustomUserDetails getCurrentUserDetails() {
        log.debug("{} 현재 사용자 상세 정보 조회", LOG_PREFIX);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("{} 인증되지 않은 요청", LOG_PREFIX);
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            log.warn("{} 잘못된 Principal 타입: {}", LOG_PREFIX, principal.getClass().getName());
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }

        return (CustomUserDetails) principal;
    }

    /**
     * 현재 인증된 사용자의 User 엔티티를 조회하여 반환 DB에서 최신 정보를 조회합니다.
     *
     * @return User 엔티티
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    public User getCurrentUser() {
        Integer userId = getCurrentUserId();
        log.debug("{} User 엔티티 조회: userId={}", LOG_PREFIX, userId);

        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("{} 사용자를 찾을 수 없음: userId={}", LOG_PREFIX, userId);
                    return new BusinessException(GlobalErrorCode.USER_NOT_FOUND);
                });
    }

    /**
     * 현재 인증된 사용자가 특정 사용자 ID와 일치하는지 확인
     *
     * @param targetUserId 확인할 사용자 ID
     * @return true: 일치, false: 불일치
     */
    public boolean isCurrentUser(Integer targetUserId) {
        Integer currentUserId = getCurrentUserId();
        boolean isMatch = currentUserId.equals(targetUserId);
        log.debug("{} 사용자 일치 여부: current={}, target={}, match={}",
                LOG_PREFIX, currentUserId, targetUserId, isMatch);
        return isMatch;
    }

    /**
     * 현재 인증된 사용자가 특정 사용자 ID와 일치하는지 검증 일치하지 않으면 예외 발생
     *
     * @param targetUserId 확인할 사용자 ID
     * @throws BusinessException 권한이 없는 경우
     */
    public void validateCurrentUser(Integer targetUserId) {
        if (!isCurrentUser(targetUserId)) {
            log.warn("{} 권한 없음: current={}, target={}",
                    LOG_PREFIX, getCurrentUserId(), targetUserId);
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }
    }
}
