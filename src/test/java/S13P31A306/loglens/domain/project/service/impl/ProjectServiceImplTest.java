package S13P31A306.loglens.domain.project.service.impl;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.ACCESS_FORBIDDEN;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.project.dto.response.ProjectConnectionResponse;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.mapper.ProjectMapper;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock
    private ProjectValidator projectValidator;

    @Mock
    private LogRepository logRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Nested
    @DisplayName("프로젝트 연결 상태 확인")
    class CheckProjectConnection {

        private static final String TEST_PROJECT_UUID = "test-project-uuid";
        private static final Integer TEST_PROJECT_ID = 1;

        @Test
        void 프로젝트가_연결된_경우_isConnected가_true를_반환한다() {
            // given
            Project project = mock(Project.class);
            given(project.getId()).willReturn(TEST_PROJECT_ID);

            ProjectConnectionResponse expectedResponse = new ProjectConnectionResponse(TEST_PROJECT_UUID, true);

            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateProjectAccess(TEST_PROJECT_ID);
            given(logRepository.existsByProjectUuid(TEST_PROJECT_UUID)).willReturn(true);
            given(projectMapper.toConnectionResponse(TEST_PROJECT_UUID, true)).willReturn(expectedResponse);

            // when
            ProjectConnectionResponse result = projectService.checkProjectConnection(TEST_PROJECT_UUID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.projectUuid()).isEqualTo(TEST_PROJECT_UUID);
            assertThat(result.isConnected()).isTrue();

            verify(projectValidator, times(1)).validateProjectExists(TEST_PROJECT_UUID);
            verify(projectValidator, times(1)).validateProjectAccess(TEST_PROJECT_ID);
            verify(logRepository, times(1)).existsByProjectUuid(TEST_PROJECT_UUID);
            verify(projectMapper, times(1)).toConnectionResponse(TEST_PROJECT_UUID, true);
        }

        @Test
        void 프로젝트가_연결되지_않은_경우_isConnected가_false를_반환한다() {
            // given
            Project project = mock(Project.class);
            given(project.getId()).willReturn(TEST_PROJECT_ID);

            ProjectConnectionResponse expectedResponse = new ProjectConnectionResponse(TEST_PROJECT_UUID, false);

            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(project);
            willDoNothing().given(projectValidator).validateProjectAccess(TEST_PROJECT_ID);
            given(logRepository.existsByProjectUuid(TEST_PROJECT_UUID)).willReturn(false);
            given(projectMapper.toConnectionResponse(TEST_PROJECT_UUID, false)).willReturn(expectedResponse);

            // when
            ProjectConnectionResponse result = projectService.checkProjectConnection(TEST_PROJECT_UUID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.projectUuid()).isEqualTo(TEST_PROJECT_UUID);
            assertThat(result.isConnected()).isFalse();

            verify(projectValidator, times(1)).validateProjectExists(TEST_PROJECT_UUID);
            verify(projectValidator, times(1)).validateProjectAccess(TEST_PROJECT_ID);
            verify(logRepository, times(1)).existsByProjectUuid(TEST_PROJECT_UUID);
            verify(projectMapper, times(1)).toConnectionResponse(TEST_PROJECT_UUID, false);
        }

        @Test
        void 프로젝트가_존재하지_않으면_예외가_발생한다() {
            // given
            willThrow(new BusinessException(PROJECT_NOT_FOUND))
                    .given(projectValidator).validateProjectExists(TEST_PROJECT_UUID);

            // when & then
            assertThatThrownBy(() -> projectService.checkProjectConnection(TEST_PROJECT_UUID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PROJECT_NOT_FOUND);

            verify(projectValidator, times(1)).validateProjectExists(TEST_PROJECT_UUID);
            verify(projectValidator, times(0)).validateProjectAccess(TEST_PROJECT_ID);
            verify(logRepository, times(0)).existsByProjectUuid(TEST_PROJECT_UUID);
            verify(projectMapper, times(0)).toConnectionResponse(TEST_PROJECT_UUID, false);
        }

        @Test
        void 프로젝트_접근_권한이_없으면_예외가_발생한다() {
            // given
            Project project = mock(Project.class);
            given(project.getId()).willReturn(TEST_PROJECT_ID);

            given(projectValidator.validateProjectExists(TEST_PROJECT_UUID)).willReturn(project);
            willThrow(new BusinessException(ACCESS_FORBIDDEN))
                    .given(projectValidator).validateProjectAccess(TEST_PROJECT_ID);

            // when & then
            assertThatThrownBy(() -> projectService.checkProjectConnection(TEST_PROJECT_UUID))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ACCESS_FORBIDDEN);

            verify(projectValidator, times(1)).validateProjectExists(TEST_PROJECT_UUID);
            verify(projectValidator, times(1)).validateProjectAccess(TEST_PROJECT_ID);
            verify(logRepository, times(0)).existsByProjectUuid(TEST_PROJECT_UUID);
            verify(projectMapper, times(0)).toConnectionResponse(TEST_PROJECT_UUID, false);
        }
    }
}
