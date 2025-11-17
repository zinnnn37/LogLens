package S13P31A306.loglens.domain.dashboard.dto.response;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "API 통계 조회 응답")
public record ApiEndpointResponse(
        @Schema(description = "프로젝트 ID", example = "12345")
        @Sensitive
        Integer projectId,

        @Schema(description = "엔드포인트 목록")
        List<EndpointStats> endpoints,

        @Schema(description = "통계 요약")
        Summary summary
) {

    public record EndpointStats(
            @Schema(description = "엔드포인트 ID", example = "1")
            Integer id,

            @Schema(description = "엔드포인트 경로", example = "/api/logs/search")
            String endpointPath,

            @Schema(description = "HTTP 메서드", example = "GET")
            String httpMethod,

            @Schema(description = "총 요청 수", example = "45230")
            Integer totalRequests,

            @Schema(description = "에러 발생 수", example = "678")
            Integer errorCount,

            @Schema(description = "에러율 (%)", example = "1.5")
            BigDecimal errorRate,

            @Schema(description = "평균 응답시간 (ms)", example = "320")
            BigDecimal avgResponseTime,

            @Schema(description = "이상치 횟수", example = "12")
            Integer anomalyCount,

            @Schema(description = "마지막 접근시간", example = "2025-10-17T15:30:00Z")
            java.time.LocalTime lastAccessed
    ) {
    }

    public record Summary(
            @Schema(description = "전체 엔드포인트 수", example = "35")
            Integer totalEndpoints,

            @Schema(description = "전체 요청 수 합계", example = "125430")
            Long totalRequests,

            @Schema(description = "전체 에러 수 합계", example = "1254")
            Long totalErrors,

            @Schema(description = "전체 에러율 (%)", example = "1.0")
            BigDecimal overallErrorRate,

            @Schema(description = "전체 평균 응답시간 (ms)", example = "245")
            BigDecimal avgResponseTime,

            @Schema(description = "임계치 초과 엔드포인트 수", example = "2")
            Integer criticalEndpoints
    ) {
    }

}
