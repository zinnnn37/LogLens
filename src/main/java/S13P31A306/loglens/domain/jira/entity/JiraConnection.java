package S13P31A306.loglens.domain.jira.entity;

import S13P31A306.loglens.global.annotation.Sensitive;
import S13P31A306.loglens.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Jira 연동 정보 엔티티
 * 프로젝트별 Jira 연동 설정을 저장합니다.
 */
@Entity
@Table(name = "jira_connections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class JiraConnection extends BaseEntity {

    /**
     * Jira 인스턴스 URL
     * 예: https://your-domain.atlassian.net
     */
    @Sensitive
    @Column(name = "jira_url", nullable = false, length = 255)
    private String jiraUrl;

    /**
     * Jira 계정 이메일
     * API 인증에 사용됩니다.
     */
    @Sensitive
    @Column(name = "jira_email", nullable = false, length = 255)
    private String jiraEmail;

    /**
     * Jira API 토큰 (암호화 저장)
     * 민감 정보로 로그에 출력되지 않습니다.
     */
    @Sensitive
    @Column(name = "jira_api_token", nullable = false, length = 255)
    private String jiraApiToken;

    /**
     * Jira 프로젝트 키
     * 예: LOGLENS, PROJ
     */
    @Column(name = "jira_project_key", nullable = false, length = 255)
    private String jiraProjectKey;

    /**
     * LogLens 프로젝트 ID (FK)
     * projects 테이블과 연관
     */
    @Column(name = "project_id", nullable = false, columnDefinition = "TINYINT")
    private Integer projectId;
}
