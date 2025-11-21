package a306.dependency_logger_starter.logging.context;

import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

public class MDCContext {

    public static Map<String, String> capture() {
        return MDC.getCopyOfContextMap();
    }

    public static void restore(Map<String, String> context) {
        if (context != null) {
            MDC.setContextMap(context);
        } else {
            MDC.clear();
        }
    }

    public static void clear() {
        MDC.clear();
    }

    public static Runnable wrap(Runnable runnable) {
        Map<String, String> context = capture();
        return () -> {
            try {
                restore(context);
                runnable.run();
            } finally {
                clear();
            }
        };
    }

    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        Map<String, String> context = capture();
        return () -> {
            try {
                restore(context);
                return supplier.get();
            } finally {
                clear();
            }
        };
    }
}
