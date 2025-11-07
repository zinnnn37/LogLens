package S13P31A306.loglens.domain.log.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LogSuccessCode implements SuccessCode {

    LOGS_READ_SUCCESS("LG200-2", "로그 목록을 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    TRACE_LOGS_READ_SUCCESS("LG200-3", "TraceID로 로그를 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    LOG_DETAIL_READ_SUCCESS("LG200-4", "로그 상세 정보를 성공적으로 조회했습니다.", HttpStatus.OK.value());

    private final String code;
    private final String message;
    private final int status;
}
