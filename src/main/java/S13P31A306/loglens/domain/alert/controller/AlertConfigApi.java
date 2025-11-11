package S13P31A306.loglens.domain.alert.controller;

import S13P31A306.loglens.domain.alert.dto.AlertConfigCreateRequest;
import S13P31A306.loglens.domain.alert.dto.AlertConfigResponse;
import S13P31A306.loglens.domain.alert.dto.AlertConfigUpdateRequest;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.config.swagger.annotation.ApiUnauthorizedError;
import S13P31A306.loglens.global.constants.SwaggerMessages;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
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
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH)
    )
    ResponseEntity<? extends BaseResponse> createAlertConfig(
            @Valid @RequestBody AlertConfigCreateRequest request);

    @Operation(
            summary = "알림 설정 조회",
            description = "프로젝트의 알림 설정을 조회합니다. 설정이 없는 경우 data는 null입니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH)
    )
    ResponseEntity<? extends BaseResponse> getAlertConfig(
            @RequestParam Integer projectId);

    @Operation(
            summary = "알림 설정 수정",
            description = "프로젝트의 알림 설정을 수정합니다. 제공된 필드만 수정됩니다 (부분 업데이트).",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH)
    )
    ResponseEntity<? extends BaseResponse> updateAlertConfig(
            @Valid @RequestBody AlertConfigUpdateRequest request);
}
