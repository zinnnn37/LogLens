package S13P31A306.loglens.domain.project.controller.impl;

import S13P31A306.loglens.domain.project.constants.ProjectPageNumber;
import S13P31A306.loglens.domain.project.constants.ProjectSuccessCode;
import S13P31A306.loglens.domain.project.controller.ProjectApi;
import S13P31A306.loglens.domain.project.dto.request.ProjectCreateRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectListRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectMemberInviteRequest;
import S13P31A306.loglens.domain.project.dto.response.*;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.INVALID_PAGE_NUMBER;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.INVALID_PAGE_SIZE;

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
			@ModelAttribute ProjectListRequest request
	) {
		// 검증
		validatePageRequest(request);

		ProjectListResponse response = projectService.getProjects(
				request.page(),
				request.size(),
				request.getSortParam(),
				request.getOrderParam()
		);
		return ApiResponseFactory.success(
				ProjectSuccessCode.PROJECT_LIST_RETRIEVED,
				response
		);
	}

	@GetMapping("/{projectId}")
	@Override
	public ResponseEntity<? extends BaseResponse> getProjectDetail(@PathVariable int projectId) {
		ProjectDetailResponse response = projectService.getProjectDetail(projectId);
		return ApiResponseFactory.success(
				ProjectSuccessCode.PROJECT_RETRIEVED,
				response
		);
	}

	@DeleteMapping("/{projectId}")
	@Override
	public ResponseEntity<? extends BaseResponse> deleteProject(@PathVariable int projectId) {
		projectService.deleteProject(projectId);
		return ApiResponseFactory.success(
				ProjectSuccessCode.PROJECT_DELETED,
				projectId
		);
	}

	@PostMapping("/{projectId}/members")
	@Override
	public ResponseEntity<? extends BaseResponse> inviteMember(
			@PathVariable int projectId,
			ProjectMemberInviteRequest request
	) {
		ProjectMemberInviteResponse response = projectService.inviteMember(projectId, request);
		return ApiResponseFactory.success(
				ProjectSuccessCode.MEMBER_INVITED,
				response
		);
	}

	@DeleteMapping("/{projectId}/members/{memberId}")
	@Override
	public ResponseEntity<? extends BaseResponse> deleteMember(
			@PathVariable int projectId,
			@PathVariable int memberId
	) {
		projectService.deleteMember(projectId, memberId);
		return ApiResponseFactory.success(
				ProjectSuccessCode.MEMBER_DELETED,
				projectId
		);
	}

	private void validatePageRequest(ProjectListRequest request) {
		if (request.page() < ProjectPageNumber.MIN_PAGE_NUMBER) {
			throw new BusinessException(INVALID_PAGE_NUMBER);
		}
		if (request.size() < ProjectPageNumber.MIN_PAGE_SIZE || request.size() > ProjectPageNumber.MAX_PAGE_SIZE) {
			throw new BusinessException(INVALID_PAGE_SIZE);
		}
	}

}
