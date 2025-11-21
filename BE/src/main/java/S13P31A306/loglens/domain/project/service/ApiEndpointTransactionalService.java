package S13P31A306.loglens.domain.project.service;

import S13P31A306.loglens.domain.project.entity.Project;

import java.time.LocalDateTime;

/**
 * API 엔드포인트 메트릭 트랜잭션 서비스
 * OpenSearch 쿼리 + DB 저장을 처리
 */
public interface ApiEndpointTransactionalService {

    /**
     * 프로젝트의 API 엔드포인트 메트릭을 증분 집계
     * OpenSearch에서 조회 후 DB에 저장
     */
    void aggregateApiEndpointMetrics(
            Project project,
            LocalDateTime from,
            LocalDateTime to
    );

}
