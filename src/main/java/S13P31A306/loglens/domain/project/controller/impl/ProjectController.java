package S13P31A306.loglens.domain.project.controller.impl;

import S13P31A306.loglens.domain.project.constants.ProjectSuccessCode;
import S13P31A306.loglens.domain.project.controller.ProjectApi;
import S13P31A306.loglens.domain.project.dto.request.ProjectCreateRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectMemberInviteRequest;
import S13P31A306.loglens.domain.project.dto.response.*;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController implements ProjectApi {

	private final ProjectService projectService;

	@PostMapping
	@Override
	public ResponseEntity<? extends BaseResponse> createProject(ProjectCreateRequest request) {
		ProjectCreateResponse response = projectService.createProject(request);
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(SuccessResponse.of(ProjectSuccessCode.PROJECT_CREATED, response));
	}

	@GetMapping
	@Override
	public ResponseEntity<? extends BaseResponse> getProjects(
			int page,
			int size,
			String sort,
			String order
	) {
		ProjectListResponse response = projectService.getProjects(page, size, sort, order);
		return ResponseEntity.ok(
				SuccessResponse.of(ProjectSuccessCode.PROJECT_LIST_RETRIEVED, response)
		);
	}

	@GetMapping("/{projectId}")
	@Override
	public ResponseEntity<? extends BaseResponse> getProjectDetail(@PathVariable int projectId) {
		ProjectDetailResponse response = projectService.getProjectDetail(projectId);
		return ResponseEntity.ok(
				SuccessResponse.of(ProjectSuccessCode.PROJECT_RETRIEVED, response)
		);
	}

	@DeleteMapping("/{projectId}")
	@Override
	public ResponseEntity<? extends BaseResponse> deleteProject(@PathVariable int projectId) {
		projectService.deleteProject(projectId);
		return ResponseEntity.ok(
				SuccessResponse.of(ProjectSuccessCode.PROJECT_DELETED)
		);
	}

	@PostMapping("/{projectId}/members")
	@Override
	public ResponseEntity<? extends BaseResponse> inviteMember(
			@PathVariable int projectId,
			ProjectMemberInviteRequest request
	) {
		ProjectMemberInviteResponse response = projectService.inviteMember(projectId, request);
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(SuccessResponse.of(ProjectSuccessCode.MEMBER_INVITED, response));
	}

	@DeleteMapping("/{projectId}/members/{memberId}")
	@Override
	public ResponseEntity<? extends BaseResponse> deleteMember(
			@PathVariable int projectId,
			@PathVariable int memberId
	) {
		projectService.deleteMember(projectId, memberId);
		return ResponseEntity.ok(
				SuccessResponse.of(ProjectSuccessCode.MEMBER_DELETED)
		);
	}
}