package S13P31A306.loglens.domain.dependency.repository;

import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependencyGraphRepository extends JpaRepository<DependencyGraph, Integer> {
    void deleteByProjectId(Integer projectId);
    Integer countByProjectId(Integer projectId);
}
