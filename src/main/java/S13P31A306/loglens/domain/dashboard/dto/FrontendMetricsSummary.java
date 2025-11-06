package S13P31A306.loglens.domain.dashboard.dto;

public record FrontendMetricsSummary(
        Integer totalTraces,           // 전체 trace 수 (= 프론트엔드 요청 수)
        Integer totalInfoLogs,         // 전체 INFO 로그 수
        Integer totalWarnLogs,         // 전체 WARN 로그 수
        Integer totalErrorLogs,        // 전체 ERROR 로그 수
        Double errorRate               // 에러율 (errorLogs / totalTraces * 100)
) {
    /**
     * 빈 Frontend 메트릭 생성 (조회 실패 시 사용)
     */
    public static FrontendMetricsSummary empty() {
        return new FrontendMetricsSummary(0, 0, 0, 0, 0.0);
    }

    /**
     * Frontend 메트릭 생성 (에러율 자동 계산)
     *
     * @param totalTraces 전체 트레이스 수
     * @param totalInfoLogs INFO 레벨 로그 수
     * @param totalWarnLogs WARN 레벨 로그 수
     * @param totalErrorLogs ERROR 레벨 로그 수
     * @return FrontendMetricsSummary
     */
    public static FrontendMetricsSummary of(
            Integer totalTraces,
            Integer totalInfoLogs,
            Integer totalWarnLogs,
            Integer totalErrorLogs
    ) {
        // 에러율 계산 (소수점 2자리)
        double errorRate = calculateErrorRate(totalTraces, totalErrorLogs);

        return new FrontendMetricsSummary(
                totalTraces != null ? totalTraces : 0,
                totalInfoLogs != null ? totalInfoLogs : 0,
                totalWarnLogs != null ? totalWarnLogs : 0,
                totalErrorLogs != null ? totalErrorLogs : 0,
                errorRate
        );
    }

    /**
     * 에러율 계산
     *
     * @param totalTraces 전체 트레이스 수
     * @param totalErrorLogs 에러 로그 수
     * @return 에러율 (0.00 ~ 100.00)
     */
    private static double calculateErrorRate(Integer totalTraces, Integer totalErrorLogs) {
        if (totalTraces == null || totalTraces == 0) {
            return 0.0;
        }

        int errors = totalErrorLogs != null ? totalErrorLogs : 0;

        // (에러 수 / 전체 트레이스 수) * 100
        // 소수점 2자리 반올림
        return Math.round((errors * 100.0 / totalTraces) * 100.0) / 100.0;
    }
}
