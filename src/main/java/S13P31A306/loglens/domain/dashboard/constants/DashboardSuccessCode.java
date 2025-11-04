package S13P31A306.loglens.domain.dashboard.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DashboardSuccessCode implements SuccessCode {

    // 200 OK
    OVERVIEW_RETRIVED("DSB200-1", "대시보드 통계를 성공적으로 조회했습니다.", HttpStatus.OK.value());

    private final String code;
    private final String message;
    private final int status;

}
