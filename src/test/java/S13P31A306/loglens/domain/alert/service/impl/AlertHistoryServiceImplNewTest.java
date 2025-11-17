package S13P31A306.loglens.domain.alert.service.impl;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.domain.alert.entity.AlertHistory;
import S13P31A306.loglens.domain.alert.exception.AlertErrorCode;
import S13P31A306.loglens.domain.alert.mapper.AlertHistoryMapper;
import S13P31A306.loglens.domain.alert.repository.AlertHistoryRepository;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AlertHistoryServiceImpl 단위 테스트 (새 필드 포함)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertHistoryServiceImpl 테스트")
class AlertHistoryServiceImplNewTest {

    private AlertHistoryServiceImpl alertHistoryService;

    @Mock
    private AlertHistoryRepository alertHistoryRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private AuthenticationHelper authHelper;

    @Mock
    private ScheduledExecutorService sseScheduler;

    @Mock
    private AlertHistoryMapper alertHistoryMapper;

    private static final String PROJECT_UUID = "test-project-uuid";
    private static final Integer PROJECT_ID = 1;
    private static final Integer USER_ID = 100;
    private static final Integer ALERT_ID = 1;
    private static final long SSE_TIMEOUT = 3600000L;

    @BeforeEach
    void setUp() {
        alertHistoryService = new AlertHistoryServiceImpl(
                alertHistoryRepository,
                projectRepository,
                projectMemberRepository,
                projectService,
                authHelper,
                sseScheduler,
                SSE_TIMEOUT,
                alertHistoryMapper
        );
    }

    @Nested
    @DisplayName("getAlertHistories 메서드 테스트")
    class GetAlertHistoriesTest {

        @Test
        @DisplayName("알림_이력을_정상_조회한다_alertLevel_traceId_포함")
        void getAlertHistories_성공_새필드포함() {
            // given
            LocalDateTime now = LocalDateTime.now();
            AlertHistory alert1 = AlertHistory.builder()
                    .id(1)
                    .alertMessage("에러 알림")
                    .alertTime(now)
                    .resolvedYN("N")
                    .logReference("{\"logId\": 100}")
                    .alertLevel("ERROR")
                    .traceId("trace-abc-123")
                    .projectId(PROJECT_ID)
                    .build();

            AlertHistory alert2 = AlertHistory.builder()
                    .id(2)
                    .alertMessage("경고 알림")
                    .alertTime(now.minusMinutes(5))
                    .resolvedYN("Y")
                    .logReference("{\"logId\": 200}")
                    .alertLevel("WARN")
                    .traceId("trace-def-456")
                    .projectId(PROJECT_ID)
                    .build();

            List<AlertHistory> histories = Arrays.asList(alert1, alert2);

            AlertHistoryResponse response1 = new AlertHistoryResponse(
                    1, "에러 알림", now, "N", "{\"logId\": 100}", "ERROR", "trace-abc-123", PROJECT_UUID
            );
            AlertHistoryResponse response2 = new AlertHistoryResponse(
                    2, "경고 알림", now.minusMinutes(5), "Y", "{\"logId\": 200}", "WARN", "trace-def-456", PROJECT_UUID
            );
            List<AlertHistoryResponse> expectedResponses = Arrays.asList(response1, response2);

            Project project = Project.builder().projectUuid(PROJECT_UUID).build();
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdOrderByAlertTimeDesc(PROJECT_ID)).willReturn(histories);
            given(alertHistoryMapper.toResponseList(histories, PROJECT_UUID)).willReturn(expectedResponses);

