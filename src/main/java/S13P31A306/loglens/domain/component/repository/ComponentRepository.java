package S13P31A306.loglens.domain.component.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import S13P31A306.loglens.domain.component.entity.Component;

public interface ComponentRepository extends JpaRepository<Component, Integer> {
}
