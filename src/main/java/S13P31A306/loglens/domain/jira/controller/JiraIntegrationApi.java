package S13P31A306.loglens.domain.jira.controller;

import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.request.JiraIssueCreateRequest;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.config.swagger.annotation.ApiUnauthorizedError;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Jira 연동 API 인터페이스
 * Swagger 문서화 및 API 스펙 정의
 */
@ApiInternalServerError
@ApiUnauthorizedError
@Tag(name = "Jira Integration API", description = "Jira 연동 관련 API")
public interface JiraIntegrationApi {

    @Operation(
            summary = "Jira 연동 설정",
            description = "LogLens와 Jira를 연동합니다. 연동 설정 후 자동으로 연결 테스트를 수행하여 유효성을 검증합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Jira 연동 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "JiraConnectSuccess", value = """
                                            {
                                              "code": "JI201-1",
                                              "message": "Jira 연동이 성공적으로 설정되었습니다.",
                                              "status": 201,
                                              "timestamp": "2025-11-02T10:30:05Z",
                                              "data": {
                                                "id": 1,
                                                "projectUuid": "550e8400-e29b-41d4-a716-446655440000",
                                                "jiraUrl": "https://your-domain.atlassian.net",
                                                "jiraEmail": "admin@example.com",
                                                "jiraProjectKey": "LOGLENS",
                                                "connectionTest": {
                                                  "status": "SUCCESS",
                                                  "message": "Jira 연결이 성공적으로 테스트되었습니다.",
                                                  "testedAt": "2025-11-02T10:30:00Z"
                                                }
                                              }
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값 유효성 검증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "ValidationError", value = """
                                            {
                                              "code": "JI400-2",
                                              "message": "Jira URL은 필수입니다.",
                                              "status": 400,
                                              "timestamp": "2025-11-02T10:30:05Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "Forbidden", value = """
                                            {
                                              "code": "G403",
                                              "message": "접근 권한이 없습니다",
                                              "status": 403,
                                              "timestamp": "2025-11-02T10:30:05Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "ProjectNotFound", value = """
                                            {
                                              "code": "JI404-1",
                                              "message": "프로젝트를 찾을 수 없습니다.",
                                              "status": 404,
                                              "timestamp": "2025-11-02T10:30:05Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "이미 연동되어 있음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "AlreadyExists", value = """
                                            {
                                              "code": "JI409-1",
                                              "message": "해당 프로젝트는 이미 Jira와 연동되어 있습니다.",
                                              "status": 409,
                                              "timestamp": "2025-11-02T10:30:05Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "502",
                            description = "Jira 서버 연결 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "ConnectionFailed", value = """
                                            {
                                              "code": "JI502-1",
                                              "message": "Jira 서버에 연결할 수 없습니다. URL과 네트워크 연결을 확인해주세요.",
                                              "status": 502,
                                              "timestamp": "2025-11-02T10:30:05Z"
                                            }
                                            """)
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> connect(
            @Valid @RequestBody JiraConnectRequest request
    );

    @Operation(
            summary = "Jira 연동 상태 조회",
            description = "특정 프로젝트의 Jira 연동 상태를 조회합니다. 연동이 존재하는 경우 기본 정보를 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "ConnectionExists",
                                                    description = "Jira 연동이 존재하는 경우",
                                                    value = """
                                                            {
                                                              "code": "JI200-1",
                                                              "message": "Jira 연동 상태를 성공적으로 조회했습니다.",
                                                              "status": 200,
                                                              "timestamp": "2025-11-03T10:30:05Z",
                                                              "data": {
                                                                "exists": true,
                                                                "projectUuid": "550e8400-e29b-41d4-a716-446655440000",
                                                                "connectionId": 1,
                                                                "jiraProjectKey": "LOGLENS"
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "ConnectionNotExists",
                                                    description = "Jira 연동이 존재하지 않는 경우",
                                                    value = """
                                                            {
                                                              "code": "JI200-1",
                                                              "message": "Jira 연동 상태를 성공적으로 조회했습니다.",
                                                              "status": 200,
                                                              "timestamp": "2025-11-03T10:30:05Z",
                                                              "data": {
                                                                "exists": false,
                                                                "projectUuid": "550e8400-e29b-41d4-a716-446655440000",
                                                                "connectionId": null,
                                                                "jiraProjectKey": null
                                                              }
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "접근 권한 없음 (프로젝트 멤버가 아님)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "Forbidden", value = """
                                            {
                                              "code": "G403",
                                              "message": "접근 권한이 없습니다",
                                              "status": 403,
                                              "timestamp": "2025-11-03T10:30:05Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "ProjectNotFound", value = """
                                            {
                                              "code": "JI404-1",
                                              "message": "프로젝트를 찾을 수 없습니다.",
                                              "status": 404,
                                              "timestamp": "2025-11-03T10:30:05Z"
                                            }
                                            """)
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getConnectionStatus(
            @RequestParam(name = "projectUuid") String projectUuid
    );

    @Operation(
            summary = "Jira 이슈 생성",
            description = "로그 정보를 기반으로 Jira 이슈를 생성합니다. 생성된 이슈 정보는 별도로 저장하지 않으며, Jira에서만 관리됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Jira 이슈 생성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "IssueCreateSuccess", value = """
                                            {
                                              "code": "JI201-2",
                                              "message": "Jira 이슈가 성공적으로 생성되었습니다.",
                                              "status": 201,
                                              "timestamp": "2025-11-02T10:35:05Z",
                                              "data": {
                                                "issueKey": "LOGLENS-1234",
                                                "jiraUrl": "https://your-domain.atlassian.net/browse/LOGLENS-1234",
                                                "createdBy": {
                                                  "userId": 1,
                                                  "email": "user@example.com",
                                                  "name": "John Doe"
                                                }
                                              }
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값 유효성 검증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "ValidationError", value = """
                                            {
                                              "code": "JI400-14",
                                              "message": "이슈 제목은 필수입니다.",
                                              "status": 400,
                                              "timestamp": "2025-11-02T10:35:05Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Jira 연동 정보를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "ConnectionNotFound", value = """
                                            {
                                              "code": "JI404-3",
                                              "message": "프로젝트에 Jira 연동이 설정되지 않았습니다.",
                                              "status": 404,
                                              "timestamp": "2025-11-02T10:35:05Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "502",
                            description = "Jira API 호출 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "ApiError", value = """
                                            {
                                              "code": "JI502-4",
                                              "message": "Jira API 호출 중 오류가 발생했습니다.",
                                              "status": 502,
                                              "timestamp": "2025-11-02T10:35:05Z"
                                            }
                                            """)
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> createIssue(
            @Valid @RequestBody JiraIssueCreateRequest request
    );
}
