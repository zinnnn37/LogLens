package S13P31A306.loglens.domain.jira.service.impl;

import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.jira.client.JiraApiClient;
import S13P31A306.loglens.domain.jira.client.dto.JiraIssueRequest;
import S13P31A306.loglens.domain.jira.client.dto.JiraIssueResponse;
import S13P31A306.loglens.domain.jira.constants.JiraErrorCode;
import S13P31A306.loglens.domain.jira.dto.request.JiraIssueCreateRequest;
import S13P31A306.loglens.domain.jira.dto.response.CreatedByResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraIssueCreateResponse;
import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import S13P31A306.loglens.domain.jira.mapper.JiraMapper;
import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
import S13P31A306.loglens.domain.jira.validator.JiraValidator;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.utils.EncryptionUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * JiraIssueServiceImpl 테스트
 */
@ExtendWith(MockitoExtension.class)
class JiraIssueServiceImplTest {

    @InjectMocks
    private JiraIssueServiceImpl jiraIssueService;

    @Mock
    private AuthenticationHelper authenticationHelper;

    @Mock
    private JiraConnectionRepository jiraConnectionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private JiraApiClient jiraApiClient;

    @Mock
    private JiraValidator jiraValidator;

    @Mock
    private JiraMapper jiraMapper;

    @Mock
    private EncryptionUtils encryptionUtils;

    @Nested
    @DisplayName("Jira 이슈 생성 테스트")
    class CreateIssueTest {

        @Test
        @DisplayName("Jira_이슈_생성_성공_시_JiraIssueCreateResponse를_반환한다")
        void Jira_이슈_생성_성공_시_JiraIssueCreateResponse를_반환한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            Integer logId = 100;
            String summary = "Test Issue";
            String description = "Test Description";

            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    projectUuid,
                    logId,
                    summary,
                    description,
                    "Bug",
                    "High"
            );

