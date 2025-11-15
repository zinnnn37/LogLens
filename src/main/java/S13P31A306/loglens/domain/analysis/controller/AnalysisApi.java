package S13P31A306.loglens.domain.analysis.controller;

import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 분석 문서 생성 API
 */
@Tag(name = "Analysis", description = "AI 기반 프로젝트 분석 및 에러 분석 문서 생성 API")
public interface AnalysisApi {

    @Operation(
            summary = "프로젝트 종합 분석 문서 생성",
            description = "프로젝트의 전체 현황을 AI가 분석하여 아름다운 HTML, PDF, Markdown, JSON 형식의 문서를 생성합니다. " +
                    "대시보드 메트릭, 로그 통계, 에러 분석, 권장사항 등이 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "문서 생성 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (시간 범위, 포맷 등)"),
            @ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
            @ApiResponse(responseCode = "500", description = "문서 생성 실패 (AI 서비스 오류, HTML 검증 실패 등)")
    })
    ResponseEntity<? extends BaseResponse> generateProjectAnalysisReport(
            @Parameter(description = "프로젝트 UUID", required = true, example = "abc-123-def")
            @PathVariable
            @ValidUuid
            String projectUuid,

            @Parameter(description = "프로젝트 분석 요청", required = true)
            @RequestBody
            @Valid
            ProjectAnalysisRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            UserDetails userDetails
    );

    @Operation(
            summary = "에러 상세 분석 문서 생성",
            description = "특정 에러 로그에 대한 근본 원인, 영향 범위, 해결 방법 등을 AI가 상세히 분석한 문서를 생성합니다. " +
                    "관련 로그, 유사 에러, 코드 예시 등이 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "문서 생성 성공",
                    content = @Content(schema = @Schema(implementation = BaseResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (옵션 설정 등)"),
            @ApiResponse(responseCode = "403", description = "프로젝트 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "로그를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "문서 생성 실패")
    })
    ResponseEntity<? extends BaseResponse> generateErrorAnalysisReport(
            @Parameter(description = "로그 ID", required = true, example = "12345")
            @PathVariable
            Long logId,

            @Parameter(description = "에러 분석 요청", required = true)
            @RequestBody
            @Valid
            ErrorAnalysisRequest request,

            @Parameter(hidden = true)
            @AuthenticationPrincipal
            UserDetails userDetails
    );

    @Operation(
            summary = "PDF 문서 다운로드",
            description = "생성된 PDF 파일을 다운로드합니다. PDF는 생성 후 1시간 동안 유효합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF 다운로드 성공",
                    content = @Content(mediaType = "application/pdf")
            ),
            @ApiResponse(responseCode = "404", description = "PDF 파일을 찾을 수 없음 (만료되었거나 존재하지 않음)")
    })
    ResponseEntity<Resource> downloadPdfDocument(
            @Parameter(description = "파일 ID", required = true, example = "abc123-def456")
            @PathVariable
            String fileId
    );
}
