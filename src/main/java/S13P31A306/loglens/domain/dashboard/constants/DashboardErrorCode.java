package S13P31A306.loglens.domain.dashboard.constants;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DashboardErrorCode implements ErrorCode {

    // 400 BAD REQUEST
    INVALID_TIME_FORMAT("DSB400-1", "유효하지 않은 시간 형식입니다", HttpStatus.BAD_REQUEST.value()),
    INVALID_TIME_RANGE("DSB400-2", "유효하지 않은 시강 범위입니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_LIMIT("DSB400-3", "유효하지 않은 limit 값입니다.", HttpStatus.BAD_REQUEST.value()),
    PERIOD_EXCEEDS_LIMIT("DSB400-4", "조회 기간은 최대 90일을 초과할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    PROJECT_UUID_REQUIRED("DSB400-5", "프로젝트 ID는 필수입니다.",  HttpStatus.BAD_REQUEST.value()),
    INVALID_LOG_LEVEL("DSB400-6", "유효하지 않은 로그 레벨입니다.",  HttpStatus.BAD_REQUEST.value()),

    // 403 FORBIDDEN
    ACCESS_DENIED("DSB403-1", "해당 프로젝트에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),

    // 404 NOT FOUND
    PROJECT_NOT_FOUND("DSB404-1", "해당 프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    TRACE_ID_NOT_FOUND("DSB404-2", "존재하지 않는 trace ID입니다.", HttpStatus.NOT_FOUND.value());

    private final String code;
    private final String message;
    private final int status;

}
