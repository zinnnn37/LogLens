package S13P31A306.loglens.domain.alert.service.impl;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.FORBIDDEN;

import S13P31A306.loglens.domain.alert.dto.AlertConfigCreateRequest;
import S13P31A306.loglens.domain.alert.dto.AlertConfigResponse;
import S13P31A306.loglens.domain.alert.dto.AlertConfigUpdateRequest;
import S13P31A306.loglens.domain.alert.entity.AlertConfig;
import S13P31A306.loglens.domain.alert.exception.AlertErrorCode;
import S13P31A306.loglens.domain.alert.mapper.AlertConfigMapper;
import S13P31A306.loglens.domain.alert.repository.AlertConfigRepository;
import S13P31A306.loglens.domain.alert.service.AlertConfigService;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.global.exception.BusinessException;
import a306.dependency_logger_starter.logging.annotation.NoLogging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 설정 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertConfigServiceImpl implements AlertConfigService {

    private static final String LOG_PREFIX = "[AlertConfigService]";

    private final AlertConfigRepository alertConfigRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final AlertConfigMapper alertConfigMapper;

    @Override
    @Transactional
    public AlertConfigResponse createAlertConfig(AlertConfigCreateRequest request, Integer userId) {
        log.info("{} 알림 설정 생성 시작: projectUuid={}", LOG_PREFIX, request.projectUuid());

        // 1. UUID → ID 변환 및 프로젝트 존재 여부 확인
        Integer projectId = projectService.getProjectIdByUuid(request.projectUuid());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트 없음: projectId={}", LOG_PREFIX, projectId);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });

        // 2. 사용자의 프로젝트 접근 권한 확인
        validateProjectAccess(projectId, userId);

        // 3. 기존 알림 설정 중복 확인
        if (alertConfigRepository.existsByProjectId(projectId)) {
            log.warn("{} 이미 알림 설정 존재: projectId={}", LOG_PREFIX, projectId);
            throw new BusinessException(AlertErrorCode.ALERT_CONFIG_ALREADY_EXISTS);
        }

        // 4. activeYN 유효성 검증
        validateActiveYN(request.activeYN());

        // 5. 알림 설정 생성
        AlertConfig alertConfig = AlertConfig.builder()
                .alertType(request.alertType())
                .thresholdValue(request.thresholdValue())
                .activeYN(request.activeYN())
                .projectId(projectId)
                .build();

        AlertConfig saved = alertConfigRepository.save(alertConfig);

        log.info("{} 알림 설정이 생성되었습니다: id={}", LOG_PREFIX, saved.getId());

        return alertConfigMapper.toResponse(saved, project.getProjectName(), project.getProjectUuid());
    }

    @NoLogging
    @Override
    public AlertConfigResponse getAlertConfig(String projectUuid, Integer userId) {
        log.info("{} 알림 설정 조회 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        // 1. UUID → ID 변환 및 프로젝트 존재 여부 확인
        Integer projectId = projectService.getProjectIdByUuid(projectUuid);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트 없음: projectId={}", LOG_PREFIX, projectId);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });

        // 2. 사용자의 프로젝트 접근 권한 확인
        validateProjectAccess(projectId, userId);

        // 3. 알림 설정 조회 (없으면 null 반환)
        return alertConfigRepository.findByProjectId(projectId)
                .map(alertConfig -> alertConfigMapper.toResponse(alertConfig, project.getProjectName(),
                        project.getProjectUuid()))
                .orElse(null);
    }

    @Override
    @Transactional
    public AlertConfigResponse updateAlertConfig(AlertConfigUpdateRequest request, Integer userId) {
        log.info("{} 알림 설정 수정 시작: alertConfigId={}", LOG_PREFIX, request.alertConfigId());

        // 1. 알림 설정 존재 여부 확인
        AlertConfig alertConfig = alertConfigRepository.findById(request.alertConfigId())
                .orElseThrow(() -> {
                    log.warn("{} 알림 설정 없음: id={}", LOG_PREFIX, request.alertConfigId());
                    return new BusinessException(AlertErrorCode.ALERT_CONFIG_NOT_FOUND);
                });

        // 2. 프로젝트 존재 여부 확인
        Project project = projectRepository.findById(alertConfig.getProjectId())
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트 없음: projectId={}", LOG_PREFIX, alertConfig.getProjectId());
                    return new BusinessException(PROJECT_NOT_FOUND);
                });

        // 3. 사용자의 프로젝트 접근 권한 확인
        validateProjectAccess(alertConfig.getProjectId(), userId);

        // 4. activeYN 유효성 검증 (제공된 경우만)
        if (request.activeYN() != null) {
            validateActiveYN(request.activeYN());
        }

        // 5. 알림 설정 수정 (부분 업데이트)
        alertConfig.update(request.alertType(), request.thresholdValue(), request.activeYN());

        log.info("{} 알림 설정이 수정되었습니다: id={}", LOG_PREFIX, alertConfig.getId());

        return alertConfigMapper.toResponse(alertConfig, project.getProjectName(), project.getProjectUuid());
    }

    /**
     * 프로젝트 접근 권한 검증
     */
    private void validateProjectAccess(Integer projectId, Integer userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 접근 권한 없음: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
            throw new BusinessException(FORBIDDEN);
        }
    }

    /**
     * activeYN 값 검증
     */
    private void validateActiveYN(String activeYN) {
        if (activeYN != null && !activeYN.equals("Y") && !activeYN.equals("N")) {
            log.warn("{} 잘못된 activeYN 값: {}", LOG_PREFIX, activeYN);
            throw new BusinessException(AlertErrorCode.INVALID_ACTIVE_YN);
        }
    }
}
