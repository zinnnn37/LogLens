package S13P31A306.loglens.global.aop;

public record TraceStatus(
        TraceId traceId,
        long startTime,
        String methodSignature) {
}
