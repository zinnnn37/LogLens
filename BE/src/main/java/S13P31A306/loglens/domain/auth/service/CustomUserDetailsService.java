package S13P31A306.loglens.domain.auth.service;

import S13P31A306.loglens.domain.auth.model.CustomUserDetails;
import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final String LOG_PREFIX = "[CustomUserDetailsService]";
    private final UserRepository userRepository;

    /**
     * 이메일을 기반으로 사용자 정보를 로드합니다.
     *
     * @param email 사용자 이메일
     * @return UserDetails 사용자 인증 정보
     */
    @Override
    public UserDetails loadUserByUsername(final String email) throws UsernameNotFoundException {
        log.info("{} 이메일로 사용자 조회 시도: {}", LOG_PREFIX, email);

        return userRepository.findByEmail(email)
                .map(user -> {
                    log.info("{} 사용자 조회 성공: {}", LOG_PREFIX, user.getEmail());
                    return new CustomUserDetails(user);
                })
                .orElseThrow(() -> {
                    log.warn("{} 사용자를 찾을 수 없음: {}", LOG_PREFIX, email);
                    return new BusinessException(GlobalErrorCode.USER_NOT_FOUND);
                });
    }
}
