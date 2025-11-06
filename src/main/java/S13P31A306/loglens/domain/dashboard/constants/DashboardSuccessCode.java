package S13P31A306.loglens.domain.dashboard.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DashboardSuccessCode implements SuccessCode {
    COMPONENTS_RETRIEVED("DASH200-1", "컴포넌트 목록을 성공적으로 조회했습니다.", HttpStatus.OK.value()),
    COMPONENT_DEPENDENCY_RETRIEVED("DASH200-2", "컴포넌트 의존성 목록을 성공적으로 조회했습니다.", HttpStatus.OK.value());
    private final String code;
    private final String message;
    private final int status;
}
