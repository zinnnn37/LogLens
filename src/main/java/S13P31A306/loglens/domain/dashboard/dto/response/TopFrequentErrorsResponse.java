package S13P31A306.loglens.domain.dashboard.dto.response;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "자주 발생하는 에러 top N 응답")
public record TopFrequentErrorsResponse(
        @Sensitive
        @Schema(description = "프로젝트 UUID", example = "48d96cd7-bf8d-38f5-891c-9c2f6430d871")
        String projectUuid,

        @Schema(description = "조회 기간")
        Period period,
        
        @Schema(description = "에러 목록")
        List<ErrorInfo> errors,

        @Schema(description = "에러 통계 요약")
        ErrorSummary summary
) {

    public record Period(
            @Schema(description = "조회 시작 시간", example = "2025-10-10T00:00:00Z")
            LocalDateTime startTime,

            @Schema(description = "조회 종료 시간", example = "2025-10-17T23:59:59Z")
            LocalDateTime endTime
    ) {
    }

    public record ErrorInfo(
            @Schema(description = "순위", example = "1")
            Integer rank,

            @Schema(description = "예외 타입", example = "java.sql.SQLException")
            String exceptionType,

            @Schema(description = "에러 메시지", example = "Database connection timeout after 5000ms")
            String message,

            @Schema(description = "에러 발생 횟수", example="3456")
            Integer count,

            @Schema(description = "전체 에러 중 비율 (%)", example = "23.4")
            Double percentage,

            @Schema(description = "최초 발생 시각", example = "2025-10-10T08:23:15Z")
            LocalDateTime firstOccurrence,

            @Schema(description = "최근 발생 시각", example = "2025-10-17T14:52:30Z")
            LocalDateTime lastOccurrence,

            @Schema(description = "스택 트레이스 (첫 라인)", example = "at com.loglens.db.ConnectionPool.getConnection(ConnectionPool.java:145)")
            String stackTrace,

            @Schema(description = "영향받은 컴포넌트 목록")
            List<ComponentInfo> components
    ) {

        public record ComponentInfo(
                @Schema(description = "컴포넌트 ID", example = "1")
                Integer id,

                @Schema(description = "컴포넌트 이름", example = "auth-service")
                String name
        ) {
        }

    }

    public record ErrorSummary(

            @Schema(description = "조회 기간 총 에러 수", example = "14765")
            Long totalErrors,

            @Schema(description = "고유 에러 타입 수", example = "47")
            Integer uniqueErrorTypes,

            @Schema(description = "Top 10이 차지하는 비율 (%)", example = "68.3")
            Double top10Percentage

    ) {
    }

}