            // when
            List<AlertHistoryResponse> result = alertHistoryService.getAlertHistories(PROJECT_UUID, USER_ID, null);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).alertLevel()).isEqualTo("ERROR");
            assertThat(result.get(0).traceId()).isEqualTo("trace-abc-123");
            assertThat(result.get(1).alertLevel()).isEqualTo("WARN");
            assertThat(result.get(1).traceId()).isEqualTo("trace-def-456");

            verify(projectService).getProjectIdByUuid(PROJECT_UUID);
            verify(projectRepository).findById(PROJECT_ID);
            verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, USER_ID);
            verify(alertHistoryRepository).findByProjectIdOrderByAlertTimeDesc(PROJECT_ID);
            verify(alertHistoryMapper).toResponseList(histories, PROJECT_UUID);
        }

        @Test
        @DisplayName("읽지_않은_알림만_필터링하여_조회한다")
        void getAlertHistories_읽지않은알림만() {
            // given
            LocalDateTime now = LocalDateTime.now();
            AlertHistory unreadAlert = AlertHistory.builder()
                    .id(1)
                    .alertMessage("읽지 않은 알림")
                    .alertTime(now)
                    .resolvedYN("N")
                    .logReference("{}")
                    .alertLevel("ERROR")
                    .traceId("trace-unread")
                    .projectId(PROJECT_ID)
                    .build();

            List<AlertHistory> histories = Collections.singletonList(unreadAlert);
            AlertHistoryResponse response = new AlertHistoryResponse(
                    1, "읽지 않은 알림", now, "N", "{}", "ERROR", "trace-unread", PROJECT_UUID
            );
            List<AlertHistoryResponse> expectedResponses = Collections.singletonList(response);

            Project project = Project.builder().projectUuid(PROJECT_UUID).build();
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdAndResolvedYNOrderByAlertTimeDesc(PROJECT_ID, "N"))
                    .willReturn(histories);
            given(alertHistoryMapper.toResponseList(histories, PROJECT_UUID)).willReturn(expectedResponses);

            // when
            List<AlertHistoryResponse> result = alertHistoryService.getAlertHistories(PROJECT_UUID, USER_ID, "N");

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).resolvedYN()).isEqualTo("N");
            assertThat(result.get(0).alertLevel()).isEqualTo("ERROR");

            verify(alertHistoryRepository).findByProjectIdAndResolvedYNOrderByAlertTimeDesc(PROJECT_ID, "N");
        }

        @Test
        @DisplayName("빈_알림_목록을_조회한다")
        void getAlertHistories_빈목록() {
            // given
            Project project = Project.builder().projectUuid(PROJECT_UUID).build();
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdOrderByAlertTimeDesc(PROJECT_ID))
                    .willReturn(Collections.emptyList());
            given(alertHistoryMapper.toResponseList(Collections.emptyList(), PROJECT_UUID))
                    .willReturn(Collections.emptyList());

            // when
            List<AlertHistoryResponse> result = alertHistoryService.getAlertHistories(PROJECT_UUID, USER_ID, null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("접근_권한이_없으면_예외를_던진다")
        void getAlertHistories_권한없음() {
            // given
            Project project = Project.builder().projectUuid(PROJECT_UUID).build();
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alertHistoryService.getAlertHistories(PROJECT_UUID, USER_ID, null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("markAsRead 메서드 테스트")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림을_읽음_처리하고_새_필드를_포함하여_반환한다")
        void markAsRead_성공_새필드포함() {
            // given
            LocalDateTime alertTime = LocalDateTime.now();
            AlertHistory alertHistory = AlertHistory.builder()
                    .id(ALERT_ID)
                    .alertMessage("에러 알림")
                    .alertTime(alertTime)
                    .resolvedYN("N")
                    .logReference("{\"logId\": 100}")
                    .alertLevel("ERROR")
                    .traceId("trace-read-test")
                    .projectId(PROJECT_ID)
                    .build();

            Project project = Project.builder()
                    .projectUuid(PROJECT_UUID)
                    .build();
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);

            AlertHistoryResponse expectedResponse = new AlertHistoryResponse(
                    ALERT_ID, "에러 알림", alertTime, "Y", "{\"logId\": 100}", "ERROR", "trace-read-test", PROJECT_UUID
            );

            given(alertHistoryRepository.findById(ALERT_ID)).willReturn(Optional.of(alertHistory));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(alertHistoryMapper.toResponse(alertHistory, PROJECT_UUID)).willReturn(expectedResponse);

            // when
            AlertHistoryResponse result = alertHistoryService.markAsRead(ALERT_ID, USER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(ALERT_ID);
            assertThat(result.alertLevel()).isEqualTo("ERROR");
            assertThat(result.traceId()).isEqualTo("trace-read-test");

            verify(alertHistoryRepository).findById(ALERT_ID);
            verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, USER_ID);
            verify(projectRepository).findById(PROJECT_ID);
            verify(alertHistoryMapper).toResponse(alertHistory, PROJECT_UUID);
        }

        @Test
        @DisplayName("이미_읽은_알림도_정상_처리한다_멱등성")
        void markAsRead_이미읽음() {
            // given
            LocalDateTime alertTime = LocalDateTime.now();
            AlertHistory alertHistory = AlertHistory.builder()
                    .id(ALERT_ID)
                    .alertMessage("이미 읽은 알림")
                    .alertTime(alertTime)
                    .resolvedYN("Y")
                    .logReference("{}")
                    .alertLevel("WARN")
                    .traceId("trace-already-read")
                    .projectId(PROJECT_ID)
                    .build();

            Project project = Project.builder()
                    .projectUuid(PROJECT_UUID)
                    .build();
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);

            AlertHistoryResponse expectedResponse = new AlertHistoryResponse(
                    ALERT_ID, "이미 읽은 알림", alertTime, "Y", "{}", "WARN", "trace-already-read", PROJECT_UUID
            );

            given(alertHistoryRepository.findById(ALERT_ID)).willReturn(Optional.of(alertHistory));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(alertHistoryMapper.toResponse(alertHistory, PROJECT_UUID)).willReturn(expectedResponse);

            // when
            AlertHistoryResponse result = alertHistoryService.markAsRead(ALERT_ID, USER_ID);

            // then
            assertThat(result.resolvedYN()).isEqualTo("Y");
            assertThat(result.alertLevel()).isEqualTo("WARN");
        }

        @Test
        @DisplayName("존재하지_않는_알림이면_예외를_던진다")
        void markAsRead_알림없음() {
            // given
            given(alertHistoryRepository.findById(ALERT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alertHistoryService.markAsRead(ALERT_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AlertErrorCode.ALERT_HISTORY_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getUnreadCount 메서드 테스트")
    class GetUnreadCountTest {

        @Test
        @DisplayName("읽지_않은_알림_개수를_정상_조회한다")
        void getUnreadCount_성공() {
            // given
            Project project = Project.builder().projectUuid(PROJECT_UUID).build();
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.countByProjectIdAndResolvedYN(PROJECT_ID, "N")).willReturn(5L);

            // when
            long count = alertHistoryService.getUnreadCount(PROJECT_UUID, USER_ID);

            // then
            assertThat(count).isEqualTo(5L);

            verify(alertHistoryRepository).countByProjectIdAndResolvedYN(PROJECT_ID, "N");
        }

        @Test
        @DisplayName("읽지_않은_알림이_없으면_0을_반환한다")
        void getUnreadCount_없음() {
            // given
            Project project = Project.builder().projectUuid(PROJECT_UUID).build();
            ReflectionTestUtils.setField(project, "id", PROJECT_ID);

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.countByProjectIdAndResolvedYN(PROJECT_ID, "N")).willReturn(0L);

            // when
            long count = alertHistoryService.getUnreadCount(PROJECT_UUID, USER_ID);

            // then
            assertThat(count).isEqualTo(0L);
        }
    }
}
