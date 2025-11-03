package S13P31A306.loglens.domain.jira.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Jira 연동 성공 코드 정의
 * 코드 형식: JI{HTTP상태코드}[-{서브코드}]
 */
@Getter
@RequiredArgsConstructor
public enum JiraSuccessCode implements SuccessCode {

    // 201 Created
    JIRA_CONNECT_SUCCESS("JI201-1", "Jira 연동이 성공적으로 설정되었습니다.", 201),
    JIRA_ISSUE_CREATE_SUCCESS("JI201-2", "Jira 이슈가 성공적으로 생성되었습니다.", 201);

    private final String code;
    private final String message;
    private final int status;
}
