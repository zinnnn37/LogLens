package S13P31A306.loglens.domain.statistics.validator;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.MEMBER_NOT_FOUND;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static S13P31A306.loglens.domain.statistics.constants.StatisticsErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * StatisticsValidator 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsValidator 테스트")
class StatisticsValidatorTest {

    @InjectMocks
    private StatisticsValidator statisticsValidator;

    @Mock
    private ProjectValidator projectValidator;

    @Mock
    private AuthenticationHelper authHelper;

    private static final String PROJECT_UUID = "test-project-uuid";
    private static final Integer USER_ID = 1;
    private static final Integer PROJECT_ID = 1;

    @Nested
    @DisplayName("프로젝트 접근 검증 테스트")
    class ValidateProjectAccessTest {

        @Test
        void 프로젝트_접근_검증_성공() {
            // given
            Project project = createProject();

            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);

            // when
            Project result = statisticsValidator.validateProjectAccess(PROJECT_UUID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PROJECT_ID);
            verify(authHelper).getCurrentUserId();
            verify(projectValidator).validateProjectExists(PROJECT_UUID);
            verify(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);
        }

        @Test
        void 프로젝트가_존재하지_않으면_예외발생() {
            // given
            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID))
                    .willThrow(new BusinessException(PROJECT_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateProjectAccess(PROJECT_UUID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PROJECT_NOT_FOUND);
        }

        @Test
        void 사용자가_프로젝트_멤버가_아니면_예외발생() {
            // given
            Project project = createProject();

            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID)).willReturn(project);
            willThrow(new BusinessException(MEMBER_NOT_FOUND))
                    .given(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateProjectAccess(PROJECT_UUID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("시간 범위 검증 테스트")
    class ValidateTimeRangeTest {

        @Test
        void 시간_범위_검증_성공() {
            // given
            LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);

            // when & then (예외 발생하지 않음)
            statisticsValidator.validateTimeRange(startTime, endTime);
        }

        @Test
        void 시작_시간이_종료_시간보다_늦으면_예외발생() {
            // given
            LocalDateTime startTime = LocalDateTime.of(2025, 11, 14, 15, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 13, 15, 0);

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateTimeRange(startTime, endTime))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_TIME_RANGE);
        }

        @Test
        void 시작_시간과_종료_시간이_같으면_예외발생() {
            // given
            LocalDateTime sameTime = LocalDateTime.of(2025, 11, 13, 15, 0);

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateTimeRange(sameTime, sameTime))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_TIME_RANGE);
        }

        @Test
        void 시작_시간이_null이면_예외발생() {
            // given
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateTimeRange(null, endTime))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_TIME_RANGE);
        }

        @Test
        void 종료_시간이_null이면_예외발생() {
            // given
            LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateTimeRange(startTime, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_TIME_RANGE);
        }
    }

    @Nested
    @DisplayName("집계 간격 검증 테스트")
    class ValidateIntervalTest {

        @Test
        void 집계_간격_검증_성공() {
            // given
            int intervalHours = 3;

            // when & then (예외 발생하지 않음)
            statisticsValidator.validateInterval(intervalHours);
        }

        @Test
        void 집계_간격이_0이면_예외발생() {
            // given
            int intervalHours = 0;

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateInterval(intervalHours))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_INTERVAL);
        }

        @Test
        void 집계_간격이_음수면_예외발생() {
            // given
            int intervalHours = -1;

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateInterval(intervalHours))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_INTERVAL);
        }

        @Test
        void 집계_간격이_최대값을_초과하면_예외발생() {
            // given
            int intervalHours = 25; // MAX_INTERVAL_HOURS = 24

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateInterval(intervalHours))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_INTERVAL);
        }
    }

    @Nested
    @DisplayName("조회 기간 제한 검증 테스트")
    class ValidatePeriodLimitTest {

        @Test
        void 조회_기간_제한_검증_성공() {
            // given
            LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0); // 1일
            int maxDays = 90;

            // when & then (예외 발생하지 않음)
            statisticsValidator.validatePeriodLimit(startTime, endTime, maxDays);
        }

        @Test
        void 조회_기간이_최대값을_초과하면_예외발생() {
            // given
            LocalDateTime startTime = LocalDateTime.of(2025, 8, 1, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 12, 1, 0, 0); // 122일
            int maxDays = 90;

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validatePeriodLimit(startTime, endTime, maxDays))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PERIOD_EXCEEDS_LIMIT);
        }

        @Test
        void 조회_기간이_정확히_최대값이면_검증_성공() {
            // given
            LocalDateTime startTime = LocalDateTime.of(2025, 8, 16, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 0, 0); // 90일
            int maxDays = 90;

            // when & then (예외 발생하지 않음)
            statisticsValidator.validatePeriodLimit(startTime, endTime, maxDays);
        }
    }

    @Nested
    @DisplayName("로그 추이 요청 검증 테스트")
    class ValidateLogTrendRequestTest {

        @Test
        void 로그_추이_요청_검증_성공() {
            // given
            Project project = createProject();

            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);

            // when
            Project result = statisticsValidator.validateLogTrendRequest(PROJECT_UUID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PROJECT_ID);
        }
    }

    @Nested
    @DisplayName("Traffic 요청 검증 테스트")
    class ValidateTrafficRequestTest {

        @Test
        void Traffic_요청_검증_성공() {
            // given
            Project project = createProject();

            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);

            // when
            Project result = statisticsValidator.validateTrafficRequest(PROJECT_UUID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PROJECT_ID);
        }
    }

    @Nested
    @DisplayName("커스텀 시간 범위 요청 검증 테스트")
    class ValidateCustomTimeRangeRequestTest {

        @Test
        void 커스텀_시간_범위_요청_검증_성공() {
            // given
            Project project = createProject();
            LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);
            int intervalHours = 3;

            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);

            // when
            Project result = statisticsValidator.validateCustomTimeRangeRequest(
                    PROJECT_UUID, startTime, endTime, intervalHours
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PROJECT_ID);
        }

        @Test
        void 커스텀_요청_시_시간_범위가_잘못되면_예외발생() {
            // given
            Project project = createProject();
            LocalDateTime startTime = LocalDateTime.of(2025, 11, 14, 15, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 13, 15, 0); // 잘못된 순서
            int intervalHours = 3;

            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateCustomTimeRangeRequest(
                    PROJECT_UUID, startTime, endTime, intervalHours
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_TIME_RANGE);
        }

        @Test
        void 커스텀_요청_시_집계_간격이_잘못되면_예외발생() {
            // given
            Project project = createProject();
            LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);
            int intervalHours = 0; // 잘못된 간격

            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateCustomTimeRangeRequest(
                    PROJECT_UUID, startTime, endTime, intervalHours
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", INVALID_INTERVAL);
        }

        @Test
        void 커스텀_요청_시_조회_기간이_초과되면_예외발생() {
            // given
            Project project = createProject();
            LocalDateTime startTime = LocalDateTime.of(2025, 8, 1, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 12, 1, 0, 0); // 122일
            int intervalHours = 3;

            given(authHelper.getCurrentUserId()).willReturn(USER_ID);
            given(projectValidator.validateProjectExists(PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateMemberExists(PROJECT_ID, USER_ID);

            // when & then
            assertThatThrownBy(() -> statisticsValidator.validateCustomTimeRangeRequest(
                    PROJECT_UUID, startTime, endTime, intervalHours
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PERIOD_EXCEEDS_LIMIT);
        }
    }

    // 헬퍼 메서드
    private Project createProject() {
        Project project = mock(Project.class);
        given(project.getId()).willReturn(PROJECT_ID);
        given(project.getProjectName()).willReturn("Test Project");
        return project;
    }
}
