package S13P31A306.loglens.domain.dependency.service.impl;

import S13P31A306.loglens.domain.component.entity.Component;
import S13P31A306.loglens.domain.component.repository.ComponentRepository;
import S13P31A306.loglens.domain.dependency.dto.request.DependencyGraphBatchRequest;
import S13P31A306.loglens.domain.dependency.dto.request.DependencyRelationRequest;
import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;
import S13P31A306.loglens.domain.dependency.repository.DependencyGraphRepository;
import S13P31A306.loglens.domain.dependency.service.DependencyGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 의존성 그래프 Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DependencyGraphServiceImpl implements DependencyGraphService {

    private final DependencyGraphRepository dependencyGraphRepository;
    private final ComponentRepository componentRepository;

    @Override
    @Transactional
    public void saveAll(DependencyGraphBatchRequest request, Integer projectId) {
        Integer existingCount = dependencyGraphRepository.countByProjectId(projectId);
        if (existingCount > 0) {
            log.info("🗑️ 기존 의존성 관계 삭제 시작: projectId={}, 개수={}", projectId, existingCount);
            dependencyGraphRepository.deleteByProjectId(projectId);
            log.info("✅ 기존 의존성 관계 삭제 완료");
        }

        log.info("📊 의존성 관계 저장 시작: projectId={}, 관계 수={}",
                projectId, request.dependencies().size());

        int savedCount = 0;
        int skippedCount = 0;

        for (DependencyRelationRequest relation : request.dependencies()) {
            // ✅ projectId와 name으로 컴포넌트 조회
            Optional<Component> fromComponent = componentRepository
                    .findByProjectIdAndName(projectId, relation.from());
            Optional<Component> toComponent = componentRepository
                    .findByProjectIdAndName(projectId, relation.to());

            // 둘 다 있으면 저장
            if (fromComponent.isPresent() && toComponent.isPresent()) {
                DependencyGraph graph = DependencyGraph.builder()
                        .projectId(projectId)  // ✅ projectId 설정
                        .from(fromComponent.get().getId())
                        .to(toComponent.get().getId())
                        .build();

                dependencyGraphRepository.save(graph);
                savedCount++;
            } else {
                log.warn("⚠️ 컴포넌트를 찾을 수 없음 (projectId={}): from={}, to={}",
                        projectId, relation.from(), relation.to());
                skippedCount++;
            }
        }

        log.info("✅ 의존성 관계 저장 완료: {} 개 저장, {} 개 스킵 (projectId={})",
                savedCount, skippedCount, projectId);
    }
}
