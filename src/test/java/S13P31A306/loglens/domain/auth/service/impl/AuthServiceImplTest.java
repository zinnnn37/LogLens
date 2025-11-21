//package S13P31A306.loglens.domain.auth.service.impl;
//
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.ACCESS_TOKEN_MISSING;
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.INVALID_CREDENTIALS;
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.REFRESH_TOKEN_INVALID;
//import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.REFRESH_TOKEN_MISSING;
//import static S13P31A306.loglens.global.constants.GlobalErrorCode.UNAUTHORIZED;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.BDDMockito.any;
//import static org.mockito.BDDMockito.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.mock;
//import static org.mockito.BDDMockito.times;
//import static org.mockito.BDDMockito.verify;
//import static org.mockito.BDDMockito.willDoNothing;
//import static org.mockito.BDDMockito.willThrow;
//
//import S13P31A306.loglens.domain.auth.dto.request.UserSigninRequest;
//import S13P31A306.loglens.domain.auth.jwt.Jwt;
//import S13P31A306.loglens.domain.auth.jwt.JwtTokenProvider;
//import S13P31A306.loglens.domain.auth.respository.AuthRepository;
//import S13P31A306.loglens.domain.auth.service.CustomUserDetailsService;
//import S13P31A306.loglens.domain.auth.validator.AuthValidator;
//import S13P31A306.loglens.global.exception.BusinessException;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetails;
//
//@ExtendWith(MockitoExtension.class)
//class AuthServiceImplTest {
//
//    @InjectMocks
//    private AuthServiceImpl authService;
//
//    @Mock
//    private AuthenticationManager authenticationManager;
//    @Mock
//    private JwtTokenProvider jwtTokenProvider;
//    @Mock
//    private CustomUserDetailsService customUserDetailsService;
//    @Mock
//    private AuthRepository authRepository;
//    @Mock
//    private AuthValidator authValidator;
//
//    @Test
//    void 로그인_성공_시_JWT를_반환한다() {
//        // given
//        UserSigninRequest request = new UserSigninRequest("test@example.com", "Password123!");
//        Authentication authentication = mock(Authentication.class);
//        given(authentication.getName()).willReturn("test@example.com");
//
//        Jwt jwt = Jwt.builder()
//                .userId(1)
//                .email("test@example.com")
//                .accessToken("access-token")
//                .refreshToken("refresh-token")
//                .tokenType("Bearer")
//                .expiresIn(1800)
//                .build();
//
//        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                .willReturn(authentication);
//        given(jwtTokenProvider.generateJwt(authentication)).willReturn(jwt);
//        willDoNothing().given(authRepository).saveRefreshToken(anyString(), anyString());
//
//        // when
//        Jwt result = authService.signIn(request);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getEmail()).isEqualTo("test@example.com");
//        assertThat(result.getAccessToken()).isEqualTo("access-token");
//        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
//        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
//        verify(jwtTokenProvider, times(1)).generateJwt(authentication);
//        verify(authRepository, times(1)).saveRefreshToken("test@example.com", "refresh-token");
//    }
//
//    @Test
//    void 로그인_실패_시_INVALID_CREDENTIALS_예외가_발생한다() {
//        // given
//        UserSigninRequest request = new UserSigninRequest("test@example.com", "WrongPassword");
//
//        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                .willThrow(new BadCredentialsException("Invalid credentials"));
//
//        // when & then
//        assertThatThrownBy(() -> authService.signIn(request))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", INVALID_CREDENTIALS);
//
//        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
//        verify(jwtTokenProvider, times(0)).generateJwt(any(Authentication.class));
//        verify(authRepository, times(0)).saveRefreshToken(anyString(), anyString());
//    }
//
//    @Test
//    void 토큰_재발급_성공_시_새로운_JWT를_반환한다() {
//        // given
//        String authHeader = "Bearer old-access-token";
//        String refreshToken = "valid-refresh-token";
//        String accessToken = "old-access-token";
//        String userEmail = "test@example.com";
//
//        UserDetails userDetails = mock(UserDetails.class);
//
//        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, "", null);
//
//        Jwt newJwt = Jwt.builder()
//                .userId(1)
//                .email(userEmail)
//                .accessToken("new-access-token")
//                .refreshToken("new-refresh-token")
//                .tokenType("Bearer")
//                .expiresIn(1800)
//                .build();
//
//        given(authValidator.validateAndExtractAccessToken(authHeader)).willReturn(accessToken);
//        willDoNothing().given(authValidator).validateRefreshTokenCookie(refreshToken);
//        given(authValidator.validateRefreshToken(accessToken, refreshToken)).willReturn(userEmail);
//        given(customUserDetailsService.loadUserByUsername(userEmail)).willReturn(userDetails);
//        given(jwtTokenProvider.generateJwt(any(Authentication.class))).willReturn(newJwt);
//        willDoNothing().given(authRepository).saveRefreshToken(userEmail, newJwt.getRefreshToken());
//
//        // when
//        Jwt result = authService.reissueToken(authHeader, refreshToken);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result.getEmail()).isEqualTo(userEmail);
//        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
//        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
//        verify(authValidator, times(1)).validateAndExtractAccessToken(authHeader);
//        verify(authValidator, times(1)).validateRefreshTokenCookie(refreshToken);
//        verify(authValidator, times(1)).validateRefreshToken(accessToken, refreshToken);
//        verify(customUserDetailsService, times(1)).loadUserByUsername(userEmail);
//        verify(jwtTokenProvider, times(1)).generateJwt(any(Authentication.class));
//        verify(authRepository, times(1)).saveRefreshToken(userEmail, "new-refresh-token");
//    }
//
//    @Test
//    void 토큰_재발급_시_Access_Token_누락되면_예외가_발생한다() {
//        // given
//        String authHeader = null;
//        String refreshToken = "valid-refresh-token";
//
//        willThrow(new BusinessException(ACCESS_TOKEN_MISSING))
//                .given(authValidator).validateAndExtractAccessToken(authHeader);
//
//        // when & then
//        assertThatThrownBy(() -> authService.reissueToken(authHeader, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", ACCESS_TOKEN_MISSING);
//
//        verify(authValidator, times(1)).validateAndExtractAccessToken(authHeader);
//        verify(authValidator, times(0)).validateRefreshTokenCookie(anyString());
//    }
//
//    @Test
//    void 토큰_재발급_시_Refresh_Token_누락되면_예외가_발생한다() {
//        // given
//        String authHeader = "Bearer access-token";
//        String refreshToken = null;
//        String accessToken = "access-token";
//
//        given(authValidator.validateAndExtractAccessToken(authHeader)).willReturn(accessToken);
//        willThrow(new BusinessException(REFRESH_TOKEN_MISSING))
//                .given(authValidator).validateRefreshTokenCookie(refreshToken);
//
//        // when & then
//        assertThatThrownBy(() -> authService.reissueToken(authHeader, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_MISSING);
//
//        verify(authValidator, times(1)).validateAndExtractAccessToken(authHeader);
//        verify(authValidator, times(1)).validateRefreshTokenCookie(refreshToken);
//        verify(authValidator, times(0)).validateRefreshToken(anyString(), anyString());
//    }
//
//    @Test
//    void 토큰_재발급_시_Refresh_Token_불일치하면_예외가_발생한다() {
//        // given
//        String authHeader = "Bearer access-token";
//        String refreshToken = "invalid-refresh-token";
//        String accessToken = "access-token";
//
//        given(authValidator.validateAndExtractAccessToken(authHeader)).willReturn(accessToken);
//        willDoNothing().given(authValidator).validateRefreshTokenCookie(refreshToken);
//        willThrow(new BusinessException(REFRESH_TOKEN_INVALID))
//                .given(authValidator).validateRefreshToken(accessToken, refreshToken);
//
//        // when & then
//        assertThatThrownBy(() -> authService.reissueToken(authHeader, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_INVALID);
//
//        verify(authValidator, times(1)).validateAndExtractAccessToken(authHeader);
//        verify(authValidator, times(1)).validateRefreshTokenCookie(refreshToken);
//        verify(authValidator, times(1)).validateRefreshToken(accessToken, refreshToken);
//        verify(customUserDetailsService, times(0)).loadUserByUsername(anyString());
//    }
//
//    @Test
//    void 로그아웃_성공_시_Refresh_Token이_삭제된다() {
//        // given
//        String userEmail = "test@example.com";
//        String refreshToken = "valid-refresh-token";
//        Authentication authentication = mock(Authentication.class);
//        given(authentication.getName()).willReturn(userEmail);
//
//        willDoNothing().given(authValidator).validateAuthentication(authentication);
//        willDoNothing().given(authValidator).validateRefreshTokenCookie(refreshToken);
//        willDoNothing().given(authRepository).deleteRefreshTokenByEmail(userEmail);
//
//        // when
//        authService.signOut(authentication, refreshToken);
//
//        // then
//        verify(authValidator, times(1)).validateAuthentication(authentication);
//        verify(authValidator, times(1)).validateRefreshTokenCookie(refreshToken);
//        verify(authRepository, times(1)).deleteRefreshTokenByEmail(userEmail);
//    }
//
//    @Test
//    void 로그아웃_시_Authentication이_null이면_예외가_발생한다() {
//        // given
//        Authentication authentication = null;
//        String refreshToken = "valid-refresh-token";
//
//        willThrow(new BusinessException(UNAUTHORIZED))
//                .given(authValidator).validateAuthentication(authentication);
//
//        // when & then
//        assertThatThrownBy(() -> authService.signOut(authentication, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", UNAUTHORIZED);
//
//        verify(authValidator, times(1)).validateAuthentication(authentication);
//        verify(authValidator, times(0)).validateRefreshTokenCookie(anyString());
//        verify(authRepository, times(0)).deleteRefreshTokenByEmail(anyString());
//    }
//
//    @Test
//    void 로그아웃_시_Refresh_Token_누락되면_예외가_발생한다() {
//        // given
//        String refreshToken = null;
//        Authentication authentication = mock(Authentication.class);
//
//        willDoNothing().given(authValidator).validateAuthentication(authentication);
//        willThrow(new BusinessException(REFRESH_TOKEN_MISSING))
//                .given(authValidator).validateRefreshTokenCookie(refreshToken);
//
//        // when & then
//        assertThatThrownBy(() -> authService.signOut(authentication, refreshToken))
//                .isInstanceOf(BusinessException.class)
//                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_MISSING);
//
//        verify(authValidator, times(1)).validateAuthentication(authentication);
//        verify(authValidator, times(1)).validateRefreshTokenCookie(refreshToken);
//        verify(authRepository, times(0)).deleteRefreshTokenByEmail(anyString());
//    }
//}
