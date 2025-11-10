package S13P31A306.loglens.domain.log.dto.request;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@Builder
public class LogSearchRequest {

    @Sensitive
    @Schema(description = "프로젝트 UUID", example = "9911573f-8a1d-3b96-98b4-5a0def93513b")
    private String projectUuid;

    @Schema(description = "페이지네이션 커서", example = "eyJzb3J0IjpbMTcwNTMxMjgwMDAwMCwiYWJjMTIzIl19")
    private String cursor;

    @Schema(description = "페이지 크기", example = "50", defaultValue = "100")
    private Integer size = 100;

    @Schema(description = "검색 시작 시간", example = "2025-11-01T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @Schema(description = "검색 종료 시간", example = "2025-12-31T23:59:59")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @Schema(description = "로그 레벨 필터", example = "[\"INFO\"]")
    private List<String> logLevel;

    @Schema(description = "소스 타입 필터", example = "[\"BE\"]")
    private List<String> sourceType;

    @Schema(description = "검색 키워드", example = "NullPointerException")
    private String keyword;

    @Schema(description = "Trace ID", example = "d5d9098c-c959-41aa-825a-fa417790292d")
    private String traceId;

    @Schema(description = "정렬 옵션 (필드,방향)", example = "TIMESTAMP,DESC", defaultValue = "TIMESTAMP,DESC")
    private String sort = "TIMESTAMP,DESC";
}
