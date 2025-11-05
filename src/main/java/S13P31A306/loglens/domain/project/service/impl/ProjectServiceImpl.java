package S13P31A306.loglens.domain.project.service.impl;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.ACCESS_FORBIDDEN;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.CANNOT_DELETE_SELF;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.MEMBER_DELETE_FORBIDDEN;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.MEMBER_EXISTS;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.MEMBER_NOT_FOUND;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_DELETE_FORBIDDEN;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NAME_DUPLICATED;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.USER_NOT_FOUND;

import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.project.constants.ProjectOrderParam;
import S13P31A306.loglens.domain.project.constants.ProjectSortParam;
import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
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
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private static final String LOG_PREFIX = "[ProjectService]";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final JiraConnectionRepository jiraConnectionRepository;

    private final AuthenticationHelper authHelper;
    private final ProjectValidator projectValidator;

    @Override
    @Transactional
    public ProjectCreateResponse createProject(ProjectCreateRequest request) {
        log.info("{} 프로젝트 생성 시작: {}", LOG_PREFIX, request.projectName());

        User user = authHelper.getCurrentUser();

        try {
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
            log.info("{} 중복된 프로젝트 이름입니다: {}", LOG_PREFIX, request.projectName());
            throw new BusinessException(PROJECT_NAME_DUPLICATED);
        }
    }

    @Override
    @Transactional
    public ProjectMemberInviteResponse inviteMember(String projectUuid, ProjectMemberInviteRequest request) {
        log.info("{} 프로젝트에 사용자 초대 시도", LOG_PREFIX);

        Integer inviteeId = request.userId();

        // 프로젝트 존재 여부 확인
        Project project = projectValidator.validateProjectExists(projectUuid);
        int projectId = project.getId();

        // 현재 사용자의 프로젝트 접근 권한 확인
        projectValidator.validateProjectAccess(projectId);

        // 초대하는 사용자 존재 여부 확인
        User targetUser = projectValidator.validateUserExists(inviteeId);

        // 이미 초대된 경우
        projectValidator.validateMemberNotExists(projectId, inviteeId);

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
    public ProjectListResponse getProjects(int page, int size, ProjectSortParam sort, ProjectOrderParam order) {
        log.info("{} 프로젝트 목록 조회: page={}, size={}, sort={}, order={}", LOG_PREFIX, page, size, sort, order);

        Integer userId = authHelper.getCurrentUserId();

        List<Project> allProjects = projectRepository.findProjectsWithMembersByUserId(userId);

        // 정렬
        Comparator<Project> comparator = getComparator(sort, order);
        allProjects.sort(comparator);

        // 페이징: Fetch Join 사용하여 DB 페이징 불가
        int totalElements = allProjects.size();
        int start = page * size;

        // 범위를 벗어난 페이지 요청 처리
        if (start > 0 && start >= totalElements) {
            log.warn("{} 유효하지 않은 페이지 - start={}, total={}", LOG_PREFIX, start, totalElements);
            log.info("엥");
            throw new BusinessException(PAGE_SIZE_EXCCEED);
        }

        int end = Math.min(start + size, allProjects.size());

        List<Project> pagedProjects = allProjects.subList(start, end);

        // Jira 연결 정보를 Map으로 생성
        Map<Integer, Boolean> jiraConnectionMap = pagedProjects.stream()
                .collect(Collectors.toMap(
                        Project::getId,
                        project -> jiraConnectionRepository.existsByProjectId(project.getId())
                ));

        // DTO 변환
        List<ProjectListResponse.ProjectInfo> projectInfos =
                projectMapper.toProjectInfoList(pagedProjects, jiraConnectionMap);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        log.info("{} 프로젝트 목록 조회 완료: page={}, size={}, total={}",
                LOG_PREFIX, page, projectInfos.size(), totalElements);

        return new ProjectListResponse(
                projectInfos,
                new ProjectListResponse.Pagination(page, size, sort.toString(), order.toString()),
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }

    @Override
    public ProjectDetailResponse getProjectDetail(String projectUuid) {
        log.info("{} 프로젝트 조회 시도", LOG_PREFIX);

        // 프로젝트 존재 여부
        Project project = projectValidator.validateProjectExists(projectUuid);

        // 현재 사용자의 프로젝트 권한 확인
        projectValidator.validateProjectAccess(project.getId());

        log.info("{} 프로젝트 조회 성공: project={}", LOG_PREFIX, project.getProjectName());

        return projectMapper.toDetailResponse(project);
    }

    @Override
    public Integer getProjectIdByUuid(String uuid) {
        log.info("{} UUID로 프로젝트 조회 시도", LOG_PREFIX);

        return projectRepository.findByProjectUuid(uuid)
                .map(Project::getId)
                .orElseThrow(() -> {
                    log.warn("{} 유효하지 않은 API Key입니다.", LOG_PREFIX);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });
    }

    @Override
    @Transactional
    public void deleteProject(String projectUuid) {
        log.info("{} 프로젝트 삭제 시도", LOG_PREFIX);

        // 프로젝트 존재 여부
        Project project = projectValidator.validateProjectExists(projectUuid);

        // 현재 사용자의 프로젝트 권한 확인
        projectValidator.validateProjectAccess(project.getId());

        projectRepository.delete(project);

        log.info("{} 프로젝트 삭제 완료: project={}", LOG_PREFIX, project.getProjectName());
    }

    @Override
    @Transactional
    public void deleteMember(String projectUuid, int memberId) {
        log.info("{} 프로젝트 멤버 삭제 시도", LOG_PREFIX);

        // 현재 사용자가 자기 자신 삭제 방지
        projectValidator.validateCurrentUserNotSelfDeletion(memberId);

        // 프로젝트 존재 여부
        Project project = projectValidator.validateProjectExists(projectUuid);
        int projectId = project.getId();

        // 현재 사용자의 프로젝트 권한 확인
        projectValidator.validateProjectAccess(projectId);

        // 삭제하고자 하는 멤버가 프로젝트에 존재하는지 여부 확인
        projectValidator.validateMemberExists(projectId, memberId);

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, memberId);

        log.info("{} 멤버 삭제 완료: project={}", LOG_PREFIX, project.getProjectName());
    }

    private Comparator<Project> getComparator(ProjectSortParam sort, ProjectOrderParam order) {
        Comparator<Project> comparator = switch (sort) {
            case PROJECT_NAME -> Comparator.comparing(Project::getProjectName);
            case UPDATED_AT -> Comparator.comparing(Project::getUpdatedAt);
            case CREATED_AT -> Comparator.comparing(Project::getCreatedAt);
        };
        return order == ProjectOrderParam.ASC ? comparator : comparator.reversed();
    }

}
