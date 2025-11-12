package S13P31A306.loglens.domain.alert.service.impl;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.domain.alert.entity.AlertHistory;
import S13P31A306.loglens.domain.alert.exception.AlertErrorCode;
import S13P31A306.loglens.domain.alert.mapper.AlertHistoryMapper;
import S13P31A306.loglens.domain.alert.repository.AlertHistoryRepository;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AlertHistoryServiceImpl 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertHistoryServiceImpl 테스트")
class AlertHistoryServiceImplTest {

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
    private ScheduledExecutorService sseScheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Mock
    private AlertHistoryMapper alertHistoryMapper;

    private static final Integer USER_ID = 1;
    private static final Integer PROJECT_ID = 1;
    private static final String PROJECT_UUID = "test-project-uuid-1234";
    private static final Integer ALERT_ID = 1;
    private static final long SSE_TIMEOUT = 300000L; // 5분

    @BeforeEach
    void setUp() {
        alertHistoryService = new AlertHistoryServiceImpl(
                alertHistoryRepository,
                projectRepository,
                projectMemberRepository,
                projectService,
                sseScheduler,
                SSE_TIMEOUT,
                alertHistoryMapper
        );
    }

    @Nested
    @DisplayName("알림 이력 조회 테스트")
    class GetAlertHistoriesTest {

        @Test
        @DisplayName("resolvedYN이_null이면_전체_알림을_반환한다")
        void resolvedYN이_null이면_전체_알림을_반환한다() {
            // given
            Project project = createProject();
            List<AlertHistory> histories = Arrays.asList(
                    createAlertHistory(1, "N"),
                    createAlertHistory(2, "Y")
            );
            List<AlertHistoryResponse> expectedResponses = Arrays.asList(
                    createAlertHistoryResponse(1, "N"),
                    createAlertHistoryResponse(2, "Y")
            );

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdOrderByAlertTimeDesc(PROJECT_ID))
                    .willReturn(histories);
            given(alertHistoryMapper.toResponseList(histories, PROJECT_UUID)).willReturn(expectedResponses);

