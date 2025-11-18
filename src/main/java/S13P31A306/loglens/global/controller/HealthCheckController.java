package S13P31A306.loglens.global.controller;

import a306.dependency_logger_starter.logging.annotation.NoLogging;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * <p>
 * Jenkins 파이프라인 및 외부 모니터링 도구에서 사용하는 헬스 체크 엔드포인트를 제공합니다.
 * Spring Security 설정에서 인증 없이 접근 가능하도록 설정되어 있습니다.
 * </p>
 */
@RestController
@NoLogging
public class HealthCheckController {

    /**
     * 헬스 체크 엔드포인트
     * <p>
     * 애플리케이션의 기본적인 상태를 확인합니다.
     * 이 엔드포인트는 Spring Security 인증 없이 접근 가능합니다.
     * </p>
     *
     * @return HTTP 200 OK with status information
     */
    @GetMapping("/health-check")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("message", "Application is running");
        return ResponseEntity.ok(health);
    }
}
