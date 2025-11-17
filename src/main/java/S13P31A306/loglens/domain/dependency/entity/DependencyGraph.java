package S13P31A306.loglens.domain.dependency.entity;

import S13P31A306.loglens.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "dependency_graphs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DependencyGraph extends BaseEntity {

    /**
     * 프로젝트 ID (FK)
     * projects 테이블과 연관
     */
    @NotNull
    @Column(name = "project_id", nullable = false, columnDefinition = "INT")
    private Integer projectId;

    /**
     * 호출하는 컴포넌트 이름
     * 예: UserController, UserService
     */
    @Column(name = "from_component_id", nullable = false)
    private Integer from;

    /**
     * 호출받는 컴포넌트 이름
     * 예: UserService, UserRepository
     */
    @Column(name = "to_component_id", nullable = false)
    private Integer to;
}
