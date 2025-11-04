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

    // 403 FORBIDDEN
    ACCESS_DENIED("DSB403-1", "해당 프로젝트에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),

    // 404 NOT FOUND
    PROJECT_NOT_FOUND("DSB404-1", "프로젝트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value());

    private final String code;
    private final String message;
    private final int status;

}
