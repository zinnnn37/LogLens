package S13P31A306.loglens.domain.component.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OpenSearch 필드명 Enum
 */
@Getter
@RequiredArgsConstructor
public enum OpenSearchField {
    TRACE_ID("trace_id.keyword"),
    COMPONENT_NAME("component_name.keyword"),
    PROJECT_UUID("project_uuid.keyword"),
    SOURCE_TYPE("source_type.keyword"),
    LOG_LEVEL("level.keyword");

    private final String fieldName;
}
