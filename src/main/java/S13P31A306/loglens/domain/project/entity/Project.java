package S13P31A306.loglens.domain.project.entity;

import S13P31A306.loglens.global.annotation.Sensitive;
import S13P31A306.loglens.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "projects",
        uniqueConstraints = @UniqueConstraint(columnNames = "project_name")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "TINYINT")
    private Integer id;

    @Column(name = "project_name", length = 100, nullable = false)
    private String projectName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Sensitive
    @Column(name = "api_key", length = 64)
    private String apiKey; // TODO: NOT NULL & UNIQUE로 수정 필요

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> members = new ArrayList<>();

    @Builder
    public Project(String projectName, String description, String apiKey) {
        this.projectName = projectName;
        this.description = description;
        this.apiKey = apiKey;
    }

}