            Project mockProject = Project.builder()
                    .projectName("Test Project")
                    .projectUuid(projectUuid)
                    .build();
            try {
                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockProject, projectId);
            } catch (Exception e) {
                throw new RuntimeException("테스트 데이터 설정 실패", e);
            }

            String jiraUrl = "https://test.atlassian.net";
            String jiraEmail = "admin@example.com";
            String encryptedToken = "encrypted-token";
            String decryptedToken = "decrypted-token";
            String jiraProjectKey = "TEST";

            JiraConnection connection = JiraConnection.builder()
                    .projectId(projectId)
                    .jiraUrl(jiraUrl)
                    .jiraEmail(jiraEmail)
                    .jiraApiToken(encryptedToken)
                    .jiraProjectKey(jiraProjectKey)
                    .build();

            String logDescription = "LogLens에서 자동 생성된 이슈입니다.";
            JiraIssueRequest jiraRequest = new JiraIssueRequest(
                    new JiraIssueRequest.Fields(
                            new JiraIssueRequest.Project(jiraProjectKey),
                            summary,
                            new JiraIssueRequest.Description("doc", 1, List.of()),
                            new JiraIssueRequest.IssueType("Bug"),
                            new JiraIssueRequest.Priority("High")
                    )
            );

            JiraIssueResponse jiraResponse = new JiraIssueResponse(
                    "10001",
                    "TEST-1234",
                    jiraUrl + "/rest/api/3/issue/10001"
            );

            User user = User.builder()
                    .email("user@example.com")
                    .name("Test User")
                    .build();

            CreatedByResponse createdBy = new CreatedByResponse(
                    userId,
                    "user@example.com",
                    "Test User"
            );

            JiraIssueCreateResponse expectedResponse = new JiraIssueCreateResponse(
                    "TEST-1234",
                    jiraUrl + "/browse/TEST-1234",
                    createdBy
            );

            // Mocking
            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
            willDoNothing().given(jiraValidator).validateLogExists(logId);
            given(jiraConnectionRepository.findByProjectId(projectId)).willReturn(Optional.of(connection));
            given(encryptionUtils.decrypt(encryptedToken)).willReturn(decryptedToken);
            given(jiraMapper.toJiraApiRequest(any(), eq(jiraProjectKey), anyString())).willReturn(jiraRequest);
            given(jiraApiClient.createIssue(jiraUrl, jiraEmail, decryptedToken, jiraRequest))
                    .willReturn(jiraResponse);
            given(authenticationHelper.getCurrentUser()).willReturn(user);
            given(jiraMapper.toCreatedByResponse(user)).willReturn(createdBy);

            // when
            JiraIssueCreateResponse response = jiraIssueService.createIssue(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.issueKey()).isEqualTo("TEST-1234");
            assertThat(response.jiraUrl()).isEqualTo(jiraUrl + "/browse/TEST-1234");
            assertThat(response.createdBy()).isNotNull();
            assertThat(response.createdBy().userId()).isEqualTo(userId);
            assertThat(response.createdBy().email()).isEqualTo("user@example.com");
            assertThat(response.createdBy().name()).isEqualTo("Test User");

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(1)).validateLogExists(logId);
            verify(jiraConnectionRepository, times(1)).findByProjectId(projectId);
            verify(encryptionUtils, times(1)).decrypt(encryptedToken);
            verify(jiraMapper, times(1)).toJiraApiRequest(any(), eq(jiraProjectKey), anyString());
            verify(jiraApiClient, times(1)).createIssue(jiraUrl, jiraEmail, decryptedToken, jiraRequest);
            verify(authenticationHelper, times(1)).getCurrentUser();
            verify(jiraMapper, times(1)).toCreatedByResponse(user);
        }

        @Test
        @DisplayName("프로젝트_접근_권한_없으면_예외가_발생한다")
        void 프로젝트_접근_권한_없으면_예외가_발생한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    projectUuid,
                    100,
                    "Test Issue",
                    "Test Description",
                    "Bug",
                    "High"
            );

            Project mockProject = Project.builder()
                    .projectName("Test Project")
                    .projectUuid(projectUuid)
                    .build();
            try {
                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockProject, projectId);
            } catch (Exception e) {
                throw new RuntimeException("테스트 데이터 설정 실패", e);
            }

            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
            willThrow(new BusinessException(GlobalErrorCode.FORBIDDEN))
                    .given(jiraValidator).validateProjectAccess(projectId, userId);

            // when & then
            assertThatThrownBy(() -> jiraIssueService.createIssue(request))
                    .isInstanceOf(BusinessException.class);

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(0)).validateLogExists(any());
            verify(jiraConnectionRepository, times(0)).findByProjectId(any());
        }

        @Test
        @DisplayName("로그_존재하지_않으면_예외가_발생한다")
        void 로그_존재하지_않으면_예외가_발생한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            Integer logId = 100;
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    projectUuid,
                    logId,
                    "Test Issue",
                    "Test Description",
                    "Bug",
                    "High"
            );

            Project mockProject = Project.builder()
                    .projectName("Test Project")
                    .projectUuid(projectUuid)
                    .build();
            try {
                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockProject, projectId);
            } catch (Exception e) {
                throw new RuntimeException("테스트 데이터 설정 실패", e);
            }

            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
            willThrow(new BusinessException(GlobalErrorCode.NOT_FOUND))
                    .given(jiraValidator).validateLogExists(logId);

            // when & then
            assertThatThrownBy(() -> jiraIssueService.createIssue(request))
                    .isInstanceOf(BusinessException.class);

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(1)).validateLogExists(logId);
            verify(jiraConnectionRepository, times(0)).findByProjectId(any());
        }

        @Test
        @DisplayName("Jira_연동_정보_없으면_예외가_발생한다")
        void Jira_연동_정보_없으면_예외가_발생한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            Integer logId = 100;
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    projectUuid,
                    logId,
                    "Test Issue",
                    "Test Description",
                    "Bug",
                    "High"
            );

            Project mockProject = Project.builder()
                    .projectName("Test Project")
                    .projectUuid(projectUuid)
                    .build();
            try {
                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockProject, projectId);
            } catch (Exception e) {
                throw new RuntimeException("테스트 데이터 설정 실패", e);
            }

            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
            willDoNothing().given(jiraValidator).validateLogExists(logId);
            given(jiraConnectionRepository.findByProjectId(projectId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> jiraIssueService.createIssue(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(GlobalErrorCode.NOT_FOUND.getMessage());

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(1)).validateLogExists(logId);
            verify(jiraConnectionRepository, times(1)).findByProjectId(projectId);
            verify(encryptionUtils, times(0)).decrypt(anyString());
            verify(jiraApiClient, times(0)).createIssue(anyString(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Jira_API_호출_실패_시_예외가_발생한다")
        void Jira_API_호출_실패_시_예외가_발생한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            Integer logId = 100;
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    projectUuid,
                    logId,
                    "Test Issue",
                    "Test Description",
                    "Bug",
                    "High"
            );

            Project mockProject = Project.builder()
                    .projectName("Test Project")
                    .projectUuid(projectUuid)
                    .build();
            try {
                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(mockProject, projectId);
            } catch (Exception e) {
                throw new RuntimeException("테스트 데이터 설정 실패", e);
            }

            String jiraUrl = "https://test.atlassian.net";
            String jiraEmail = "admin@example.com";
            String encryptedToken = "encrypted-token";
            String decryptedToken = "decrypted-token";
            String jiraProjectKey = "TEST";

            JiraConnection connection = JiraConnection.builder()
                    .projectId(projectId)
                    .jiraUrl(jiraUrl)
                    .jiraEmail(jiraEmail)
                    .jiraApiToken(encryptedToken)
                    .jiraProjectKey(jiraProjectKey)
                    .build();

            JiraIssueRequest jiraRequest = new JiraIssueRequest(
                    new JiraIssueRequest.Fields(
                            new JiraIssueRequest.Project(jiraProjectKey),
                            "Test Issue",
                            new JiraIssueRequest.Description("doc", 1, List.of()),
                            new JiraIssueRequest.IssueType("Bug"),
                            new JiraIssueRequest.Priority("High")
                    )
            );

            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
            willDoNothing().given(jiraValidator).validateLogExists(logId);
            given(jiraConnectionRepository.findByProjectId(projectId)).willReturn(Optional.of(connection));
            given(encryptionUtils.decrypt(encryptedToken)).willReturn(decryptedToken);
            given(jiraMapper.toJiraApiRequest(any(), eq(jiraProjectKey), anyString())).willReturn(jiraRequest);
            willThrow(new BusinessException(JiraErrorCode.JIRA_API_ERROR))
                    .given(jiraApiClient).createIssue(jiraUrl, jiraEmail, decryptedToken, jiraRequest);

            // when & then
            assertThatThrownBy(() -> jiraIssueService.createIssue(request))
                    .isInstanceOf(BusinessException.class);

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(1)).validateLogExists(logId);
            verify(jiraConnectionRepository, times(1)).findByProjectId(projectId);
            verify(encryptionUtils, times(1)).decrypt(encryptedToken);
            verify(jiraMapper, times(1)).toJiraApiRequest(any(), eq(jiraProjectKey), anyString());
            verify(jiraApiClient, times(1)).createIssue(jiraUrl, jiraEmail, decryptedToken, jiraRequest);
            verify(authenticationHelper, times(0)).getCurrentUser();
        }

    }
}
