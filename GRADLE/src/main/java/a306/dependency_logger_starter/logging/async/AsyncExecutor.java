package a306.dependency_logger_starter.logging.async;

import a306.dependency_logger_starter.logging.context.MDCContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 비동기 작업 실행 헬퍼
 * MDC 전파를 자동으로 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncExecutor {

    private final Executor taskExecutor;

    /**
     * 비동기 작업 즉시 실행 (MDC 자동 전파)
     *
     * @param task 실행할 작업
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> run(Runnable task) {
        return CompletableFuture.runAsync(
                MDCContext.wrap(task),
                taskExecutor
        );
    }

    /**
     * 비동기 작업 즉시 실행 (반환값 있음)
     *
     * @param task 실행할 작업
     * @return CompletableFuture<T>
     */
    public <T> CompletableFuture<T> supply(Supplier<T> task) {
        return CompletableFuture.supplyAsync(
                MDCContext.wrap(task),  // ✅ 이제 동작함
                taskExecutor
        );
    }

    /**
     * 지연 후 비동기 작업 실행
     *
     * @param task 실행할 작업
     * @param delay 지연 시간
     * @return CompletableFuture<Void>
     */
    public CompletableFuture<Void> runAfterDelay(Runnable task, Duration delay) {
        return CompletableFuture.runAsync(
                MDCContext.wrap(() -> {
                    try {
                        Thread.sleep(delay.toMillis());
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("비동기 작업 중 인터럽트 발생", e);
                    }
                }),
                taskExecutor
        );
    }

    /**
     * 지연 후 비동기 작업 실행 (반환값 있음)
     */
    public <T> CompletableFuture<T> supplyAfterDelay(Supplier<T> task, Duration delay) {
        Supplier<T> wrappedTask = () -> {  // ✅ 명시적 타입 지정
            try {
                Thread.sleep(delay.toMillis());
                return task.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("비동기 작업 중 인터럽트 발생", e);
                return null;
            }
        };

        return CompletableFuture.supplyAsync(
                MDCContext.wrap(wrappedTask),
                taskExecutor
        );
    }

    /**
     * 여러 작업을 병렬 실행
     */
    @SafeVarargs
    public final CompletableFuture<Void> runAll(Runnable... tasks) {
        CompletableFuture<?>[] futures = new CompletableFuture[tasks.length];

        for (int i = 0; i < tasks.length; i++) {
            futures[i] = run(tasks[i]);
        }

        return CompletableFuture.allOf(futures);
    }

    /**
     * 여러 작업을 병렬 실행 (반환값 있음)
     */
    @SafeVarargs
    public final <T> CompletableFuture<List<T>> supplyAll(Supplier<T>... tasks) {
        CompletableFuture<T>[] futures = new CompletableFuture[tasks.length];

        for (int i = 0; i < tasks.length; i++) {
            futures[i] = supply(tasks[i]);
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> Arrays.stream(futures)
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
}
