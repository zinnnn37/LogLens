package S13P31A306.loglens.domain.dashboard.repository;

import S13P31A306.loglens.domain.dashboard.entity.ApiEndpoint;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApiStatsRepository extends JpaRepository<ApiEndpoint, Integer> {

    /**
     * 프로젝트의 API 엔드포인트 목록을 총 요청 수 기준 내림차순으로 조회
     *
     * @param projectId 프로젝트 ID
     * @param limit 조회할 최대 엔드포인트 개수 (1~50)
     * @return 요청 수가 많은 순서로 정렬된 API 엔드포인트 목록
     */
    @Query(value = """
            SELECT ae
            FROM ApiEndpoint ae
            WHERE ae.projectId = :projectId
            ORDER BY ae.totalRequests DESC
            LIMIT :limit
            """)
    List<ApiEndpoint> findTopByProjectIdOrderByTotalRequests(
            @Param("projectId") Integer projectId,
            @Param("limit") Integer limit
    );

    /**
     * 프로젝트의 전체 엔드포인트 개수 조회
     *
     * @param projectId 프로젝트 ID
     * @return 프로젝트에 등록된 총 엔드포인트 개수
     */
    Integer countByProjectId(Integer projectId);

    /**
     * 프로젝트의 모든 엔드포인트의 총 요청 수 합계 조회
     *
     * @param projectId 프로젝트 ID
     * @return 모든 엔드포인트의 총 요청 수 합계, 데이터가 없으면 0(coalesce)
     */
    @Query("SELECT COALESCE(SUM(ae.totalRequests), 0) FROM ApiEndpoint ae WHERE ae.projectId = :projectId")
    Long sumTotalRequestsByProjectId(@Param("projectId") Integer projectId);

    /**
     * 프로젝트의 모든 엔드포인트의 총 에러 발생 수 합계 조회
     *
     * @param projectId 프로젝트 ID
     * @return 모든 엔드포인트의 총 에러 발생 수 합계, 데이터가 없으면 0
     */
    @Query("SELECT COALESCE(SUM(ae.errorCount), 0) FROM ApiEndpoint ae WHERE ae.projectId = :projectId")
    Long sumErrorCountByProjectId(@Param("projectId") Integer projectId);

    /**
     * 프로젝트의 모든 엔드포인트의 평균 응답시간 조회
     *
     * @param projectId 프로젝트 ID
     * @return 모든 엔드포인트의 평균 응답시간 (ms), 데이터가 없으면 0
     */
    @Query("SELECT COALESCE(AVG(ae.avgResponseTime), 0) FROM ApiEndpoint ae WHERE ae.projectId = :projectId")
    Double avgResponseTimeByProjectId(@Param("projectId") Integer projectId);

    /**
     * 임계치를 초과한 위험 엔드포인트 개수 조회
     *
     * 다음 조건 중 하나라도 만족하는 경우 위험 엔드포인트로 분류:
     * 1. 에러율(errorCount / totalRequests * 100) >= 5.0%
     * 2. 이상치 발생 횟수(anomalyCount) >= 10
     *
     * @param projectId 프로젝트 ID
     * @return 임계치를 초과한 엔드포인트 개수
     */
    @Query("""
            SELECT COUNT(ae)
            FROM ApiEndpoint ae
            WHERE ae.projectId = :projectId
            AND ((CAST(ae.errorCount AS double) / ae.totalRequests * 100) >= 5.0
                 OR ae.anomalyCount >= 10)
            """)
    long countCriticalEndpointsByProjectId(@Param("projectId") Integer projectId);

}
