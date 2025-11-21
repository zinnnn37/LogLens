package S13P31A306.loglens.domain.component.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ComponentSuccessCode implements SuccessCode {
    COMPONENT_RETRIEVED("C200", "컴포넌트 조회 성공", 200),
    COMPONENTS_RETRIEVED("C200-1", "컴포넌트 목록 조회 성공", 200),
    COMPONENT_DELETED("C200-2", "컴포넌트 삭제 성공", 200),

    // 201 Created
    COMPONENT_CREATED("C201", "컴포넌트 생성 성공", 201),
    COMPONENTS_BATCH_CREATED("C201-1", "컴포넌트 배치 생성 성공", 201);

    private final String code;
    private final String message;
    private final int status;
}
