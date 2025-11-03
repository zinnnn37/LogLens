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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private static final String LOG_PREFIX = "[ProjectService]";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    private final AuthenticationHelper authHelper;

    @Override
    @Transactional
    public ProjectCreateResponse createProject(ProjectCreateRequest request) {
        log.info("{} 프로젝트 생성 시작: {}", LOG_PREFIX, request.projectName());

        User user = authHelper.getCurrentUser();

        try {
            // 프로젝트 생성
            Project project = Project.builder()
                    .projectName(request.projectName())
                    .description(request.description())
                    .build();

            Project savedProject = projectRepository.save(project);

            ProjectMember creator = ProjectMember.builder()
                    .project(savedProject)
                    .user(user)
                    .build();

            projectMemberRepository.save(creator);

            log.info("{} 프로젝트가 생성되었습니다: {}", LOG_PREFIX, savedProject.getProjectName());

            return projectMapper.toCreateResponse(savedProject);
        } catch (DataIntegrityViolationException e) {
            // TODO: 추후 projectUuid 추가 됐을 때 UNIQUE면 분기 필요
            log.info("{} 중복된 프로젝트 이름입니다: {}", LOG_PREFIX, request.projectName());
            throw new BusinessException(PROJECT_NAME_DUPLICATED);
        }
    }

    @Override
    @Transactional
    public ProjectMemberInviteResponse inviteMember(int projectId, ProjectMemberInviteRequest request) {
        log.info("{} 프로젝트에 사용자 초대 시도", LOG_PREFIX);

        Integer inviter = authHelper.getCurrentUserId();
        Integer invitee = request.userId();

        // 프로젝트가 존재하지 않는 경우
        Project project = findProjectById(projectId);

        // 프로젝트 초대 권한 확인
        validateProjectAccess(projectId, inviter);

        // 사용자 존재 여부 확인
        User targetUser = userRepository.findById(invitee)
                .orElseThrow(() -> {
                    log.warn("{} 사용자가 존재하지 않습니다.", LOG_PREFIX);
                    return new BusinessException(USER_NOT_FOUND);
                });

        // 이미 초되된 경우
        if (projectMemberRepository.findByProjectIdAndUserId(projectId, invitee).isPresent()) {
            log.warn("{} 이미 초대된 사용자입니다.", LOG_PREFIX);
            throw new BusinessException(MEMBER_EXISTS);
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(targetUser)
                .build();

        project.getMembers().add(member);
        projectMemberRepository.save(member);

        log.info("{} 사용자가 초대되었습니다: {}", LOG_PREFIX, project.getProjectName());

        return projectMemberMapper.toInviteResponse(member);
    }

    @Override
    public ProjectListResponse getProjects(int page, int size, String sort, String order) {
        log.info("{} 프로젝트 목록 조회: page={}, size={}, sort={}, order={}", LOG_PREFIX, page, size, sort, order);

        Integer userId = authHelper.getCurrentUserId();

        List<Project> allProjects = projectRepository.findProjectsWithMembersByUserId(userId);

        // 정렬
        Comparator<Project> comparator = getComparator(sort, order);
        allProjects.sort(comparator);

        // 페이징: Fetch Join 사용하여 DB 페이징 불가
        int start = page * size;
        int end = Math.min(start + size, allProjects.size());
        List<Project> pagedProjects = allProjects.subList(start, end);

        // DTO 변환 (기존 Mapper 그대로 사용)
        List<ProjectListResponse.ProjectInfo> projectInfos =
                projectMapper.toProjectInfoList(pagedProjects);

        // 페이지 정보
        int totalElements = allProjects.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        log.info("{} 프로젝트 목록 조회 완료: page={}, size={}, total={}",
                LOG_PREFIX, page, projectInfos.size(), totalElements);

        return new ProjectListResponse(
                projectInfos,
                new ProjectListResponse.Pagination(page, size, sort, order),
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );

    }

    @Override
    public ProjectDetailResponse getProjectDetail(int projectId) {
        log.info("{} 프로젝트 조회 시도", LOG_PREFIX);

        Integer userId = authHelper.getCurrentUserId();

        // 프로젝트 존재 여부
        Project project = findProjectById(projectId);

        // 프로젝트 권한 확인
        validateProjectAccess(projectId, userId);

        log.info("{} 프로젝트 조회 성공: project={}", LOG_PREFIX, project.getProjectName());

        return projectMapper.toDetailResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(int projectId) {
        log.info("{} 프로젝트 삭제 시도", LOG_PREFIX);

        Integer userId = authHelper.getCurrentUserId();

        // 프로젝트 존재 여부
        Project project = findProjectById(projectId);

        // 프로젝트 권한 확인
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 프로젝트 삭제 권한이 없습니다: projectId={}", LOG_PREFIX, project.getProjectName());
            throw new BusinessException(PROJECT_DELETE_FORBIDDEN);
        }

        projectRepository.delete(project);

        log.info("{} 프로젝트 삭제 완료: project={}", LOG_PREFIX, project.getProjectName());
    }

    @Override
    @Transactional
    public void deleteMember(int projectId, int memberId) {
        log.info("{} 프로젝트 멤버 삭제 시도", LOG_PREFIX);

        Integer userId = authHelper.getCurrentUserId();

        // 자기 자신 삭제 방지
        if (userId.equals(memberId)) {
            log.info("{} 자기 자신은 삭제할 수 없습니다.", LOG_PREFIX);
            throw new BusinessException(CANNOT_DELETE_SELF);
        }

        // 프로젝트 존재 여부
        Project project = findProjectById(projectId);

        // 프로젝트 권한 확인
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 멤버 삭제 권한이 없습니다.", LOG_PREFIX);
            throw new BusinessException(MEMBER_DELETE_FORBIDDEN);
        }

        // 삭제하고자 하는 멤버가 프로젝트에 존재하는지 여부 확인
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, memberId)) {
            log.warn("{} 해당 멤버가 존재하지 않습니다.", LOG_PREFIX);
            throw new BusinessException(MEMBER_NOT_FOUND);
        }

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, memberId);

        log.info("{} 멤버 삭제 완료: project={}", LOG_PREFIX, project.getProjectName());
    }

    private Comparator<Project> getComparator(String sort, String order) {
        // TODO: 용어 통일 필요?
        Comparator<Project> comparator = switch (sort.toLowerCase()) {
            case "PROJECT_NAME" -> Comparator.comparing(Project::getProjectName);
            case "UPDATED_AT" -> Comparator.comparing(Project::getUpdatedAt);
            default -> Comparator.comparing(Project::getCreatedAt);
        };
        return order.equalsIgnoreCase("asc") ? comparator : comparator.reversed();
    }

    private Project findProjectById(int projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트를 찾을 수 없습니다.", LOG_PREFIX);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });
    }

    private void validateProjectAccess(int projectId, int userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 프로젝트 접근 권한이 없습니다: projectId={}", LOG_PREFIX, projectId);
            throw new BusinessException(ACCESS_FORBIDDEN);
        }
    }
}
