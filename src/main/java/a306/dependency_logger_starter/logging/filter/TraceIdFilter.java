package a306.dependency_logger_starter.logging.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = extractOrGenerateTraceId(httpRequest);

        try {
            MDC.put(MDC_TRACE_ID_KEY, traceId);

            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("요청 처리 중 예외 발생 [trace_id: {}]", traceId, e);
            throw e;
        } finally {
            // 반드시 MDC 정리 (Thread Pool 사용 시 메모리 누수 방지)
            MDC.remove(MDC_TRACE_ID_KEY);

            log.debug("✅ Trace ID 정리: {} [{}] {} - Status: {}",
                    traceId,
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpResponse.getStatus());
        }

    }

    /**
     * Trace ID 추출 또는 생성
     *
     * @param request HTTP 요청
     * @return Trace ID
     */
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (traceId != null && !traceId.trim().isEmpty()) {
            // 클라이언트가 제공한 Trace ID 사용
            log.debug("📥 클라이언트 Trace ID 사용: {}", traceId);
            return traceId.trim();
        }

        // 새로운 Trace ID 생성
        String newTraceId = generateTraceId();
        log.debug("🆕 새로운 Trace ID 생성: {}", newTraceId);
        return newTraceId;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("TraceIdFilter 초기화 완료");
    }

    @Override
    public void destroy() {
        MDC.clear();
    }
}
