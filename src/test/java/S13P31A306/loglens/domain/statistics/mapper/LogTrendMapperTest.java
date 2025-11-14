//package S13P31A306.loglens.domain.statistics.mapper;
//
//import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
//import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * LogTrendMapper 테스트
// */
//@SpringBootTest
//@DisplayName("LogTrendMapper 테스트")
//class LogTrendMapperTest {
//
//    @Autowired
//    private LogTrendMapper logTrendMapper;
//
//    @Test
//    void LogTrendAggregation을_DataPoint로_변환한다() {
//        // given
//        LocalDateTime timestamp = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LogTrendAggregation aggregation = new LogTrendAggregation(
//                timestamp, 1523, 1200, 250, 73
//        );
//
//        // when
//        LogTrendResponse.DataPoint dataPoint = logTrendMapper.toDataPoint(aggregation);
//
//        // then
//        assertThat(dataPoint).isNotNull();
//        assertThat(dataPoint.hour()).isEqualTo("15:00");
//        assertThat(dataPoint.totalCount()).isEqualTo(1523);
//        assertThat(dataPoint.infoCount()).isEqualTo(1200);
//        assertThat(dataPoint.warnCount()).isEqualTo(250);
//        assertThat(dataPoint.errorCount()).isEqualTo(73);
//    }
//
//    @Test
//    void hour_필드가_HH_mm_형식으로_변환된다() {
//        // given
//        LocalDateTime timestamp = LocalDateTime.of(2025, 11, 14, 3, 0);
//        LogTrendAggregation aggregation = new LogTrendAggregation(
//                timestamp, 750, 650, 80, 20
//        );
//
//        // when
//        LogTrendResponse.DataPoint dataPoint = logTrendMapper.toDataPoint(aggregation);
//
//        // then
//        assertThat(dataPoint.hour()).isEqualTo("03:00");
//    }
//
//    @Test
//    void timestamp가_올바르게_ISO_8601로_변환된다() {
//        // given
//        LocalDateTime timestamp = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LogTrendAggregation aggregation = new LogTrendAggregation(
//                timestamp, 1523, 1200, 250, 73
//        );
//
//        // when
//        LogTrendResponse.DataPoint dataPoint = logTrendMapper.toDataPoint(aggregation);
//
//        // then
//        assertThat(dataPoint.timestamp()).contains("2025-11-13T15:00:00");
//        assertThat(dataPoint.timestamp()).contains("+09:00"); // Asia/Seoul 타임존
//    }
//
//    @Test
//    void 전체_응답을_생성한다() {
//        // given
//        String projectUuid = "test-uuid";
//        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);
//
//        List<LogTrendAggregation> aggregations = List.of(
//                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 15, 0), 1523, 1200, 250, 73),
//                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 18, 0), 1820, 1450, 280, 90)
//        );
//
//        // when
//        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
//                projectUuid, startTime, endTime, aggregations
//        );
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.projectUuid()).isEqualTo(projectUuid);
//        assertThat(response.dataPoints()).hasSize(2);
//        assertThat(response.interval()).isEqualTo("3h");
//        assertThat(response.summary()).isNotNull();
//        assertThat(response.summary().totalLogs()).isEqualTo(3343); // 1523 + 1820
//    }
//
//    @Test
//    void Summary가_올바르게_계산된다() {
//        // given
//        String projectUuid = "test-uuid";
//        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);
//
//        List<LogTrendAggregation> aggregations = List.of(
//                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 15, 0), 1000, 800, 150, 50),
//                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 18, 0), 1500, 1200, 200, 100),
//                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 21, 0), 2000, 1600, 300, 100),
//                new LogTrendAggregation(LocalDateTime.of(2025, 11, 14, 0, 0), 1200, 1000, 150, 50)
//        );
//
//        // when
//        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
//                projectUuid, startTime, endTime, aggregations
//        );
//
//        // then
//        LogTrendResponse.Summary summary = response.summary();
//        assertThat(summary.totalLogs()).isEqualTo(5700); // 1000 + 1500 + 2000 + 1200
//        assertThat(summary.avgLogsPerInterval()).isEqualTo(1425); // 5700 / 4
//        assertThat(summary.peakHour()).isEqualTo("21:00"); // 2000개로 최대
//        assertThat(summary.peakCount()).isEqualTo(2000);
//    }
//
//    @Test
//    void 로그가_없는_경우_0으로_처리된다() {
//        // given
//        String projectUuid = "test-uuid";
//        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);
//
//        List<LogTrendAggregation> aggregations = List.of(
//                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 15, 0), 0, 0, 0, 0)
//        );
//
//        // when
//        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
//                projectUuid, startTime, endTime, aggregations
//        );
//
//        // then
//        assertThat(response.summary().totalLogs()).isZero();
//        assertThat(response.summary().avgLogsPerInterval()).isZero();
//    }
//}
