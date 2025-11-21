package S13P31A306.loglens.domain.dependency.repository;

import S13P31A306.loglens.domain.dependency.entity.ProjectDatabase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectDatabaseRepository extends JpaRepository<ProjectDatabase, Integer> {
    void deleteByProjectId(Integer projectId);
    List<ProjectDatabase> findByProjectId(Integer projectId);
}
