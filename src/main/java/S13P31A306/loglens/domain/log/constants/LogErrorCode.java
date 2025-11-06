package S13P31A306.loglens.domain.log.constants;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LogErrorCode implements ErrorCode {
    // 400 Bad Request
    PROJECT_UUID_REQUIRED("LG400-09", "project-uuid는 필수입니다.", 400),
    INVALID_CURSOR("LG400-10", "커서 형식이 올바르지 않습니다.", 400),
    INVALID_SIZE("LG400-11", "size는 1 이상 100 이하여야 합니다.", 400),
    INVALID_START_TIME_FORMAT("LG400-12", "startTime 형식이 올바르지 않습니다.", 400),
    INVALID_END_TIME_FORMAT("LG400-13", "endTime 형식이 올바르지 않습니다.", 400),
    INVALID_TIME_RANGE("LG400-14", "startTime은 endTime보다 이전이어야 합니다.", 400),
    INVALID_LOG_LEVEL("LG400-15", "로그 레벨은 ERROR, WARN, INFO 중 하나여야 합니다.", 400),
    INVALID_SOURCE_TYPE("LG400-16", "로그 출처는 FE, BE, INFRA 중 하나여야 합니다.", 400),
    INVALID_SORT("LG400-17", "정렬 기준이 유효하지 않습니다.", 400),
    CURSOR_ENCODING_FAILED("LG400-18", "커서 인코딩에 실패했습니다.", 400),

    // 403 Forbidden
    PROJECT_FORBIDDEN("LG403-01", "해당 프로젝트에 대한 접근 권한이 없습니다.", 403);

    private final String code;
    private final String message;
    private final int status;
}
