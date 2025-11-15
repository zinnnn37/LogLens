package S13P31A306.loglens.domain.analysis.validator;

import S13P31A306.loglens.domain.analysis.constants.AnalysisErrorCode;
import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisOptions;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisValidator 테스트")
class AnalysisValidatorTest {

    @InjectMocks
    private AnalysisValidator analysisValidator;

    @Mock
    private ProjectValidator projectValidator;

    @Mock
    private LogService logService;

    private UserDetails userDetails;
    private static final String TEST_PROJECT_UUID = "test-project-uuid";
    private static final Long TEST_LOG_ID = 12345L;

    @BeforeEach
    void setup() {
        userDetails = mock(UserDetails.class);
        given(userDetails.getUsername()).willReturn("testuser");
    }

    @Nested
    @DisplayName("프로젝트 분석 요청 검증 테스트")
    class ValidateProjectAnalysisRequestTest {

        @Test
        @DisplayName("유효한 요청이면 검증을 통과한다")
        void 유효한_요청이면_검증을_통과한다() {
            // given
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .startTime(LocalDateTime.now().minusDays(7))
                    .endTime(LocalDateTime.now())
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);

            // when & then
            assertThatCode(() -> analysisValidator.validateProjectAnalysisRequest(
                    TEST_PROJECT_UUID, request, userDetails
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("시간 범위 없이도 검증을 통과한다")
        void 시간_범위_없이도_검증을_통과한다() {
            // given
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);

            // when & then
            assertThatCode(() -> analysisValidator.validateProjectAnalysisRequest(
                    TEST_PROJECT_UUID, request, userDetails
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("프로젝트 접근 권한이 없으면 예외를 발생시킨다")
        void 프로젝트_접근_권한이_없으면_예외를_발생시킨다() {
            // given
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willThrow(new BusinessException(AnalysisErrorCode.INVALID_TIME_RANGE, "접근 권한 없음"))
                    .given(projectValidator).validateProjectAccess(any(Integer.class));

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateProjectAnalysisRequest(
                    TEST_PROJECT_UUID, request, userDetails
            )).isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("시작 시간이 종료 시간보다 늦으면 INVALID_TIME_RANGE 예외를 발생시킨다")
        void 시작_시간이_종료_시간보다_늦으면_예외를_발생시킨다() {
            // given
            LocalDateTime now = LocalDateTime.now();
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .startTime(now)
                    .endTime(now.minusDays(1))  // 종료가 시작보다 빠름
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateProjectAnalysisRequest(
                    TEST_PROJECT_UUID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.INVALID_TIME_RANGE)
                    .hasMessageContaining("시작 시간이 종료 시간보다 늦을 수 없습니다");
        }

        @Test
        @DisplayName("시간 범위가 1년을 초과하면 INVALID_TIME_RANGE 예외를 발생시킨다")
        void 시간_범위가_1년을_초과하면_예외를_발생시킨다() {
            // given
            LocalDateTime now = LocalDateTime.now();
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .startTime(now.minusDays(366))
                    .endTime(now)
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateProjectAnalysisRequest(
                    TEST_PROJECT_UUID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.INVALID_TIME_RANGE)
                    .hasMessageContaining("시간 범위는 최대 365일까지 가능합니다");
        }

        @Test
        @DisplayName("종료 시간이 미래면 INVALID_TIME_RANGE 예외를 발생시킨다")
        void 종료_시간이_미래면_예외를_발생시킨다() {
            // given
            LocalDateTime now = LocalDateTime.now();
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .startTime(now.minusDays(1))
                    .endTime(now.plusDays(1))  // 미래
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateProjectAnalysisRequest(
                    TEST_PROJECT_UUID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.INVALID_TIME_RANGE)
                    .hasMessageContaining("종료 시간은 현재 시간 이후일 수 없습니다");
        }

        @Test
        @DisplayName("문서 형식이 null이면 INVALID_FORMAT 예외를 발생시킨다")
        void 문서_형식이_null이면_예외를_발생시킨다() {
            // given
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(null)  // null
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateProjectAnalysisRequest(
                    TEST_PROJECT_UUID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.INVALID_FORMAT);
        }

        @ParameterizedTest
        @MethodSource("provideInvalidTimeRanges")
        @DisplayName("잘못된 시간 범위면 예외를 발생시킨다")
        void 잘못된_시간_범위면_예외를_발생시킨다(LocalDateTime start, LocalDateTime end, String errorMessage) {
            // given
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .startTime(start)
                    .endTime(end)
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateProjectAnalysisRequest(
                    TEST_PROJECT_UUID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.INVALID_TIME_RANGE);
        }

        private static Stream<Arguments> provideInvalidTimeRanges() {
            LocalDateTime now = LocalDateTime.now();
            return Stream.of(
                    Arguments.of(now.plusDays(1), now, "시작이 미래"),
                    Arguments.of(now.minusDays(400), now, "범위 초과"),
                    Arguments.of(now, now.minusDays(1), "시작이 종료보다 늦음")
            );
        }
    }

    @Nested
    @DisplayName("에러 분석 요청 검증 테스트")
    class ValidateErrorAnalysisRequestTest {

        @Test
        @DisplayName("유효한 요청이면 검증을 통과한다")
        void 유효한_요청이면_검증을_통과한다() {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(TEST_PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);
            given(logService.getLogDetail(TEST_LOG_ID, TEST_PROJECT_UUID))
                    .willReturn(mock(LogDetailResponse.class));

            // when & then
            assertThatCode(() -> analysisValidator.validateErrorAnalysisRequest(
                    TEST_LOG_ID, request, userDetails
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("maxRelatedLogs가 유효 범위면 검증을 통과한다")
        void maxRelatedLogs가_유효_범위면_검증을_통과한다() {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(TEST_PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .options(ErrorAnalysisOptions.builder()
                            .maxRelatedLogs(50)
                            .build())
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);
            given(logService.getLogDetail(TEST_LOG_ID, TEST_PROJECT_UUID))
                    .willReturn(mock(LogDetailResponse.class));

            // when & then
            assertThatCode(() -> analysisValidator.validateErrorAnalysisRequest(
                    TEST_LOG_ID, request, userDetails
            )).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("로그가 존재하지 않으면 LOG_NOT_FOUND 예외를 발생시킨다")
        void 로그가_존재하지_않으면_예외를_발생시킨다() {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(TEST_PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);
            given(logService.getLogDetail(TEST_LOG_ID, TEST_PROJECT_UUID)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateErrorAnalysisRequest(
                    TEST_LOG_ID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.LOG_NOT_FOUND);
        }

        @Test
        @DisplayName("로그 조회 중 예외가 발생하면 LOG_NOT_FOUND 예외를 발생시킨다")
        void 로그_조회_중_예외가_발생하면_예외를_발생시킨다() {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(TEST_PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);
            given(logService.getLogDetail(TEST_LOG_ID, TEST_PROJECT_UUID))
                    .willThrow(new RuntimeException("OpenSearch error"));

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateErrorAnalysisRequest(
                    TEST_LOG_ID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.LOG_NOT_FOUND);
        }

        @Test
        @DisplayName("문서 형식이 null이면 INVALID_FORMAT 예외를 발생시킨다")
        void 문서_형식이_null이면_예외를_발생시킨다() {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(TEST_PROJECT_UUID)
                    .format(null)
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);
            given(logService.getLogDetail(TEST_LOG_ID, TEST_PROJECT_UUID))
                    .willReturn(mock(LogDetailResponse.class));

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateErrorAnalysisRequest(
                    TEST_LOG_ID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.INVALID_FORMAT);
        }

        @Test
        @DisplayName("maxRelatedLogs가 1 미만이면 INVALID_OPTIONS 예외를 발생시킨다")
        void maxRelatedLogs가_1_미만이면_예외를_발생시킨다() {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(TEST_PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .options(ErrorAnalysisOptions.builder()
                            .maxRelatedLogs(0)
                            .build())
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);
            given(logService.getLogDetail(TEST_LOG_ID, TEST_PROJECT_UUID))
                    .willReturn(mock(LogDetailResponse.class));

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateErrorAnalysisRequest(
                    TEST_LOG_ID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.INVALID_OPTIONS)
                    .hasMessageContaining("1~100 범위 내여야 합니다");
        }

        @Test
        @DisplayName("maxRelatedLogs가 100을 초과하면 INVALID_OPTIONS 예외를 발생시킨다")
        void maxRelatedLogs가_100을_초과하면_예외를_발생시킨다() {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(TEST_PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .options(ErrorAnalysisOptions.builder()
                            .maxRelatedLogs(150)
                            .build())
                    .build();

            Project mockProject = mock(Project.class);
            given(mockProject.getId()).willReturn(1);
            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(mockProject);
            willDoNothing().given(projectValidator).validateProjectAccess(1);
            given(logService.getLogDetail(TEST_LOG_ID, TEST_PROJECT_UUID))
                    .willReturn(mock(LogDetailResponse.class));

            // when & then
            assertThatThrownBy(() -> analysisValidator.validateErrorAnalysisRequest(
                    TEST_LOG_ID, request, userDetails
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AnalysisErrorCode.INVALID_OPTIONS)
                    .hasMessageContaining("1~100 범위 내여야 합니다");
        }
    }
}
