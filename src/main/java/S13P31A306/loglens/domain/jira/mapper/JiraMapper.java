package S13P31A306.loglens.domain.jira.mapper;

import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.jira.client.dto.JiraIssueRequest;
import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.request.JiraIssueCreateRequest;
import S13P31A306.loglens.domain.jira.dto.response.CreatedByResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectionTestResponse;
import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Jira 연동 Mapper
 * MapStruct를 사용하여 DTO와 Entity 간 변환을 처리합니다.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JiraMapper {

    /**
     * JiraConnectRequest → JiraConnection Entity
     *
     * @param request        연동 요청 DTO
     * @param encryptedToken 암호화된 API 토큰
     * @return JiraConnection Entity
     */
    @Mapping(target = "jiraApiToken", source = "encryptedToken")
    JiraConnection toEntity(JiraConnectRequest request, String encryptedToken);

    /**
     * JiraConnection Entity → JiraConnectResponse
     *
     * @param connection JiraConnection Entity
     * @return JiraConnectResponse DTO
     */
    @Mapping(target = "connectionTest", expression = "java(createConnectionTestResponse())")
    JiraConnectResponse toConnectResponse(JiraConnection connection);

    /**
     * User Entity → CreatedByResponse
     *
     * @param user User Entity
     * @return CreatedByResponse DTO
     */
    @Mapping(target = "userId", source = "id")
    CreatedByResponse toCreatedByResponse(User user);

    /**
     * JiraIssueCreateRequest → Jira API용 JiraIssueRequest
     *
     * @param request       이슈 생성 요청 DTO
     * @param projectKey    Jira 프로젝트 키
     * @param logDescription 로그 정보가 포함된 설명
     * @return Jira API용 요청 DTO
     */
    default JiraIssueRequest toJiraApiRequest(
            JiraIssueCreateRequest request,
            String projectKey,
            String logDescription
    ) {
        // 설명이 없으면 로그 설명 사용, 있으면 결합
        String finalDescription = request.description() != null && !request.description().isEmpty()
                ? request.description() + "\n\n" + logDescription
                : logDescription;

        // Jira API Description 형식 생성
        JiraIssueRequest.Description.Text text = new JiraIssueRequest.Description.Text("text", finalDescription);
        JiraIssueRequest.Description.Content content = JiraIssueRequest.Description.Content.builder()
                .type("paragraph")
                .content(List.of(text))
                .build();
        JiraIssueRequest.Description description = new JiraIssueRequest.Description(
                "doc",
                1,
                List.of(content)
        );

        // Fields 생성 (간소화)
        JiraIssueRequest.Fields fields = JiraIssueRequest.Fields.builder()
                .project(new JiraIssueRequest.Project(projectKey))
                .summary(request.summary())
                .description(description)
                .issuetype(new JiraIssueRequest.IssueType(
                        request.issueType() != null && !request.issueType().isEmpty()
                                ? request.issueType()
                                : "Bug"
                ))
                .priority(new JiraIssueRequest.Priority(
                        request.priority() != null && !request.priority().isEmpty()
                                ? request.priority()
                                : "Medium"
                ))
                .build();

        return new JiraIssueRequest(fields);
    }

    /**
     * 연결 테스트 응답 생성 (헬퍼 메서드)
     */
    default JiraConnectionTestResponse createConnectionTestResponse() {
        return new JiraConnectionTestResponse(
                "SUCCESS",
                "Jira 연결이 성공적으로 테스트되었습니다.",
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
        );
    }
}
