package S13P31A306.loglens.domain.jira.mapper;

import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.jira.client.dto.JiraIssueRequest;
import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.request.JiraIssueCreateRequest;
import S13P31A306.loglens.domain.jira.dto.response.CreatedByResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectionTestResponse;
import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.*;

/**
 * JiraMapper 단위 테스트
 * 타임존 포맷 검증 및 모든 매핑 메서드 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JiraMapper 테스트")
class JiraMapperTest {

    @Autowired
    private JiraMapper jiraMapper;

    @Nested
    @DisplayName("createConnectionTestResponse 메서드 테스트")
    class CreateConnectionTestResponseTest {

        @Test
        @DisplayName("JiraConnectionTestResponse가 올바른 형식으로 생성된다")
        void createConnectionTestResponse_올바른_형식() {
            // when
            JiraConnectionTestResponse response = jiraMapper.createConnectionTestResponse();

            // then
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo("SUCCESS");
            assertThat(response.message()).isEqualTo("Jira 연결이 성공적으로 테스트되었습니다.");
            assertThat(response.testedAt()).isNotNull();
        }

        @Test
        @DisplayName("testedAt이 ISO_OFFSET_DATE_TIME 형식이다")
        void testedAt_ISO_OFFSET_DATE_TIME_형식() {
            // when
            JiraConnectionTestResponse response = jiraMapper.createConnectionTestResponse();

            // then
            // ISO 8601 형식 검증: YYYY-MM-DDTHH:mm:ss+00:00 또는 Z
            assertThat(response.testedAt())
                    .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})");
        }

        @Test
        @DisplayName("testedAt이 UTC 타임존으로 반환된다")
        void testedAt_UTC_타임존() {
            // when
            JiraConnectionTestResponse response = jiraMapper.createConnectionTestResponse();

            // then
            // UTC 타임존 검증 (Z 또는 +00:00)
            assertThat(response.testedAt()).matches(".*Z$|.*\\+00:00$");
        }

        @Test
        @DisplayName("testedAt이 파싱 가능한 형식이다")
        void testedAt_파싱_가능() {
            // when
            JiraConnectionTestResponse response = jiraMapper.createConnectionTestResponse();

            // then
            assertThatCode(() -> {
                ZonedDateTime.parse(response.testedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("연속 호출 시 시간이 증가한다")
        void 연속_호출_시_시간_증가() throws InterruptedException {
            // given
            JiraConnectionTestResponse response1 = jiraMapper.createConnectionTestResponse();
            Thread.sleep(100); // 시간차 보장

            // when
            JiraConnectionTestResponse response2 = jiraMapper.createConnectionTestResponse();

            // then
            ZonedDateTime time1 = ZonedDateTime.parse(response1.testedAt());
            ZonedDateTime time2 = ZonedDateTime.parse(response2.testedAt());
            assertThat(time2).isAfter(time1);
        }
    }

    @Nested
    @DisplayName("toEntity 메서드 테스트")
    class ToEntityTest {

        @Test
        @DisplayName("JiraConnectRequest를 JiraConnection 엔티티로 변환한다")
        void toEntity_정상_변환() {
            // given
            JiraConnectRequest request = new JiraConnectRequest(
                    "test-uuid",
                    "https://test.atlassian.net",
                    "test@example.com",
                    "test-token",
                    "TEST"
            );
            Integer projectId = 1;
            String encryptedToken = "encrypted-token";

            // when
            JiraConnection entity = jiraMapper.toEntity(request, projectId, encryptedToken);

            // then
            assertThat(entity).isNotNull();
            assertThat(entity.getProjectId()).isEqualTo(projectId);
            assertThat(entity.getJiraUrl()).isEqualTo("https://test.atlassian.net");
            assertThat(entity.getJiraEmail()).isEqualTo("test@example.com");
            assertThat(entity.getJiraApiToken()).isEqualTo(encryptedToken);
            assertThat(entity.getJiraProjectKey()).isEqualTo("TEST");
        }
    }

    @Nested
    @DisplayName("toConnectResponse 메서드 테스트")
    class ToConnectResponseTest {

        @Test
        @DisplayName("JiraConnection을 JiraConnectResponse로 변환한다")
        void toConnectResponse_정상_변환() {
            // given
            JiraConnection connection = JiraConnection.builder()
                    .projectId(1)
                    .jiraUrl("https://test.atlassian.net")
                    .jiraEmail("test@example.com")
                    .jiraApiToken("encrypted-token")
                    .jiraProjectKey("TEST")
                    .build();
            String projectUuid = "test-uuid";

            // when
            JiraConnectResponse response = jiraMapper.toConnectResponse(connection, projectUuid);

            // then
            assertThat(response).isNotNull();
            assertThat(response.projectUuid()).isEqualTo(projectUuid);
            assertThat(response.jiraUrl()).isEqualTo("https://test.atlassian.net");
            assertThat(response.jiraEmail()).isEqualTo("test@example.com");
            assertThat(response.jiraProjectKey()).isEqualTo("TEST");
            assertThat(response.connectionTest()).isNotNull();
            assertThat(response.connectionTest().status()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("connectionTest가 올바른 타임존 형식으로 생성된다")
        void connectionTest_타임존_형식() {
            // given
            JiraConnection connection = JiraConnection.builder()
                    .projectId(1)
                    .jiraUrl("https://test.atlassian.net")
                    .jiraEmail("test@example.com")
                    .jiraApiToken("encrypted-token")
                    .jiraProjectKey("TEST")
                    .build();

            // when
            JiraConnectResponse response = jiraMapper.toConnectResponse(connection, "test-uuid");

            // then
            assertThat(response.connectionTest().testedAt())
                    .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})");
        }
    }

    @Nested
    @DisplayName("toCreatedByResponse 메서드 테스트")
    class ToCreatedByResponseTest {

        @Test
        @DisplayName("User를 CreatedByResponse로 변환한다")
        void toCreatedByResponse_정상_변환() {
            // given
            User user = User.builder()
                    .email("user@example.com")
                    .name("Test User")
                    .build();

            // Reflection으로 id 설정
            try {
                java.lang.reflect.Field idField = user.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(user, 1);
            } catch (Exception e) {
                throw new RuntimeException("테스트 데이터 설정 실패", e);
            }

            // when
            CreatedByResponse response = jiraMapper.toCreatedByResponse(user);

            // then
            assertThat(response).isNotNull();
            assertThat(response.userId()).isEqualTo(1);
            assertThat(response.email()).isEqualTo("user@example.com");
            assertThat(response.name()).isEqualTo("Test User");
        }
    }

    @Nested
    @DisplayName("toJiraApiRequest 메서드 테스트")
    class ToJiraApiRequestTest {

        @Test
        @DisplayName("JiraIssueCreateRequest를 JiraIssueRequest로 변환한다")
        void toJiraApiRequest_정상_변환() {
            // given
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    "test-uuid",
                    1L,
                    "Test Issue",
                    "Test Description",
                    "Bug",
                    "High"
            );
            String projectKey = "TEST";
            String logDescription = "Log Info: Error occurred";

            // when
            JiraIssueRequest apiRequest = jiraMapper.toJiraApiRequest(request, projectKey, logDescription);

            // then
            assertThat(apiRequest).isNotNull();
            assertThat(apiRequest.fields()).isNotNull();
            assertThat(apiRequest.fields().project().key()).isEqualTo("TEST");
            assertThat(apiRequest.fields().summary()).isEqualTo("Test Issue");
            assertThat(apiRequest.fields().issuetype().name()).isEqualTo("Bug");
            assertThat(apiRequest.fields().priority().name()).isEqualTo("High");
        }

        @Test
        @DisplayName("description이 null이면 logDescription만 사용한다")
        void toJiraApiRequest_description_null() {
            // given
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    "test-uuid",
                    1L,
                    "Test Issue",
                    null,
                    "Bug",
                    "High"
            );
            String logDescription = "Log Info";

            // when
            JiraIssueRequest apiRequest = jiraMapper.toJiraApiRequest(request, "TEST", logDescription);

            // then
            assertThat(apiRequest.fields().description()).isNotNull();
            assertThat(apiRequest.fields().description().content()).hasSize(1);
            assertThat(apiRequest.fields().description().content().get(0).content()).hasSize(1);
            assertThat(apiRequest.fields().description().content().get(0).content().get(0).text())
                    .isEqualTo("Log Info");
        }

        @Test
        @DisplayName("description이 empty면 logDescription만 사용한다")
        void toJiraApiRequest_description_empty() {
            // given
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    "test-uuid",
                    1L,
                    "Test Issue",
                    "",
                    "Bug",
                    "High"
            );
            String logDescription = "Log Info";

            // when
            JiraIssueRequest apiRequest = jiraMapper.toJiraApiRequest(request, "TEST", logDescription);

            // then
            assertThat(apiRequest.fields().description().content().get(0).content().get(0).text())
                    .isEqualTo("Log Info");
        }

        @Test
        @DisplayName("issueType이 null이면 기본값 Bug를 사용한다")
        void toJiraApiRequest_issueType_null() {
            // given
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    "test-uuid",
                    1L,
                    "Test Issue",
                    "Description",
                    null,
                    "High"
            );

            // when
            JiraIssueRequest apiRequest = jiraMapper.toJiraApiRequest(request, "TEST", "Log Info");

            // then
            assertThat(apiRequest.fields().issuetype().name()).isEqualTo("Bug");
        }

        @Test
        @DisplayName("priority가 null이면 기본값 Medium을 사용한다")
        void toJiraApiRequest_priority_null() {
            // given
            JiraIssueCreateRequest request = new JiraIssueCreateRequest(
                    "test-uuid",
                    1L,
                    "Test Issue",
                    "Description",
                    "Bug",
                    null
            );

            // when
            JiraIssueRequest apiRequest = jiraMapper.toJiraApiRequest(request, "TEST", "Log Info");

            // then
            assertThat(apiRequest.fields().priority().name()).isEqualTo("Medium");
        }
    }
}
