package S13P31A306.loglens.domain.component.service.impl;

import S13P31A306.loglens.domain.component.dto.request.ComponentBatchRequest;
import S13P31A306.loglens.domain.component.mapper.ComponentMapper;
import S13P31A306.loglens.domain.component.repository.ComponentRepository;
import S13P31A306.loglens.domain.component.service.ComponentService;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import S13P31A306.loglens.domain.component.entity.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComponentServiceImpl implements ComponentService {

    private final ComponentRepository componentRepository;
    private final ComponentMapper componentMapper;

    @Override
    @Transactional
    public void saveAll(ComponentBatchRequest request, Integer projectId) {
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

//    @Override
//    public List<Component> findByProjectId(Integer projectId) {
//        return List.of();
//    }
}
