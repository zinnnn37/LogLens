package a306.dependency_logger_starter.logging.async;

import a306.dependency_logger_starter.logging.context.MDCContext;
import org.springframework.core.task.TaskDecorator;

public class MDCTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return MDCContext.wrap(runnable);
    }
}
