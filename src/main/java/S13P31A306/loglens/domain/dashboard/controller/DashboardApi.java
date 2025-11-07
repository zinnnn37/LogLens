package S13P31A306.loglens.domain.dashboard.controller;

import S13P31A306.loglens.domain.dashboard.dto.response.ComponentDependencyResponse;
import S13P31A306.loglens.domain.dashboard.dto.response.ProjectComponentsResponse;
import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@ApiInternalServerError
@Tag(name = "Dashboard API", description = "대시보드 관련 API")
public interface DashboardApi {

    /**
     * 통계 개요 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 조회 시작 시간 (ISO 8601)
     * @param endTime 조회 종료 시간 (ISO 8601)
     * @return 총 로그 수, 에러율, API 호출 통계 등
     */
    ResponseEntity<? extends BaseResponse> getStatisticsOverview(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    /**
     * 자주 발생하는 에러 Top N 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param limit 조회할 에러 개수 (기본 10개)
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 에러 타입별 발생 횟수 및 비율
     */
    ResponseEntity<? extends BaseResponse> getTopFrequentErrors(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    /**
     * API 호출 통계 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 엔드포인트별 호출 수, 평균 응답 시간, 에러율
     */
    ResponseEntity<? extends BaseResponse> getApiCallStatistics(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    /**
     * 로그 히트맵 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 시간대별 로그 발생 패턴 (레벨별)
     */
    ResponseEntity<? extends BaseResponse> getLogHeatmap(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    /**
     * 의존성 아키텍처 전체 구조 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @return 컴포넌트 노드와 간선 정보
     */
    ResponseEntity<? extends BaseResponse> getDependencyArchitecture(
            @ValidUuid @RequestParam String projectUuid
    );

    /**
     * 트레이스 플로우 조회
     *
     * @param traceId 트레이스 ID
     * @return 로그 호출 흐름 (시간순 정렬된 컴포넌트 호출 체인)
     */
    ResponseEntity<? extends BaseResponse> getTraceFlow(
            @PathVariable String traceId
    );

    /**
     * 알림 피드 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param severity 심각도 필터 (critical, warning, info)
     * @param isRead 읽음 여부 필터
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 목록 (페이징)
     */
    ResponseEntity<? extends BaseResponse> getAlertFeed(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    );

    @Operation(
            summary = "프로젝트 컴포넌트 목록 조회",
            description = "특정 프로젝트의 모든 컴포넌트 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(in = ParameterIn.QUERY, name = "projectUuid", description = "프로젝트 UUID", required = true, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "컴포넌트 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProjectComponentsResponse.class),
                                    examples = @ExampleObject(
                                            name = "ComponentsRetrieved",
                                            summary = "컴포넌트 목록 조회 성공 예시",
                                            value = """
                                                    {
                                                       "code": "DASH200-1",
                                                       "message": "컴포넌트 목록을 성공적으로 조회했습니다.",
                                                       "status": 200,
                                                       "data": {
                                                         "projectId": 1,
                                                         "components": [
                                                           {
                                                             "id": 1,
                                                             "name": "UserController",
                                                             "type": "BE",
                                                             "classType": "UserController",
                                                             "layer": "CONTROLLER",
                                                             "packageName": "com.example.demo.domain.user.controller",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           },
                                                           {
                                                             "id": 2,
                                                             "name": "UserServiceImpl",
                                                             "type": "BE",
                                                             "classType": "UserServiceImpl",
                                                             "layer": "SERVICE",
                                                             "packageName": "com.example.demo.domain.user.service.impl",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           },
                                                           {
                                                             "id": 3,
                                                             "name": "UserJpaRepository",
                                                             "type": "BE",
                                                             "classType": "UserJpaRepository",
                                                             "layer": "REPOSITORY",
                                                             "packageName": "com.example.demo.domain.user.repository",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           },
                                                           {
                                                             "id": 4,
                                                             "name": "AsyncNotificationService",
                                                             "type": "BE",
                                                             "classType": "AsyncNotificationService",
                                                             "layer": "SERVICE",
                                                             "packageName": "com.example.demo.global.service",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           },
                                                           {
                                                             "id": 5,
                                                             "name": "UserJdbcRepositoryImpl",
                                                             "type": "BE",
                                                             "classType": "UserJdbcRepositoryImpl",
                                                             "layer": "REPOSITORY",
                                                             "packageName": "com.example.demo.domain.user.repository.impl",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           }
                                                         ]
                                                       },
                                                       "timestamp": "2025-11-05T08:09:11.291Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 UUID 형식",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InvalidUuid",
                                            summary = "잘못된 UUID 형식",
                                            value = """
                                                    {
                                                        "code": "G400",
                                                        "message": "입력값이 유효하지 않습니다",
                                                        "status": 400,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "프로젝트 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AccessForbidden",
                                            summary = "접근 권한 없음",
                                            value = """
                                                    {
                                                        "code": "PJ403-1",
                                                        "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                        "status": 403,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNotFound",
                                            summary = "프로젝트 없음",
                                            value = """
                                                    {
                                                        "code": "PJ404-1",
                                                        "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                        "status": 404,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getComponentDependencies(
            @ValidUuid @RequestParam String projectUuid,
            @AuthenticationPrincipal UserDetails userDetails
    );

    @Operation(
            summary = "컴포넌트 의존성 그래프 조회",
            description = "특정 컴포넌트의 의존성 그래프와 메트릭 정보를 조회합니다. 프로젝트의 전체 의존성 그래프를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(in = ParameterIn.PATH, name = "componentId", description = "컴포넌트 ID", required = true, schema = @Schema(type = "integer")),
                    @Parameter(in = ParameterIn.QUERY, name = "projectUuid", description = "프로젝트 UUID", required = true, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "의존성 그래프 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ComponentDependencyResponse.class),
                                    examples = @ExampleObject(
                                            name = "DependencyGraphRetrieved",
                                            summary = "의존성 그래프 조회 성공 예시",
                                            value = """
                                                    {
                                                      "code": "DASH200-2",
                                                      "message": "컴포넌트 의존성 목록을 성공적으로 조회했습니다.",
                                                      "status": 200,
                                                      "data": {
                                                        "components": [
                                                          {
                                                            "id": 1,
                                                            "name": "UserController",
                                                            "type": "BE",
                                                            "classType": "UserController",
                                                            "layer": "CONTROLLER",
                                                            "packageName": "com.example.demo.domain.user.controller",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4915028"
                                                            }
                                                          },
                                                          {
                                                            "id": 2,
                                                            "name": "UserServiceImpl",
                                                            "type": "BE",
                                                            "classType": "UserServiceImpl",
                                                            "layer": "SERVICE",
                                                            "packageName": "com.example.demo.domain.user.service.impl",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4979604"
                                                            }
                                                          },
                                                          {
                                                            "id": 3,
                                                            "name": "UserJpaRepository",
                                                            "type": "BE",
                                                            "classType": "UserJpaRepository",
                                                            "layer": "REPOSITORY",
                                                            "packageName": "com.example.demo.domain.user.repository",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4997273"
                                                            }
                                                          },
                                                          {
                                                            "id": 4,
                                                            "name": "AsyncNotificationService",
                                                            "type": "BE",
                                                            "classType": "AsyncNotificationService",
                                                            "layer": "SERVICE",
                                                            "packageName": "com.example.demo.global.service",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4997273"
                                                            }
                                                          },
                                                          {
                                                            "id": 5,
                                                            "name": "UserJdbcRepositoryImpl",
                                                            "type": "BE",
                                                            "classType": "UserJdbcRepositoryImpl",
                                                            "layer": "REPOSITORY",
                                                            "packageName": "com.example.demo.domain.user.repository.impl",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4997273"
                                                            }
                                                          }
                                                        ],
                                                        "graph": {
                                                          "edges": [
                                                            {
                                                              "from": 1,
                                                              "to": 2
                                                            },
                                                            {
                                                              "from": 2,
                                                              "to": 3
                                                            },
                                                            {
                                                              "from": 2,
                                                              "to": 4
                                                            }
                                                          ]
                                                        },
                                                        "summary": {
                                                          "totalComponents": 5,
                                                          "totalCalls": 0,
                                                          "totalErrors": 0,
                                                          "totalWarns": 0,
                                                          "averageErrorRate": 0,
                                                          "highestErrorComponent": null,
                                                          "highestCallComponent": null
                                                        }
                                                      },
                                                      "timestamp": "2025-11-05T08:09:58.499Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "BadRequest",
                                            summary = "잘못된 요청",
                                            value = """
                                                    {
                                                        "code": "G400",
                                                        "message": "입력값이 유효하지 않습니다",
                                                        "status": 400,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "Forbidden",
                                            summary = "접근 권한 없음",
                                            value = """
                                                    {
                                                        "code": "G403",
                                                        "message": "접근 권한이 없습니다",
                                                        "status": 403,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "컴포넌트 또는 프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotFound",
                                            summary = "리소스 없음",
                                            value = """
                                                    {
                                                        "code": "G404",
                                                        "message": "요청한 리소스를 찾을 수 없습니다",
                                                        "status": 404,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getComponentDependencies(
            @PathVariable Integer componentId,
            @ValidUuid @RequestParam String projectUuid,
            @AuthenticationPrincipal UserDetails userDetails
    );
}
