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
    @Schema(description = "프로젝트 UUID", example = "alwkcmqe-cf4e-3cea-8755-ebf105062705")
    private String projectUuid;

    @Schema(description = "페이지네이션 커서", example = "eyJzb3J0IjpbMTcwNTMxMjgwMDAwMCwiYWJjMTIzIl19")
    private String cursor;

    @Schema(description = "페이지 크기", example = "50", defaultValue = "50")
    private Integer size = 50;

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

    @Schema(description = "Trace ID", example = "cff8de02-ed46-4400-a430-23d16a254d1d")
    private String traceId;

    @Schema(description = "정렬 옵션 (필드,방향)", example = "TIMESTAMP,DESC", defaultValue = "TIMESTAMP,DESC")
    private String sort = "TIMESTAMP,DESC";
}
