package a306.dependency_logger_starter.dependency.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ERD components 테이블 매핑
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Component(
        Integer id,              // id (DB 자동 생성)
        String name,             // name (클래스명)
        String type,             // type (컴포넌트 타입)
        String packageName,      // package_name (패키지 경로)
        String layer,            // layer (Controller, Service, Repository, Validator)
        Integer projectId        // project_id (FK)
) {
    // 생성 시 id 없이 만들 수 있는 편의 생성자
    public Component(String name, String type, String packageName, String layer) {
        this(null, name, type, packageName, layer, null);
    }
}
