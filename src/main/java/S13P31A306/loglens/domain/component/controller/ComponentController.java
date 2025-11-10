package S13P31A306.loglens.domain.component.controller;

import S13P31A306.loglens.domain.component.dto.request.ComponentBatchRequest;
import S13P31A306.loglens.domain.component.mapper.ComponentMapper;
import S13P31A306.loglens.domain.component.service.ComponentService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/batch")
    public ResponseEntity<BaseResponse> createComponentsBatch(
            @Valid @RequestBody ComponentBatchRequest request,
            HttpServletRequest httpRequest) {

        Integer projectId = (Integer) httpRequest.getAttribute("projectId");

        log.info("📥 컴포넌트 배치 저장 요청: 개수={}, projectId={}",
                request.components().size(), projectId);

        componentService.saveAll(request, projectId);

        return ApiResponseFactory.success(COMPONENTS_BATCH_CREATED);
    }
}
