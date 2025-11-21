package S13P31A306.loglens.domain.jira.constants;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Jira 연동 에러 코드 정의
 * 코드 형식: JI{HTTP상태코드}[-{서브코드}]
 */
@Getter
@RequiredArgsConstructor
public enum JiraErrorCode implements ErrorCode {

    // 400 Bad Request
    PROJECT_ID_REQUIRED("JI400-1", "프로젝트 ID는 필수입니다.", 400),
    JIRA_URL_REQUIRED("JI400-2", "Jira URL은 필수입니다.", 400),
    JIRA_URL_INVALID_FORMAT("JI400-3", "Jira URL 형식이 올바르지 않습니다.", 400),
    JIRA_URL_TOO_LONG("JI400-4", "Jira URL은 최대 255자까지 입력 가능합니다.", 400),
    JIRA_EMAIL_REQUIRED("JI400-5", "Jira 이메일은 필수입니다.", 400),
    JIRA_EMAIL_INVALID_FORMAT("JI400-6", "Jira 이메일 형식이 올바르지 않습니다.", 400),
    JIRA_EMAIL_TOO_LONG("JI400-7", "Jira 이메일은 최대 255자까지 입력 가능합니다.", 400),
    JIRA_API_TOKEN_REQUIRED("JI400-8", "Jira API 토큰은 필수입니다.", 400),
    JIRA_API_TOKEN_TOO_LONG("JI400-9", "Jira API 토큰은 최대 255자까지 입력 가능합니다.", 400),
    JIRA_PROJECT_KEY_REQUIRED("JI400-10", "Jira 프로젝트 키는 필수입니다.", 400),
    JIRA_PROJECT_KEY_INVALID_FORMAT("JI400-11", "Jira 프로젝트 키 형식이 올바르지 않습니다.", 400),
    JIRA_PROJECT_KEY_TOO_LONG("JI400-12", "Jira 프로젝트 키는 최대 255자까지 입력 가능합니다.", 400),
    LOG_ID_REQUIRED("JI400-13", "로그 ID는 필수입니다.", 400),
    SUMMARY_REQUIRED("JI400-14", "이슈 제목은 필수입니다.", 400),
    SUMMARY_LENGTH_INVALID("JI400-15", "이슈 제목은 1-255자 이내여야 합니다.", 400),
    DESCRIPTION_TOO_LONG("JI400-16", "이슈 설명은 최대 32,767자까지 입력 가능합니다.", 400),
    ISSUE_TYPE_INVALID("JI400-17", "유효하지 않은 이슈 타입입니다. (허용값: Bug, Task, Story, Epic)", 400),
    PRIORITY_INVALID("JI400-18", "유효하지 않은 우선순위입니다. (허용값: Highest, High, Medium, Low, Lowest)", 400),
    LABELS_TOO_MANY("JI400-19", "레이블은 최대 10개까지 지정 가능합니다.", 400),

    // 404 Not Found
    PROJECT_NOT_FOUND("JI404-1", "프로젝트를 찾을 수 없습니다.", 404),
    LOG_NOT_FOUND("JI404-2", "로그를 찾을 수 없습니다.", 404),
    JIRA_CONNECTION_NOT_FOUND("JI404-3", "프로젝트에 Jira 연동이 설정되지 않았습니다.", 404),

    // 409 Conflict
    JIRA_CONNECTION_ALREADY_EXISTS("JI409-1", "해당 프로젝트는 이미 Jira와 연동되어 있습니다.", 409),

    // 502 Bad Gateway
    JIRA_API_CONNECTION_FAILED("JI502-1", "Jira 서버에 연결할 수 없습니다. URL과 네트워크 연결을 확인해주세요.", 502),
    JIRA_API_AUTHENTICATION_FAILED("JI502-2", "Jira 인증에 실패했습니다. 이메일과 API 토큰을 확인해주세요.", 502),
    JIRA_PROJECT_NOT_FOUND("JI502-3", "Jira 프로젝트를 찾을 수 없습니다. 프로젝트 키를 확인해주세요.", 502),
    JIRA_API_ERROR("JI502-4", "Jira API 호출 중 오류가 발생했습니다.", 502);

    private final String code;
    private final String message;
    private final int status;
}
