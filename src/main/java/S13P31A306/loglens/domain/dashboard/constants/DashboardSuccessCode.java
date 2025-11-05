package S13P31A306.loglens.domain.dashboard.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DashboardSuccessCode implements SuccessCode {

    // 200 OK
    OVERVIEW_RETRIEVED("DSB200-1", "대시보드 통계를 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    FREQUENT_ERROR_RETRIEVED("DSB200-2", "자주 발생하는 에러를 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    API_STATISTICS_RETRIEVED("DSB200-3", "API 통계를 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    REQUEST_FLOW_RETRIEVED("DSB200-4", "요청 흐름을 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    HEATMAP_RETRIEVED("DSB200-5", "히트맵을 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    NOTIFICATION_FEED_RETRIEVED("DS200-6", "알림 피드를 성공적으로 조회했습니다.", HttpStatus.OK.value());

    private final String code;
    private final String message;
    private final int status;

}
