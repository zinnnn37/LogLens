package S13P31A306.loglens.global.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * 전역 에러 코드 정의
 *
 * <p>에러 코드 체계:</p>
 * <ul>
 *   <li>G - Global: 전역 공통 에러</li>
 *   <li>PJ - Project: 프로젝트 도메인 에러</li>
 * </ul>
 *
 * <p>코드 형식: {도메인}{HTTP상태코드}[-{서브코드}]</p>
 * <p>예시: G404, G404-1, PJ409</p>
 */
@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    // 400 Bad Request
    VALIDATION_ERROR("G400", "입력값이 유효하지 않습니다", 400),
    INVALID_FORMAT("G400-1", "요청 형식이 올바르지 않습니다", 400),

    // 401 Unauthorized
    UNAUTHORIZED("G401", "인증이 필요합니다", 401),
    AUTH_INVALID_ID_TOKEN("G401-1", "유효하지 않은 토큰입니다", 401),
    EXPIRED_TOKEN("G401-2", "만료된 토큰입니다", 401),
    TOKEN_SIGNATURE_INVALID("G401-3", "토큰 서명이 유효하지 않습니다", 401),

    // 403 Forbidden
    FORBIDDEN("G403", "접근 권한이 없습니다", 403),
    USER_DELETED("G403-1", "탈퇴된 회원입니다.", 403),

    // 404 Not Found
    NOT_FOUND("G404", "요청한 리소스를 찾을 수 없습니다", 404),
    USER_NOT_FOUND("G404-1", "사용자를 찾을 수 없습니다.", 404),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED("G405", "허용되지 않은 HTTP 메서드입니다", 405),

    // 409 Conflict
    CONFLICT("G409", "리소스 충돌이 발생했습니다", 409),
    PROJECT_NAME_DUPLICATE("PJ409", "프로젝트 이름이 중복되었습니다.", 409),

    // 429 Too Many Requests
    TOO_MANY_REQUESTS("G429", "요청이 너무 많습니다", 429),

    // 5xx
    INTERNAL_SERVER_ERROR("G500", "서버 내부 오류가 발생했습니다", 500),
    BAD_GATEWAY("G502", "잘못된 게이트웨이 응답입니다", 502);
    private final String code;
    private final String message;
    private final int status;

    /**
     * Enum name으로 GlobalErrorCode를 안전하게 조회한다.
     * @param name GlobalErrorCode enum의 이름
     * @return 존재하면 해당 ErrorCode, 없으면 Optional.empty()
     */
    public static Optional<ErrorCode> safeValueOf(String name) {
        try {
            return Optional.of(GlobalErrorCode.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

