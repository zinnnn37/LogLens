package S13P31A306.loglens.global.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;

@Schema(description = "페이지 응답 DTO")
public record PageResponse<T>(
        @Schema(description = "페이지 내용") List<T> content,
        @Schema(description = "페이지 번호 (0부터 시작)", example = "0") int page,
        @Schema(description = "페이지당 항목 수", example = "20") int size,
        @Schema(description = "총 요소 수", example = "100") long totalElements,
        @Schema(description = "총 페이지 수", example = "5") int totalPages,
        @Schema(description = "첫 페이지 여부", example = "true") boolean first,
        @Schema(description = "마지막 페이지 여부", example = "false") boolean last,
        @Schema(description = "정렬 정보", example = "CREATED_AT,DESC") String sort
) {
    public PageResponse(Page<T> page) {
        this(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.getSort().isSorted()
                        ? page.getSort().stream()
                        .map(order -> order.getProperty().toUpperCase() + "," + order.getDirection().name())
                        .collect(Collectors.joining(","))
                        : ""
        );
    }
}
