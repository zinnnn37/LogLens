package S13P31A306.loglens.domain.log.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogPageResponse {

    @Schema(description = "로그 목록")
    private List<LogResponse> logs;

    @Schema(description = "페이지네이션 정보")
    private PaginationResponse pagination;
}
