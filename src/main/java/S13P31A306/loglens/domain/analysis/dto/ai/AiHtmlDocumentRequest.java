package S13P31A306.loglens.domain.analysis.dto.ai;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    private String projectUuid;

    /**
     * 로그 ID (에러 분석용)
     */
    private Long logId;

    /**
     * 문서 타입
     */
    private DocumentType documentType;

    /**
     * 출력 형식
     */
    private DocumentFormat format;

    /**
     * 분석 데이터 (프로젝트 또는 에러 데이터)
     */
    private Map<String, Object> data;

    /**
     * 생성 옵션
     */
    private Map<String, Object> options;

    /**
     * 스타일 선호도
     */
    private StylePreferences stylePreferences;

    /**
     * 재생성 피드백 (검증 실패 시)
     */
    private List<String> regenerationFeedback;
}
