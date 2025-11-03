package S13P31A306.loglens.domain.dependency.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String PROJECT_ID_ATTRIBUTE = "projectId";

//    private final ProjectService projectService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Collector API 경로만 필터링 (/api/dependencies/*, /api/components/*)
        if (shouldAuthenticate(requestURI)) {
            String apiKey = request.getHeader(API_KEY_HEADER);

            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.warn("API Key가 없는 요청: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"API Key is required\", \"header\": \"X-API-Key\"}");
                return;
            }

            try {
                // API Key로 project_id 조회 (캐시 적용됨)
//                Long projectId = projectService.getProjectIdByApiKey(apiKey);
                Integer projectId = getProjectIdByApiKey(apiKey);

                // request attribute에 저장 (Controller에서 사용)
                request.setAttribute(PROJECT_ID_ATTRIBUTE, projectId);

                log.debug("✅ API Key 인증 성공: projectId={}, uri={}", projectId, requestURI);

            } catch (IllegalArgumentException e) {
                log.warn("❌ 유효하지 않은 API Key: {}", apiKey.substring(0, Math.min(8, apiKey.length())));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid API Key\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 인증이 필요한 경로인지 확인
     */
    private boolean shouldAuthenticate(String uri) {
        return uri.startsWith("/api/dependencies/") ||
                uri.startsWith("/api/components/");
    }

    private Integer getProjectIdByApiKey(String apiKey) {
        // ⚠️ 임시 하드코딩 (ProjectService 구현 전까지)
        log.warn("⚠️ [TEMPORARY] projectId를 1로 하드코딩 중...");
        return 1;
    }
}
