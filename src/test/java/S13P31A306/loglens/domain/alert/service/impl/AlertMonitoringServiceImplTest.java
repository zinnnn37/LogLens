//package S13P31A306.loglens.domain.alert.service.impl;
//
//import S13P31A306.loglens.domain.alert.entity.AlertType;
//import S13P31A306.loglens.domain.alert.entity.AlertConfig;
//import S13P31A306.loglens.domain.alert.entity.AlertHistory;
//import S13P31A306.loglens.domain.alert.repository.AlertConfigRepository;
//import S13P31A306.loglens.domain.alert.repository.AlertHistoryRepository;
//import S13P31A306.loglens.domain.log.repository.LogRepository;
//import S13P31A306.loglens.domain.project.entity.Project;
//import S13P31A306.loglens.domain.project.repository.ProjectRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.lang.reflect.Field;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AlertMonitoringServiceImplTest {
//
//    private AlertMonitoringServiceImpl alertMonitoringService;
//
//    @Mock
//    private ProjectRepository projectRepository;
//
//    @Mock
//    private AlertConfigRepository alertConfigRepository;
//
//    @Mock
//    private AlertHistoryRepository alertHistoryRepository;
//
//    @Mock
//    private LogRepository logRepository;
//
//    private static final Integer PROJECT_ID_1 = 1;
//    private static final Integer PROJECT_ID_2 = 2;
//    private static final String PROJECT_UUID_1 = "project-uuid-1";
//    private static final String PROJECT_UUID_2 = "project-uuid-2";
//    private static final int ERROR_THRESHOLD = 10;
//    private static final int ERROR_COUNT_BELOW = 5;
//    private static final int ERROR_COUNT_ABOVE = 15;
//
//    @BeforeEach
//    void setUp() {
//        alertMonitoringService = new AlertMonitoringServiceImpl(
//                projectRepository,
//                alertConfigRepository,
//                alertHistoryRepository,
//                logRepository
//        );
//    }
//
//    @Nested
//    class CheckAndCreateAlertsTest {
//
//        @Test
//        void 모든_프로젝트를_순회하며_알림_체크를_수행한다() {
//            // given
//            List<Project> projects = Arrays.asList(
//                    createProject(PROJECT_ID_1, PROJECT_UUID_1),
//                    createProject(PROJECT_ID_2, PROJECT_UUID_2)
//            );
//            given(projectRepository.findAll()).willReturn(projects);
//            given(alertConfigRepository.findByProjectId(any())).willReturn(Optional.empty());
//
//            // when
//            alertMonitoringService.checkAndCreateAlerts();
//
//            // then
//            verify(projectRepository).findAll();
//            verify(alertConfigRepository, times(2)).findByProjectId(any());
//        }
//
//        @Test
//        void 프로젝트별_오류가_발생해도_다른_프로젝트는_계속_처리된다() {
//            // given
//            List<Project> projects = Arrays.asList(
//                    createProject(PROJECT_ID_1, PROJECT_UUID_1),
//                    createProject(PROJECT_ID_2, PROJECT_UUID_2)
//            );
//            given(projectRepository.findAll()).willReturn(projects);
//
//            // 첫 번째 프로젝트는 예외 발생
//            given(alertConfigRepository.findByProjectId(PROJECT_ID_1))
//                    .willThrow(new RuntimeException("Test exception"));
//
//            // 두 번째 프로젝트는 정상
//            given(alertConfigRepository.findByProjectId(PROJECT_ID_2))
//                    .willReturn(Optional.empty());
//
//            // when
//            alertMonitoringService.checkAndCreateAlerts();
//
//            // then
//            verify(alertConfigRepository).findByProjectId(PROJECT_ID_1);
//            verify(alertConfigRepository).findByProjectId(PROJECT_ID_2);
//        }
//
//        @Test
//        void 체크된_프로젝트_개수를_로깅한다() {
//            // given
//            List<Project> projects = Arrays.asList(
//                    createProject(PROJECT_ID_1, PROJECT_UUID_1),
//                    createProject(PROJECT_ID_2, PROJECT_UUID_2)
//            );
//            given(projectRepository.findAll()).willReturn(projects);
//            given(alertConfigRepository.findByProjectId(any())).willReturn(Optional.empty());
//
//            // when
//            alertMonitoringService.checkAndCreateAlerts();
//
//            // then
//            verify(projectRepository).findAll();
//        }
//    }
//
//    @Nested
//    class AlertConfigValidationTest {
//
//        private Project project;
//
//        @BeforeEach
//        void setUp() {
//            project = createProject(PROJECT_ID_1, PROJECT_UUID_1);
//        }
//
//        @Test
//        void AlertConfig가_없으면_false를_반환한다() {
//            // given
//            given(alertConfigRepository.findByProjectId(PROJECT_ID_1))
//                    .willReturn(Optional.empty());
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isFalse();
//            verify(logRepository, never())
//                    .countErrorLogsByProjectUuidAndTimeRange(any(), any(), any());
//        }
//
//        @Test
//        void AlertConfig가_비활성화_상태면_false를_반환한다() {
//            // given
//            AlertConfig inactiveConfig = createAlertConfig(PROJECT_ID_1, "N", ERROR_THRESHOLD);
//            given(alertConfigRepository.findByProjectId(PROJECT_ID_1))
//                    .willReturn(Optional.of(inactiveConfig));
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isFalse();
//            verify(logRepository, never())
//                    .countErrorLogsByProjectUuidAndTimeRange(any(), any(), any());
//        }
//
//        @Test
//        void AlertConfig가_활성화_상태면_OpenSearch를_조회한다() {
//            // given
//            AlertConfig activeConfig = createAlertConfig(PROJECT_ID_1, "Y", ERROR_THRESHOLD);
//            given(alertConfigRepository.findByProjectId(PROJECT_ID_1))
//                    .willReturn(Optional.of(activeConfig));
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willReturn(0L);
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isFalse();
//            verify(logRepository)
//                    .countErrorLogsByProjectUuidAndTimeRange(
//                            eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class));
//        }
//
//        @Test
//        void 지원하지_않는_알림_타입이면_false를_반환한다() {
//            // given
//            AlertConfig latencyConfig = AlertConfig.builder()
//                    .projectId(PROJECT_ID_1)
//                    .alertType(AlertType.LATENCY)
//                    .thresholdValue(ERROR_THRESHOLD)
//                    .activeYN("Y")
//                    .build();
//            given(alertConfigRepository.findByProjectId(PROJECT_ID_1))
//                    .willReturn(Optional.of(latencyConfig));
//
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willReturn((long) ERROR_COUNT_ABOVE);
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isFalse();
//            verify(alertHistoryRepository, never()).save(any());
//        }
//    }
//
//    @Nested
//    class ErrorCountQueryTest {
//
//        private Project project;
//        private AlertConfig config;
//
//        @BeforeEach
//        void setUp() {
//            project = createProject(PROJECT_ID_1, PROJECT_UUID_1);
//            config = createAlertConfig(PROJECT_ID_1, "Y", ERROR_THRESHOLD);
//            given(alertConfigRepository.findByProjectId(PROJECT_ID_1))
//                    .willReturn(Optional.of(config));
//        }
//
//        @Test
//        void OpenSearch_조회_실패시_false를_반환한다() {
//            // given
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willThrow(new RuntimeException("OpenSearch connection failed"));
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isFalse();
//            verify(alertHistoryRepository, never()).save(any());
//        }
//
//        @Test
//        void ERROR_개수가_조회되면_임계값_체크를_수행한다() {
//            // given
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willReturn((long) ERROR_COUNT_BELOW);
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isFalse(); // 임계값 미만이므로 false
//            verify(alertHistoryRepository, never()).save(any());
//        }
//    }
//
//    @Nested
//    class ErrorThresholdCheckTest {
//
//        private Project project;
//        private AlertConfig config;
//
//        @BeforeEach
//        void setUp() {
//            project = createProject(PROJECT_ID_1, PROJECT_UUID_1);
//            config = createAlertConfig(PROJECT_ID_1, "Y", ERROR_THRESHOLD);
//            given(alertConfigRepository.findByProjectId(PROJECT_ID_1))
//                    .willReturn(Optional.of(config));
//        }
//
//        @Test
//        void 에러_개수가_임계값_미만이면_false를_반환한다() {
//            // given
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willReturn((long) ERROR_COUNT_BELOW);
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isFalse();
//            verify(alertHistoryRepository, never())
//                    .findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(any(), any());
//            verify(alertHistoryRepository, never()).save(any());
//        }
//
//        @Test
//        void 에러_개수가_임계값_이상이면_중복_체크를_수행한다() {
//            // given
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willReturn((long) ERROR_COUNT_ABOVE);
//            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
//                    any(), any())).willReturn(Collections.emptyList());
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isTrue();
//            verify(alertHistoryRepository)
//                    .findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(eq(PROJECT_ID_1), any());
//        }
//
//        @Test
//        void 최근_5분_이내_중복_알림이_있으면_false를_반환한다() {
//            // given
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willReturn((long) ERROR_COUNT_ABOVE);
//
//            AlertHistory recentAlert = createAlertHistory(PROJECT_ID_1,
//                    LocalDateTime.now().minusMinutes(2));
//            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
//                    any(), any())).willReturn(Collections.singletonList(recentAlert));
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isFalse();
//            verify(alertHistoryRepository, never()).save(any());
//        }
//
//        @Test
//        void 최근_5분_이내_중복_알림이_없으면_AlertHistory를_생성하고_true를_반환한다() {
//            // given
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willReturn((long) ERROR_COUNT_ABOVE);
//            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
//                    any(), any())).willReturn(Collections.emptyList());
//
//            // when
//            boolean result = alertMonitoringService
//                    .checkProjectAlertsInNewTransaction(project);
//
//            // then
//            assertThat(result).isTrue();
//            verify(alertHistoryRepository).save(any(AlertHistory.class));
//        }
//
//        @Test
//        void 생성된_AlertHistory가_올바른_필드값을_가진다() {
//            // given
//            given(logRepository.countErrorLogsByProjectUuidAndTimeRange(
//                    eq(PROJECT_UUID_1), any(LocalDateTime.class), any(LocalDateTime.class)))
//                    .willReturn((long) ERROR_COUNT_ABOVE);
//            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
//                    any(), any())).willReturn(Collections.emptyList());
//
//            // when
//            alertMonitoringService.checkProjectAlertsInNewTransaction(project);
//
//            // then
//            ArgumentCaptor<AlertHistory> captor = ArgumentCaptor.forClass(AlertHistory.class);
//            verify(alertHistoryRepository).save(captor.capture());
//
//            AlertHistory saved = captor.getValue();
//            assertThat(saved.getProjectId()).isEqualTo(PROJECT_ID_1);
//            assertThat(saved.getResolvedYN()).isEqualTo("N");
//            assertThat(saved.getAlertMessage()).contains("에러 임계값 초과");
//            assertThat(saved.getAlertMessage()).contains(String.valueOf(ERROR_COUNT_ABOVE));
//            assertThat(saved.getAlertMessage()).contains(String.valueOf(ERROR_THRESHOLD));
//            assertThat(saved.getLogReference()).contains("ERROR_THRESHOLD");
//            assertThat(saved.getLogReference()).contains(String.valueOf(ERROR_COUNT_ABOVE));
//            assertThat(saved.getLogReference()).contains(String.valueOf(ERROR_THRESHOLD));
//            assertThat(saved.getLogReference()).contains(PROJECT_UUID_1);
//            assertThat(saved.getLogReference()).contains("startTime");
//            assertThat(saved.getLogReference()).contains("endTime");
//            assertThat(saved.getAlertTime()).isNotNull();
//        }
//    }
//
//    // ============ Helper Methods ============
//
//    private Project createProject(Integer id, String uuid) {
//        Project project = Project.builder()
//                .projectName("Test Project " + id)
//                .projectUuid(uuid)
//                .build();
//
//        try {
//            Field idField = Project.class.getDeclaredField("id");
//            idField.setAccessible(true);
//            idField.set(project, id);
//        } catch (Exception e) {
//            throw new RuntimeException("프로젝트 ID 설정 실패", e);
//        }
//
//        return project;
//    }
//
//    private AlertConfig createAlertConfig(Integer projectId, String activeYN, int threshold) {
//        return AlertConfig.builder()
//                .projectId(projectId)
//                .alertType(AlertType.ERROR_THRESHOLD)
//                .thresholdValue(threshold)
//                .activeYN(activeYN)
//                .build();
//    }
//
//    private AlertHistory createAlertHistory(Integer projectId, LocalDateTime alertTime) {
//        return AlertHistory.builder()
//                .projectId(projectId)
//                .alertMessage("Test alert")
//                .alertTime(alertTime)
//                .resolvedYN("N")
//                .logReference("{\"test\":true}")
//                .build();
//    }
//}
