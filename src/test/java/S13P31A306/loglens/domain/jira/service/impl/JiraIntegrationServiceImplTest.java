package S13P31A306.loglens.domain.jira.service.impl;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.jira.client.JiraApiClient;
import S13P31A306.loglens.domain.jira.constants.JiraErrorCode;
import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectionTestResponse;
import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import S13P31A306.loglens.domain.jira.mapper.JiraMapper;
import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
import S13P31A306.loglens.domain.jira.validator.JiraValidator;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * JiraIntegrationServiceImpl 테스트
 */
@ExtendWith(MockitoExtension.class)
class JiraIntegrationServiceImplTest {

    @InjectMocks
    private JiraIntegrationServiceImpl jiraIntegrationService;

    @Mock
    private AuthenticationHelper authenticationHelper;

    @Mock
    private JiraConnectionRepository jiraConnectionRepository;

    @Mock
    private JiraApiClient jiraApiClient;

    @Mock
    private JiraValidator jiraValidator;

    @Mock
    private JiraMapper jiraMapper;

    @Mock
    private EncryptionUtils encryptionUtils;

    @Nested
    @DisplayName("Jira 연동 테스트")
    class ConnectTest {

        @Test
        @DisplayName("Jira_연동_성공_시_JiraConnectResponse를_반환한다")
        void Jira_연동_성공_시_JiraConnectResponse를_반환한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            String jiraUrl = "https://test.atlassian.net";
            String jiraEmail = "test@example.com";
            String jiraApiToken = "test-token";
            String jiraProjectKey = "TEST";

            JiraConnectRequest request = new JiraConnectRequest(
                    projectId,
                    jiraUrl,
                    jiraEmail,
                    jiraApiToken,
                    jiraProjectKey
            );

            String encryptedToken = "encrypted-token";
            JiraConnection savedConnection = JiraConnection.builder()
                    .projectId(projectId)
                    .jiraUrl(jiraUrl)
                    .jiraEmail(jiraEmail)
                    .jiraApiToken(encryptedToken)
                    .jiraProjectKey(jiraProjectKey)
                    .build();

            JiraConnectionTestResponse testResponse = new JiraConnectionTestResponse(
                    "SUCCESS",
                    "Jira 연결이 성공적으로 테스트되었습니다.",
                    "2025-11-02T10:30:00Z"
            );

            JiraConnectResponse expectedResponse = new JiraConnectResponse(
                    1,
                    projectId,
                    jiraUrl,
                    jiraEmail,
                    jiraProjectKey,
                    testResponse
            );

            // Mocking
            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
            willDoNothing().given(jiraValidator).validateDuplicateConnection(projectId);
            given(encryptionUtils.encrypt(jiraApiToken)).willReturn(encryptedToken);
            given(jiraApiClient.testConnection(jiraUrl, jiraEmail, jiraApiToken, jiraProjectKey))
                    .willReturn(true);
            given(jiraMapper.toEntity(request, encryptedToken)).willReturn(savedConnection);
            given(jiraConnectionRepository.save(any(JiraConnection.class))).willReturn(savedConnection);
            given(jiraMapper.toConnectResponse(savedConnection)).willReturn(expectedResponse);

            // when
            JiraConnectResponse response = jiraIntegrationService.connect(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.projectId()).isEqualTo(projectId);
            assertThat(response.jiraUrl()).isEqualTo(jiraUrl);
            assertThat(response.jiraEmail()).isEqualTo(jiraEmail);
            assertThat(response.jiraProjectKey()).isEqualTo(jiraProjectKey);
            assertThat(response.connectionTest()).isNotNull();
            assertThat(response.connectionTest().status()).isEqualTo("SUCCESS");

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(1)).validateDuplicateConnection(projectId);
            verify(encryptionUtils, times(1)).encrypt(jiraApiToken);
            verify(jiraApiClient, times(1)).testConnection(jiraUrl, jiraEmail, jiraApiToken, jiraProjectKey);
            verify(jiraMapper, times(1)).toEntity(request, encryptedToken);
            verify(jiraConnectionRepository, times(1)).save(any(JiraConnection.class));
            verify(jiraMapper, times(1)).toConnectResponse(savedConnection);
        }

        @Test
        @DisplayName("프로젝트_접근_권한_없으면_예외가_발생한다")
        void 프로젝트_접근_권한_없으면_예외가_발생한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            JiraConnectRequest request = new JiraConnectRequest(
                    projectId,
                    "https://test.atlassian.net",
                    "test@example.com",
                    "test-token",
                    "TEST"
            );

            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            willThrow(new BusinessException(GlobalErrorCode.FORBIDDEN))
                    .given(jiraValidator).validateProjectAccess(projectId, userId);

            // when & then
            assertThatThrownBy(() -> jiraIntegrationService.connect(request))
                    .isInstanceOf(BusinessException.class);

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(0)).validateDuplicateConnection(any());
            verify(encryptionUtils, times(0)).encrypt(anyString());
            verify(jiraApiClient, times(0)).testConnection(anyString(), anyString(), anyString(), anyString());
            verify(jiraConnectionRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("중복_연동_시_예외가_발생한다")
        void 중복_연동_시_예외가_발생한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            JiraConnectRequest request = new JiraConnectRequest(
                    projectId,
                    "https://test.atlassian.net",
                    "test@example.com",
                    "test-token",
                    "TEST"
            );

            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
            willThrow(new BusinessException(JiraErrorCode.JIRA_CONNECTION_ALREADY_EXISTS))
                    .given(jiraValidator).validateDuplicateConnection(projectId);

            // when & then
            assertThatThrownBy(() -> jiraIntegrationService.connect(request))
                    .isInstanceOf(BusinessException.class);

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(1)).validateDuplicateConnection(projectId);
            verify(encryptionUtils, times(0)).encrypt(anyString());
            verify(jiraApiClient, times(0)).testConnection(anyString(), anyString(), anyString(), anyString());
            verify(jiraConnectionRepository, times(0)).save(any());
        }

        @Test
        @DisplayName("Jira_연결_테스트_실패_시_예외가_발생한다")
        void Jira_연결_테스트_실패_시_예외가_발생한다() {
            // given
            Integer userId = 1;
            Integer projectId = 1;
            String jiraUrl = "https://test.atlassian.net";
            String jiraEmail = "test@example.com";
            String jiraApiToken = "test-token";
            String jiraProjectKey = "TEST";

            JiraConnectRequest request = new JiraConnectRequest(
                    projectId,
                    jiraUrl,
                    jiraEmail,
                    jiraApiToken,
                    jiraProjectKey
            );

            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
            willDoNothing().given(jiraValidator).validateDuplicateConnection(projectId);
            given(jiraApiClient.testConnection(jiraUrl, jiraEmail, jiraApiToken, jiraProjectKey))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> jiraIntegrationService.connect(request))
                    .isInstanceOf(BusinessException.class);

            // verify
            verify(authenticationHelper, times(1)).getCurrentUserId();
            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
            verify(jiraValidator, times(1)).validateDuplicateConnection(projectId);
            verify(jiraApiClient, times(1)).testConnection(jiraUrl, jiraEmail, jiraApiToken, jiraProjectKey);
            verify(encryptionUtils, times(0)).encrypt(anyString());
            verify(jiraConnectionRepository, times(0)).save(any());
        }
    }
}
