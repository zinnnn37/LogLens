package S13P31A306.loglens.domain.project.entity;

import S13P31A306.loglens.global.annotation.Sensitive;
import S13P31A306.loglens.global.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(name = "project_uuid", length = 64)
    private String projectUuid; // TODO: NOT NULL & UNIQUE로 수정 필요

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> members = new ArrayList<>();

    @Builder
    public Project(String projectName, String description, String projectUuid) {
        this.projectName = projectName;
        this.description = description;
        this.projectUuid = projectUuid;
    }

    @PrePersist
    public void generateUUID() {
        if (Objects.isNull(this.projectUuid)) {
            String uuidKey = this.projectName + LocalTime.now();
            this.projectUuid = UUID.nameUUIDFromBytes(uuidKey.getBytes()).toString();
        }
    }
}
