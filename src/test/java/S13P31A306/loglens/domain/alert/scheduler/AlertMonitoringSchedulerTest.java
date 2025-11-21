//package S13P31A306.loglens.domain.alert.scheduler;
//
//import S13P31A306.loglens.domain.alert.service.AlertMonitoringService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.assertj.core.api.Assertions.assertThatCode;
//import static org.mockito.BDDMockito.willDoNothing;
//import static org.mockito.BDDMockito.willThrow;
//import static org.mockito.Mockito.verify;
//
//@ExtendWith(MockitoExtension.class)
//class AlertMonitoringSchedulerTest {
//
//    private AlertMonitoringScheduler alertMonitoringScheduler;
//
//    @Mock
//    private AlertMonitoringService alertMonitoringService;
//
//    @BeforeEach
//    void setUp() {
//        alertMonitoringScheduler = new AlertMonitoringScheduler(alertMonitoringService);
//    }
//
//    @Nested
//    class CheckAlertsTest {
//
//        @Test
//        void 스케줄러가_서비스를_호출한다() {
//            // given
//            willDoNothing().given(alertMonitoringService).checkAndCreateAlerts();
//
//            // when
//            alertMonitoringScheduler.checkAlerts();
//
//            // then
//            verify(alertMonitoringService).checkAndCreateAlerts();
//        }
//
//        @Test
//        void 서비스_실행_중_예외가_발생해도_스케줄러는_중단되지_않는다() {
//            // given
//            willThrow(new RuntimeException("서비스 실행 중 오류"))
//                    .given(alertMonitoringService).checkAndCreateAlerts();
//
//            // when & then
//            assertThatCode(() -> alertMonitoringScheduler.checkAlerts())
//                    .doesNotThrowAnyException();
//
//            verify(alertMonitoringService).checkAndCreateAlerts();
//        }
//
//        @Test
//        void 예외_발생_시에도_서비스_호출은_완료된다() {
//            // given
//            willThrow(new RuntimeException("서비스 실행 중 오류"))
//                    .given(alertMonitoringService).checkAndCreateAlerts();
//
//            // when
//            alertMonitoringScheduler.checkAlerts();
//
//            // then
//            verify(alertMonitoringService).checkAndCreateAlerts();
//        }
//    }
//}
