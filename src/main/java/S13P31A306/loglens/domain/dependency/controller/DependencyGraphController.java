package S13P31A306.loglens.domain.dependency.controller;

import S13P31A306.loglens.domain.dependency.dto.request.DependencyGraphBatchRequest;
import S13P31A306.loglens.domain.dependency.service.DependencyGraphService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static S13P31A306.loglens.domain.dependency.constants.DependencyGraphSuccessCode.*;

/**
 * 의존성 그래프 Controller
 * 컴포넌트 간 의존 관계를 관리하는 API
 */
@Slf4j
@RestController
@RequestMapping("/api/dependencies")
@RequiredArgsConstructor
@Tag(name = "Dependency Graph", description = "의존성 그래프 API")
public class DependencyGraphController {

    private final DependencyGraphService dependencyGraphService;

    /**
     * 의존성 그래프 배치 저장
     * Starter에서 수집한 의존성 정보를 일괄 저장
     *
     * @param request 배치 저장 요청
     * @return 성공 응답
     */
    @PostMapping("/relations")
    @Operation(summary = "의존성 그래프 배치 저장", description = "프로젝트의 의존성 관계를 일괄 저장합니다")
    public ResponseEntity<BaseResponse> createDependenciesBatch(
            @Valid @RequestBody DependencyGraphBatchRequest request) {

        log.info("📥 의존성 그래프 배치 저장 요청");
//        log.info("  - 프로젝트: {}", request.projectName());
        log.info("  - 의존성 개수: {}", request.dependencies().size());

        dependencyGraphService.saveAll(request);

        return ApiResponseFactory.success(DEPENDENCY_GRAPHS_BATCH_CREATED);
    }

    /**
     * 프로젝트별 의존성 조회
     *
     * @param projectId 프로젝트 ID
     * @return 의존성 목록
     */
//    @GetMapping("/projects/{projectId}")
//    @Operation(summary = "프로젝트 의존성 조회", description = "특정 프로젝트의 모든 의존성을 조회합니다")
//    public ResponseEntity<BaseResponse> getDependenciesByProject(
//            @PathVariable Integer projectId) {
//
//        log.info("🔍 프로젝트 의존성 조회 요청: projectId={}", projectId);
//
//        List<DependencyGraphResponse> dependencies = dependencyGraphService.findByProjectId(projectId);
//
//        return ApiResponseFactory.success(DEPENDENCY_GRAPHS_RETRIEVED, dependencies);
//    }

    /**
     * 특정 컴포넌트가 호출하는 대상 조회
     *
     * @param projectId 프로젝트 ID
     * @param componentName 컴포넌트 이름
     * @return 의존성 목록
     */
//    @GetMapping("/projects/{projectId}/from/{componentName}")
//    @Operation(summary = "컴포넌트가 호출하는 대상 조회", description = "특정 컴포넌트가 의존하는 대상들을 조회합니다")
//    public ResponseEntity<BaseResponse> getDependenciesFrom(
//            @PathVariable Integer projectId,
//            @PathVariable String componentName) {
//
//        log.info("🔍 의존성 조회 (FROM): projectId={}, component={}", projectId, componentName);
//
//        List<DependencyGraphResponse> dependencies =
//                dependencyGraphService.findDependenciesFrom(projectId, componentName);
//
//        return ApiResponseFactory.success(DEPENDENCY_GRAPHS_RETRIEVED, dependencies);
//    }

    /**
     * 특정 컴포넌트를 호출하는 주체 조회
     *
     * @param projectId 프로젝트 ID
     * @param componentName 컴포넌트 이름
     * @return 의존성 목록
     */
//    @GetMapping("/projects/{projectId}/to/{componentName}")
//    @Operation(summary = "컴포넌트를 호출하는 주체 조회", description = "특정 컴포넌트를 의존하는 주체들을 조회합니다")
//    public ResponseEntity<BaseResponse> getDependenciesTo(
//            @PathVariable Integer projectId,
//            @PathVariable String componentName) {
//
//        log.info("🔍 의존성 조회 (TO): projectId={}, component={}", projectId, componentName);
//
//        List<DependencyGraphResponse> dependencies =
//                dependencyGraphService.findDependenciesTo(projectId, componentName);
//
//        return ApiResponseFactory.success(DEPENDENCY_GRAPHS_RETRIEVED, dependencies);
//    }

//    /**
//     * 프로젝트 의존성 전체 삭제
//     *
//     * @param projectId 프로젝트 ID
//     * @return 성공 응답
//     */
//    @DeleteMapping("/projects/{projectId}")
//    @Operation(summary = "프로젝트 의존성 삭제", description = "특정 프로젝트의 모든 의존성을 삭제합니다")
//    public ResponseEntity<BaseResponse> deleteDependenciesByProject(
//            @PathVariable Integer projectId) {
//
//        log.info("🗑️ 프로젝트 의존성 삭제 요청: projectId={}", projectId);
//
//        dependencyGraphService.deleteByProjectId(projectId);
//
//        return ApiResponseFactory.success(DEPENDENCY_GRAPH_DELETED);
//    }
}
