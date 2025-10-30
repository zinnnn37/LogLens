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
    public void saveAll(DependencyGraphBatchRequest request) {
//        log.info("📊 의존성 관계 저장 시작: 프로젝트={}, 관계 수={}",
//                request.projectName(), request.dependencies().size());

        int savedCount = 0;

        for (DependencyRelationRequest relation : request.dependencies()) {
            // name으로 컴포넌트 조회
            Optional<Component> fromComponent = componentRepository.findByName(relation.from());
            Optional<Component> toComponent = componentRepository.findByName(relation.to());

            // 둘 다 있으면 저장
            if (fromComponent.isPresent() && toComponent.isPresent()) {
                DependencyGraph graph = DependencyGraph.builder()
                        .from(fromComponent.get().getId())
                        .to(toComponent.get().getId())
                        .build();

                dependencyGraphRepository.save(graph);
                savedCount++;
            } else {
                log.warn("⚠️ 컴포넌트를 찾을 수 없음: from={}, to={}", relation.from(), relation.to());
            }
        }

        log.info("✅ 의존성 관계 저장 완료: {} 개", savedCount);
    }
}
