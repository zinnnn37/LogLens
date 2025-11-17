package S13P31A306.loglens.domain.dashboard.dto.opensearch;

/**
 * API 엔드포인트 통계 DTO (배치 집계용)
 * OpenSearch에서 조회한 API 엔드포인트별 호출 통계를 담는 DTO
 * 배치 스케줄러와 트랜잭션 서비스 간 데이터 전달에 사용
 *
 * @param endpointPath API 엔드포인트 경로
 * @param httpMethod HTTP 메서드 (GET, POST 등)
 * @param totalRequests 총 요청 수
 * @param errorCount 에러 발생 수 (4xx, 5xx)
 * @param avgResponseTime 평균 응답 시간 (ms)
 * @param lastAccessedTimestamp 마지막 접근 시간 (epoch millis)
 */
public record ApiEndpointStats(
        String endpointPath,
        String httpMethod,
        long totalRequests,
        long errorCount,
        Double avgResponseTime,
        Double lastAccessedTimestamp
) {
}
