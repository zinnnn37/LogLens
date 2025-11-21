package S13P31A306.loglens.domain.component.service;

import S13P31A306.loglens.domain.component.dto.request.ComponentBatchRequest;
import S13P31A306.loglens.domain.component.entity.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ComponentService {

    /**
     * 여러 컴포넌트 배치 저장
     */
    void saveAll(ComponentBatchRequest request, Integer projectId);
    List<Component> getProjectComponents(Integer projectId);
    Map<Integer, Component> getComponentMapByIds(Set<Integer> componentIds);
//    /**
//     * 프로젝트 ID로 컴포넌트 목록 조회
//     */
//    List<Component> findByProjectId(Integer projectId);

//    /**
//     * 프로젝트의 컴포넌트 전체 삭제 (재수집 시)
//     */
//    void deleteByProjectId(Integer projectId);
}
