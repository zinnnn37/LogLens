package S13P31A306.loglens.domain.analysis.dto.ai;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * AI HTML 문서 생성 요청
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiHtmlDocumentRequest {

    /**
     * 프로젝트 UUID
     */
    @JsonProperty("project_uuid")
    private String projectUuid;

    /**
     * 로그 ID (에러 분석용)
     */
    @JsonProperty("log_id")
    private Long logId;

    /**
     * 문서 타입
     */
    @JsonProperty("document_type")
    private DocumentType documentType;

    /**
     * 출력 형식
     */
    @JsonProperty("format")
    private DocumentFormat format;

    /**
     * 분석 데이터 (프로젝트 또는 에러 데이터)
     */
    @JsonProperty("data")
    private Map<String, Object> data;

    /**
     * 생성 옵션
     */
    @JsonProperty("options")
    private Map<String, Object> options;

    /**
     * 스타일 선호도
     */
    @JsonProperty("style_preferences")
    private StylePreferences stylePreferences;

    /**
     * 재생성 피드백 (검증 실패 시)
     */
    @JsonProperty("regeneration_feedback")
    private List<String> regenerationFeedback;
}
