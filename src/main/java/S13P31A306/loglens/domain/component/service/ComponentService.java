package S13P31A306.loglens.domain.component.service;

import S13P31A306.loglens.domain.component.dto.request.ComponentBatchRequest;
import org.springframework.stereotype.Component;

import java.util.List;

public interface ComponentService {

    /**
     * 여러 컴포넌트 배치 저장
     */
    void saveAll(ComponentBatchRequest request);

//    /**
//     * 프로젝트 ID로 컴포넌트 목록 조회
//     */
//    List<Component> findByProjectId(Integer projectId);

//    /**
//     * 프로젝트의 컴포넌트 전체 삭제 (재수집 시)
//     */
//    void deleteByProjectId(Integer projectId);
}
