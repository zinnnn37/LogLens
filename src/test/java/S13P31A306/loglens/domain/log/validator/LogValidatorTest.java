package S13P31A306.loglens.domain.log.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.domain.project.util.ProjectMembershipHelper;
import S13P31A306.loglens.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LogValidatorTest {

    @InjectMocks
    private LogValidator logValidator;

    @Mock
    private ProjectMembershipHelper projectMembershipHelper;

    @Mock
    private ProjectService projectService;

    private LogSearchRequest baseRequest;

    @BeforeEach
    void setup() {
        baseRequest = LogSearchRequest.builder()
                .projectUuid("550e8400-e29b-41d4-a716-446655440000")
                .size(50)
                .sort("TIMESTAMP,DESC")
                .build();

        // 기본 Mock 설정 (일부 테스트에서 사용되지 않을 수 있음)
        lenient().when(projectService.getProjectIdByUuid(anyString())).thenReturn(1);
    }

    @Test
    void validate_정상_요청이면_검증을_통과한다() {
        // when & then
        assertThatCode(() -> logValidator.validate(baseRequest))
                .doesNotThrowAnyException();

        verify(projectService).getProjectIdByUuid(baseRequest.getProjectUuid());
        verify(projectMembershipHelper).validateProjectMembership(1);
    }

    @Test
    void validate_모든_필터_옵션이_있어도_검증을_통과한다() {
        // given
        baseRequest.setStartTime(LocalDateTime.of(2024, 1, 1, 0, 0));
        baseRequest.setEndTime(LocalDateTime.of(2024, 1, 31, 23, 59));
        baseRequest.setLogLevel(Arrays.asList("ERROR", "WARN"));
        baseRequest.setSourceType(Arrays.asList("BE", "FE"));

        // when & then
        assertThatCode(() -> logValidator.validate(baseRequest))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void validateProjectUuid_프로젝트_UUID가_null이거나_공백이면_예외를_발생시킨다(String invalidUuid) {
        // given
        baseRequest.setProjectUuid(invalidUuid);

        // when & then
        assertThatThrownBy(() -> logValidator.validate(baseRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", LogErrorCode.PROJECT_UUID_REQUIRED);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 50, 100})
    void validateSize_유효한_크기면_검증을_통과한다(int size) {
        // given
        baseRequest.setSize(size);

        // when & then
        assertThatCode(() -> logValidator.validate(baseRequest))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, 1000})
    void validateSize_유효하지_않은_크기면_예외를_발생시킨다(int size) {
        // given
        baseRequest.setSize(size);

        // when & then
        assertThatThrownBy(() -> logValidator.validate(baseRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", LogErrorCode.INVALID_SIZE);
    }

    @ParameterizedTest
    @MethodSource("provideValidTimeRanges")
    void validateTimeRange_유효한_시간_범위면_검증을_통과한다(LocalDateTime startTime, LocalDateTime endTime) {
        // given
        baseRequest.setStartTime(startTime);
        baseRequest.setEndTime(endTime);

        // when & then
        assertThatCode(() -> logValidator.validate(baseRequest))
                .doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideValidTimeRanges() {
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);
        LocalDateTime same = LocalDateTime.of(2024, 1, 15, 12, 0);

        return Stream.of(
                Arguments.of(start, null),          // 시작 시간만
                Arguments.of(null, end),            // 종료 시간만
                Arguments.of(start, end),           // 정상 범위
                Arguments.of(same, same)            // 같은 시간
        );
    }

    @Test
    void validateTimeRange_시작_시간이_종료_시간보다_늦으면_예외를_발생시킨다() {
        // given
        baseRequest.setStartTime(LocalDateTime.of(2024, 1, 31, 23, 59));
        baseRequest.setEndTime(LocalDateTime.of(2024, 1, 1, 0, 0));

        // when & then
        assertThatThrownBy(() -> logValidator.validate(baseRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", LogErrorCode.INVALID_TIME_RANGE);
    }

    @ParameterizedTest
    @MethodSource("provideValidLogLevels")
    void validateLogLevel_유효한_로그_레벨이면_검증을_통과한다(List<String> logLevels) {
        // given
        baseRequest.setLogLevel(logLevels);

        // when & then
        assertThatCode(() -> logValidator.validate(baseRequest))
                .doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideValidLogLevels() {
        return Stream.of(
                Arguments.of(Arrays.asList("ERROR", "WARN", "INFO")),
                Arguments.of(Arrays.asList("error", "WaRn", "INFO")),
                Arguments.of((List<String>) null),
                Arguments.of(Collections.emptyList())
        );
    }

    @Test
    void validateLogLevel_유효하지_않은_로그_레벨이면_예외를_발생시킨다() {
        // given
        baseRequest.setLogLevel(Arrays.asList("ERROR", "INVALID_LEVEL"));

        // when & then
        assertThatThrownBy(() -> logValidator.validate(baseRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", LogErrorCode.INVALID_LOG_LEVEL);
    }

    @ParameterizedTest
    @MethodSource("provideValidSourceTypes")
    void validateSourceType_유효한_소스_타입이면_검증을_통과한다(List<String> sourceTypes) {
        // given
        baseRequest.setSourceType(sourceTypes);

        // when & then
        assertThatCode(() -> logValidator.validate(baseRequest))
                .doesNotThrowAnyException();
    }

    private static Stream<Arguments> provideValidSourceTypes() {
        return Stream.of(
                Arguments.of(Arrays.asList("BE", "FE")),
                Arguments.of(Arrays.asList("be", "Fe")),
                Arguments.of((List<String>) null),
                Arguments.of(Collections.emptyList())
        );
    }

    @Test
    void validateSourceType_유효하지_않은_소스_타입이면_예외를_발생시킨다() {
        // given
        baseRequest.setSourceType(Arrays.asList("BE", "INVALID_TYPE"));

        // when & then
        assertThatThrownBy(() -> logValidator.validate(baseRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", LogErrorCode.INVALID_SOURCE_TYPE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TIMESTAMP,ASC", "TIMESTAMP,DESC", "timestamp,asc", "TimEsTamp,dEsC"})
    void validateSort_유효한_정렬_옵션이면_검증을_통과한다(String sort) {
        // given
        baseRequest.setSort(sort);

        // when & then
        assertThatCode(() -> logValidator.validate(baseRequest))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "TIMESTAMP",                // 쉼표 없음
            "INVALID_FIELD,ASC",        // 잘못된 필드
            "TIMESTAMP,INVALID",        // 잘못된 방향
            "TIMESTAMP,ASC,EXTRA"       // 파라미터 3개 이상
    })
    void validateSort_유효하지_않은_정렬_옵션이면_예외를_발생시킨다(String sort) {
        // given
        baseRequest.setSort(sort);

        // when & then
        assertThatThrownBy(() -> logValidator.validate(baseRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", LogErrorCode.INVALID_SORT);
    }
}
