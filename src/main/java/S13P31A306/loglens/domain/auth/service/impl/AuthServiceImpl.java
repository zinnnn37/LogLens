package S13P31A306.loglens.domain.auth.service.impl;

import S13P31A306.loglens.domain.auth.constants.AuthErrorCode;
import S13P31A306.loglens.domain.auth.dto.request.UserSigninRequest;
import S13P31A306.loglens.domain.auth.jwt.Jwt;
import S13P31A306.loglens.domain.auth.jwt.JwtTokenProvider;
import S13P31A306.loglens.domain.auth.respository.AuthRepository;
import S13P31A306.loglens.domain.auth.service.AuthService;
import S13P31A306.loglens.domain.auth.service.CustomUserDetailsService;
import S13P31A306.loglens.domain.auth.validator.AuthValidator;
import S13P31A306.loglens.global.annotation.Sensitive;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String LOG_PREFIX = "[AuthService]";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthRepository authRepository;
    private final AuthValidator authValidator;


    @Override
    @Transactional
    public Jwt signIn(final UserSigninRequest request) {
        log.info("{} 사용자 로그인 시도: {}", LOG_PREFIX, request.email());
        try {
            // 1. email/password 를 기반으로 Authentication 객체 생성
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password());

            // 2. 실제 검증 (비밀번호 일치 확인)
            // AuthenticationManager가 UserDetailsService와 PasswordEncoder를 사용하여 인증 처리
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            log.debug("{} 사용자 인증 성공: {}", LOG_PREFIX, authentication.getName());

            // 3. 인증 정보를 기반으로 JWT 생성
            Jwt jwt = jwtTokenProvider.generateJwt(authentication);
            log.debug("{} JWT 생성 완료: {}", LOG_PREFIX, authentication.getName());

            // 4. Refresh Token을 Repository를 통해 저장
            authRepository.saveRefreshToken(authentication.getName(), jwt.getRefreshToken());
            log.debug("{} Refresh Token 저장 완료: {}", LOG_PREFIX, authentication.getName());

            log.info("{} 로그인 성공: {}", LOG_PREFIX, request.email());
            return jwt;
        } catch (AuthenticationException e) {
            log.warn("{} 로그인 실패: 이메일 또는 비밀번호 불일치 - {}", LOG_PREFIX, request.email());
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    @Transactional
    public Jwt reissueToken(@Sensitive final String accessToken,
                            @Sensitive final String refreshToken) {
        log.info("{} 토큰 재발급 시도", LOG_PREFIX);

        // 1. Validator를 통해 모든 토큰 검증을 위임하고, 검증된 사용자 email을 받음
        String userEmail = authValidator.validateRefreshToken(accessToken, refreshToken);
        log.debug("{} Refresh Token 검증 완료: {}", LOG_PREFIX, userEmail);

        // 2. 사용자 정보로 Authentication 객체 생성
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "",
                userDetails.getAuthorities());
        log.debug("{} Authentication 객체 생성 완료: {}", LOG_PREFIX, userEmail);

        // 3. 새로운 JWT(Access + Refresh) 생성
        Jwt newJwt = jwtTokenProvider.generateJwt(authentication);
        log.debug("{} 새로운 JWT 생성 완료: {}", LOG_PREFIX, userEmail);

        // 4. Repository의 Refresh Token 정보 업데이트 (토큰 회전)
        authRepository.saveRefreshToken(userEmail, newJwt.getRefreshToken());
        log.debug("{} 새로운 Refresh Token 저장 완료: {}", LOG_PREFIX, userEmail);

        log.info("{} 토큰 재발급 성공: {}", LOG_PREFIX, userEmail);
        return newJwt;
    }

    @Override
    @Transactional
    public void signOut(final Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("{} 사용자 로그아웃 시도: {}", LOG_PREFIX, userEmail);
        authRepository.deleteRefreshTokenByEmail(userEmail);
        log.info("{} 로그아웃 처리 완료: Refresh Token 삭제 - {}", LOG_PREFIX, userEmail);
    }
}
