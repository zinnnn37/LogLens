//package S13P31A306.loglens.domain.jira.service.impl;
//
//import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
//import S13P31A306.loglens.domain.jira.client.JiraApiClient;
//import S13P31A306.loglens.domain.jira.constants.JiraErrorCode;
//import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
//import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
//import S13P31A306.loglens.domain.jira.dto.response.JiraConnectionStatusResponse;
//import S13P31A306.loglens.domain.jira.dto.response.JiraConnectionTestResponse;
//import S13P31A306.loglens.domain.jira.entity.JiraConnection;
//import S13P31A306.loglens.domain.jira.mapper.JiraMapper;
//import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
//import S13P31A306.loglens.domain.jira.validator.JiraValidator;
//import S13P31A306.loglens.domain.project.entity.Project;
//import S13P31A306.loglens.domain.project.repository.ProjectRepository;
//import S13P31A306.loglens.global.constants.GlobalErrorCode;
//import S13P31A306.loglens.global.exception.BusinessException;
//import S13P31A306.loglens.global.utils.EncryptionUtils;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.*;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//
///**
// * JiraIntegrationServiceImpl 테스트
// */
//@ExtendWith(MockitoExtension.class)
//class JiraIntegrationServiceImplTest {
//
//    @InjectMocks
//    private JiraIntegrationServiceImpl jiraIntegrationService;
//
//    @Mock
//    private AuthenticationHelper authenticationHelper;
//
//    @Mock
//    private JiraConnectionRepository jiraConnectionRepository;
//
//    @Mock
//    private ProjectRepository projectRepository;
//
//    @Mock
//    private JiraApiClient jiraApiClient;
//
//    @Mock
//    private JiraValidator jiraValidator;
//
//    @Mock
//    private JiraMapper jiraMapper;
//
//    @Mock
//    private EncryptionUtils encryptionUtils;
//
//    @Nested
//    @DisplayName("Jira 연동 테스트")
//    class ConnectTest {
//
//        @Test
//        @DisplayName("Jira_연동_성공_시_JiraConnectResponse를_반환한다")
//        void Jira_연동_성공_시_JiraConnectResponse를_반환한다() {
//            // given
//            Integer userId = 1;
//            Integer projectId = 1;
//            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//            String jiraUrl = "https://test.atlassian.net";
//            String jiraEmail = "test@example.com";
//            String jiraApiToken = "test-token";
//            String jiraProjectKey = "TEST";
//
//            JiraConnectRequest request = new JiraConnectRequest(
//                    projectUuid,
//                    jiraUrl,
//                    jiraEmail,
//                    jiraApiToken,
//                    jiraProjectKey
//            );
//
//            Project mockProject = Project.builder()
//                    .projectName("Test Project")
//                    .projectUuid(projectUuid)
//                    .build();
//            // Reflection으로 id 설정
//            try {
//                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(mockProject, projectId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            String encryptedToken = "encrypted-token";
//            JiraConnection savedConnection = JiraConnection.builder()
//                    .projectId(projectId)
//                    .jiraUrl(jiraUrl)
//                    .jiraEmail(jiraEmail)
//                    .jiraApiToken(encryptedToken)
//                    .jiraProjectKey(jiraProjectKey)
//                    .build();
//
//            JiraConnectionTestResponse testResponse = new JiraConnectionTestResponse(
//                    "SUCCESS",
//                    "Jira 연결이 성공적으로 테스트되었습니다.",
//                    "2025-11-02T10:30:00Z"
//            );
//
//            JiraConnectResponse expectedResponse = new JiraConnectResponse(
//                    1,
//                    projectUuid,
//                    jiraUrl,
//                    jiraEmail,
//                    jiraProjectKey,
//                    testResponse
//            );
//
//            // Mocking
//            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
//            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
//            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
//            willDoNothing().given(jiraValidator).validateDuplicateConnection(projectId);
//            given(encryptionUtils.encrypt(jiraApiToken)).willReturn(encryptedToken);
//            given(jiraApiClient.testConnection(jiraUrl, jiraEmail, jiraApiToken, jiraProjectKey))
//                    .willReturn(true);
//            given(jiraMapper.toEntity(request, projectId, encryptedToken)).willReturn(savedConnection);
//            given(jiraConnectionRepository.save(any(JiraConnection.class))).willReturn(savedConnection);
//            given(jiraMapper.toConnectResponse(savedConnection, projectUuid)).willReturn(expectedResponse);
//
//            // when
//            JiraConnectResponse response = jiraIntegrationService.connect(request);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.projectUuid()).isEqualTo(projectUuid);
//            assertThat(response.jiraUrl()).isEqualTo(jiraUrl);
//            assertThat(response.jiraEmail()).isEqualTo(jiraEmail);
//            assertThat(response.jiraProjectKey()).isEqualTo(jiraProjectKey);
//            assertThat(response.connectionTest()).isNotNull();
//            assertThat(response.connectionTest().status()).isEqualTo("SUCCESS");
//
//            // verify
//            verify(authenticationHelper, times(1)).getCurrentUserId();
//            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
//            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
//            verify(jiraValidator, times(1)).validateDuplicateConnection(projectId);
//            verify(encryptionUtils, times(1)).encrypt(jiraApiToken);
//            verify(jiraApiClient, times(1)).testConnection(jiraUrl, jiraEmail, jiraApiToken, jiraProjectKey);
//            verify(jiraMapper, times(1)).toEntity(request, projectId, encryptedToken);
//            verify(jiraConnectionRepository, times(1)).save(any(JiraConnection.class));
//            verify(jiraMapper, times(1)).toConnectResponse(savedConnection, projectUuid);
//        }
//
//        @Test
//        @DisplayName("프로젝트_접근_권한_없으면_예외가_발생한다")
//        void 프로젝트_접근_권한_없으면_예외가_발생한다() {
//            // given
//            Integer userId = 1;
//            Integer projectId = 1;
//            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//            JiraConnectRequest request = new JiraConnectRequest(
//                    projectUuid,
//                    "https://test.atlassian.net",
//                    "test@example.com",
//                    "test-token",
//                    "TEST"
//            );
//
//            Project mockProject = Project.builder()
//                    .projectName("Test Project")
//                    .projectUuid(projectUuid)
//                    .build();
//            try {
//                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(mockProject, projectId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
//            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
//            willThrow(new BusinessException(GlobalErrorCode.FORBIDDEN))
//                    .given(jiraValidator).validateProjectAccess(projectId, userId);
//
//            // when & then
//            assertThatThrownBy(() -> jiraIntegrationService.connect(request))
//                    .isInstanceOf(BusinessException.class);
//
//            // verify
//            verify(authenticationHelper, times(1)).getCurrentUserId();
//            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
//            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
//            verify(jiraValidator, times(0)).validateDuplicateConnection(any());
//            verify(encryptionUtils, times(0)).encrypt(anyString());
//            verify(jiraApiClient, times(0)).testConnection(anyString(), anyString(), anyString(), anyString());
//            verify(jiraConnectionRepository, times(0)).save(any());
//        }
//
//        @Test
//        @DisplayName("중복_연동_시_예외가_발생한다")
//        void 중복_연동_시_예외가_발생한다() {
//            // given
//            Integer userId = 1;
//            Integer projectId = 1;
//            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//            JiraConnectRequest request = new JiraConnectRequest(
//                    projectUuid,
//                    "https://test.atlassian.net",
//                    "test@example.com",
//                    "test-token",
//                    "TEST"
//            );
//
//            Project mockProject = Project.builder()
//                    .projectName("Test Project")
//                    .projectUuid(projectUuid)
//                    .build();
//            try {
//                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(mockProject, projectId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
//            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
//            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
//            willThrow(new BusinessException(JiraErrorCode.JIRA_CONNECTION_ALREADY_EXISTS))
//                    .given(jiraValidator).validateDuplicateConnection(projectId);
//
//            // when & then
//            assertThatThrownBy(() -> jiraIntegrationService.connect(request))
//                    .isInstanceOf(BusinessException.class);
//
//            // verify
//            verify(authenticationHelper, times(1)).getCurrentUserId();
//            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
//            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
//            verify(jiraValidator, times(1)).validateDuplicateConnection(projectId);
//            verify(encryptionUtils, times(0)).encrypt(anyString());
//            verify(jiraApiClient, times(0)).testConnection(anyString(), anyString(), anyString(), anyString());
//            verify(jiraConnectionRepository, times(0)).save(any());
//        }
//
//        @Test
//        @DisplayName("긴_API_토큰_암호화_후_저장_성공")
//        void 긴_API_토큰_암호화_후_저장_성공() {
//            // given
//            Integer userId = 1;
//            Integer projectId = 1;
//            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//            String jiraUrl = "https://test.atlassian.net";
//            String jiraEmail = "test@example.com";
//
//            // 실제 Atlassian API 토큰 형식 (204자)
//            String longJiraApiToken = "ATATT3xFfGF0hVy8_m6ZWYwkHYBHTY74D1RDeRIyJc_-o_lCiueEGR7TeRLLgqiMpW6rPKLS17MAJYzFMn_ITjwQnAIImlOaZHOJPP5f_qbFdpB-BDzPg78ITOrnyQ3hyC8o5QNeUwOZzQsajPCkMnrGHw5DWrqF4v4jNa1BuOpI06Pk3EwvHRQ=5F3E4DAA";
//            String jiraProjectKey = "S13P31A306";
//
//            // 암호화된 토큰 (280자)
//            String encryptedLongToken = "a".repeat(280); // 암호화 후 280자가 되는 시나리오
//
//            JiraConnectRequest request = new JiraConnectRequest(
//                    projectUuid,
//                    jiraUrl,
//                    jiraEmail,
//                    longJiraApiToken,
//                    jiraProjectKey
//            );
//
//            Project mockProject = Project.builder()
//                    .projectName("Test Project")
//                    .projectUuid(projectUuid)
//                    .build();
//            // Reflection으로 id 설정
//            try {
//                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(mockProject, projectId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            JiraConnection savedConnection = JiraConnection.builder()
//                    .projectId(projectId)
//                    .jiraUrl(jiraUrl)
//                    .jiraEmail(jiraEmail)
//                    .jiraApiToken(encryptedLongToken)
//                    .jiraProjectKey(jiraProjectKey)
//                    .build();
//
//            JiraConnectionTestResponse testResponse = new JiraConnectionTestResponse(
//                    "SUCCESS",
//                    "Jira 연결이 성공적으로 테스트되었습니다.",
//                    "2025-11-02T10:30:00Z"
//            );
//
//            JiraConnectResponse expectedResponse = new JiraConnectResponse(
//                    1,
//                    projectUuid,
//                    jiraUrl,
//                    jiraEmail,
//                    jiraProjectKey,
//                    testResponse
//            );
//
//            // Mocking
//            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
//            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
//            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
//            willDoNothing().given(jiraValidator).validateDuplicateConnection(projectId);
//            given(encryptionUtils.encrypt(longJiraApiToken)).willReturn(encryptedLongToken);
//            given(jiraApiClient.testConnection(jiraUrl, jiraEmail, longJiraApiToken, jiraProjectKey))
//                    .willReturn(true);
//            given(jiraMapper.toEntity(request, projectId, encryptedLongToken)).willReturn(savedConnection);
//            given(jiraConnectionRepository.save(any(JiraConnection.class))).willReturn(savedConnection);
//            given(jiraMapper.toConnectResponse(savedConnection, projectUuid)).willReturn(expectedResponse);
//
//            // when
//            JiraConnectResponse response = jiraIntegrationService.connect(request);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.projectUuid()).isEqualTo(projectUuid);
//            assertThat(response.jiraUrl()).isEqualTo(jiraUrl);
//            assertThat(response.jiraEmail()).isEqualTo(jiraEmail);
//            assertThat(response.jiraProjectKey()).isEqualTo(jiraProjectKey);
//            assertThat(response.connectionTest()).isNotNull();
//            assertThat(response.connectionTest().status()).isEqualTo("SUCCESS");
//
//            // 암호화된 토큰 길이가 512자를 넘지 않는지 검증
//            assertThat(encryptedLongToken.length()).isLessThanOrEqualTo(512);
//
//            // verify
//            verify(authenticationHelper, times(1)).getCurrentUserId();
//            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
//            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
//            verify(jiraValidator, times(1)).validateDuplicateConnection(projectId);
//            verify(encryptionUtils, times(1)).encrypt(longJiraApiToken);
//            verify(jiraApiClient, times(1)).testConnection(jiraUrl, jiraEmail, longJiraApiToken, jiraProjectKey);
//            verify(jiraMapper, times(1)).toEntity(request, projectId, encryptedLongToken);
//            verify(jiraConnectionRepository, times(1)).save(any(JiraConnection.class));
//            verify(jiraMapper, times(1)).toConnectResponse(savedConnection, projectUuid);
//        }
//
//        @Test
//        @DisplayName("Jira_연결_테스트_실패_시_예외가_발생한다")
//        void Jira_연결_테스트_실패_시_예외가_발생한다() {
//            // given
//            Integer userId = 1;
//            Integer projectId = 1;
//            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//            String jiraUrl = "https://test.atlassian.net";
//            String jiraEmail = "test@example.com";
//            String jiraApiToken = "test-token";
//            String jiraProjectKey = "TEST";
//
//            JiraConnectRequest request = new JiraConnectRequest(
//                    projectUuid,
//                    jiraUrl,
//                    jiraEmail,
//                    jiraApiToken,
//                    jiraProjectKey
//            );
//
//            Project mockProject = Project.builder()
//                    .projectName("Test Project")
//                    .projectUuid(projectUuid)
//                    .build();
//            try {
//                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(mockProject, projectId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
//            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
//            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
//            willDoNothing().given(jiraValidator).validateDuplicateConnection(projectId);
//            given(jiraApiClient.testConnection(jiraUrl, jiraEmail, jiraApiToken, jiraProjectKey))
//                    .willReturn(false);
//
//            // when & then
//            assertThatThrownBy(() -> jiraIntegrationService.connect(request))
//                    .isInstanceOf(BusinessException.class);
//
//            // verify
//            verify(authenticationHelper, times(1)).getCurrentUserId();
//            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
//            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
//            verify(jiraValidator, times(1)).validateDuplicateConnection(projectId);
//            verify(jiraApiClient, times(1)).testConnection(jiraUrl, jiraEmail, jiraApiToken, jiraProjectKey);
//            verify(encryptionUtils, times(0)).encrypt(anyString());
//            verify(jiraConnectionRepository, times(0)).save(any());
//        }
//    }
//
//    @Nested
//    @DisplayName("Jira 연동 상태 조회 테스트")
//    class GetConnectionStatusTest {
//
//        @Test
//        @DisplayName("Jira_연동_존재_시_상태_정보를_반환한다")
//        void Jira_연동_존재_시_상태_정보를_반환한다() {
//            // given
//            Integer userId = 1;
//            Integer projectId = 1;
//            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//            Integer connectionId = 10;
//            String jiraProjectKey = "LOGLENS";
//
//            Project mockProject = Project.builder()
//                    .projectName("Test Project")
//                    .projectUuid(projectUuid)
//                    .build();
//            try {
//                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(mockProject, projectId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            JiraConnection connection = JiraConnection.builder()
//                    .projectId(projectId)
//                    .jiraUrl("https://test.atlassian.net")
//                    .jiraEmail("test@example.com")
//                    .jiraApiToken("encrypted-token")
//                    .jiraProjectKey(jiraProjectKey)
//                    .build();
//
//            // Reflection을 사용하여 id 필드 설정 (BaseEntity의 private id 필드)
//            try {
//                java.lang.reflect.Field idField = connection.getClass().getSuperclass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(connection, connectionId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            // Mocking
//            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
//            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
//            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
//            given(jiraConnectionRepository.findByProjectId(projectId))
//                    .willReturn(Optional.of(connection));
//
//            // when
//            JiraConnectionStatusResponse response = jiraIntegrationService.getConnectionStatus(projectUuid);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.exists()).isTrue();
//            assertThat(response.projectUuid()).isEqualTo(projectUuid);
//            assertThat(response.connectionId()).isEqualTo(connectionId);
//            assertThat(response.jiraProjectKey()).isEqualTo(jiraProjectKey);
//
//            // verify
//            verify(authenticationHelper, times(1)).getCurrentUserId();
//            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
//            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
//            verify(jiraConnectionRepository, times(1)).findByProjectId(projectId);
//        }
//
//        @Test
//        @DisplayName("Jira_연동_없음_시_상태_정보를_반환한다")
//        void Jira_연동_없음_시_상태_정보를_반환한다() {
//            // given
//            Integer userId = 1;
//            Integer projectId = 1;
//            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//
//            Project mockProject = Project.builder()
//                    .projectName("Test Project")
//                    .projectUuid(projectUuid)
//                    .build();
//            try {
//                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(mockProject, projectId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            // Mocking
//            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
//            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
//            willDoNothing().given(jiraValidator).validateProjectAccess(projectId, userId);
//            given(jiraConnectionRepository.findByProjectId(projectId))
//                    .willReturn(Optional.empty());
//
//            // when
//            JiraConnectionStatusResponse response = jiraIntegrationService.getConnectionStatus(projectUuid);
//
//            // then
//            assertThat(response).isNotNull();
//            assertThat(response.exists()).isFalse();
//            assertThat(response.projectUuid()).isEqualTo(projectUuid);
//            assertThat(response.connectionId()).isNull();
//            assertThat(response.jiraProjectKey()).isNull();
//
//            // verify
//            verify(authenticationHelper, times(1)).getCurrentUserId();
//            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
//            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
//            verify(jiraConnectionRepository, times(1)).findByProjectId(projectId);
//        }
//
//        @Test
//        @DisplayName("프로젝트_접근_권한_없으면_예외가_발생한다")
//        void 프로젝트_접근_권한_없으면_예외가_발생한다() {
//            // given
//            Integer userId = 1;
//            Integer projectId = 1;
//            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//
//            Project mockProject = Project.builder()
//                    .projectName("Test Project")
//                    .projectUuid(projectUuid)
//                    .build();
//            try {
//                java.lang.reflect.Field idField = mockProject.getClass().getDeclaredField("id");
//                idField.setAccessible(true);
//                idField.set(mockProject, projectId);
//            } catch (Exception e) {
//                throw new RuntimeException("테스트 데이터 설정 실패", e);
//            }
//
//            given(authenticationHelper.getCurrentUserId()).willReturn(userId);
//            given(projectRepository.findByProjectUuid(projectUuid)).willReturn(Optional.of(mockProject));
//            willThrow(new BusinessException(GlobalErrorCode.FORBIDDEN))
//                    .given(jiraValidator).validateProjectAccess(projectId, userId);
//
//            // when & then
//            assertThatThrownBy(() -> jiraIntegrationService.getConnectionStatus(projectUuid))
//                    .isInstanceOf(BusinessException.class);
//
//            // verify
//            verify(authenticationHelper, times(1)).getCurrentUserId();
//            verify(projectRepository, times(1)).findByProjectUuid(projectUuid);
//            verify(jiraValidator, times(1)).validateProjectAccess(projectId, userId);
//            verify(jiraConnectionRepository, times(0)).findByProjectId(any());
//        }
//    }
//}
