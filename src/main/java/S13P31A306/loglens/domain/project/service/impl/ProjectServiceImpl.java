package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.project.dto.request.ProjectCreateRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectMemberInviteRequest;
import S13P31A306.loglens.domain.project.dto.response.ProjectCreateResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectDetailResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectListResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectMemberInviteResponse;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.entity.ProjectMember;
import S13P31A306.loglens.domain.project.mapper.ProjectMapper;
import S13P31A306.loglens.domain.project.mapper.ProjectMemberMapper;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private static final String LOG_PREFIX = "[ProjectService]";

    private static ProjectRepository projectRepository;
    private static ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    private static AuthenticationHelper authHelper;

    @Override
    public ProjectCreateResponse createProject(ProjectCreateRequest request) {
        log.info("{} 프로젝트 생성 시작: {}", LOG_PREFIX, request.projectName());

        User user = authHelper.getCurrentUser();

        // 프로젝트명 중복 체크
        if (projectRepository.existsByProjectName(request.projectName())) {
            throw new BusinessException(PROJECT_NAME_DUPLICATED);
        }

        // 프로젝트 생성
        Project project = Project.builder()
                .projectName(request.projectName())
                .description(request.description())
                .build();

        Project savedProject = projectRepository.save(project);

        ProjectMember owner = ProjectMember.builder()
                .project(savedProject)
                .user(user)
                .build();

        log.info("{} 프로젝트가 생성되었습니다: {}", LOG_PREFIX, savedProject.getProjectName());

        return projectMapper.toCreateResponse(savedProject);
    }

    @Override
    public ProjectMemberInviteResponse inviteMember(int projectId, ProjectMemberInviteRequest request) {
        log.info("{} 프로젝트에 사용자 초대 시도", LOG_PREFIX);

        Integer inviter = authHelper.getCurrentUserId();
        Integer invitee = request.userId();

        // 프로젝트가 존재하지 않는 경우
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트를 찾을 수 없습니다.",  LOG_PREFIX);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });

        // 프로젝트 초대 권한 확인
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, inviter)) {
            log.warn("{} 프로젝트 접근 권한이 없습니다.", LOG_PREFIX);
            throw new BusinessException(ACCESS_FORBIDDEN);
        }

        // 사용자 존재 여부 확인
        User targetUser = userRepository.findById(invitee)
                .orElseThrow(() -> {
                    log.warn("{} 사용자가 존재하지 않습니다.", LOG_PREFIX);
                    return new BusinessException(USER_NOT_FOUND);
                });

        // 이미 초되된 경우
        if (projectMemberRepository.findByProjectIdAndUserId(projectId, invitee).isEmpty()) {
            log.warn("{} 이미 초대된 사용자입니다.", LOG_PREFIX);
            throw new BusinessException(MEMBER_EXISTS);
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(targetUser)
                .build();

        projectMemberRepository.save(member);

        log.info("{} 사용자가 초대되었습니다: {}", LOG_PREFIX, project.getProjectName());

        return projectMemberMapper.toInviteResponse(member);
    }

    @Override
    public ProjectListResponse getProjects(int page, int size, String sort, String order) {
        log.info("{} 프로젝트 목록 조회: page={}, size={}, sort={}, order={}", LOG_PREFIX, page, size, sort, order);

        Integer userId = authHelper.getCurrentUserId();

        // Pagination
        Sort.Direction direction = Sort.Direction.fromString(order);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        // 사용자가 속한 프로젝트만 가져오기
        Page<Project> projectPage = projectRepository.findProjectsByMemberId(userId, pageable);

        // DTO
        List<ProjectListResponse.ProjectInfo> projectInfos = projectMapper.toProjectInfoList(projectPage.getContent());

        log.info("{} 프로젝트 목록 조회 성공: 프로젝트 목록 조회 완료: page={}, size={}, total={}",
                LOG_PREFIX, page, projectInfos.size(), projectPage.getTotalElements());

        return new ProjectListResponse(
                projectInfos,
                new ProjectListResponse.Pagination(page, size, sort, order),
                (int) projectPage.getTotalElements(),
                projectPage.getTotalPages(),
                projectPage.isFirst(),
                projectPage.isLast()
        );
    }

    @Override
    public ProjectDetailResponse getProject(int projectId) {
        return null;
    }

    @Override
    public void deleteProject(int projectId) {

    }

    @Override
    public void deleteMember(int projectId, int memberId) {

    }
}
