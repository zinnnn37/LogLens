package S13P31A306.loglens.global.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    // 400 Bad Request
    VALIDATION_ERROR("G400", "입력값이 유효하지 않습니다", 400),
    INVALID_FORMAT("G400-1", "요청 형식이 올바르지 않습니다", 400),
    PATH_IS_REQUIRED("H400", "파일 패스가 필요합니다", 400),

    // 401 Unauthorized
    UNAUTHORIZED("G401", "인증이 필요합니다", 401),
    AUTH_INVALID_ID_TOKEN("G401-1", "유효하지 않은 토큰입니다", 401),
    EXPIRED_TOKEN("G401-2", "만료된 토큰입니다", 401),
    TOKEN_SIGNATURE_INVALID("G401-3", "토큰 서명이 유효하지 않습니다", 401),
    OAUTH_ID_TOKEN_INVALID("G401-4", "유효하지 않은 구글 ID 토큰입니다", 401),

    // 403 Forbidden
    FORBIDDEN("G403", "접근 권한이 없습니다", 403),
    USER_DELETED("G403-1", "탈퇴된 회원입니다.", 403),

    // 404 Not Found
    NOT_FOUND("G404", "요청한 리소스를 찾을 수 없습니다", 404),
    PAPER_NOT_FOUND("P404","논문이 없습니다.",404),
    HDFS_NOT_FOUND("H404", "요청한 경로에서 파일을 찾을 수 없습니다", 404),
    PROJECT_NOT_FOUND("P404-1","프로젝트가 없습니다.",404),
    USER_NOT_FOUND("G404-1", "사용자를 찾을 수 없습니다.", 404),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED("G405", "허용되지 않은 HTTP 메서드입니다", 405),

    // 409 Conflict
    CONFLICT("G409", "리소스 충돌이 발생했습니다", 409),
    PROJECT_NAME_DUPLICATE("P409", "프로젝트 이름이 중복되었습니다.", 409),

    // 429 Too Many Requests
    TOO_MANY_REQUESTS("G429", "요청이 너무 많습니다", 429),
    SUMMARY_LIMIT_EXCEEDED("G429-1", "요약 가능 횟수를 초과했습니다.", 429),

    // 5xx
    HDFS_IO_ERROR("H500", "HDFS에서 I/O 오류가 발생했습니다",500),
    INTERNAL_SERVER_ERROR("G500", "서버 내부 오류가 발생했습니다", 500),
    OAUTH_CLIENT_MISCONFIGURED("G500-1", "OAuth 클라이언트 설정이 잘못되었습니다", 500),
    BAD_GATEWAY("G502", "잘못된 게이트웨이 응답입니다", 502),
    SERVICE_UNAVAILABLE("G503", "서비스를 사용할 수 없습니다", 503),
    GMS_API_ERROR("G500-2", "GMS API 호출 중 오류가 발생했습니다.", 500),
    PAPER_CONTENT_EMPTY("P500", "PAPER Content가 비어 있습니다.", 500),
    TRANSLATION_REQUEST_FAILED("T500", "번역 서버에서 응답을 받지 못했습니다.",500);
    private final String code;
    private final String message;
    private final int status;
}

