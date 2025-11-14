package S13P31A306.loglens.domain.statistics.constants;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 통계 도메인 에러 코드
 * 현재는 기존 도메인의 에러 코드를 재사용하므로 비어있음
 */
@Getter
@RequiredArgsConstructor
public enum StatisticsErrorCode implements ErrorCode {

    // 필요시 추가
    ;

    private final String code;
    private final String message;
    private final int status;
}
