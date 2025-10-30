package S13P31A306.loglens.domain.component.controller;

import S13P31A306.loglens.domain.component.dto.request.ComponentBatchRequest;
import S13P31A306.loglens.domain.component.mapper.ComponentMapper;
import S13P31A306.loglens.domain.component.service.ComponentService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static S13P31A306.loglens.domain.component.constants.ComponentSuccessCode.COMPONENTS_BATCH_CREATED;

@Slf4j
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class ComponentController {
    private final ComponentService componentService;
    private final ComponentMapper componentMapper;

    @PostMapping("/batch")
    public ResponseEntity<BaseResponse> createComponentsBatch(
            @Valid @RequestBody ComponentBatchRequest request) {

        log.info("📥 컴포넌트 배치 저장 요청: 개수={}", request.components().size());

        componentService.saveAll(request);

        return ApiResponseFactory.success(COMPONENTS_BATCH_CREATED);
    }
}
