package a306.dependency_logger_starter.dependency.client;

import a306.dependency_logger_starter.dependency.dto.Component;
import a306.dependency_logger_starter.dependency.dto.DependencyRelation;
import a306.dependency_logger_starter.dependency.dto.ProjectDependencyInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Collector로 의존성 정보를 전송하는 클라이언트
 */
@Slf4j
public class DependencyLogSender {

    private final WebClient webClient;
    private final boolean enabled;

    /**
     * 생성자
     *
     * @param collectorUrl Collector 서버 URL
     * @param enabled 전송 활성화 여부
     */
    public DependencyLogSender(String collectorUrl, boolean enabled) {
        this.webClient = WebClient.builder()
                .baseUrl(collectorUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.enabled = enabled;

        log.info("DependencyLogSender 초기화 완료");
        log.info("  - Collector URL: {}", collectorUrl);
        log.info("  - 전송 활성화: {}", enabled);
    }

    /**
     * 프로젝트 전체 의존성 정보를 Collector에 전송 (Batch)
     *
     * @param projectInfo 프로젝트 의존성 정보
     */
    public void sendProjectDependencies(ProjectDependencyInfo projectInfo) {
        if (!enabled) {
            log.debug("의존성 전송이 비활성화되어 있습니다.");
            return;
        }

        log.info("📤 프로젝트 의존성 정보 전송 시작: {}", projectInfo.projectName());
        log.info("  - 컴포넌트: {} 개", projectInfo.components().size());
        log.info("  - 의존성 관계: {} 개", projectInfo.dependencies().size());

        try {
            webClient.post()
                    .uri("/api/dependencies/project")
                    .body(Mono.just(projectInfo), ProjectDependencyInfo.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(response ->
                            log.info("✅ 프로젝트 의존성 정보 전송 성공: {}", projectInfo.projectName())
                    )
                    .doOnError(error ->
                            log.warn("⚠️ 프로젝트 의존성 정보 전송 실패: {} - {}",
                                    projectInfo.projectName(),
                                    error.getMessage())
                    )
                    .onErrorResume(e -> {
                        // 에러가 발생해도 애플리케이션은 계속 실행
                        log.error("의존성 전송 중 예외 발생 (무시됨): {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block(); // 동기 처리 (애플리케이션 시작 시 한 번만 실행)

        } catch (Exception e) {
            log.error("의존성 전송 중 예외 발생", e);
        }
    }

    /**
     * 컴포넌트만 Collector에 전송
     *
     * @param projectName 프로젝트명
     * @param components 컴포넌트 목록
     */
    public void sendComponents(String projectName, List<Component> components) {
        if (!enabled) {
            log.debug("의존성 전송이 비활성화되어 있습니다.");
            return;
        }

        log.info("📤 컴포넌트 정보 전송 시작: {}", projectName);
        log.info("  - 컴포넌트: {} 개", components.size());

        try {
            // DTO 생성 (컴포넌트만 포함, 의존성은 빈 리스트)
            ProjectDependencyInfo componentInfo = new ProjectDependencyInfo(
                    projectName,
                    components,
                    List.of()  // 빈 의존성 리스트
            );

            webClient.post()
                    .uri("/api/components/batch")
                    .body(Mono.just(componentInfo), ProjectDependencyInfo.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(response ->
                            log.info("✅ 컴포넌트 정보 전송 성공: {}", projectName)
                    )
                    .doOnError(error ->
                            log.warn("⚠️ 컴포넌트 정보 전송 실패: {} - {}",
                                    projectName,
                                    error.getMessage())
                    )
                    .onErrorResume(e -> {
                        log.error("컴포넌트 전송 중 예외 발생 (무시됨): {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

        } catch (Exception e) {
            log.error("컴포넌트 전송 중 예외 발생", e);
        }
    }

    /**
     * 의존성 관계만 Collector에 전송
     *
     * @param projectName 프로젝트명
     * @param dependencies 의존성 관계 목록
     */
    public void sendDependencies(String projectName, List<DependencyRelation> dependencies) {
        if (!enabled) {
            log.debug("의존성 전송이 비활성화되어 있습니다.");
            return;
        }

        log.info("📤 의존성 관계 정보 전송 시작: {}", projectName);
        log.info("  - 의존성 관계: {} 개", dependencies.size());

        try {
            // DTO 생성 (의존성만 포함, 컴포넌트는 빈 리스트)
            ProjectDependencyInfo dependencyInfo = new ProjectDependencyInfo(
                    projectName,
                    List.of(),  // 빈 컴포넌트 리스트
                    dependencies
            );

            webClient.post()
                    .uri("/api/dependencies/relations")
                    .body(Mono.just(dependencyInfo), ProjectDependencyInfo.class)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(response ->
                            log.info("✅ 의존성 관계 정보 전송 성공: {}", projectName)
                    )
                    .doOnError(error ->
                            log.warn("⚠️ 의존성 관계 정보 전송 실패: {} - {}",
                                    projectName,
                                    error.getMessage())
                    )
                    .onErrorResume(e -> {
                        log.error("의존성 관계 전송 중 예외 발생 (무시됨): {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

        } catch (Exception e) {
            log.error("의존성 관계 전송 중 예외 발생", e);
        }
    }
}
