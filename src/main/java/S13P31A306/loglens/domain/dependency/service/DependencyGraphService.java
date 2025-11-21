package S13P31A306.loglens.domain.dependency.service;

import S13P31A306.loglens.domain.dependency.dto.request.DependencyGraphBatchRequest;
import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;
import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;

import java.util.List;

/**
 * 의존성 그래프 Service 인터페이스
 */
public interface DependencyGraphService {

    /**
     * 의존성 그래프 배치 저장
     * 기존 프로젝트 의존성 삭제 후 새로 저장
     *
     * @param request 배치 저장 요청
     */
    void saveAll(DependencyGraphBatchRequest request, Integer projectId);

    List<DependencyGraph> findAllDependenciesByComponentId(Integer componentId);
    /**
     * 프로젝트별 모든 의존성 조회
     *
     * @param projectId 프로젝트 ID
     * @return 의존성 목록
     */
//    List<DependencyGraphResponse> findByProjectId(Integer projectId);

    /**
     * 특정 컴포넌트가 호출하는 대상들 조회
     *
     * @param projectId 프로젝트 ID
     * @param componentName 컴포넌트 이름
     * @return 의존성 목록
     */
//    List<DependencyGraphResponse> findDependenciesFrom(Integer projectId, String componentName);

    /**
     * 특정 컴포넌트를 호출하는 주체들 조회
     *
     * @param projectId 프로젝트 ID
     * @param componentName 컴포넌트 이름
     * @return 의존성 목록
     */
//    List<DependencyGraphResponse> findDependenciesTo(Integer projectId, String componentName);

    /**
     * 프로젝트의 모든 의존성 삭제
     *
     * @param projectId 프로젝트 ID
     */
//    void deleteByProjectId(Integer projectId);
}
