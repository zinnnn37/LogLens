package S13P31A306.loglens.domain.analysis.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 문서 메타데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {

    /**
     * 문서 제목
     */
    private String title;

    /**
     * 생성 일시
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;

    /**
     * 분석 데이터 시간 범위
     */
    private TimeRange dataRange;

    /**
     * 문서 요약
     */
    private DocumentSummary summary;

    /**
     * 단어 수 (AI 생성 시)
     */
    private Integer wordCount;

    /**
     * 예상 읽기 시간 (분)
     */
    private String estimatedReadingTime;
}
