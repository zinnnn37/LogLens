package S13P31A306.loglens.domain.project.controller.impl;

import S13P31A306.loglens.domain.project.constants.ProjectSuccessCode;
import S13P31A306.loglens.domain.project.controller.ProjectApi;
import S13P31A306.loglens.domain.project.dto.request.ProjectCreateRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectMemberInviteRequest;
import S13P31A306.loglens.domain.project.dto.response.ProjectCreateResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectDetailResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectListResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectMemberInviteResponse;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController implements ProjectApi {

    private final ProjectService projectService;

    @PostMapping
    @Override
    public ResponseEntity<? extends BaseResponse> createProject(ProjectCreateRequest request) {
        ProjectCreateResponse response = projectService.createProject(request);
        return ApiResponseFactory.success(
                ProjectSuccessCode.PROJECT_CREATED,
                response
        );
    }

    @GetMapping
    @Override
    public ResponseEntity<? extends BaseResponse> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "CREATED_AT") String sort,
            @RequestParam(defaultValue = "DESC") String order
    ) {
        ProjectListResponse response = projectService.getProjects(page, size, sort, order);
        return ApiResponseFactory.success(
                ProjectSuccessCode.PROJECT_LIST_RETRIEVED,
                response);
    }


    @GetMapping("/{projectUuid}")
    @Override
    public ResponseEntity<? extends BaseResponse> getProjectDetail(
            @PathVariable String projectUuid
    ) {
        ProjectDetailResponse response = projectService.getProjectDetail(projectUuid);
        return ApiResponseFactory.success(ProjectSuccessCode.PROJECT_RETRIEVED,
                response);
    }

    @DeleteMapping("/{projectUuid}")
    @Override
    public ResponseEntity<? extends BaseResponse> deleteProject(
            @PathVariable String projectUuid
    ) {
        projectService.deleteProject(projectUuid);
        return ApiResponseFactory.success(ProjectSuccessCode.PROJECT_DELETED);
    }

    @PostMapping("/{projectUuid}/members")
    @Override
    public ResponseEntity<? extends BaseResponse> inviteMember(
            @PathVariable String projectUuid,
            @Valid @RequestBody ProjectMemberInviteRequest request
    ) {
        ProjectMemberInviteResponse response = projectService.inviteMember(projectUuid, request);
        return ApiResponseFactory.success(ProjectSuccessCode.MEMBER_INVITED,
                response);
    }

    @DeleteMapping("/{projectUuid}/members/{memberId}")
    @Override
    public ResponseEntity<? extends BaseResponse> deleteMember(
            @PathVariable String projectUuid,
            @PathVariable int memberId
    ) {
        projectService.deleteMember(projectUuid, memberId);
        return ApiResponseFactory.success(ProjectSuccessCode.MEMBER_DELETED);
    }
}
