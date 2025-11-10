package S13P31A306.loglens.global.config.sse;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SSE(Server-Sent Events) 스트리밍을 위한 스케줄러 설정
 */
@Configuration
public class SseConfig {
    private static final String SSE_THREAD_NAME_PREFIX = "sse-scheduler-";

    @Value("${sse.scheduler.pool-size:50}")
    private int poolSize;

    @Value("${sse.timeout:300000}")
    private long sseTimeout;

    //@formatter:off
    /**
     * SSE 스트리밍을 위한 스케줄러
     * - 동시에 여러 클라이언트의 SSE 연결을 처리하기 위한 스레드 풀
     * - application.yml에서 sse.scheduler.pool-size로 크기 조정 가능
     * - 기본값: 50개 (동시 50개의 SSE 연결 지원)
     */
    //@formatter:on
    @Bean(name = "sseScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService sseScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                poolSize,
                new SseThreadFactory()
        );
        executor.setRemoveOnCancelPolicy(true); // 취소된 태스크는 즉시 제거
        return executor;
    }

    @Bean(name = "sseTimeout")
    public long sseTimeout() {
        return sseTimeout;
    }

    /**
     * SSE 스케줄러 스레드 팩토리 스레드 이름을 "sse-scheduler-{번호}" 형식으로 지정
     */
    private static class SseThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, SSE_THREAD_NAME_PREFIX + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}
