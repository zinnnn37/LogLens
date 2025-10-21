package a306.dto;

import java.util.List;

/**
 * Collector에 전송할 의존성 정보
 *
 * Collector에서 받아서:
 * 1. components 테이블에 컴포넌트들 저장
 * 2. dependency_graphs 테이블에 관계 저장
 */
public record DependencyInfo(
        String projectName,             // 프로젝트명
        Component component,            // 현재 컴포넌트 정보
        List<Component> dependencies    // 의존하는 컴포넌트 목록
) {
}
