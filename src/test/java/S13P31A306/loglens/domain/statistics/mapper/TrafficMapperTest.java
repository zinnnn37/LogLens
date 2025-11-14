//package S13P31A306.loglens.domain.statistics.mapper;
//
//import S13P31A306.loglens.domain.statistics.dto.internal.TrafficAggregation;
//import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * TrafficMapper 테스트
// */
//@SpringBootTest
//@ActiveProfiles("test")
//@DisplayName("TrafficMapper 테스트")
//class TrafficMapperTest {
//
//    @Autowired
//    private TrafficMapper trafficMapper;
//
//    private static final String PROJECT_UUID = "test-project-uuid";
//
//    @Test
//    void TrafficAggregation을_DataPoint로_변환한다() {
//        // given
//        LocalDateTime timestamp = LocalDateTime.of(2025, 11, 13, 15, 0);
//        TrafficAggregation aggregation = new TrafficAggregation(timestamp, 1500, 800, 700);
//
//        // when
//        TrafficResponse.DataPoint dataPoint = trafficMapper.toDataPoint(aggregation);
//
//        // then
//        assertThat(dataPoint).isNotNull();
//        assertThat(dataPoint.timestamp()).isNotBlank();
//        assertThat(dataPoint.hour()).isEqualTo("15:00");
//        assertThat(dataPoint.totalCount()).isEqualTo(1500);
//        assertThat(dataPoint.feCount()).isEqualTo(800);
//        assertThat(dataPoint.beCount()).isEqualTo(700);
//    }
//
//    @Test
//    void hour_필드가_HH_mm_형식이다() {
//        // given
//        LocalDateTime timestamp = LocalDateTime.of(2025, 11, 13, 9, 30);
//        TrafficAggregation aggregation = new TrafficAggregation(timestamp, 1000, 500, 500);
//
//        // when
//        TrafficResponse.DataPoint dataPoint = trafficMapper.toDataPoint(aggregation);
//
//        // then
//        assertThat(dataPoint.hour()).matches("\\d{2}:\\d{2}");
//        assertThat(dataPoint.hour()).isEqualTo("09:30");
//    }
//
//    @Test
//    void TrafficResponse로_변환_시_8개의_DataPoint를_포함한다() {
//        // given
//        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LocalDateTime endTime = startTime.plusHours(24);
//        List<TrafficAggregation> aggregations = create8Aggregations();
//
//        // when
//        TrafficResponse response = trafficMapper.toTrafficResponse(
//                PROJECT_UUID, startTime, endTime, aggregations
//        );
//
//        // then
//        assertThat(response.dataPoints()).hasSize(8);
//        assertThat(response.projectUuid()).isEqualTo(PROJECT_UUID);
//        assertThat(response.interval()).isEqualTo("3h");
//    }
//
//    @Test
//    void Summary의_totalLogs가_모든_DataPoint의_합이다() {
//        // given
//        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LocalDateTime endTime = startTime.plusHours(24);
//        List<TrafficAggregation> aggregations = create8Aggregations();
//
//        // when
//        TrafficResponse response = trafficMapper.toTrafficResponse(
//                PROJECT_UUID, startTime, endTime, aggregations
//        );
//
//        // then
//        int expectedTotal = aggregations.stream()
//                .mapToInt(TrafficAggregation::totalCount)
//                .sum();
//        assertThat(response.summary().totalLogs()).isEqualTo(expectedTotal);
//    }
//
//    @Test
//    void Summary의_totalFeCount와_totalBeCount가_올바르다() {
//        // given
//        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LocalDateTime endTime = startTime.plusHours(24);
//        List<TrafficAggregation> aggregations = create8Aggregations();
//
//        // when
//        TrafficResponse response = trafficMapper.toTrafficResponse(
//                PROJECT_UUID, startTime, endTime, aggregations
//        );
//
//        // then
//        int expectedFeCount = aggregations.stream()
//                .mapToInt(TrafficAggregation::feCount)
//                .sum();
//        int expectedBeCount = aggregations.stream()
//                .mapToInt(TrafficAggregation::beCount)
//                .sum();
//
//        assertThat(response.summary().totalFeCount()).isEqualTo(expectedFeCount);
//        assertThat(response.summary().totalBeCount()).isEqualTo(expectedBeCount);
//    }
//
//    @Test
//    void Summary의_peakHour와_peakCount가_올바르다() {
//        // given
//        LocalDateTime startTime = LocalDateTime.of(2025, 11, 13, 15, 0);
//        LocalDateTime endTime = startTime.plusHours(24);
//        List<TrafficAggregation> aggregations = create8Aggregations();
//
//        // when
//        TrafficResponse response = trafficMapper.toTrafficResponse(
//                PROJECT_UUID, startTime, endTime, aggregations
//        );
//
//        // then
//        // 마지막 aggregation이 가장 큰 값 (1500 + 7*100 = 2200)
//        assertThat(response.summary().peakCount()).isEqualTo(2200);
//        assertThat(response.summary().peakHour()).isNotNull();
//    }
//
//    // 헬퍼 메서드
//    private List<TrafficAggregation> create8Aggregations() {
//        List<TrafficAggregation> aggregations = new ArrayList<>();
//        LocalDateTime start = LocalDateTime.of(2025, 11, 13, 15, 0);
//
//        for (int i = 0; i < 8; i++) {
//            aggregations.add(new TrafficAggregation(
//                    start.plusHours(i * 3),
//                    1500 + i * 100,  // totalCount 증가
//                    800 + i * 50,    // feCount 증가
//                    700 + i * 50     // beCount 증가
//            ));
//        }
//
//        return aggregations;
//    }
//}
