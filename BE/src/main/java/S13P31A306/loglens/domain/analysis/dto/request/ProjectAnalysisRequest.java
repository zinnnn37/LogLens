package S13P31A306.loglens.domain.analysis.dto.request;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로젝트 전체 분석 문서 생성 요청
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAnalysisRequest {

    /**
     * 분석 시작 시간 (optional, 미지정 시 전체 기간)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 분석 종료 시간 (optional, 미지정 시 현재 시간)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 문서 출력 형식
     */
    @NotNull(message = "문서 형식을 지정해주세요")
    private DocumentFormat format;

    /**
     * 분석 옵션
     */
    @Builder.Default
    private AnalysisOptions options = new AnalysisOptions();
}
