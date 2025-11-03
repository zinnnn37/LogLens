package S13P31A306.loglens.domain.project.controller;

import S13P31A306.loglens.domain.project.dto.request.ProjectCreateRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectMemberInviteRequest;
import S13P31A306.loglens.domain.project.dto.response.ProjectCreateResponse;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
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
@Tag(name = "Project API", description = "프로젝트 관련 API")
public interface ProjectApi {

	@Operation(
			summary = "프로젝트 생성",
			description = "새로운 프로젝트를 생성합니다.",
			security = @SecurityRequirement(name = "BearerAuthentication"),
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
													        "projectId": 0,
													        "projectName": "loglens",
													        "description": "",
													        "projectUuid": null,
													        "createdAt": "2025-11-03T13:36:36.27739",
													        "updatedAt": "2025-11-03T13:36:36.27739"
													    },
													    "timestamp": "2025-11-03T04:36:36.280Z"
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
									examples = @ExampleObject(
											name = "ProjectNameRequired",
											summary = "프로젝트 이름 없음",
											value = """
													{
													    "code": "G400",
													    "message": "입력값이 유효하지 않습니다",
													    "status": 400,
													    "data": {
													        "path": "/api/projects",
													        "errors": [
													            {
													                "field": "projectName",
													                "rejectedValue": "null",
													                "code": "G400",
													                "reason": "입력값이 유효하지 않습니다"
													            }
													        ]
													    },
													    "timestamp": "2025-11-03T04:34:55.614Z"
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
									examples = @ExampleObject(
											name = "ProjectNameInvalidFormat",
											summary = "프로젝트 설명 길이 제한",
											value = """
													{
													    "code": "G400",
													    "message": "입력값이 유효하지 않습니다",
													    "status": 400,
													    "data": {
													        "path": "/api/projects",
													        "errors": [
													            {
													                "field": "projectName",
													                "rejectedValue": "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
													                "code": "G400",
													                "reason": "입력값이 유효하지 않습니다"
													            }
													        ]
													    },
													    "timestamp": "2025-11-03T04:35:27.024Z"
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
									examples = @ExampleObject(
											name = "ProjectDescriptionInvalidFormat",
											summary = "프로젝트 이름 길이 제한",
											value = """
													{
														"code": "G400",
														"message": "입력값이 유효하지 않습니다",
														"status": 400,
														"data": {
															"path": "/api/projects",
															"errors": [
																{
																	"field": "description",
																	"rejectedValue": "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789",
																	"code": "G400",
																	"reason": "입력값이 유효하지 않습니다"
																}
															]
														},
														"timestamp": "2025-11-03T04:35:59.208Z"
													}
													"""
									)
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
														"timestamp": "2025-11-03T04:36:52.609Z"
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
			description = "참여 중인 프로젝트 목록을 조회합니다",
			security = @SecurityRequirement(name = "BearerAuthentication"),
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "프로젝트 목록 조회 성공",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "ProjectListRetrived",
											summary = "프로젝트 목록 조회 성공",
											value = """
													{
														"code": "PJ200-1",
														"message": "프로젝트 목록을 성공적으로 조회했습니다.",
														"status": 200,
														"data": {
															"content": [
																{
																	"projectId": 2,
																	"projectName": "loglens",
																	"description": "",
																	"apiKey": null,
																	"memberCount": 1,
																	"createdAt": "2025-11-03T13:36:36.27739",
																	"updatedAt": "2025-11-03T13:36:36.27739"
																}
															],
															"pageable": {
																"page": 0,
																"size": 10,
																"sort": "CREATED_AT",
																"order": "DESC"
															},
															"totalElements": 1,
															"totalPages": 1,
															"first": true,
															"last": true
														},
														"timestamp": "2025-11-03T04:37:46.748Z"
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
			description = "프로젝트 상세 정보를 조회합니다",
			security = @SecurityRequirement(name = "BearerAuthentication"),
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "프로젝트 상세 정보 조회 성공",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "ProjectDetailRetrived",
											summary = "프로젝트 상세 정보 조회 성공",
											value = """
													{
													    "code": "PJ200-2",
													    "message": "프로젝트 상세 정보를 성공적으로 조회했습니다.",
													    "status": 200,
													    "data": {
													        "projectId": 0,
													        "projectName": "loglens",
													        "description": "",
													        "projectUuid": null,
													        "members": [
													            {
													                "userId": 1,
													                "name": "test",
													                "email": "test@email.com",
													                "joinedAt": "2025-11-03T13:36:36"
													            }
													        ],
													        "createdAt": "2025-11-03T13:36:36.27739",
													        "updatedAt": "2025-11-03T13:36:36.27739"
													    },
													    "timestamp": "2025-11-03T04:41:19.171Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "403",
							description = "프로젝트 조회 권한 없음",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "ProjectAccessForbidden",
											summary = "프로젝트 조회 권한 없음",
											value = """
													{
													    "code": "PJ403-1",
													    "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
													    "status": 403,
													    "timestamp": "2025-11-03T04:43:12.518Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "404",
							description = "프로젝트 없음",
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
													    "timestamp": "2025-11-03T04:43:27.200Z"
													}
													"""
									)
							)
					)
			}
	)
	ResponseEntity<? extends BaseResponse> getProjectDetail(
			@PathVariable int projectId
	);

	@Operation(
			summary = "프로젝트 삭제",
			description = "프로젝트를 삭제합니다",
			security = @SecurityRequirement(name = "BearerAuthentication"),
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "프로젝트 삭제 성공",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "ProjectDeleted",
											summary = "프로젝트 삭제 성공",
											value = """
													{
													    "code": "PJ200-3",
													    "message": "프로젝트를 성공적으로 삭제했습니다.",
													    "status": 200,
													    "data": 1,
													    "timestamp": "2025-11-03T04:37:34.442Z"
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
													    "timestamp": "2025-11-03T04:44:03.848Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "404",
							description = "프로젝트 없음",
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
													    "timestamp": "2025-11-03T04:44:24.674Z"
													}
													"""
									)
							)
					)
			}
	)
	ResponseEntity<? extends BaseResponse> deleteProject(
			@PathVariable int projectId
	);

	@Operation(
			summary = "멤버 초대",
			description = "프로젝트에 멤버를 추가합니다",
			security = @SecurityRequirement(name = "BearerAuthentication"),
			responses = {
					@ApiResponse(
							responseCode = "201",
							description = "멤버 초대 성공",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "MemberInvited",
											summary = "멤버 초대 성공",
											value = """
													{
													    "code": "PJ201-2",
													    "message": "멤버가 성공적으로 초대되었습니다.",
													    "status": 201,
													    "data": {
													        "projectId": 0,
													        "member": null
													    },
													    "timestamp": "2025-11-03T04:49:12.688Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "404",
							description = "프로젝트 없음",
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
													    "timestamp": "2025-11-03T04:45:25.595Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "404",
							description = "사용자 없음",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "UserNotFound",
											summary = "사용자 없음",
											value = """
													{
													    "code": "PJ404-2",
													    "message": "해당 사용자를 찾을 수 없습니다.",
													    "status": 404,
													    "timestamp": "2025-11-03T04:45:57.749Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "409",
							description = "중복된 사용자",
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
													    "timestamp": "2025-11-03T04:45:41.818Z"
													}
													"""
									)
							)
					)
			}
	)
	ResponseEntity<? extends BaseResponse> inviteMember(
			@PathVariable int projectId,
			@Valid @RequestBody ProjectMemberInviteRequest request
	);

	@Operation(
			summary = "프로젝트 멤버 삭제",
			description = "프로젝트에 참여 중인 멤버를 삭제합니다",
			security = @SecurityRequirement(name = "BearerAuthentication"),
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "멤버 삭제 성공",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "MemberDeleted",
											summary = "멤버 삭제 성공",
											value = """
													{
													    "code": "PJ200-4",
													    "message": "멤버를 성공적으로 삭제했습니다.",
													    "status": 200,
													    "data": 2,
													    "timestamp": "2025-11-03T04:49:21.621Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "404",
							description = "프로젝트 없음",
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
													    "timestamp": "2025-11-03T04:46:42.828Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "403",
							description = "자기 자신 삭제 권한 없음",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "CannotDeleteSelf",
											summary = "자기 자신 삭제 권한 없음",
											value = """
													{
													    "code": "PJ403-4",
													    "message": "자기 자신을 프로젝트에서 삭제할 수 없습니다.",
													    "status": 403,
													    "timestamp": "2025-11-03T04:46:23.035Z"
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
													    "timestamp": "2025-11-03T04:47:12.818Z"
													}
													"""
									)
							)
					),
					@ApiResponse(
							responseCode = "404",
							description = "프로젝트에 멤버 없음",
							content = @Content(
									mediaType = "application/json",
									schema = @Schema(implementation = ErrorResponse.class),
									examples = @ExampleObject(
											name = "MemberNotFound",
											summary = "프로젝트에 멤버 없음",
											value = """
													{
													    "code": "PJ404-3",
													    "message": "해당 멤버를 찾을 수 없습니다.",
													    "status": 404,
													    "timestamp": "2025-11-03T04:47:47.538Z"
													}
													"""
									)
							)
					)
			}
	)
	ResponseEntity<? extends BaseResponse> deleteMember(
			@PathVariable int projectId,
			@PathVariable int memberId
	);
}
