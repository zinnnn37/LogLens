package S13P31A306.loglens.domain.jira.controller.impl;

import S13P31A306.loglens.domain.jira.constants.JiraSuccessCode;
import S13P31A306.loglens.domain.jira.controller.JiraIntegrationApi;
import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.request.JiraIssueCreateRequest;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraIssueCreateResponse;
import S13P31A306.loglens.domain.jira.service.JiraIntegrationService;
import S13P31A306.loglens.domain.jira.service.JiraIssueService;
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

/**
 * Jira 연동 Controller
 * Jira 연동 및 이슈 생성 API 엔드포인트를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/integrations/jira")
@RequiredArgsConstructor
public class JiraIntegrationController implements JiraIntegrationApi {

    private final JiraIntegrationService jiraIntegrationService;
    private final JiraIssueService jiraIssueService;

    /**
     * Jira 연동 설정
     *
     * @param request 연동 요청 DTO
     * @return ResponseEntity<BaseResponse>
     */
    @Override
    @PostMapping("/connect")
    public ResponseEntity<? extends BaseResponse> connect(
            @Valid @RequestBody JiraConnectRequest request
    ) {
        log.info("📥 Jira 연동 설정 요청: projectId={}, jiraUrl={}",
                request.projectId(), request.jiraUrl());

        JiraConnectResponse response = jiraIntegrationService.connect(request);

        log.info("✅ Jira 연동 설정 완료: connectionId={}, projectId={}",
                response.id(), response.projectId());

        return ApiResponseFactory.success(JiraSuccessCode.JIRA_CONNECT_SUCCESS, response);
    }

    /**
     * Jira 이슈 생성
     *
     * @param request 이슈 생성 요청 DTO
     * @return ResponseEntity<BaseResponse>
     */
    @Override
    @PostMapping("/issues")
    public ResponseEntity<? extends BaseResponse> createIssue(
            @Valid @RequestBody JiraIssueCreateRequest request
    ) {
        log.info("📥 Jira 이슈 생성 요청: projectId={}, logId={}, summary={}",
                request.projectId(), request.logId(), request.summary());

        JiraIssueCreateResponse response = jiraIssueService.createIssue(request);

        log.info("✅ Jira 이슈 생성 완료: issueKey={}, projectId={}",
                response.issueKey(), request.projectId());

        return ApiResponseFactory.success(JiraSuccessCode.JIRA_ISSUE_CREATE_SUCCESS, response);
    }
}
