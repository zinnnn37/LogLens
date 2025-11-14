//package S13P31A306.loglens.domain.statistics.service.impl;
//
//import S13P31A306.loglens.domain.log.repository.LogRepository;
//import S13P31A306.loglens.domain.project.entity.Project;
//import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
//import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
//import S13P31A306.loglens.domain.statistics.mapper.LogTrendMapper;
//import S13P31A306.loglens.domain.statistics.validator.StatisticsValidator;
//import S13P31A306.loglens.global.exception.BusinessException;
//
//import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.MEMBER_NOT_FOUND;
//import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.BDDMockito.willDoNothing;
//import static org.mockito.BDDMockito.willThrow;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//
///**
// * LogTrendServiceImpl 단위 테스트
// */
//@ExtendWith(MockitoExtension.class)
//@DisplayName("LogTrendServiceImpl 테스트")
//class LogTrendServiceImplTest {
//
//    @InjectMocks
//    private LogTrendServiceImpl logTrendService;
//
//    @Mock
//    private LogRepository logRepository;
//
//    @Mock
//    private LogTrendMapper logTrendMapper;
//
//    @Mock
//    private StatisticsValidator statisticsValidator;
//
//    private static final String PROJECT_UUID = "test-project-uuid";
//    private static final Integer USER_ID = 1;
//    private static final Integer PROJECT_ID = 1;
//
//    @Nested
//    @DisplayName("로그 추이 조회 테스트")
//    class GetLogTrendTest {
//
//        @Test
//        void 로그_추이_조회_성공_시_8개의_데이터포인트를_반환한다() {
//            // given
//            Project project = createProject();
//            List<LogTrendAggregation> aggregations = create8Aggregations();
//            LogTrendResponse expectedResponse = createResponse();
//
//            given(statisticsValidator.validateLogTrendRequest(PROJECT_UUID)).willReturn(project);
//            given(logRepository.aggregateLogTrendByTimeRange(
//                    eq(PROJECT_UUID), any(), any(), eq("3h")
//            )).willReturn(aggregations);
//            given(logTrendMapper.toLogTrendResponse(
//                    eq(PROJECT_UUID), any(), any(), eq(aggregations)
//            )).willReturn(expectedResponse);
//
//            // when
//            LogTrendResponse response = logTrendService.getLogTrend(PROJECT_UUID);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.dataPoints()).hasSize(8);
//            verify(statisticsValidator).validateLogTrendRequest(PROJECT_UUID);
//        }
//
//        @Test
//        void 각_데이터포인트의_hour_필드가_HH_mm_형식이다() {
//            // given
//            Project project = createProject();
//            List<LogTrendAggregation> aggregations = create8Aggregations();
//            LogTrendResponse expectedResponse = createResponse();
//
//            given(statisticsValidator.validateLogTrendRequest(PROJECT_UUID)).willReturn(project);
//            given(logRepository.aggregateLogTrendByTimeRange(
//                    any(), any(), any(), any()
//            )).willReturn(aggregations);
//            given(logTrendMapper.toLogTrendResponse(
//                    any(), any(), any(), any()
//            )).willReturn(expectedResponse);
//
//            // when
//            LogTrendResponse response = logTrendService.getLogTrend(PROJECT_UUID);
//
//            // then
//            response.dataPoints().forEach(dataPoint -> {
//                assertThat(dataPoint.hour()).matches("\\d{2}:\\d{2}");
//            });
//        }
//
//        @Test
//        void 프로젝트가_존재하지_않으면_예외를_발생시킨다() {
//            // given
//            given(statisticsValidator.validateLogTrendRequest(PROJECT_UUID))
//                    .willThrow(new BusinessException(PROJECT_NOT_FOUND));
//
//            // when & then
//            assertThatThrownBy(() -> logTrendService.getLogTrend(PROJECT_UUID))
//                    .isInstanceOf(BusinessException.class);
//        }
//
//        @Test
//        void 사용자가_프로젝트_멤버가_아니면_예외를_발생시킨다() {
//            // given
//            given(statisticsValidator.validateLogTrendRequest(PROJECT_UUID))
//                    .willThrow(new BusinessException(MEMBER_NOT_FOUND));
//
//            // when & then
//            assertThatThrownBy(() -> logTrendService.getLogTrend(PROJECT_UUID))
//                    .isInstanceOf(BusinessException.class);
//        }
//
//        @Test
//        void 로그가_없는_경우_0으로_채워진_8개_데이터를_반환한다() {
//            // given
//            Project project = createProject();
//            List<LogTrendAggregation> emptyAggregations = createEmptyAggregations();
//            LogTrendResponse expectedResponse = createEmptyResponse();
//
//            given(statisticsValidator.validateLogTrendRequest(PROJECT_UUID)).willReturn(project);
//            given(logRepository.aggregateLogTrendByTimeRange(
//                    any(), any(), any(), any()
//            )).willReturn(emptyAggregations);
//            given(logTrendMapper.toLogTrendResponse(
//                    any(), any(), any(), any()
//            )).willReturn(expectedResponse);
//
//            // when
//            LogTrendResponse response = logTrendService.getLogTrend(PROJECT_UUID);
//
//            // then
//            assertThat(response.dataPoints()).hasSize(8);
//            response.dataPoints().forEach(dataPoint -> {
//                assertThat(dataPoint.totalCount()).isZero();
//                assertThat(dataPoint.infoCount()).isZero();
//                assertThat(dataPoint.warnCount()).isZero();
//                assertThat(dataPoint.errorCount()).isZero();
//            });
//        }
//    }
//
//    // 헬퍼 메서드들
//    private Project createProject() {
//        return mock(Project.class);
//    }
//
//    private List<LogTrendAggregation> create8Aggregations() {
//        List<LogTrendAggregation> aggregations = new ArrayList<>();
//        LocalDateTime start = LocalDateTime.of(2025, 11, 13, 15, 0);
//
//        for (int i = 0; i < 8; i++) {
//            aggregations.add(new LogTrendAggregation(
//                    start.plusHours(i * 3),
//                    1500 + i * 100,
//                    1200,
//                    250,
//                    50
//            ));
//        }
//
//        return aggregations;
//    }
//
//    private List<LogTrendAggregation> createEmptyAggregations() {
//        List<LogTrendAggregation> aggregations = new ArrayList<>();
//        LocalDateTime start = LocalDateTime.of(2025, 11, 13, 15, 0);
//
//        for (int i = 0; i < 8; i++) {
//            aggregations.add(new LogTrendAggregation(
//                    start.plusHours(i * 3),
//                    0, 0, 0, 0
//            ));
//        }
//
//        return aggregations;
//    }
//
//    private LogTrendResponse createResponse() {
//        // 8개의 DataPoint 생성
//        List<LogTrendResponse.DataPoint> dataPoints = new ArrayList<>();
//        LocalDateTime start = LocalDateTime.of(2025, 11, 13, 15, 0);
//
//        for (int i = 0; i < 8; i++) {
//            dataPoints.add(new LogTrendResponse.DataPoint(
//                    start.plusHours(i * 3).toString(),
//                    String.format("%02d:00", (15 + i * 3) % 24),
//                    1500 + i * 100,
//                    1200, 250, 50
//            ));
//        }
//
//        return new LogTrendResponse(
//                PROJECT_UUID,
//                new LogTrendResponse.Period(start.toString(), start.plusHours(24).toString()),
//                "3h",
//                dataPoints,
//                new LogTrendResponse.Summary(12000, 1500, "12:00", 2100)
//        );
//    }
//
//    private LogTrendResponse createEmptyResponse() {
//        List<LogTrendResponse.DataPoint> dataPoints = new ArrayList<>();
//        LocalDateTime start = LocalDateTime.of(2025, 11, 13, 15, 0);
//
//        for (int i = 0; i < 8; i++) {
//            dataPoints.add(new LogTrendResponse.DataPoint(
//                    start.plusHours(i * 3).toString(),
//                    String.format("%02d:00", (15 + i * 3) % 24),
//                    0, 0, 0, 0
//            ));
//        }
//
//        return new LogTrendResponse(
//                PROJECT_UUID,
//                new LogTrendResponse.Period(start.toString(), start.plusHours(24).toString()),
//                "3h",
//                dataPoints,
//                new LogTrendResponse.Summary(0, 0, "15:00", 0)
//        );
//    }
//}
