package S13P31A306.loglens.domain.alert.exception;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 도메인 에러 코드
 *
 * <p>에러 코드 체계:</p>
 * <ul>
 *   <li>AL - Alert: 알림 도메인 에러</li>
 * </ul>
 *
 * <p>코드 형식: AL{HTTP상태코드}[-{서브코드}]</p>
 * <p>예시: AL404, AL400-1</p>
 */
@Getter
@RequiredArgsConstructor
public enum AlertErrorCode implements ErrorCode {

    // 400 Bad Request
    ALERT_CONFIG_ALREADY_EXISTS("AL400-1", "해당 프로젝트에 이미 알림 설정이 존재합니다.", 400),
    INVALID_ALERT_TYPE("AL400-2", "유효하지 않은 알림 타입입니다.", 400),
    INVALID_ACTIVE_YN("AL400-3", "활성화 여부는 'Y' 또는 'N'이어야 합니다.", 400),

    // 403 Forbidden
    ALERT_ACCESS_DENIED("AL403", "해당 알림에 접근 권한이 없습니다.", 403),

    // 404 Not Found
    ALERT_CONFIG_NOT_FOUND("AL404", "알림 설정을 찾을 수 없습니다.", 404),
    ALERT_HISTORY_NOT_FOUND("AL404-1", "알림 이력을 찾을 수 없습니다.", 404);

    private final String code;
    private final String message;
    private final int status;
}
