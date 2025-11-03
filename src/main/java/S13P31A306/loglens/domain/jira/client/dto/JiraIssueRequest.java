package S13P31A306.loglens.domain.jira.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * Jira API 이슈 생성 요청 DTO
 * Jira REST API v3 스펙에 맞춘 요청 형식
 *
 * Jira에 이슈를 생성할 때 실제로 보내야 하는 JSON 형식
 *   {
 *     "fields": {
 *       "project": {
 *         "key": "PROJ"
 *       },
 *       "summary": "이슈 제목",
 *       "description": {
 *         "type": "doc",
 *         "version": 1,
 *         "content": [
 *           {
 *             "type": "paragraph",
 *             "content": [
 *               {
 *                 "type": "text",
 *                 "text": "실제 이슈 설명 내용"
 *               }
 *             ]
 *           }
 *         ]
 *       },
 *       "issuetype": {
 *         "name": "Bug"
 *       },
 *       "priority": {
 *         "name": "High"
 *       }
 *     }
 *   }
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JiraIssueRequest(
        Fields fields
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Fields(
            Project project,
            String summary,
            Description description,
            IssueType issuetype,
            Priority priority
    ) {
    }

    public record Project(String key) {
    }

    public record Description(
            String type,
            Integer version,
            List<Content> content
    ) {
        @Builder
        public record Content(
                String type,
                List<Text> content
        ) {
        }

        public record Text(
                String type,
                String text
        ) {
        }
    }

    public record IssueType(String name) {
    }

    public record Priority(String name) {
    }
}
