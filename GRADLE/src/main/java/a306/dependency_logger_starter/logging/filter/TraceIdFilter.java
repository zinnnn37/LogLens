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
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String MDC_TRACE_ID_KEY = "traceId";
    private static final String MDC_CLIENT_IP_KEY = "client_ip";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = extractOrGenerateTraceId(httpRequest);
        String clientIp = extractClientIp(httpRequest);

        try {
            MDC.put(MDC_TRACE_ID_KEY, traceId);
            MDC.put(MDC_CLIENT_IP_KEY, clientIp);

            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("요청 처리 중 예외 발생 [trace_id: {}, client_ip: {}]", traceId, clientIp, e);
            throw e;
        } finally {
            // 반드시 MDC 정리 (Thread Pool 사용 시 메모리 누수 방지)
            MDC.remove(MDC_TRACE_ID_KEY);
            MDC.remove(MDC_CLIENT_IP_KEY);

            log.debug("✅ MDC 정리: trace_id={}, client_ip={}, [{}] {} - Status: {}",
                    traceId,
                    clientIp,
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

    /**
     * 클라이언트 IP 추출
     *
     * 우선순위:
     * 1. X-Forwarded-For 헤더의 맨 왼쪽 IP (실제 클라이언트 IP)
     * 2. request.getRemoteAddr() (직접 연결된 클라이언트 IP)
     *
     * X-Forwarded-For 형식: "client_ip, proxy1_ip, proxy2_ip, ..."
     *
     * @param request HTTP 요청
     * @return 클라이언트 IP 주소
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);

        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // X-Forwarded-For 헤더가 있는 경우, 맨 왼쪽 IP만 추출
            String clientIp = xForwardedFor.split(",")[0].trim();

            // IP 유효성 간단 체크 (비어있지 않고, "unknown"이 아닌 경우)
            if (!clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
                log.debug("🌐 X-Forwarded-For에서 클라이언트 IP 추출: {}", clientIp);
                return clientIp;
            }
        }

        // X-Forwarded-For가 없거나 유효하지 않은 경우, 직접 연결된 IP 사용
        String remoteAddr = request.getRemoteAddr();
        log.debug("🔌 Remote Address 사용: {}", remoteAddr);
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("TraceIdFilter 초기화 완료 - Trace ID 및 Client IP 추적 활성화");
    }

    @Override
    public void destroy() {
        MDC.clear();
        log.info("TraceIdFilter 종료");
    }
}
