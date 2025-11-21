package a306.dependency_logger_starter.logging.filter;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 프론트엔드 로그 수집 필터
 *
 * 조건:
 * - POST /api/logs/frontend 요청만 처리
 * - 받은 데이터를 그대로 yml 설정 경로에 저장
 */
@Slf4j
public class FrontendLogFilter implements Filter {

    private static final String LOG_ENDPOINT = "/api/logs/frontend";

    private final String logFilePath;

    public FrontendLogFilter(String logFilePath) {
        this.logFilePath = logFilePath;

        // 로그 디렉토리 생성
        createLogDirectory();

        log.info("FrontendLogFilter 초기화 완료 - 로그 경로: {}", logFilePath);
    }

    /**
     * 로그 디렉토리 생성
     */
    private void createLogDirectory() {
        try {
            java.io.File logFile = new java.io.File(logFilePath);
            java.io.File logDir = logFile.getParentFile();

            if (logDir != null && !logDir.exists()) {
                if (logDir.mkdirs()) {
                    log.info("로그 디렉토리 생성: {}", logDir.getAbsolutePath());
                } else {
                    log.warn("로그 디렉토리 생성 실패: {}", logDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("로그 디렉토리 생성 중 오류", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. URI 체크 - /api/logs/frontend가 아니면 통과
        if (!LOG_ENDPOINT.equals(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        // 2. POST 메서드 체크
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) {
            sendError(httpResponse, 405, "Only POST method is allowed");
            return;
        }

        // 3. 로그 데이터 읽기 및 저장
        try {
            String logData = httpRequest.getReader().lines()
                    .collect(java.util.stream.Collectors.joining("\n"));

            if (logData.isEmpty()) {
                sendError(httpResponse, 400, "Empty log data");
                return;
            }

            // 파일에 그대로 저장
            saveLog(logData);

            // 성공 응답
            sendSuccess(httpResponse);

        } catch (Exception e) {
            log.error("프론트엔드 로그 저장 실패", e);
            sendError(httpResponse, 500, "Failed to save log");
        }
    }

    /**
     * 로그 파일에 저장 (받은 데이터 그대로)
     */
    private void saveLog(String logData) throws IOException {
        try (FileWriter fw = new FileWriter(logFilePath, true);
             BufferedWriter bw = new BufferedWriter(fw)) {

            bw.write(logData);
            bw.newLine();

            log.debug("프론트엔드 로그 저장 완료: {} bytes", logData.length());
        }
    }

    /**
     * 성공 응답
     */
    private void sendSuccess(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":\"success\",\"message\":\"Log saved\"}");
    }

    /**
     * 에러 응답
     */
    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                String.format("{\"status\":\"error\",\"message\":\"%s\"}", message)
        );
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("FrontendLogFilter 시작됨");
    }

    @Override
    public void destroy() {
        log.info("FrontendLogFilter 종료됨");
    }
}
