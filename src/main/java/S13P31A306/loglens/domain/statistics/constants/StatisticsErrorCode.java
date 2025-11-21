package S13P31A306.loglens.domain.statistics.constants;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 통계 도메인 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum StatisticsErrorCode implements ErrorCode {

    // 시간 범위 관련 에러
    INVALID_TIME_RANGE("ST400-1", "시작 시간은 종료 시간보다 앞서야 합니다.", HttpStatus.BAD_REQUEST.value()),
    PERIOD_EXCEEDS_LIMIT("ST400-2", "조회 기간이 최대 허용 범위를 초과했습니다.", HttpStatus.BAD_REQUEST.value()),

    // 집계 간격 관련 에러
    INVALID_INTERVAL("ST400-3", "유효하지 않은 집계 간격입니다.", HttpStatus.BAD_REQUEST.value()),

    // 데이터 포인트 관련 에러
    INVALID_DATA_POINTS("ST400-4", "데이터 포인트 개수가 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value()),

    // 통계 데이터 없음
    NO_STATISTICS_DATA("ST404-1", "해당 기간에 통계 데이터가 없습니다.", HttpStatus.NOT_FOUND.value())
    ;

    private final String code;
    private final String message;
    private final int status;
}
