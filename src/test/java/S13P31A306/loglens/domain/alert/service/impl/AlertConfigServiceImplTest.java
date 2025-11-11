package S13P31A306.loglens.domain.alert.service.impl;

import S13P31A306.loglens.domain.alert.dto.AlertConfigCreateRequest;
import S13P31A306.loglens.domain.alert.dto.AlertConfigResponse;
import S13P31A306.loglens.domain.alert.dto.AlertConfigUpdateRequest;
import S13P31A306.loglens.domain.alert.entity.AlertConfig;
import S13P31A306.loglens.domain.alert.entity.AlertType;
import S13P31A306.loglens.domain.alert.exception.AlertErrorCode;
import S13P31A306.loglens.domain.alert.repository.AlertConfigRepository;
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

import java.util.Optional;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AlertConfigServiceImpl 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertConfigServiceImpl 테스트")
class AlertConfigServiceImplTest {

    private AlertConfigServiceImpl alertConfigService;

    @Mock
    private AlertConfigRepository alertConfigRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ProjectService projectService;

    private static final Integer USER_ID = 1;
    private static final Integer PROJECT_ID = 1;
    private static final String PROJECT_UUID = "test-project-uuid-1234";
    private static final String PROJECT_NAME = "Test Project";

    @BeforeEach
    void setUp() {
        alertConfigService = new AlertConfigServiceImpl(
                alertConfigRepository,
                projectRepository,
                projectMemberRepository,
                projectService
        );
    }

    @Nested
    @DisplayName("알림 설정 생성 테스트")
    class CreateAlertConfigTest {

        @Test
        @DisplayName("알림_설정_생성_성공_시_AlertConfigResponse를_반환한다")
        void 알림_설정_생성_성공_시_AlertConfigResponse를_반환한다() {
            // given
            AlertConfigCreateRequest request = createValidRequest();
            Project project = createProject();
            AlertConfig savedConfig = createAlertConfig();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertConfigRepository.existsByProjectId(PROJECT_ID)).willReturn(false);
            given(alertConfigRepository.save(any(AlertConfig.class))).willReturn(savedConfig);

            // when
            AlertConfigResponse response = alertConfigService.createAlertConfig(request, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1);
            assertThat(response.alertType()).isEqualTo(AlertType.ERROR_THRESHOLD);
            assertThat(response.thresholdValue()).isEqualTo(10);
            assertThat(response.activeYN()).isEqualTo("Y");
            assertThat(response.projectUuid()).isEqualTo(PROJECT_UUID);
            assertThat(response.projectName()).isEqualTo(PROJECT_NAME);

