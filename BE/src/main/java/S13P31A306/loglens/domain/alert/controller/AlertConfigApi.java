package S13P31A306.loglens.domain.alert.controller;

import S13P31A306.loglens.domain.alert.dto.AlertConfigCreateRequest;
import S13P31A306.loglens.domain.alert.dto.AlertConfigResponse;
import S13P31A306.loglens.domain.alert.dto.AlertConfigUpdateRequest;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.config.swagger.annotation.ApiUnauthorizedError;
import S13P31A306.loglens.global.constants.SwaggerMessages;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 알림 설정 API 인터페이스
 */
@ApiInternalServerError
@ApiUnauthorizedError
@Tag(name = "Alert Config API", description = "알림 설정 관련 API")
public interface AlertConfigApi {

    @Operation(
            summary = "알림 설정 생성",
            description = "프로젝트의 알림 설정을 생성합니다. 프로젝트당 1개의 알림 설정만 생성 가능합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "알림 설정 생성 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AlertConfigResponse.class),
                                    examples = @ExampleObject(
                                            name = "AlertConfigCreateSuccess",
                                            summary = "알림 설정 생성 성공 예시",
                                            value = """
                                                    {
                                                      "code": "AL201-1",
                                                      "message": "알림 설정이 성공적으로 생성되었습니다.",
                                                      "status": 201,
                                                      "data": {
                                                        "id": 1,
                                                        "alertType": "ERROR_THRESHOLD",
                                                        "thresholdValue": 10,
                                                        "activeYN": "Y",
                                                        "projectUuid": "9911573f-8a1d-3b96-98b4-5a0def93513b",
                                                        "projectName": "Loglens"
                                                      },
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값 유효성 검증 실패 또는 알림 설정 중복",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "ProjectUuidRequired",
                                                    summary = "프로젝트 UUID 누락",
                                                    value = """
                                                            {
                                                              "code": "G400",
                                                              "message": "입력값이 유효하지 않습니다.",
                                                              "status": 400,
                                                              "data": {
                                                                "path": "/api/alert/config",
                                                                "errors": [
                                                                  {
                                                                    "field": "projectUuid",
                                                                    "rejectedValue": "null",
                                                                    "code": "G400",
                                                                    "reason": "프로젝트 UUID는 필수입니다."
                                                                  }
                                                                ]
                                                              },
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "AlertTypeRequired",
                                                    summary = "알림 타입 누락",
                                                    value = """
                                                            {
                                                              "code": "G400",
                                                              "message": "입력값이 유효하지 않습니다.",
                                                              "status": 400,
                                                              "data": {
                                                                "path": "/api/alert/config",
                                                                "errors": [
                                                                  {
                                                                    "field": "alertType",
                                                                    "rejectedValue": "null",
                                                                    "code": "G400",
                                                                    "reason": "알림 타입은 필수입니다."
                                                                  }
                                                                ]
                                                              },
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "ThresholdValueOutOfRange",
                                                    summary = "임계값 범위 초과",
                                                    value = """
                                                            {
                                                              "code": "G400",
                                                              "message": "입력값이 유효하지 않습니다.",
                                                              "status": 400,
                                                              "data": {
                                                                "path": "/api/alert/config",
                                                                "errors": [
                                                                  {
                                                                    "field": "thresholdValue",
                                                                    "rejectedValue": "300",
                                                                    "code": "G400",
                                                                    "reason": "임계값은 255 이하여야 합니다."
                                                                  }
                                                                ]
                                                              },
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "AlertConfigAlreadyExists",
                                                    summary = "알림 설정 중복",
                                                    value = """
                                                            {
                                                              "code": "AL400-1",
                                                              "message": "해당 프로젝트에 이미 알림 설정이 존재합니다.",
                                                              "status": 400,
                                                              "timestamp": "2025-11-13T10:30:00Z"
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
                                            name = "ProjectAccessDenied",
                                            summary = "프로젝트 접근 권한 없음",
                                            value = """
                                                    {
                                                      "code": "PJ403",
                                                      "message": "해당 프로젝트에 접근 권한이 없습니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            summary = "프로젝트 미존재",
                                            value = """
                                                    {
                                                      "code": "PJ404",
                                                      "message": "프로젝트를 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> createAlertConfig(
            @Valid @RequestBody AlertConfigCreateRequest request);

    @Operation(
            summary = "알림 설정 조회",
            description = "프로젝트의 알림 설정을 조회합니다. 설정이 없는 경우 data는 null입니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "알림 설정 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AlertConfigResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "AlertConfigFoundSuccess",
                                                    summary = "알림 설정 조회 성공 (설정 존재)",
                                                    value = """
                                                            {
                                                              "code": "AL200-1",
                                                              "message": "알림 설정을 성공적으로 조회했습니다.",
                                                              "status": 200,
                                                              "data": {
                                                                "id": 1,
                                                                "alertType": "ERROR_THRESHOLD",
                                                                "thresholdValue": 10,
                                                                "activeYN": "Y",
                                                                "projectUuid": "9911573f-8a1d-3b96-98b4-5a0def93513b",
                                                                "projectName": "Loglens"
                                                              },
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "AlertConfigNotFoundSuccess",
                                                    summary = "알림 설정 조회 성공 (설정 없음)",
                                                    value = """
                                                            {
                                                              "code": "AL200-1",
                                                              "message": "알림 설정을 성공적으로 조회했습니다.",
                                                              "status": 200,
                                                              "data": null,
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "프로젝트 UUID 누락",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectUuidMissing",
                                            summary = "프로젝트 UUID 누락",
                                            value = """
                                                    {
                                                      "code": "G400",
                                                      "message": "입력값이 유효하지 않습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            name = "ProjectAccessDenied",
                                            summary = "프로젝트 접근 권한 없음",
                                            value = """
                                                    {
                                                      "code": "PJ403",
                                                      "message": "해당 프로젝트에 접근 권한이 없습니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            summary = "프로젝트 미존재",
                                            value = """
                                                    {
                                                      "code": "PJ404",
                                                      "message": "프로젝트를 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getAlertConfig(
            @Parameter(description = "프로젝트 UUID", required = true, example = "9911573f-8a1d-3b96-98b4-5a0def93513b")
            @RequestParam String projectUuid);

    @Operation(
            summary = "알림 설정 수정",
            description = "프로젝트의 알림 설정을 수정합니다. 제공된 필드만 수정됩니다 (부분 업데이트).",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "알림 설정 수정 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AlertConfigResponse.class),
                                    examples = @ExampleObject(
                                            name = "AlertConfigUpdateSuccess",
                                            summary = "알림 설정 수정 성공 예시",
                                            value = """
                                                    {
                                                      "code": "AL200-2",
                                                      "message": "알림 설정을 성공적으로 수정했습니다.",
                                                      "status": 200,
                                                      "data": {
                                                        "id": 1,
                                                        "alertType": "LATENCY",
                                                        "thresholdValue": 100,
                                                        "activeYN": "N",
                                                        "projectUuid": "9911573f-8a1d-3b96-98b4-5a0def93513b",
                                                        "projectName": "Loglens"
                                                      },
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                                    name = "AlertConfigIdRequired",
                                                    summary = "알림 설정 ID 누락",
                                                    value = """
                                                            {
                                                              "code": "G400",
                                                              "message": "입력값이 유효하지 않습니다.",
                                                              "status": 400,
                                                              "data": {
                                                                "path": "/api/alert/config",
                                                                "errors": [
                                                                  {
                                                                    "field": "alertConfigId",
                                                                    "rejectedValue": "null",
                                                                    "code": "G400",
                                                                    "reason": "알림 설정 ID는 필수입니다."
                                                                  }
                                                                ]
                                                              },
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "ThresholdValueOutOfRange",
                                                    summary = "임계값 범위 초과",
                                                    value = """
                                                            {
                                                              "code": "G400",
                                                              "message": "입력값이 유효하지 않습니다.",
                                                              "status": 400,
                                                              "data": {
                                                                "path": "/api/alert/config",
                                                                "errors": [
                                                                  {
                                                                    "field": "thresholdValue",
                                                                    "rejectedValue": "0",
                                                                    "code": "G400",
                                                                    "reason": "임계값은 1 이상이어야 합니다."
                                                                  }
                                                                ]
                                                              },
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidActiveYN",
                                                    summary = "활성화 여부 유효하지 않음",
                                                    value = """
                                                            {
                                                              "code": "AL400-3",
                                                              "message": "활성화 여부는 'Y' 또는 'N'이어야 합니다.",
                                                              "status": 400,
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "알림 설정 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AlertAccessDenied",
                                            summary = "알림 설정 접근 권한 없음",
                                            value = """
                                                    {
                                                      "code": "AL403",
                                                      "message": "해당 알림에 접근 권한이 없습니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "알림 설정을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AlertConfigNotFound",
                                            summary = "알림 설정 미존재",
                                            value = """
                                                    {
                                                      "code": "AL404",
                                                      "message": "알림 설정을 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> updateAlertConfig(
            @Valid @RequestBody AlertConfigUpdateRequest request);
}
