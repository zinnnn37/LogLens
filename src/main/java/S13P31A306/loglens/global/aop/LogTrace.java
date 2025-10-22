package S13P31A306.loglens.global.aop;

import static S13P31A306.loglens.global.constants.Messages.LOG_COMPLETE_PREFIX;
import static S13P31A306.loglens.global.constants.Messages.LOG_EXCEPTION_PREFIX;
import static S13P31A306.loglens.global.constants.Messages.LOG_START_PREFIX;
import static S13P31A306.loglens.global.constants.Messages.LOG_UNSUPPORTED_LEVEL;

import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.utils.TimestampUtils;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * 메서드 실행 추적을 위한 로깅을 처리하는 컴포넌트
 */
@Slf4j
@Component
public class LogTrace {

    // 로그 메시지 패턴 상수
    private static final String MESSAGE_PATTERN = "[%s] %s%s";
    private static final String COMPLETE_MESSAGE_PATTERN = "[%s] %s%s time=%dms%s";
    private static final String EXCEPTION_FORMAT = " ex=%s";

    // 스레드별 TraceId를 관리하는 ThreadLocal 객체
    private final ThreadLocal<TraceId> traceIdHolder = new ThreadLocal<>();

    /**
     * 메서드 실행 추적을 시작합니다.
     */
    public TraceStatus begin(final String methodSignature, final LogLevel level) {
        TraceId traceId = syncTraceId();
        String formattedSpace = formatSpace(LOG_START_PREFIX.message(), traceId.level());
        String message = String.format(MESSAGE_PATTERN, traceId.id(), formattedSpace, methodSignature);

        logByLevel(level, message, null);
        return new TraceStatus(traceId, TimestampUtils.currentTimeMillis(), methodSignature);
    }

    /**
     * 메서드 실행 추적을 정상 종료합니다.
     */
    public void end(final TraceStatus status, final LogLevel level) {
        complete(status, null, level);
    }

    /**
     * 메서드 실행 추적을 예외 발생으로 종료합니다.
     */
    public void exception(final TraceStatus status, final Exception e, final LogLevel level) {
        complete(status, e, level);
    }

    /**
     * 메서드 실행 추적을 완료하고 로깅합니다.
     */
    private void complete(final TraceStatus status, final Exception e, final LogLevel level) {
        long duration = TimestampUtils.currentTimeMillis() - status.startTime();
        TraceId traceId = status.traceId();
        String message = formatLogMessage(status, e, traceId, duration);

        logByLevel(level, message, e);
        releaseTraceId();
    }

    /**
     * 로그 메시지를 포맷팅합니다.
     */
    private String formatLogMessage(final TraceStatus status, final Exception e, final TraceId traceId,
                                    final long duration) {
        String prefix =
                Objects.isNull(e) ? LOG_COMPLETE_PREFIX.message() : LOG_EXCEPTION_PREFIX.message();
        String formattedSpace = formatSpace(prefix, traceId.level());
        String exceptionMessage = Optional.ofNullable(e)
                .map(ex -> String.format(EXCEPTION_FORMAT, ex.getClass().getSimpleName())) // 예외 클래스 이름만 출력
                .orElse("");

        return String.format(COMPLETE_MESSAGE_PATTERN,
                traceId.id(),
                formattedSpace,
                status.methodSignature(),
                duration,
                exceptionMessage);
    }

    /**
     * 로그 레벨에 따라 적절한 로깅을 수행합니다.
     */
    private void logByLevel(final LogLevel level, final String message, final Exception e) {
        // BusinessException 또는 AuthenticationException은 예상된 예외이므로 StackTrace 없이 메시지만 로깅
        if (e instanceof BusinessException || e instanceof AuthenticationException) {
            switch (level) {
                case LogLevel.TRACE -> log.trace(message);
                case LogLevel.DEBUG -> log.debug(message);
                case LogLevel.INFO -> log.info(message);
                default -> log.warn(LOG_UNSUPPORTED_LEVEL.message(), level);
            }
            return;
        }

        // 그 외 예상치 못한 예외는 StackTrace와 함께 로깅
        switch (level) {
            case LogLevel.TRACE -> log.trace(message, e);
            case LogLevel.DEBUG -> log.debug(message, e);
            case LogLevel.INFO -> log.info(message, e);
            default -> log.warn(LOG_UNSUPPORTED_LEVEL.message(), level);
        }
    }

    /**
     * TraceId를 동기화합니다. 처음 호출 시 새로운 TraceId를 생성하고, 이후 호출 시 다음 레벨의 TraceId를 생성합니다.
     */
    private TraceId syncTraceId() {
        TraceId traceId = traceIdHolder.get();
        if (Objects.isNull(traceId)) {
            traceIdHolder.set(TraceId.create());
            return traceIdHolder.get();
        }

        TraceId nextId = traceId.createNextId();
        traceIdHolder.set(nextId);
        return nextId;
    }

    /**
     * TraceId를 해제합니다. 첫 번째 레벨인 경우 ThreadLocal에서 제거하고, 그 외의 경우 이전 레벨의 TraceId로 설정합니다.
     */
    private void releaseTraceId() {
        TraceId traceId = traceIdHolder.get();
        if (Objects.isNull(traceId)) {
            return;
        }

        if (traceId.isFirstLevel()) {
            traceIdHolder.remove();
            return;
        }

        traceIdHolder.set(traceId.createPreviousId());
    }

    /**
     * 로그 메시지의 들여쓰기를 포맷팅합니다.
     */
    private String formatSpace(final String prefix, final int level) {
        if (level <= 0) {
            return "";
        }
        return "| ".repeat(level - 1) + "|" + prefix + " ";
    }
}
