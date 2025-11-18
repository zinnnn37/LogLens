package S13P31A306.loglens.domain.statistics.mapper;

import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogTrendMapper 테스트
 */
@SpringBootTest
@DisplayName("LogTrendMapper 테스트")
class LogTrendMapperTest {

    @Autowired
    private LogTrendMapper logTrendMapper;

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

    @Test
    void 전체_응답을_생성한다() {
        // given
        String projectUuid = "test-uuid";
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);

        List<LogTrendAggregation> aggregations = List.of(
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 15, 0), 1523, 1200, 250, 73),
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 18, 0), 1820, 1450, 280, 90)
        );

        // when
        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
                projectUuid, startTime, endTime, aggregations
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.projectUuid()).isEqualTo(projectUuid);
        assertThat(response.dataPoints()).hasSize(8);  // 24시간 / 3시간 간격 = 8개 고정
        assertThat(response.interval()).isEqualTo("3h");

        // 실제 데이터가 있는 시간대 검증
        assertThat(response.dataPoints().get(0).totalCount()).isEqualTo(1523);  // 15:00 실제 데이터
        assertThat(response.dataPoints().get(1).totalCount()).isEqualTo(1820);  // 18:00 실제 데이터

        // 빈 데이터가 0으로 채워졌는지 검증
        assertThat(response.dataPoints().get(2).totalCount()).isEqualTo(0);     // 21:00 빈 데이터
        assertThat(response.dataPoints().get(7).totalCount()).isEqualTo(0);     // 12:00 빈 데이터

        assertThat(response.summary()).isNotNull();
        assertThat(response.summary().totalLogs()).isEqualTo(3343); // 1523 + 1820
    }

    @Test
    void Summary가_올바르게_계산된다() {
        // given
        String projectUuid = "test-uuid";
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);

        List<LogTrendAggregation> aggregations = List.of(
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 15, 0), 1000, 800, 150, 50),
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 18, 0), 1500, 1200, 200, 100),
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 21, 0), 2000, 1600, 300, 100),
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 14, 0, 0), 1200, 1000, 150, 50)
        );

        // when
        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
                projectUuid, startTime, endTime, aggregations
        );

        // then
        LogTrendResponse.Summary summary = response.summary();
        assertThat(summary.totalLogs()).isEqualTo(5700); // 1000 + 1500 + 2000 + 1200
        assertThat(summary.avgLogsPerInterval()).isEqualTo(1425); // 5700 / 4
        assertThat(summary.peakHour()).isEqualTo("21:00"); // 2000개로 최대
        assertThat(summary.peakCount()).isEqualTo(2000);
    }

    @Test
    void 로그가_없는_경우_0으로_처리된다() {
        // given
        String projectUuid = "test-uuid";
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);

        List<LogTrendAggregation> aggregations = List.of(
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 15, 0), 0, 0, 0, 0)
        );

        // when
        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
                projectUuid, startTime, endTime, aggregations
        );

        // then
        assertThat(response.summary().totalLogs()).isZero();
        assertThat(response.summary().avgLogsPerInterval()).isZero();
    }

    @Test
    @DisplayName("타임스탬프가_정확히_일치하지_않아도_같은_시간대로_매칭된다")
    void 타임스탬프가_정확히_일치하지_않아도_같은_시간대로_매칭된다() {
        // given - OpenSearch에서 반환되는 타임스탬프가 정확히 시간 정각이 아닌 경우를 시뮬레이션
        String projectUuid = "test-uuid";
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);  // 정각
        LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 15, 0);

        // OpenSearch가 15:30:45, 18:15:30 등 정각이 아닌 타임스탬프를 반환하는 경우
        List<LogTrendAggregation> aggregations = List.of(
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 15, 30, 45), 1523, 1200, 250, 73),  // 15시대
                new LogTrendAggregation(LocalDateTime.of(2025, 11, 13, 18, 15, 30), 1820, 1450, 280, 90)   // 18시대
        );

        // when
        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
                projectUuid, startTime, endTime, aggregations
        );

        // then - 시간 단위로 truncate되어 매칭되어야 함
        assertThat(response.dataPoints()).hasSize(8);

        // 15:00 슬롯에 15:30:45 데이터가 매칭됨
        assertThat(response.dataPoints().get(0).totalCount()).isEqualTo(1523);
        assertThat(response.dataPoints().get(0).infoCount()).isEqualTo(1200);
        assertThat(response.dataPoints().get(0).warnCount()).isEqualTo(250);
        assertThat(response.dataPoints().get(0).errorCount()).isEqualTo(73);

        // 18:00 슬롯에 18:15:30 데이터가 매칭됨
        assertThat(response.dataPoints().get(1).totalCount()).isEqualTo(1820);
        assertThat(response.dataPoints().get(1).infoCount()).isEqualTo(1450);
        assertThat(response.dataPoints().get(1).warnCount()).isEqualTo(280);
        assertThat(response.dataPoints().get(1).errorCount()).isEqualTo(90);

        // 나머지 슬롯은 0
        assertThat(response.dataPoints().get(2).totalCount()).isEqualTo(0);

        // Summary도 정상 계산
        assertThat(response.summary().totalLogs()).isEqualTo(3343);
        assertThat(response.summary().peakHour()).isIn("15:30", "18:15");  // 원래 timestamp의 시간
    }
}

