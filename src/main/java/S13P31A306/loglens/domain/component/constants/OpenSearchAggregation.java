package S13P31A306.loglens.domain.component.constants;

/**
 * OpenSearch 집계 관련 상수
 */
public final class OpenSearchAggregation {

    private OpenSearchAggregation() {
        throw new IllegalStateException("Constants class");
    }

    /**
     * 집계 최대 크기 (terms aggregation)
     */
    public static final int MAX_SIZE = 10000;

    /**
     * 집계 이름
     */
    public static final class Name {
        private Name() {
            throw new IllegalStateException("Constants class");
        }

        public static final String BY_COMPONENT = "by_component";
        public static final String TOTAL_CALLS = "total_calls";
        public static final String ERROR_TRACES = "error_traces";
        public static final String ERROR_COUNT = "error_count";
        public static final String WARN_TRACES = "warn_traces";
        public static final String WARN_COUNT = "warn_count";
        public static final String TOTAL_TRACES = "total_traces";
        public static final String INFO_LOGS = "info_logs";
        public static final String WARN_LOGS = "warn_logs";
        public static final String ERROR_LOGS = "error_logs";
    }
}
