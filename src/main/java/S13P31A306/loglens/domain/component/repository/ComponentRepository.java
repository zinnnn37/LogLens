package S13P31A306.loglens.domain.component.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import S13P31A306.loglens.domain.component.entity.Component;

import java.util.Optional;

public interface ComponentRepository extends JpaRepository<Component, Integer> {
    Optional<Component> findByName(String name);
    Optional<Component> findByProjectIdAndName(Integer projectId, String name);
    void deleteByProjectId(Integer projectId);
    Integer countByProjectId(Integer projectId);
}
