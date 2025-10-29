package S13P31A306.loglens.domain.auth.service.impl;

import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.EMAIL_DUPLICATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import S13P31A306.loglens.domain.auth.constants.UserErrorCode;
import S13P31A306.loglens.domain.auth.dto.request.UserSignupRequest;
import S13P31A306.loglens.domain.auth.dto.response.EmailValidateResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSearchResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSignupResponse;
import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.auth.mapper.UserMapper;
import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.domain.auth.validator.UserValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserValidator userValidator;

    @Test
    void 회원가입_성공_시_UserSignupResponse을_반환한다() {
        // given
        UserSignupRequest request = new UserSignupRequest("홍길동", "test@example.com", "Password123!", "Password123!");
        User user = mock(User.class);

        UserSignupResponse response = new UserSignupResponse(1L, "홍길동", "test@example.com", user.getCreatedAt());

        // Mock 동작 정의
        willDoNothing().given(userValidator).validatePasswordConfirmation(anyString(), anyString());
        willDoNothing().given(userValidator).validateDuplicateEmail(anyString());
        given(passwordEncoder.encode(request.password())).willReturn("encodedPw");
        given(userMapper.toEntity(any(UserSignupRequest.class), anyString())).willReturn(user);
        given(userRepository.save(any(User.class))).willReturn(user);
        given(userMapper.toSignupResponse(any(User.class))).willReturn(response);

        // when
        UserSignupResponse result = userService.signup(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("test@example.com");
        verify(userValidator, times(1)).validatePasswordConfirmation(anyString(), anyString());
        verify(userValidator, times(1)).validateDuplicateEmail(anyString());
        verify(passwordEncoder, times(1)).encode("Password123!");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toSignupResponse(any(User.class));
    }

    @Test
    void 비밀번호_불일치시_BusinessException예외가_발생한다() {
        // given
        UserSignupRequest request = new UserSignupRequest("홍길동", "test@example.com", "Password123!", "WrongPassword!");

        willThrow(new BusinessException(PASSWORD_CONFIRMATION_MISMATCH))
                .given(userValidator).validatePasswordConfirmation(anyString(), anyString());

        // when & then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PASSWORD_CONFIRMATION_MISMATCH);

        verify(userValidator, times(1)).validatePasswordConfirmation(anyString(), anyString());
        verify(userValidator, times(0)).validateDuplicateEmail(anyString());
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void 이메일_중복시_예외가_발생한다() {
        // given
        UserSignupRequest request = new UserSignupRequest("홍길동", "dup@example.com", "Password123!", "Password123!");

        willDoNothing().given(userValidator).validatePasswordConfirmation(anyString(), anyString());
        willThrow(new BusinessException(EMAIL_DUPLICATED))
                .given(userValidator).validateDuplicateEmail(anyString());

        // when & then
        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", EMAIL_DUPLICATED);

        verify(userValidator, times(1)).validateDuplicateEmail(anyString());
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void 이메일_중복이_없으면_사용_가능하다() {
        // given
        String email = "new@example.com";
        given(userValidator.isEmailAvailable(email)).willReturn(true);

        // when
        EmailValidateResponse response = userService.checkEmailAvailability(email);

        // then
        assertThat(response).isNotNull();
        assertThat(response.available()).isTrue();
        verify(userValidator, times(1)).isEmailAvailable(email);
    }

    @Test
    void 이메일_중복이_있으면_사용_불가능하다() {
        // given
        String email = "existing@example.com";
        given(userValidator.isEmailAvailable(email)).willReturn(false);

        // when
        EmailValidateResponse response = userService.checkEmailAvailability(email);

        // then
        assertThat(response.available()).isFalse();
        verify(userValidator, times(1)).isEmailAvailable(email);
    }

    @Nested
    @DisplayName("이름으로 사용자 검색")
    class FindUsersByName {

        @Test
        void 이름_검색을_성공한다() {
            // given
            String name = "홍길동";
            Pageable pageable = PageRequest.of(0, 20);
            User user = mock(User.class);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
            UserSearchResponse response = new UserSearchResponse(1, "홍길동", "test@test.com");

            willDoNothing().given(userValidator).validateFindUsersByName(name, 0, 20);
            given(userRepository.findByNameContaining(name, pageable)).willReturn(userPage);
            given(userMapper.toSearchResponse(user)).willReturn(response);

            // when
            Page<UserSearchResponse> result = userService.findUsersByName(name, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(userValidator, times(1)).validateFindUsersByName(name, 0, 20);
            verify(userRepository, times(1)).findByNameContaining(name, pageable);
        }

        @Test
        void 검색_결과가_없으면_빈_페이지를_반환한다() {
            // given
            String name = "없는이름";
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> emptyPage = Page.empty(pageable);

            willDoNothing().given(userValidator).validateFindUsersByName(name, 0, 20);
            given(userRepository.findByNameContaining(name, pageable)).willReturn(emptyPage);

            // when
            Page<UserSearchResponse> result = userService.findUsersByName(name, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.isEmpty()).isTrue();
            verify(userValidator, times(1)).validateFindUsersByName(name, 0, 20);
            verify(userRepository, times(1)).findByNameContaining(name, pageable);
        }

        @Test
        void 이름이_없으면_예외가_발생한다() {
            // given
            String name = "";
            Pageable pageable = PageRequest.of(0, 20);
            willThrow(new BusinessException(UserErrorCode.NAME_REQUIRED))
                    .given(userValidator).validateFindUsersByName(name, 0, 20);

            // when & then
            assertThatThrownBy(() -> userService.findUsersByName(name, pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NAME_REQUIRED);

            verify(userRepository, times(0)).findByNameContaining(anyString(), any(Pageable.class));
        }
    }
}
