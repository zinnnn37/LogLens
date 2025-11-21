package S13P31A306.loglens.domain.alert.mapper;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.domain.alert.entity.AlertHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AlertHistoryMapper 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AlertHistoryMapper 테스트")
class AlertHistoryMapperTest {

    @Autowired
    private AlertHistoryMapper alertHistoryMapper;

    private static final String PROJECT_UUID = "test-project-uuid";
    private static final Integer PROJECT_ID = 1;
    private static final String ALERT_MESSAGE = "에러 발생 건수가 임계값(10건)을 초과했습니다.";
    private static final String LOG_REFERENCE = "{\"logId\": 12345, \"errorCount\": 15}";
    private static final String ALERT_LEVEL = "ERROR";
    private static final String TRACE_ID = "trace-abc-123-xyz";

    @Nested
    @DisplayName("toResponse 메서드 테스트")
    class ToResponseTest {

        @Test
        @DisplayName("AlertHistory를_AlertHistoryResponse로_정상_변환한다")
        void toResponse_성공() {
            // given
            LocalDateTime alertTime = LocalDateTime.now();
            AlertHistory alertHistory = AlertHistory.builder()
                    .id(1)
                    .alertMessage(ALERT_MESSAGE)
                    .alertTime(alertTime)
                    .resolvedYN("N")
                    .logReference(LOG_REFERENCE)
                    .alertLevel(ALERT_LEVEL)
                    .traceId(TRACE_ID)
                    .projectId(PROJECT_ID)
                    .build();

            // when
            AlertHistoryResponse response = alertHistoryMapper.toResponse(alertHistory, PROJECT_UUID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1);
            assertThat(response.alertMessage()).isEqualTo(ALERT_MESSAGE);
            assertThat(response.alertTime()).isEqualTo(alertTime);
            assertThat(response.resolvedYN()).isEqualTo("N");
            assertThat(response.logReference()).isEqualTo(LOG_REFERENCE);
            assertThat(response.alertLevel()).isEqualTo(ALERT_LEVEL);
            assertThat(response.traceId()).isEqualTo(TRACE_ID);
            assertThat(response.projectUuid()).isEqualTo(PROJECT_UUID);
        }

        @Test
        @DisplayName("alertLevel이_null인_경우에도_정상_변환한다")
        void toResponse_alertLevel_null() {
            // given
            LocalDateTime alertTime = LocalDateTime.now();
            AlertHistory alertHistory = AlertHistory.builder()
                    .id(2)
                    .alertMessage(ALERT_MESSAGE)
                    .alertTime(alertTime)
                    .resolvedYN("Y")
                    .logReference(LOG_REFERENCE)
                    .alertLevel(null)
                    .traceId(null)
                    .projectId(PROJECT_ID)
                    .build();

            // when
            AlertHistoryResponse response = alertHistoryMapper.toResponse(alertHistory, PROJECT_UUID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(2);
            assertThat(response.alertLevel()).isNull();
            assertThat(response.traceId()).isNull();
            assertThat(response.resolvedYN()).isEqualTo("Y");
        }

        @Test
        @DisplayName("읽음_처리된_알림을_정상_변환한다")
        void toResponse_읽음처리() {
            // given
            AlertHistory alertHistory = AlertHistory.builder()
                    .id(3)
                    .alertMessage("경고 알림")
                    .alertTime(LocalDateTime.now())
                    .resolvedYN("Y")
                    .logReference("{}")
                    .alertLevel("WARN")
                    .traceId("trace-warn-456")
                    .projectId(PROJECT_ID)
                    .build();

            // when
            AlertHistoryResponse response = alertHistoryMapper.toResponse(alertHistory, PROJECT_UUID);

            // then
            assertThat(response.resolvedYN()).isEqualTo("Y");
            assertThat(response.alertLevel()).isEqualTo("WARN");
        }
    }

    @Nested
    @DisplayName("toResponseList 메서드 테스트")
    class ToResponseListTest {

