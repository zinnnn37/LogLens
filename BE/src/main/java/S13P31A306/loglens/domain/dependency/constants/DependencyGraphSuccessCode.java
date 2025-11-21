package S13P31A306.loglens.domain.dependency.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DependencyGraphSuccessCode implements SuccessCode {
    // 200 OK
    DEPENDENCY_GRAPH_RETRIEVED("DG200", "의존성 그래프 조회 성공", 200),
    DEPENDENCY_GRAPHS_RETRIEVED("DG200-1", "의존성 그래프 목록 조회 성공", 200),
    DEPENDENCY_GRAPH_DELETED("DG200-2", "의존성 그래프 삭제 성공", 200),

    // 201 Created
    DEPENDENCY_GRAPH_CREATED("DG201", "의존성 그래프 생성 성공", 201),
    DEPENDENCY_GRAPHS_BATCH_CREATED("DG201-1", "의존성 그래프 배치 생성 성공", 201);

    private final String code;
    private final String message;
    private final int status;
}
