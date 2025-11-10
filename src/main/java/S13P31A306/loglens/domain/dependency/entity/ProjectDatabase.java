package S13P31A306.loglens.domain.dependency.entity;

import S13P31A306.loglens.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project_databases")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDatabase extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "database_type", nullable = false, length = 50)
    private String databaseType;
}