        @Test
        @DisplayName("AlertHistory_리스트를_AlertHistoryResponse_리스트로_변환한다")
        void toResponseList_성공() {
            // given
            LocalDateTime now = LocalDateTime.now();
            AlertHistory alert1 = AlertHistory.builder()
                    .id(1)
                    .alertMessage("에러 알림 1")
                    .alertTime(now)
                    .resolvedYN("N")
                    .logReference("{\"logId\": 100}")
                    .alertLevel("ERROR")
                    .traceId("trace-1")
                    .projectId(PROJECT_ID)
                    .build();

            AlertHistory alert2 = AlertHistory.builder()
                    .id(2)
                    .alertMessage("경고 알림 2")
                    .alertTime(now.minusMinutes(10))
                    .resolvedYN("Y")
                    .logReference("{\"logId\": 200}")
                    .alertLevel("WARN")
                    .traceId("trace-2")
                    .projectId(PROJECT_ID)
                    .build();

            AlertHistory alert3 = AlertHistory.builder()
                    .id(3)
                    .alertMessage("정보 알림 3")
                    .alertTime(now.minusMinutes(20))
                    .resolvedYN("N")
                    .logReference("{\"logId\": 300}")
                    .alertLevel("INFO")
                    .traceId("trace-3")
                    .projectId(PROJECT_ID)
                    .build();

            List<AlertHistory> histories = Arrays.asList(alert1, alert2, alert3);

            // when
            List<AlertHistoryResponse> responses = alertHistoryMapper.toResponseList(histories, PROJECT_UUID);

            // then
            assertThat(responses).hasSize(3);
            assertThat(responses.get(0).id()).isEqualTo(1);
            assertThat(responses.get(0).alertLevel()).isEqualTo("ERROR");
            assertThat(responses.get(0).traceId()).isEqualTo("trace-1");
            assertThat(responses.get(0).projectUuid()).isEqualTo(PROJECT_UUID);

            assertThat(responses.get(1).id()).isEqualTo(2);
            assertThat(responses.get(1).alertLevel()).isEqualTo("WARN");
            assertThat(responses.get(1).traceId()).isEqualTo("trace-2");

            assertThat(responses.get(2).id()).isEqualTo(3);
            assertThat(responses.get(2).alertLevel()).isEqualTo("INFO");
            assertThat(responses.get(2).traceId()).isEqualTo("trace-3");
        }

        @Test
        @DisplayName("빈_리스트를_변환하면_빈_리스트를_반환한다")
        void toResponseList_빈리스트() {
            // given
            List<AlertHistory> emptyList = Collections.emptyList();

            // when
            List<AlertHistoryResponse> responses = alertHistoryMapper.toResponseList(emptyList, PROJECT_UUID);

            // then
            assertThat(responses).isEmpty();
        }

        @Test
        @DisplayName("다양한_alertLevel이_포함된_리스트를_변환한다")
        void toResponseList_다양한레벨() {
            // given
            AlertHistory errorAlert = AlertHistory.builder()
                    .id(1)
                    .alertMessage("ERROR 알림")
                    .alertTime(LocalDateTime.now())
                    .resolvedYN("N")
                    .logReference("{}")
                    .alertLevel("ERROR")
                    .traceId("trace-error")
                    .projectId(PROJECT_ID)
                    .build();

            AlertHistory warnAlert = AlertHistory.builder()
                    .id(2)
                    .alertMessage("WARN 알림")
                    .alertTime(LocalDateTime.now())
                    .resolvedYN("N")
                    .logReference("{}")
                    .alertLevel("WARN")
                    .traceId("trace-warn")
                    .projectId(PROJECT_ID)
                    .build();

            AlertHistory infoAlert = AlertHistory.builder()
                    .id(3)
                    .alertMessage("INFO 알림")
                    .alertTime(LocalDateTime.now())
                    .resolvedYN("N")
                    .logReference("{}")
                    .alertLevel("INFO")
                    .traceId("trace-info")
                    .projectId(PROJECT_ID)
                    .build();

            List<AlertHistory> histories = Arrays.asList(errorAlert, warnAlert, infoAlert);

            // when
            List<AlertHistoryResponse> responses = alertHistoryMapper.toResponseList(histories, PROJECT_UUID);

            // then
            assertThat(responses).hasSize(3);
            assertThat(responses.stream().map(AlertHistoryResponse::alertLevel))
                    .containsExactly("ERROR", "WARN", "INFO");
            assertThat(responses.stream().map(AlertHistoryResponse::traceId))
                    .containsExactly("trace-error", "trace-warn", "trace-info");
        }
    }
}
