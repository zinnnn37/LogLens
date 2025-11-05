package S13P31A306.loglens.domain.component.service.impl;

import S13P31A306.loglens.domain.component.dto.request.ComponentBatchRequest;
import S13P31A306.loglens.domain.component.mapper.ComponentMapper;
import S13P31A306.loglens.domain.component.repository.ComponentRepository;
import S13P31A306.loglens.domain.component.service.ComponentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import S13P31A306.loglens.domain.component.entity.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComponentServiceImpl implements ComponentService {

    private final ComponentRepository componentRepository;
    private final ComponentMapper componentMapper;

    @Override
    @Transactional
    public void saveAll(final ComponentBatchRequest request, final Integer projectId) {
        Integer existingCount = componentRepository.countByProjectId(projectId);
        if (existingCount > 0) {
            log.info("🗑️ 기존 컴포넌트 삭제 시작: projectId={}, 개수={}", projectId, existingCount);
            componentRepository.deleteByProjectId(projectId);
            log.info("✅ 기존 컴포넌트 삭제 완료");
        }

        List<Component> components = componentMapper.toEntityList(request.components(), projectId);  // ✅ projectId 전달
        componentRepository.saveAll(components);

        log.info("✅ 배치 저장 완료: {} 개 저장됨", components.size());
    }

    @Override
    public List<Component> getProjectComponents(final Integer projectId) {
        return componentRepository.findAllByProjectId(projectId);
    }

    @Override
    public Map<Integer, Component> getComponentMapByIds(final Set<Integer> componentIds) {
        log.debug("컴포넌트 일괄 조회: count={}", componentIds.size());

        return componentRepository
                .findAllById(componentIds)
                .stream()
                .collect(Collectors.toMap(Component::getId, component -> component));
    }

//    @Overridea
//    public List<Component> findByProjectId(Integer projectId) {
//        return List.of();
//    }
}
