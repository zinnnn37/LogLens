package S13P31A306.loglens.domain.project.controller;

import S13P31A306.loglens.domain.project.dto.request.ProjectCreateRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectMemberInviteRequest;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@ApiInternalServerError
@Tag(name = "Project API", description = "프로젝트 관련 API")
public interface ProjectApi {

	ResponseEntity<? extends BaseResponse> createProject(
			@Valid @RequestBody ProjectCreateRequest request
	);

	ResponseEntity<? extends BaseResponse> getProjects(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "CREATED_AT") String sort,
			@RequestParam(defaultValue = "DESC") String order
	);

	ResponseEntity<? extends BaseResponse> getProjectDetail(
			@PathVariable int projectId
	);

	ResponseEntity<? extends BaseResponse> deleteProject(
			@PathVariable int projectId
	);

	ResponseEntity<? extends BaseResponse> inviteMember(
			@PathVariable int projectId,
			@Valid @RequestBody ProjectMemberInviteRequest request
	);

	ResponseEntity<? extends BaseResponse> deleteMember(
			@PathVariable int projectId,
			@PathVariable int memberId
	);
}