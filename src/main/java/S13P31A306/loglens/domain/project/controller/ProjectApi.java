package S13P31A306.loglens.domain.project.controller;

import S13P31A306.loglens.domain.project.dto.request.ProjectCreateRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectMemberInviteRequest;
import S13P31A306.loglens.domain.project.dto.response.ProjectConnectionResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectCreateResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectDetailResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectListResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectMemberInviteResponse;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.config.swagger.annotation.ApiUnauthorizedError;
import S13P31A306.loglens.global.constants.SwaggerMessages;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@ApiInternalServerError
@ApiUnauthorizedError
@Tag(name = "Project API", description = "프로젝트 관련 API")
public interface ProjectApi {

    @Operation(
            summary = "프로젝트 생성",
            description = "새로운 프로젝트를 생성합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "프로젝트 생성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProjectCreateResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectCreateSuccess",
                                            summary = "프로젝트 생성 성공 예시",
                                            value = """
                                                    {
                                                        "code": "PJ201-1",
                                                        "message": "프로젝트가 성공적으로 생성되었습니다.",
                                                        "status": 201,
                                                        "data": {
                                                            "projectName": "LogLens",
                                                            "description": "로그 수집 및 분석 프로젝트",
                                                            "projectUuid": "pk_1a2b3c4d5e6f",
                                                            "createdAt": "2025-11-03T13:36:36"
                                                        },
                                                        "timestamp": "2025-11-03T13:36:36Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값 유효성 검증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "ProjectNameRequired",
                                                    summary = "프로젝트 이름 없음",
                                                    value = """
                                                            {
                                                                "code": "G400",
                                                                "message": "입력값이 유효하지 않습니다.",
                                                                "status": 400,
                                                                "data": {
                                                                    "path": "/api/projects",
                                                                    "errors": [
                                                                        {
                                                                            "field": "projectName",
                                                                            "rejectedValue": "null",
                                                                            "code": "G400",
                                                                            "reason": "입력값이 유효하지 않습니다."
                                                                        }
                                                                    ]
                                                                },
                                                                "timestamp": "2025-11-03T04:34:55Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "ProjectNameInvalidLength",
                                                    summary = "프로젝트 이름 길이 제한 초과",
                                                    value = """
                                                            {
                                                                "code": "G400",
                                                                "message": "입력값이 유효하지 않습니다.",
                                                                "status": 400,
                                                                "data": {
                                                                    "path": "/api/projects",
                                                                    "errors": [
                                                                        {
                                                                            "field": "projectName",
                                                                            "rejectedValue": "AAAA....",
                                                                            "code": "G400",
                                                                            "reason": "입력값이 유효하지 않습니다."
                                                                        }
                                                                    ]
                                                                },
                                                                "timestamp": "2025-11-03T04:35:27Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "ProjectDescriptionInvalidLength",
                                                    summary = "프로젝트 설명 길이 제한 초과",
                                                    value = """
                                                            {
                                                                "code": "G400",
                                                                "message": "입력값이 유효하지 않습니다.",
                                                                "status": 400,
                                                                "data": {
                                                                    "path": "/api/projects",
                                                                    "errors": [
                                                                        {
                                                                            "field": "description",
                                                                            "rejectedValue": "AAAAA...",
                                                                            "code": "G400",
                                                                            "reason": "입력값이 유효하지 않습니다."
                                                                        }
                                                                    ]
                                                                },
                                                                "timestamp": "2025-11-03T04:35:59Z"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "중복된 입력값",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNameDuplicated",
                                            summary = "중복된 프로젝트 이름",
                                            value = """
                                                    {
                                                        "code": "PJ409-1",
                                                        "message": "이미 사용 중인 프로젝트 이름입니다.",
                                                        "status": 409,
                                                        "timestamp": "2025-11-03T04:36:52Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request
    );

    @Operation(
            summary = "프로젝트 목록 조회",
            description = "현재 사용자가 참여 중인 프로젝트 목록을 조회합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프로젝트 목록 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = ProjectListResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectListRetrieved",
                                            summary = "프로젝트 목록 조회 성공 예시",
                                            value = """
                                                    {
                                                        "code": "PJ200-1",
                                                        "message": "프로젝트 목록을 성공적으로 조회했습니다.",
                                                        "status": 200,
                                                        "data": {
                                                            "content": [
                                                                {
                                                                    "projectName": "LogLens",
                                                                    "description": "로그 수집 및 분석 프로젝트",
                                                                    "projectUuid": "pk_1a2b3c4d5e6f",
                                                                    "memberCount": 6,
                                                                    "createdAt": "2025-11-03T13:36:36",
                                                                    "updatedAt": "2025-11-03T13:36:36",
                                                                    "jiraConnectionExist": true
                                                                },
                                                                {
                                                                    "projectName": "AI Monitor",
                                                                    "description": "AI 모델 상태 모니터링 서비스",
                                                                    "projectUuid": "pk_9z8y7x6w5v4u",
                                                                    "memberCount": 4,
                                                                    "createdAt": "2025-10-15T09:00:00",
                                                                    "updatedAt": "2025-10-20T09:00:00",
                                                                    "jiraConnectionExist": false
                                                                }
                                                            ],
                                                            "pageable": {
                                                                "page": 0,
                                                                "size": 10,
                                                                "sort": "CREATED_AT",
                                                                "order": "DESC"
                                                            },
                                                            "totalElements": 2,
                                                            "totalPages": 1,
                                                            "first": true,
                                                            "last": true
                                                        },
                                                        "timestamp": "2025-11-03T04:37:46Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "CREATED_AT") String sort,
            @RequestParam(defaultValue = "DESC") String order
    );

    @Operation(
            summary = "프로젝트 상세 정보 조회",
            description = "프로젝트의 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프로젝트 상세 정보 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = ProjectDetailResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectDetailRetrieved",
                                            summary = "프로젝트 상세 정보 조회 성공 예시",
                                            value = """
                                                    {
                                                        "code": "PJ200-2",
                                                        "message": "프로젝트 상세 정보를 성공적으로 조회했습니다.",
                                                        "status": 200,
                                                        "data": {
                                                            "projectName": "LogLens",
                                                            "description": "로그 수집 및 분석 프로젝트",
                                                            "projectUuid": "pk_1a2b3c4d5e6f",
                                                            "members": [
                                                                {
                                                                    "userId": 1,
                                                                    "name": "홍길동",
                                                                    "email": "hong@example.com",
                                                                    "joinedAt": "2025-11-03T13:36:36"
                                                                },
                                                                {
                                                                    "userId": 2,
                                                                    "name": "김영희",
                                                                    "email": "kim@example.com",
                                                                    "joinedAt": "2025-11-04T09:12:00"
                                                                }
                                                            ],
                                                            "createdAt": "2025-11-03T13:36:36",
                                                            "updatedAt": "2025-11-03T13:36:36"
                                                        },
                                                        "timestamp": "2025-11-03T04:41:19Z"
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
                                            name = "ProjectAccessForbidden",
                                            summary = "프로젝트 접근 권한 없음",
                                            value = """
                                                    {
                                                        "code": "PJ403-1",
                                                        "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                        "status": 403,
                                                        "timestamp": "2025-11-03T04:43:12Z"
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
                                            summary = "존재하지 않는 프로젝트",
                                            value = """
                                                    {
                                                        "code": "PJ404-1",
                                                        "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                        "status": 404,
                                                        "timestamp": "2025-11-03T04:43:27Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getProjectDetail(
            @PathVariable String projectUuid
    );

    @Operation(
            summary = "프로젝트 삭제",
            description = "프로젝트를 삭제합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프로젝트 삭제 성공",
                            content = @Content(
                                    schema = @Schema(implementation = BaseResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectDeleted",
                                            summary = "프로젝트 삭제 성공",
                                            value = """
                                                    {
                                                        "code": "PJ200-3",
                                                        "message": "프로젝트를 성공적으로 삭제했습니다.",
                                                        "status": 200,
                                                        "timestamp": "2025-11-03T04:37:34Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "프로젝트 삭제 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectDeleteForbidden",
                                            summary = "프로젝트 삭제 권한 없음",
                                            value = """
                                                    {
                                                        "code": "PJ403-2",
                                                        "message": "프로젝트를 삭제할 권한이 없습니다.",
                                                        "status": 403,
                                                        "timestamp": "2025-11-03T04:44:03Z"
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
                                                        "timestamp": "2025-11-03T04:44:24Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> deleteProject(
            @PathVariable String projectUuid
    );

    @Operation(
            summary = "멤버 초대",
            description = "특정 프로젝트에 새 멤버를 초대합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "멤버 초대 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProjectMemberInviteResponse.class),
                                    examples = @ExampleObject(
                                            name = "MemberInvited",
                                            summary = "멤버 초대 성공 예시",
                                            value = """
                                                    {
                                                        "code": "PJ201-2",
                                                        "message": "멤버가 성공적으로 초대되었습니다.",
                                                        "status": 201,
                                                        "data": {
                                                            "member": {
                                                                "userId": 42,
                                                                "userName": "김철수",
                                                                "email": "email@email.com",
                                                                "joinedAt": "2025-11-03T13:36:36"
                                                            }
                                                        },
                                                        "timestamp": "2025-11-03T04:49:12Z"
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
                                            summary = "존재하지 않는 프로젝트",
                                            value = """
                                                    {
                                                        "code": "PJ404-1",
                                                        "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                        "status": 404,
                                                        "timestamp": "2025-11-03T04:45:25Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "UserNotFound",
                                            summary = "존재하지 않는 사용자",
                                            value = """
                                                    {
                                                        "code": "PJ404-2",
                                                        "message": "해당 사용자를 찾을 수 없습니다.",
                                                        "status": 404,
                                                        "timestamp": "2025-11-03T04:45:57Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "이미 프로젝트에 존재하는 멤버",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "MemberExists",
                                            summary = "중복된 사용자",
                                            value = """
                                                    {
                                                        "code": "PJ409-2",
                                                        "message": "이미 프로젝트 멤버입니다.",
                                                        "status": 409,
                                                        "timestamp": "2025-11-03T04:45:41Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> inviteMember(
            @PathVariable String projectUuid,
            @Valid @RequestBody ProjectMemberInviteRequest request
    );

    @Operation(
            summary = "프로젝트 멤버 삭제",
            description = "프로젝트에 참여 중인 멤버를 삭제합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "멤버 삭제 성공",
                            content = @Content(
                                    schema = @Schema(implementation = BaseResponse.class),
                                    examples = @ExampleObject(
                                            name = "MemberDeleted",
                                            summary = "멤버 삭제 성공 예시",
                                            value = """
                                                    {
                                                        "code": "PJ200-4",
                                                        "message": "멤버를 성공적으로 삭제했습니다.",
                                                        "status": 200,
                                                        "timestamp": "2025-11-03T04:49:21Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "자기 자신 삭제 불가",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "CannotDeleteSelf",
                                            summary = "자기 자신 삭제 불가",
                                            value = """
                                                    {
                                                        "code": "PJ403-4",
                                                        "message": "자기 자신을 프로젝트에서 삭제할 수 없습니다.",
                                                        "status": 403,
                                                        "timestamp": "2025-11-03T04:46:23Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "멤버 삭제 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "MemberDeleteForbidden",
                                            summary = "멤버 삭제 권한 없음",
                                            value = """
                                                    {
                                                        "code": "PJ403-3",
                                                        "message": "멤버를 삭제할 권한이 없습니다.",
                                                        "status": 403,
                                                        "timestamp": "2025-11-03T04:47:12Z"
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
                                            summary = "존재하지 않는 프로젝트",
                                            value = """
                                                    {
                                                        "code": "PJ404-1",
                                                        "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                        "status": 404,
                                                        "timestamp": "2025-11-03T04:46:42Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "멤버를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "MemberNotFound",
                                            summary = "프로젝트 내 멤버 없음",
                                            value = """
                                                    {
                                                        "code": "PJ404-3",
                                                        "message": "해당 멤버를 찾을 수 없습니다.",
                                                        "status": 404,
                                                        "timestamp": "2025-11-03T04:47:47Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> deleteMember(
            @PathVariable String projectUuid,
            @PathVariable int memberId
    );

    @Operation(
            summary = "프로젝트 연결 상태 확인",
            description = "OpenSearch에서 해당 프로젝트의 로그 데이터를 검색하여 프로젝트가 연결되었는지 확인합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프로젝트 연결 상태 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = ProjectConnectionResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "ProjectConnected",
                                                    summary = "프로젝트 연결됨",
                                                    value = """
                                                            {
                                                                "code": "PJ200-5",
                                                                "message": "프로젝트가 정상적으로 연결되었습니다.",
                                                                "status": 200,
                                                                "data": {
                                                                    "projectUuid": "48d96cd7-bf8d-38f5-891c-9c2f6430d871",
                                                                    "isConnected": true
                                                                },
                                                                "timestamp": "2025-11-06T00:00:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "ProjectNotConnected",
                                                    summary = "프로젝트 연결 안됨",
                                                    value = """
                                                            {
                                                                "code": "PJ200-6",
                                                                "message": "프로젝트가 아직 연결되지 않았습니다.",
                                                                "status": 200,
                                                                "data": {
                                                                    "projectUuid": "48d96cd7-bf8d-38f5-891c-9c2f6430d871",
                                                                    "isConnected": false
                                                                },
                                                                "timestamp": "2025-11-06T00:00:00Z"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "프로젝트 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectAccessForbidden",
                                            summary = "프로젝트 접근 권한 없음",
                                            value = """
                                                    {
                                                        "code": "PJ403-1",
                                                        "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                        "status": 403,
                                                        "timestamp": "2025-11-06T00:00:00Z"
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
                                            summary = "존재하지 않는 프로젝트",
                                            value = """
                                                    {
                                                        "code": "PJ404-1",
                                                        "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                        "status": 404,
                                                        "timestamp": "2025-11-06T00:00:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> checkProjectConnection(
            @PathVariable String projectUuid
    );
}
