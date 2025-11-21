package S13P31A306.loglens.domain.analysis.dto.request;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.global.annotation.ValidUuid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 에러 상세 분석 문서 생성 요청
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorAnalysisRequest {

    /**
     * 프로젝트 UUID (접근 권한 검증용)
     */
    @NotNull(message = "프로젝트 UUID를 입력해주세요")
    @ValidUuid
    private String projectUuid;

    /**
     * 문서 출력 형식
     */
    @NotNull(message = "문서 형식을 지정해주세요")
    private DocumentFormat format;

    /**
     * 분석 옵션
     */
    @Builder.Default
    private ErrorAnalysisOptions options = new ErrorAnalysisOptions();
}