            verify(projectService).getProjectIdByUuid(PROJECT_UUID);
            verify(projectRepository).findById(PROJECT_ID);
            verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, USER_ID);
            verify(alertConfigRepository).existsByProjectId(PROJECT_ID);
            verify(alertConfigRepository).save(any(AlertConfig.class));
        }

        @Test
        @DisplayName("프로젝트가_없으면_PROJECT_NOT_FOUND_예외를_발생시킨다")
        void 프로젝트가_없으면_PROJECT_NOT_FOUND_예외를_발생시킨다() {
            // given
            AlertConfigCreateRequest request = createValidRequest();
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alertConfigService.createAlertConfig(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PROJECT_NOT_FOUND);

            verify(projectService).getProjectIdByUuid(PROJECT_UUID);
            verify(projectRepository).findById(PROJECT_ID);
            verify(projectMemberRepository, never()).existsByProjectIdAndUserId(anyInt(), anyInt());
            verify(alertConfigRepository, never()).save(any());
        }

        @Test
        @DisplayName("프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다")
        void 프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다() {
            // given
            AlertConfigCreateRequest request = createValidRequest();
            Project project = createProject();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alertConfigService.createAlertConfig(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FORBIDDEN);

            verify(projectService).getProjectIdByUuid(PROJECT_UUID);
            verify(projectRepository).findById(PROJECT_ID);
            verify(projectMemberRepository).existsByProjectIdAndUserId(PROJECT_ID, USER_ID);
            verify(alertConfigRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미_알림_설정이_있으면_ALERT_CONFIG_ALREADY_EXISTS_예외를_발생시킨다")
        void 이미_알림_설정이_있으면_ALERT_CONFIG_ALREADY_EXISTS_예외를_발생시킨다() {
            // given
            AlertConfigCreateRequest request = createValidRequest();
            Project project = createProject();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertConfigRepository.existsByProjectId(PROJECT_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> alertConfigService.createAlertConfig(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AlertErrorCode.ALERT_CONFIG_ALREADY_EXISTS);

            verify(alertConfigRepository).existsByProjectId(PROJECT_ID);
            verify(alertConfigRepository, never()).save(any());
        }

        @Test
        @DisplayName("activeYN이_Y또는_N이_아니면_INVALID_ACTIVE_YN_예외를_발생시킨다")
        void activeYN이_Y또는_N이_아니면_INVALID_ACTIVE_YN_예외를_발생시킨다() {
            // given
            AlertConfigCreateRequest request = new AlertConfigCreateRequest(
                    PROJECT_UUID, AlertType.ERROR_THRESHOLD, 10, "X");
            Project project = createProject();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertConfigRepository.existsByProjectId(PROJECT_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alertConfigService.createAlertConfig(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AlertErrorCode.INVALID_ACTIVE_YN);

            verify(alertConfigRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("알림 설정 조회 테스트")
    class GetAlertConfigTest {

        @Test
        @DisplayName("알림_설정이_있으면_AlertConfigResponse를_반환한다")
        void 알림_설정이_있으면_AlertConfigResponse를_반환한다() {
            // given
            Project project = createProject();
            AlertConfig alertConfig = createAlertConfig();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertConfigRepository.findByProjectId(PROJECT_ID)).willReturn(Optional.of(alertConfig));

            // when
            AlertConfigResponse response = alertConfigService.getAlertConfig(PROJECT_UUID, USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1);
            assertThat(response.projectName()).isEqualTo(PROJECT_NAME);

            verify(projectService).getProjectIdByUuid(PROJECT_UUID);
            verify(projectRepository).findById(PROJECT_ID);
            verify(alertConfigRepository).findByProjectId(PROJECT_ID);
        }

        @Test
        @DisplayName("알림_설정이_없으면_null을_반환한다")
        void 알림_설정이_없으면_null을_반환한다() {
            // given
            Project project = createProject();

            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);
            given(alertConfigRepository.findByProjectId(PROJECT_ID)).willReturn(Optional.empty());

            // when
            AlertConfigResponse response = alertConfigService.getAlertConfig(PROJECT_UUID, USER_ID);

            // then
            assertThat(response).isNull();

            verify(alertConfigRepository).findByProjectId(PROJECT_ID);
        }

        @Test
        @DisplayName("프로젝트가_없으면_PROJECT_NOT_FOUND_예외를_발생시킨다")
        void 프로젝트가_없으면_PROJECT_NOT_FOUND_예외를_발생시킨다() {
            // given
            given(projectService.getProjectIdByUuid(PROJECT_UUID)).willReturn(PROJECT_ID);
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alertConfigService.getAlertConfig(PROJECT_UUID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PROJECT_NOT_FOUND);

            verify(alertConfigRepository, never()).findByProjectId(anyInt());
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
            assertThatThrownBy(() -> alertConfigService.getAlertConfig(PROJECT_UUID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FORBIDDEN);

            verify(alertConfigRepository, never()).findByProjectId(anyInt());
        }
    }

    @Nested
    @DisplayName("알림 설정 수정 테스트")
    class UpdateAlertConfigTest {

        @Test
        @DisplayName("모든_필드를_수정하면_AlertConfigResponse를_반환한다")
        void 모든_필드를_수정하면_AlertConfigResponse를_반환한다() {
            // given
            AlertConfigUpdateRequest request = new AlertConfigUpdateRequest(
                    1, AlertType.LATENCY, 100, "N");
            AlertConfig alertConfig = createAlertConfig();
            Project project = createProject();

            given(alertConfigRepository.findById(1)).willReturn(Optional.of(alertConfig));
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);

            // when
            AlertConfigResponse response = alertConfigService.updateAlertConfig(request, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(alertConfigRepository).findById(1);
            verify(projectRepository).findById(PROJECT_ID);
        }

        @Test
        @DisplayName("alertType만_수정하면_나머지는_유지된다")
        void alertType만_수정하면_나머지는_유지된다() {
            // given
            AlertConfigUpdateRequest request = new AlertConfigUpdateRequest(
                    1, AlertType.LATENCY, null, null);
            AlertConfig alertConfig = createAlertConfig();
            Project project = createProject();

            given(alertConfigRepository.findById(1)).willReturn(Optional.of(alertConfig));
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);

            // when
            alertConfigService.updateAlertConfig(request, USER_ID);

            // then
            verify(alertConfigRepository).findById(1);
        }

        @Test
        @DisplayName("thresholdValue만_수정하면_나머지는_유지된다")
        void thresholdValue만_수정하면_나머지는_유지된다() {
            // given
            AlertConfigUpdateRequest request = new AlertConfigUpdateRequest(
                    1, null, 50, null);
            AlertConfig alertConfig = createAlertConfig();
            Project project = createProject();

            given(alertConfigRepository.findById(1)).willReturn(Optional.of(alertConfig));
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);

            // when
            alertConfigService.updateAlertConfig(request, USER_ID);

            // then
            verify(alertConfigRepository).findById(1);
        }

        @Test
        @DisplayName("activeYN만_수정하면_나머지는_유지된다")
        void activeYN만_수정하면_나머지는_유지된다() {
            // given
            AlertConfigUpdateRequest request = new AlertConfigUpdateRequest(
                    1, null, null, "N");
            AlertConfig alertConfig = createAlertConfig();
            Project project = createProject();

            given(alertConfigRepository.findById(1)).willReturn(Optional.of(alertConfig));
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(true);

            // when
            alertConfigService.updateAlertConfig(request, USER_ID);

            // then
            verify(alertConfigRepository).findById(1);
        }

        @Test
        @DisplayName("알림_설정이_없으면_ALERT_CONFIG_NOT_FOUND_예외를_발생시킨다")
        void 알림_설정이_없으면_ALERT_CONFIG_NOT_FOUND_예외를_발생시킨다() {
            // given
            AlertConfigUpdateRequest request = new AlertConfigUpdateRequest(
                    1, AlertType.LATENCY, 100, "N");

            given(alertConfigRepository.findById(1)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> alertConfigService.updateAlertConfig(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", AlertErrorCode.ALERT_CONFIG_NOT_FOUND);

            verify(projectRepository, never()).findById(anyInt());
        }

        @Test
        @DisplayName("프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다")
        void 프로젝트_멤버가_아니면_FORBIDDEN_예외를_발생시킨다() {
            // given
            AlertConfigUpdateRequest request = new AlertConfigUpdateRequest(
                    1, AlertType.LATENCY, 100, "N");
            AlertConfig alertConfig = createAlertConfig();
            Project project = createProject();

            given(alertConfigRepository.findById(1)).willReturn(Optional.of(alertConfig));
            given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(project));
            given(projectMemberRepository.existsByProjectIdAndUserId(PROJECT_ID, USER_ID)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> alertConfigService.updateAlertConfig(request, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", FORBIDDEN);
        }
    }

    // ============ Helper Methods ============

    private AlertConfigCreateRequest createValidRequest() {
        return new AlertConfigCreateRequest(
                PROJECT_UUID,
                AlertType.ERROR_THRESHOLD,
                10,
                "Y"
        );
    }

    private AlertConfig createAlertConfig() {
        return AlertConfig.builder()
                .id(1)
                .projectId(PROJECT_ID)
                .alertType(AlertType.ERROR_THRESHOLD)
                .thresholdValue(10)
                .activeYN("Y")
                .build();
    }

    private Project createProject() {
        return Project.builder()
                .projectUuid(PROJECT_UUID)
                .projectName(PROJECT_NAME)
                .build();
    }
}
