package S13P31A306.loglens.domain.alert.service;

import S13P31A306.loglens.domain.alert.dto.AlertConfigCreateRequest;
import S13P31A306.loglens.domain.alert.dto.AlertConfigResponse;
import S13P31A306.loglens.domain.alert.dto.AlertConfigUpdateRequest;

/**
 * 알림 설정 서비스 인터페이스
 */
public interface AlertConfigService {

    /**
     * 알림 설정 생성
     *
     * @param request 알림 설정 생성 요청
     * @param userId  사용자 ID
     * @return 생성된 알림 설정
     */
    AlertConfigResponse createAlertConfig(AlertConfigCreateRequest request, Integer userId);

    /**
     * 알림 설정 조회
     *
     * @param projectId 프로젝트 ID
     * @param userId    사용자 ID
     * @return 알림 설정 (없으면 null)
     */
    AlertConfigResponse getAlertConfig(Integer projectId, Integer userId);

    /**
     * 알림 설정 수정
     *
     * @param request 알림 설정 수정 요청
     * @param userId  사용자 ID
     * @return 수정된 알림 설정
     */
    AlertConfigResponse updateAlertConfig(AlertConfigUpdateRequest request, Integer userId);
}
