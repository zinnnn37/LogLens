package S13P31A306.loglens.domain.dependency.filter;

import S13P31A306.loglens.domain.project.service.ProjectService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String UUID_HEADER = "X-UUID";
    private static final String PROJECT_ID_ATTRIBUTE = "projectId";

    private final ProjectService projectService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Collector API 경로만 필터링 (/api/dependencies/*, /api/components/*)
        if (shouldAuthenticate(requestURI)) {
            String uuid = request.getHeader(UUID_HEADER);

            if (Objects.isNull(uuid) || uuid.trim().isEmpty()) {
                log.warn("UUID가 없는 요청: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"UUID가 is required\", \"header\": \"X-UUID가\"}");
                return;
            }

            try {
                Integer projectId = projectService.getProjectIdByUuid(uuid);

                request.setAttribute(PROJECT_ID_ATTRIBUTE, projectId);

                log.debug("✅ UUID 인증 성공: projectId={}, uri={}", projectId, requestURI);

            } catch (IllegalArgumentException e) {
                log.warn("❌ 유효하지 않은 UUID: {}", uuid.substring(0, Math.min(8, uuid.length())));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Invalid UUID\"}");
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

}
