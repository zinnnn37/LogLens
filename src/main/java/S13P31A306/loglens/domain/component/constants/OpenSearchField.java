package S13P31A306.loglens.domain.component.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OpenSearch 필드명 Enum
 */
@Getter
@RequiredArgsConstructor
public enum OpenSearchField {
    TRACE_ID("trace_id"),
    COMPONENT_NAME("component_name"),
    PROJECT_UUID("project_uuid.keyword"),
    SOURCE_TYPE("source_type"),
    LOG_LEVEL("level");

    private final String fieldName;
}
