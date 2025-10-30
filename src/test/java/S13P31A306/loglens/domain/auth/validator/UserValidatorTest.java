package S13P31A306.loglens.domain.auth.validator;

import static S13P31A306.loglens.domain.auth.constants.AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.EMAIL_DUPLICATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

import S13P31A306.loglens.domain.auth.constants.UserErrorCode;
import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserValidator userValidator;

    @Test
    void 이메일_중복되지_않으면_예외없음() {
        // given
        String email = "new@example.com";
        given(userRepository.existsByEmail(email)).willReturn(false);

        // when
        userValidator.validateDuplicateEmail(email);

        // then
        then(userRepository).should(times(1)).existsByEmail(email);
    }

    @Test
    void 이메일_중복시_BusinessException_EMAIL_DUPLICATED_발생() {
        // given
        String email = "dup@example.com";
        given(userRepository.existsByEmail(email)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userValidator.validateDuplicateEmail(email))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", EMAIL_DUPLICATED);

        then(userRepository).should(times(1)).existsByEmail(email);
    }

    @Test
    void 비밀번호_일치시_예외없음() {
        // given
        String pw = "Password123!";
        String confirm = "Password123!";

        // when
        userValidator.validatePasswordConfirmation(pw, confirm);

        // then
        // 예외 없음 → 통과
    }

    @Test
    void 비밀번호_불일치시_BusinessException_PASSWORD_CONFIRMATION_MISMATCH_발생한다() {
        // given
        String pw = "Password123!";
        String confirm = "Different!";

        // when & then
        assertThatThrownBy(() -> userValidator.validatePasswordConfirmation(pw, confirm))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", PASSWORD_CONFIRMATION_MISMATCH);
    }

    @Test
    void isEmailAvailable_true_false_정상동작() {
        // given
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(userRepository.existsByEmail("used@example.com")).willReturn(true);

        // when
        boolean available1 = userValidator.isEmailAvailable("new@example.com");
        boolean available2 = userValidator.isEmailAvailable("used@example.com");

        // then
        assertThat(available1).isTrue();
        assertThat(available2).isFalse();
        then(userRepository).should(times(1)).existsByEmail("new@example.com");
        then(userRepository).should(times(1)).existsByEmail("used@example.com");
    }

    @Test
    void 이름으로_회원검색에_성공한다() {
        // given
        String name = "홍길동";
        int page = 0;
        int size = 20;

        // when & then
        assertDoesNotThrow(() -> userValidator.validateFindUsersByName(name, page, size));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void 이름에_누락_또는_공백_시_BusinessException_NAME_REQUIRED이_발생한다(String name) {
        // given
        int page = 0;
        int size = 20;

        // when & then
        assertThatThrownBy(() -> userValidator.validateFindUsersByName(name, page, size))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.NAME_REQUIRED.getMessage());
    }

    @Test
    void 이름_길이_초과시_BusinessException_NAME_LENGTH_INVALID이_발생한다() {
        // given
        String name = "a".repeat(51);
        int page = 0;
        int size = 20;

        // when & then
        assertThatThrownBy(() -> userValidator.validateFindUsersByName(name, page, size))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.NAME_LENGTH_INVALID.getMessage());
    }

    @Test
    void 이름형식에_오류가있으면_BusinessException_NAME_FORMAT_INVALID이_발생한다() {
        // given
        String name = "홍길동123";
        int page = 0;
        int size = 20;

        // when & then
        assertThatThrownBy(() -> userValidator.validateFindUsersByName(name, page, size))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.NAME_FORMAT_INVALID.getMessage());
    }

    @Test
    void 페이지_번호에_음수가_있으면_BusinessException_PAGE_INVALID이_발생한다() {
        // given
        String name = "홍길동";
        int page = -1;
        int size = 20;

        // when & then
        assertThatThrownBy(() -> userValidator.validateFindUsersByName(name, page, size))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.PAGE_INVALID.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void 페이지_크기가_벗어나면_BusinessException_SIZE_INVALID이_발생한다(int size) {
        // given
        String name = "홍길동";
        int page = 0;

        // when & then
        assertThatThrownBy(() -> userValidator.validateFindUsersByName(name, page, size))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.SIZE_INVALID.getMessage());
    }
}
