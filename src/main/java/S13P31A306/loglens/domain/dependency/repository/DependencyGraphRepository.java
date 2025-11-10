package S13P31A306.loglens.domain.dependency.repository;

import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface DependencyGraphRepository extends JpaRepository<DependencyGraph, Integer> {
    void deleteByProjectId(Integer projectId);
    Integer countByProjectId(Integer projectId);

    /**
     * 특정 컴포넌트가 의존하는 대상 조회 (from → to)
     */
    @Query("SELECT dg FROM DependencyGraph dg WHERE dg.from = :componentId")
    List<DependencyGraph> findByFrom(@Param("componentId") Integer componentId);

    /**
     * 특정 컴포넌트에 의존하는 주체 조회 (to ← from)
     */
    @Query("SELECT dg FROM DependencyGraph dg WHERE dg.to = :componentId")
    List<DependencyGraph> findByTo(@Param("componentId") Integer componentId);

    @Query("SELECT d FROM DependencyGraph d WHERE d.projectId = :projectId AND (d.from IN :componentIds OR d.to IN :componentIds)")
    List<DependencyGraph> findAllByProjectIdAndComponentIds(
            @Param("projectId") Integer projectId,
            @Param("componentIds") Set<Integer> componentIds
    );
}
