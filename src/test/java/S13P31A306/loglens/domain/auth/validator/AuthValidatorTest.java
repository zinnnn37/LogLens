//package S13P31A306.loglens.domain.auth.validator;
//
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.ACCESS_TOKEN_MISSING;
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.INVALID_TOKEN_FORMAT;
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.REFRESH_TOKEN_EXPIRED;
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.REFRESH_TOKEN_INVALID;
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.REFRESH_TOKEN_MISSING;
//import static S13P31A306.loglens.global.constants.GlobalErrorCode.UNAUTHORIZED;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.then;
//import static org.mockito.BDDMockito.times;
//
//import S13P31A306.loglens.domain.auth.jwt.JwtTokenProvider;
//import S13P31A306.loglens.domain.auth.respository.AuthRepository;
//import S13P31A306.loglens.global.exception.BusinessException;
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.MalformedJwtException;
//import io.jsonwebtoken.UnsupportedJwtException;
//import java.util.Optional;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.core.Authentication;
//
//@ExtendWith(MockitoExtension.class)
//class AuthValidatorTest {
//
//    @Mock
//    private JwtTokenProvider jwtTokenProvider;
//
//    @Mock
//    private AuthRepository authRepository;
//
//    @InjectMocks
//    private AuthValidator authValidator;
//
//    @Test
//    void validateRefreshToken_성공_시_사용자_email을_반환한다() {
//        // given
//        String accessToken = "valid-access-token";
//        String refreshToken = "valid-refresh-token";
//        String userEmail = "test@example.com";
//
//        given(jwtTokenProvider.getSubject(refreshToken)).willReturn(userEmail);
//        given(jwtTokenProvider.getSubjectFromExpiredToken(accessToken)).willReturn(userEmail);
//        given(authRepository.findRefreshTokenByEmail(userEmail)).willReturn(Optional.of(refreshToken));
//
//        // when
//        String result = authValidator.validateRefreshToken(accessToken, refreshToken);
//
//        // then
//        assertThat(result).isEqualTo(userEmail);
//        then(jwtTokenProvider).should(times(1)).getSubject(refreshToken);
//        then(jwtTokenProvider).should(times(1)).getSubjectFromExpiredToken(accessToken);
//        then(authRepository).should(times(1)).findRefreshTokenByEmail(userEmail);
//    }
//
//    @Test
//    void validateRefreshToken_만료된_Refresh_Token이면_REFRESH_TOKEN_EXPIRED_예외가_발생한다() {
//        // given
//        String accessToken = "valid-access-token";
//        String refreshToken = "expired-refresh-token";
//
//        given(jwtTokenProvider.getSubject(refreshToken)).willThrow(ExpiredJwtException.class);
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshToken(accessToken, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_EXPIRED);
//
//        then(jwtTokenProvider).should(times(1)).getSubject(refreshToken);
//        then(jwtTokenProvider).should(times(0)).getSubjectFromExpiredToken(accessToken);
//    }
//
//    @Test
//    void validateRefreshToken_잘못된_서명이면_REFRESH_TOKEN_INVALID_예외가_발생한다() {
//        // given
//        String accessToken = "valid-access-token";
//        String refreshToken = "malformed-refresh-token";
//
//        given(jwtTokenProvider.getSubject(refreshToken)).willThrow(MalformedJwtException.class);
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshToken(accessToken, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_INVALID);
//
//        then(jwtTokenProvider).should(times(1)).getSubject(refreshToken);
//    }
//
//    @Test
//    void validateRefreshToken_지원되지_않는_토큰이면_REFRESH_TOKEN_INVALID_예외가_발생한다() {
//        // given
//        String accessToken = "valid-access-token";
//        String refreshToken = "unsupported-refresh-token";
//
//        given(jwtTokenProvider.getSubject(refreshToken)).willThrow(UnsupportedJwtException.class);
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshToken(accessToken, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_INVALID);
//
//        then(jwtTokenProvider).should(times(1)).getSubject(refreshToken);
//    }
//
//    @Test
//    void validateRefreshToken_Access_Token과_Refresh_Token의_소유자가_다르면_예외가_발생한다() {
//        // given
//        String accessToken = "user1-access-token";
//        String refreshToken = "user2-refresh-token";
//        String userEmailFromRt = "user2@example.com";
//        String userEmailFromAt = "user1@example.com";
//
//        given(jwtTokenProvider.getSubject(refreshToken)).willReturn(userEmailFromRt);
//        given(jwtTokenProvider.getSubjectFromExpiredToken(accessToken)).willReturn(userEmailFromAt);
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshToken(accessToken, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_INVALID);
//
//        then(jwtTokenProvider).should(times(1)).getSubject(refreshToken);
//        then(jwtTokenProvider).should(times(1)).getSubjectFromExpiredToken(accessToken);
//        then(authRepository).should(times(0)).findRefreshTokenByEmail(userEmailFromRt);
//    }
//
//    @Test
//    void validateRefreshToken_Redis에_저장된_토큰이_없으면_예외가_발생한다() {
//        // given
//        String accessToken = "valid-access-token";
//        String refreshToken = "valid-refresh-token";
//        String userEmail = "test@example.com";
//
//        given(jwtTokenProvider.getSubject(refreshToken)).willReturn(userEmail);
//        given(jwtTokenProvider.getSubjectFromExpiredToken(accessToken)).willReturn(userEmail);
//        given(authRepository.findRefreshTokenByEmail(userEmail)).willReturn(Optional.empty());
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshToken(accessToken, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_INVALID);
//
//        then(jwtTokenProvider).should(times(1)).getSubject(refreshToken);
//        then(jwtTokenProvider).should(times(1)).getSubjectFromExpiredToken(accessToken);
//        then(authRepository).should(times(1)).findRefreshTokenByEmail(userEmail);
//    }
//
//    @Test
//    void validateRefreshToken_Redis_토큰과_요청_토큰이_다르면_예외가_발생한다() {
//        // given
//        String accessToken = "valid-access-token";
//        String refreshToken = "valid-refresh-token";
//        String storedRefreshToken = "different-refresh-token";
//        String userEmail = "test@example.com";
//
//        given(jwtTokenProvider.getSubject(refreshToken)).willReturn(userEmail);
//        given(jwtTokenProvider.getSubjectFromExpiredToken(accessToken)).willReturn(userEmail);
//        given(authRepository.findRefreshTokenByEmail(userEmail)).willReturn(Optional.of(storedRefreshToken));
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshToken(accessToken, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_INVALID);
//
//        then(jwtTokenProvider).should(times(1)).getSubject(refreshToken);
//        then(jwtTokenProvider).should(times(1)).getSubjectFromExpiredToken(accessToken);
//        then(authRepository).should(times(1)).findRefreshTokenByEmail(userEmail);
//    }
//
//    @Test
//    void validateAndExtractAccessToken_성공_시_Bearer_접두사를_제거한_토큰을_반환한다() {
//        // given
//        String authHeader = "Bearer valid-access-token";
//
//        // when
//        String result = authValidator.validateAndExtractAccessToken(authHeader);
//
//        // then
//        assertThat(result).isEqualTo("valid-access-token");
//    }
//
//    @Test
//    void validateAndExtractAccessToken_Authorization_헤더가_null이면_예외가_발생한다() {
//        // given
//        String authHeader = null;
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateAndExtractAccessToken(authHeader))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", ACCESS_TOKEN_MISSING);
//    }
//
//    @Test
//    void validateAndExtractAccessToken_Authorization_헤더가_빈_문자열이면_예외가_발생한다() {
//        // given
//        String authHeader = "";
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateAndExtractAccessToken(authHeader))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", ACCESS_TOKEN_MISSING);
//    }
//
//    @Test
//    void validateAndExtractAccessToken_Bearer_접두사가_없으면_예외가_발생한다() {
//        // given
//        String authHeader = "valid-access-token";
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateAndExtractAccessToken(authHeader))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", INVALID_TOKEN_FORMAT);
//    }
//
//    @Test
//    void validateAndExtractAccessToken_잘못된_접두사면_예외가_발생한다() {
//        // given
//        String authHeader = "Basic valid-access-token";
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateAndExtractAccessToken(authHeader))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", INVALID_TOKEN_FORMAT);
//    }
//
//    @Test
//    void validateRefreshTokenCookie_정상_쿠키면_예외없음() {
//        // given
//        String refreshToken = "valid-refresh-token";
//
//        // when & then
//        authValidator.validateRefreshTokenCookie(refreshToken);
//        // 예외 없음 → 통과
//    }
//
//    @Test
//    void validateRefreshTokenCookie_null이면_예외가_발생한다() {
//        // given
//        String refreshToken = null;
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshTokenCookie(refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_MISSING);
//    }
//
//    @Test
//    void validateRefreshTokenCookie_빈_문자열이면_예외가_발생한다() {
//        // given
//        String refreshToken = "";
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshTokenCookie(refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_MISSING);
//    }
//
//    @Test
//    void validateRefreshTokenCookie_공백만_있으면_예외가_발생한다() {
//        // given
//        String refreshToken = "   ";
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateRefreshTokenCookie(refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_MISSING);
//    }
//
//    @Test
//    void validateAuthentication_정상_Authentication이면_예외없음() {
//        // given
//        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
//
//        // when & then
//        authValidator.validateAuthentication(authentication);
//        // 예외 없음 → 통과
//    }
//
//    @Test
//    void validateAuthentication_null이면_예외가_발생한다() {
//        // given
//        Authentication authentication = null;
//
//        // when & then
//        assertThatThrownBy(() -> authValidator.validateAuthentication(authentication))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", UNAUTHORIZED);
//    }
//}