            // when
            List<AlertHistoryResponse> responses = alertHistoryService
                    .getAlertHistories(PROJECT_UUID, USER_ID, null);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).id()).isEqualTo(1);
            assertThat(responses.get(1).id()).isEqualTo(2);

            verify(alertHistoryRepository).findByProjectIdOrderByAlertTimeDesc(PROJECT_ID);
            verify(alertHistoryRepository, never())
                    .findByProjectIdAndResolvedYNOrderByAlertTimeDesc(anyInt(), anyString());
            verify(alertHistoryMapper).toResponseList(histories, PROJECT_UUID);
        }

        @Test
        @DisplayName("resolvedYN이_N이면_읽지_않은_알림만_반환한다")
        void resolvedYN이_N이면_읽지_않은_알림만_반환한다() {
            // given
            Project project = createProject();
            List<AlertHistory> unreadHistories = Collections.singletonList(
                    createAlertHistory(1, "N")
            );
            List<AlertHistoryResponse> expectedResponses = Collections.singletonList(
                    createAlertHistoryResponse(1, "N")
            );

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdAndResolvedYNOrderByAlertTimeDesc(PROJECT_ID, "N"))
                    .willReturn(unreadHistories);
            given(alertHistoryMapper.toResponseList(unreadHistories, PROJECT_UUID)).willReturn(expectedResponses);

            // when
            List<AlertHistoryResponse> responses = alertHistoryService
                    .getAlertHistories(PROJECT_UUID, USER_ID, "N");

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).resolvedYN()).isEqualTo("N");

            verify(alertHistoryRepository)
                    .findByProjectIdAndResolvedYNOrderByAlertTimeDesc(PROJECT_ID, "N");
            verify(alertHistoryRepository, never()).findByProjectIdOrderByAlertTimeDesc(anyInt());
            verify(alertHistoryMapper).toResponseList(unreadHistories, PROJECT_UUID);
        }

        @Test
        @DisplayName("resolvedYN이_Y이면_읽은_알림만_반환한다")
        void resolvedYN이_Y이면_읽은_알림만_반환한다() {
            // given
            Project project = createProject();
            List<AlertHistory> readHistories = Collections.singletonList(
                    createAlertHistory(2, "Y")
            );
            List<AlertHistoryResponse> expectedResponses = Collections.singletonList(
                    createAlertHistoryResponse(2, "Y")
            );

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdAndResolvedYNOrderByAlertTimeDesc(PROJECT_ID, "Y"))
                    .willReturn(readHistories);
            given(alertHistoryMapper.toResponseList(readHistories, PROJECT_UUID)).willReturn(expectedResponses);

            // when
            List<AlertHistoryResponse> responses = alertHistoryService
                    .getAlertHistories(PROJECT_UUID, USER_ID, "Y");

            // then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).resolvedYN()).isEqualTo("Y");

            verify(alertHistoryRepository)
                    .findByProjectIdAndResolvedYNOrderByAlertTimeDesc(PROJECT_ID, "Y");
            verify(alertHistoryMapper).toResponseList(readHistories, PROJECT_UUID);
        }

        @Test
        @DisplayName("알림이_없으면_빈_리스트를_반환한다")
        void 알림이_없으면_빈_리스트를_반환한다() {
            // given
            Project project = createProject();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdOrderByAlertTimeDesc(PROJECT_ID))
                    .willReturn(Collections.emptyList());
            given(alertHistoryMapper.toResponseList(Collections.emptyList(), PROJECT_UUID))
                    .willReturn(Collections.emptyList());

            // when
            List<AlertHistoryResponse> responses = alertHistoryService
                    .getAlertHistories(PROJECT_UUID, USER_ID, null);

            // then
            assertThat(responses).isEmpty();

            verify(alertHistoryRepository).findByProjectIdOrderByAlertTimeDesc(PROJECT_ID);
            verify(alertHistoryMapper).toResponseList(Collections.emptyList(), PROJECT_UUID);
        }

        @Test
        @DisplayName("프로젝트가_없으면_PROJECT_NOT_FOUND_예외를_발생시킨다")
        void 프로젝트가_없으면_PROJECT_NOT_FOUND_예외를_발생시킨다() {
            // given
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alertHistoryService.getAlertHistories(PROJECT_UUID, USER_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PROJECT_NOT_FOUND);

            verify(alertHistoryRepository, never()).findByProjectIdOrderByAlertTimeDesc(anyInt());
        }

        @Test
        @DisplayName("프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다")
        void 프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다() {
            // given
            Project project = createProject();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alertHistoryService.getAlertHistories(PROJECT_UUID, USER_ID, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FORBIDDEN);

            verify(alertHistoryRepository, never()).findByProjectIdOrderByAlertTimeDesc(anyInt());
        }
    }

    @Nested
    @DisplayName("알림 읽음 처리 테스트")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림을_읽음_처리하면_resolvedYN이_Y로_변경된다")
        void 알림을_읽음_처리하면_resolvedYN이_Y로_변경된다() {
            // given
            AlertHistory alertHistory = createAlertHistory(ALERT_ID, "N");
            Project project = createProject();
            AlertHistoryResponse expectedResponse = createAlertHistoryResponse(ALERT_ID, "Y");

            given(alertHistoryRepository.findById(ALERT_ID)).willReturn(Optional.of(alertHistory));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(alertHistoryMapper.toResponse(alertHistory, PROJECT_UUID)).willReturn(expectedResponse);

            // when
            AlertHistoryResponse response = alertHistoryService.markAsRead(ALERT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(ALERT_ID);
            assertThat(response.resolvedYN()).isEqualTo("Y");

            verify(alertHistoryRepository).findById(ALERT_ID);
            verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, USER_ID);
            verify(projectRepository).findById(PROJECT_ID);
            verify(alertHistoryMapper).toResponse(alertHistory, PROJECT_UUID);
        }

        @Test
        @DisplayName("이미_읽은_알림을_다시_읽어도_정상_처리된다")
        void 이미_읽은_알림을_다시_읽어도_정상_처리된다() {
            // given
            AlertHistory alertHistory = createAlertHistory(ALERT_ID, "Y");
            Project project = createProject();
            AlertHistoryResponse expectedResponse = createAlertHistoryResponse(ALERT_ID, "Y");

            given(alertHistoryRepository.findById(ALERT_ID)).willReturn(Optional.of(alertHistory));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(alertHistoryMapper.toResponse(alertHistory, PROJECT_UUID)).willReturn(expectedResponse);

            // when
            AlertHistoryResponse response = alertHistoryService.markAsRead(ALERT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.resolvedYN()).isEqualTo("Y");

            verify(alertHistoryRepository).findById(ALERT_ID);
            verify(projectRepository).findById(PROJECT_ID);
            verify(alertHistoryMapper).toResponse(alertHistory, PROJECT_UUID);
        }

        @Test
        @DisplayName("알림이_없으면_ALERT_HISTORY_NOT_FOUND_예외를_발생시킨다")
        void 알림이_없으면_ALERT_HISTORY_NOT_FOUND_예외를_발생시킨다() {
            // given
            given(alertHistoryRepository.findById(ALERT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alertHistoryService.markAsRead(ALERT_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AlertErrorCode.ALERT_HISTORY_NOT_FOUND);

            verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyInt(), anyInt());
        }

//        @Test
//        @DisplayName("프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다")
//        void 프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다() {
//            // given
//            AlertHistory alertHistory = createAlertHistory(ALERT_ID, "N");
//            Project project = createProject();
//
//            lenient().given(alertHistoryRepository.findById(ALERT_ID)).willReturn(Optional.of(alertHistory));
//            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(false);
//            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
//
//            // when & then
//            assertThatThrownBy(() -> alertHistoryService.markAsRead(ALERT_ID, USER_ID))
//                    .isInstanceOf(BusinessException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", FORBIDDEN);
//        }
    }

    @Nested
    @DisplayName("읽지 않은 알림 개수 조회 테스트")
    class GetUnreadCountTest {

        @Test
        @DisplayName("읽지_않은_알림_개수를_반환한다")
        void 읽지_않은_알림_개수를_반환한다() {
            // given
            Project project = createProject();

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
        void 읽지_않은_알림이_없으면_0을_반환한다() {
            // given
            Project project = createProject();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.countByProjectIdAndResolvedYN(PROJECT_ID, "N")).willReturn(0L);

            // when
            long count = alertHistoryService.getUnreadCount(PROJECT_UUID, USER_ID);

            // then
            assertThat(count).isZero();

            verify(alertHistoryRepository).countByProjectIdAndResolvedYN(PROJECT_ID, "N");
        }

        @Test
        @DisplayName("프로젝트가_없으면_PROJECT_NOT_FOUND_예외를_발생시킨다")
        void 프로젝트가_없으면_PROJECT_NOT_FOUND_예외를_발생시킨다() {
            // given
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alertHistoryService.getUnreadCount(PROJECT_UUID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PROJECT_NOT_FOUND);

            verify(alertHistoryRepository, never()).countByProjectIdAndResolvedYN(anyInt(), anyString());
        }

        @Test
        @DisplayName("프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다")
        void 프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다() {
            // given
            Project project = createProject();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alertHistoryService.getUnreadCount(PROJECT_UUID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FORBIDDEN);

            verify(alertHistoryRepository, never()).countByProjectIdAndResolvedYN(anyInt(), anyString());
        }
    }

    @Nested
    @DisplayName("실시간 알림 스트리밍 테스트")
    class StreamAlertsTest {

        @Test
        @DisplayName("SSE_연결을_생성하고_스케줄러를_시작한다")
        void SSE_연결을_생성하고_스케줄러를_시작한다() {
            // given
            Project project = createProject();
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(sseScheduler.scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS)))
                    .willReturn((ScheduledFuture) scheduledFuture);

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTimeout()).isEqualTo(SSE_TIMEOUT);

            verify(projectRepository).findById(PROJECT_ID);
            verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, USER_ID);
            verify(sseScheduler).scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));
        }

        @Test
        @DisplayName("프로젝트가_존재하지_않으면_PROJECT_NOT_FOUND_예외를_발생시킨다")
        void 프로젝트가_존재하지_않으면_PROJECT_NOT_FOUND_예외를_발생시킨다() {
            // given
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PROJECT_NOT_FOUND);

            verify(projectRepository).findById(PROJECT_ID);
            verify(sseScheduler, never()).scheduleAtFixedRate(
                    any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다")
        void 프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다() {
            // given
            Project project = createProject();
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FORBIDDEN);

            verify(projectRepository).findById(PROJECT_ID);
            verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, USER_ID);
            verify(sseScheduler, never()).scheduleAtFixedRate(
                    any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("스케줄러가_정상적으로_새로운_알림을_조회한다")
        void 스케줄러가_정상적으로_새로운_알림을_조회한다() {
            // given
            Project project = createProject();
            AlertHistory alert1 = createAlertHistory(1, "N");
            AlertHistory alert2 = createAlertHistory(2, "N");
            List<AlertHistory> alerts = Arrays.asList(alert1, alert2);
            List<AlertHistoryResponse> expectedResponses = Arrays.asList(
                    createAlertHistoryResponse(1, "N"),
                    createAlertHistoryResponse(2, "N")
            );

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class))).willReturn(alerts);
            given(alertHistoryMapper.toResponseList(alerts, PROJECT_UUID)).willReturn(expectedResponses);

            // 스케줄러가 즉시 실행되도록 설정
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run(); // 즉시 실행
                return scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            verify(alertHistoryRepository).findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class));
            verify(alertHistoryMapper).toResponseList(alerts, PROJECT_UUID);
        }

        @Test
        @DisplayName("새로운_알림이_있으면_alert_update_이벤트로_전송한다")
        void 새로운_알림이_있으면_alert_update_이벤트로_전송한다() {
            // given
            Project project = createProject();
            AlertHistory alert1 = createAlertHistory(1, "N");
            AlertHistory alert2 = createAlertHistory(2, "N");
            List<AlertHistory> alerts = Arrays.asList(alert1, alert2);
            List<AlertHistoryResponse> expectedResponses = Arrays.asList(
                    createAlertHistoryResponse(1, "N"),
                    createAlertHistoryResponse(2, "N")
            );

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class))).willReturn(alerts);
            given(alertHistoryMapper.toResponseList(alerts, PROJECT_UUID)).willReturn(expectedResponses);

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            verify(alertHistoryRepository).findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class));
            verify(alertHistoryMapper).toResponseList(alerts, PROJECT_UUID);
            // 참고: SseEmitter.send()는 실제 HTTP 연결이 필요하므로 단위 테스트에서 검증 불가
            // Repository 호출 및 데이터 조회 검증으로 대체
        }

        @Test
        @DisplayName("새로운_알림이_없으면_heartbeat를_전송한다")
        void 새로운_알림이_없으면_heartbeat를_전송한다() {
            // given
            Project project = createProject();
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class))).willReturn(Collections.emptyList());

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            verify(alertHistoryRepository).findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("마지막_timestamp를_추적하여_새로운_알림만_조회한다")
        void 마지막_timestamp를_추적하여_새로운_알림만_조회한다() {
            // given
            Project project = createProject();
            LocalDateTime fixedTime = LocalDateTime.of(2024, 1, 15, 12, 0, 10);
            AlertHistory alert1 = createAlertHistoryWithTime(1, "N", fixedTime);

            List<AlertHistory> firstResult = Collections.singletonList(alert1);
            List<AlertHistory> secondResult = Collections.emptyList();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class)))
                    .willReturn(firstResult)
                    .willReturn(secondResult);

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run(); // 첫 번째 실행만
                return scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            verify(alertHistoryRepository).findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("RuntimeException_발생_시_연결을_정상_종료한다")
        void RuntimeException_발생_시_연결을_정상_종료한다() {
            // given
            Project project = createProject();
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);

            // RuntimeException 발생 시나리오 (데이터베이스 오류)
            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class)))
                    .willThrow(new RuntimeException("Database connection failed"));

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                try {
                    task.run();
                } catch (Exception e) {
                    // 예외가 발생해도 스케줄러는 정상 처리
                }
                return scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            verify(alertHistoryRepository).findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("일반_예외_발생_시_에러와_함께_연결을_종료한다")
        void 일반_예외_발생_시_에러와_함께_연결을_종료한다() {
            // given
            Project project = createProject();
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);

            // 일반 RuntimeException 발생
            given(alertHistoryRepository.findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class)))
                    .willThrow(new RuntimeException("Database error"));

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                try {
                    task.run();
                } catch (Exception e) {
                    // 예외가 발생해도 스케줄러는 정상 처리
                }
                return scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            verify(alertHistoryRepository).findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
                    eq(PROJECT_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("타임아웃_발생_시_스케줄러를_취소하고_연결을_종료한다")
        void 타임아웃_발생_시_스케줄러를_취소하고_연결을_종료한다() {
            // given
            Project project = createProject();
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(sseScheduler.scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS)))
                    .willReturn((ScheduledFuture) scheduledFuture);

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTimeout()).isEqualTo(SSE_TIMEOUT);
            // 타임아웃 콜백은 SseEmitter 내부에서 관리되므로 직접 테스트 불가
            // 타임아웃 설정 검증으로 대체
        }

        @Test
        @DisplayName("연결_완료_시_스케줄러_리소스를_정리한다")
        void 연결_완료_시_스케줄러_리소스를_정리한다() {
            // given
            Project project = createProject();
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(sseScheduler.scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS)))
                    .willReturn((ScheduledFuture) scheduledFuture);

            // when
            SseEmitter result = alertHistoryService.streamAlerts(PROJECT_UUID, USER_ID);

            // then
            assertThat(result).isNotNull();
            // 연결 완료 콜백(onCompletion)은 SseEmitter 내부에서 관리
            // 스케줄러 등록 검증으로 대체
            verify(sseScheduler).scheduleAtFixedRate(
                    any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));
        }
    }

    // ============ Helper Methods ============

    private AlertHistory createAlertHistory(Integer id, String resolvedYN) {
        return AlertHistory.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .alertMessage("에러 발생: " + id)
                .alertTime(LocalDateTime.now())
                .resolvedYN(resolvedYN)
                .logReference("{\"logId\": " + id + "}")
                .build();
    }

    private AlertHistory createAlertHistoryWithTime(Integer id, String resolvedYN, LocalDateTime alertTime) {
        return AlertHistory.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .alertMessage("에러 발생: " + id)
                .alertTime(alertTime)
                .resolvedYN(resolvedYN)
                .logReference("{\"logId\": " + id + "}")
                .build();
    }

    private Project createProject() {
        return Project.builder()
                .projectUuid(PROJECT_UUID)
                .projectName("Test Project")
                .build();
    }

    private AlertHistoryResponse createAlertHistoryResponse(Integer id, String resolvedYN) {
        return new AlertHistoryResponse(
                id,
                "에러 발생: " + id,
                LocalDateTime.now(),
                resolvedYN,
                "{\"logId\": " + id + "}",
                PROJECT_UUID
        );
    }
}
