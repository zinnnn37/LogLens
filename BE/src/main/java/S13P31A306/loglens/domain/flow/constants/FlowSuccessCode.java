package S13P31A306.loglens.domain.flow.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FlowSuccessCode implements SuccessCode {

    // 200 OK
    TRACE_LOGS_RETRIEVED("FLOW200-1", "Trace 로그를 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    TRACE_FLOW_RETRIEVED("FLOW200-2", "요청 흐름을 성공적으로 조회했습니다.", HttpStatus.OK.value());

    private final String code;
    private final String message;
    private final int status;
}
