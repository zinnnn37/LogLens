package S13P31A306.loglens.domain.component.entity;

import S13P31A306.loglens.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 컴포넌트 엔티티
 */
@Entity
@Table(name = "components")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Component extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "class_type", length = 100)
    private String classType;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 100)
    private ComponentType componentType;

    @Column(name = "package_name", length = 255)
    private String packageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer", length = 50)
    private ComponentLayer layer;

    @Column(name = "technology", nullable = false, length = 50)
    private String technology;
}
