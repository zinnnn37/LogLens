package S13P31A306.loglens.domain.dashboard.dto;

public record FrontendMetricsSummary(
        Integer totalTraces,           // 전체 trace 수 (= 프론트엔드 요청 수)
        Integer totalInfo,         // 전체 INFO 로그 수
        Integer totalWarn,         // 전체 WARN 로그 수
        Integer totalError,        // 전체 ERROR 로그 수
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
     * @param totalInfo INFO 레벨 로그 수
     * @param totalWarn WARN 레벨 로그 수
     * @param totalError ERROR 레벨 로그 수
     * @return FrontendMetricsSummary
     */
    public static FrontendMetricsSummary of(
            Integer totalTraces,
            Integer totalInfo,
            Integer totalWarn,
            Integer totalError
    ) {
        // 에러율 계산 (소수점 2자리)
        double errorRate = calculateErrorRate(totalInfo+totalWarn+totalError, totalError);

        return new FrontendMetricsSummary(
                totalTraces != null ? totalTraces : 0,
                totalInfo,
                totalWarn,
                totalError,
                errorRate
        );
    }

    /**
     * 에러율 계산
     *
     * @param totalTraces 전체 트레이스 수
     * @param totalError 에러 로그 수
     * @return 에러율 (0.00 ~ 100.00)
     */
    private static double calculateErrorRate(Integer totalTraces, Integer totalError) {
        if (totalTraces == null || totalTraces == 0) {
            return 0.0;
        }

        int errors = totalError != null ? totalError : 0;

        // (에러 수 / 전체 트레이스 수) * 100
        // 소수점 2자리 반올림
        return Math.round((errors * 100.0 / totalTraces) * 100.0) / 100.0;
    }
}
