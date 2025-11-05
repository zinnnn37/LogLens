package S13P31A306.loglens.domain.log.dto.request;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class LogSearchRequest {

    @Sensitive
    @Schema(description = "프로젝트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String projectUuid;

    @Schema(description = "페이지네이션 커서", example = "eyJzb3J0IjpbMTcwNTMxMjgwMDAwMCwiYWJjMTIzIl19")
    private String cursor;

    @Schema(description = "페이지 크기", example = "50", defaultValue = "50")
    private Integer size = 50;

    @Schema(description = "검색 시작 시간", example = "2024-01-15T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @Schema(description = "검색 종료 시간", example = "2024-01-15T23:59:59")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @Schema(description = "로그 레벨 필터", example = "[\"ERROR\", \"WARN\"]")
    private List<String> logLevel;

    @Schema(description = "소스 타입 필터", example = "[\"BE\", \"FE\"]")
    private List<String> sourceType;

    @Schema(description = "검색 키워드", example = "NullPointerException")
    private String keyword;

    @Schema(description = "Trace ID", example = "trace-abc-123")
    private String traceId;

    @Schema(description = "정렬 옵션 (필드,방향)", example = "TIMESTAMP,DESC", defaultValue = "TIMESTAMP,DESC")
    private String sort = "TIMESTAMP,DESC";
}
