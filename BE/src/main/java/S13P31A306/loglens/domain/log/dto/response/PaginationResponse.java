package S13P31A306.loglens.domain.log.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaginationResponse {
    @Schema(description = "다음 페이지 커서", example = "eyJzb3J0IjpbMTcwNTMxMjgwMDAwMCwiYWJjMTIzIl19")
    private String nextCursor;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "페이지 크기", example = "50")
    private Integer size;
}
