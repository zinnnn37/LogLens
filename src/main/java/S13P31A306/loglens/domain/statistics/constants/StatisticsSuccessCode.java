package S13P31A306.loglens.domain.statistics.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 통계 도메인 성공 응답 코드
 */
@Getter
@RequiredArgsConstructor
public enum StatisticsSuccessCode implements SuccessCode {

    LOG_TREND_RETRIEVED("STATISTICS_2001", "로그 추이 조회 성공", HttpStatus.OK.value()),
    TRAFFIC_RETRIEVED("STATISTICS_2002", "Traffic 조회 성공", HttpStatus.OK.value()),
    AI_COMPARISON_RETRIEVED("STATISTICS_2003", "AI vs DB 통계 비교 검증 성공", HttpStatus.OK.value());

    private final String code;
    private final String message;
    private final int status;
}
