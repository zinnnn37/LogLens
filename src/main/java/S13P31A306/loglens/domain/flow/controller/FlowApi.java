package S13P31A306.loglens.domain.flow.controller;

import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Flow API", description = "요청 흐름 추적 API")
public interface FlowApi {

    @Operation(
            summary = "Trace 로그 조회",
            description = "특정 TraceID의 로그를 시간순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "Trace 또는 프로젝트를 찾을 수 없음")
    })
    ResponseEntity<? extends BaseResponse> getTraceLogs(
            @Parameter(description = "추적 ID", required = true, example = "trace_abc123")
            @PathVariable String traceId,

            @Parameter(description = "프로젝트 ID", required = true, example = "12345")
            @ValidUuid @RequestParam String projectUuid
    );
}
