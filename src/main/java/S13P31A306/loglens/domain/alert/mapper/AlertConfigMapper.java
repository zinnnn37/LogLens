package S13P31A306.loglens.domain.alert.mapper;

import S13P31A306.loglens.domain.alert.dto.AlertConfigResponse;
import S13P31A306.loglens.domain.alert.entity.AlertConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * AlertConfig 매핑 인터페이스
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AlertConfigMapper {

    /**
     * AlertConfig Entity를 AlertConfigResponse DTO로 변환
     *
     * @param alertConfig 알림 설정 엔티티
     * @param projectName 프로젝트 이름 (외부 주입)
     * @param projectUuid 프로젝트 UUID (외부 주입)
     * @return AlertConfigResponse
     */
    @Mapping(source = "projectName", target = "projectName")
    @Mapping(source = "projectUuid", target = "projectUuid")
    AlertConfigResponse toResponse(AlertConfig alertConfig, String projectName, String projectUuid);

    /**
     * AlertConfig Entity 리스트를 AlertConfigResponse DTO 리스트로 변환
     *
     * @param alertConfigs 알림 설정 엔티티 리스트
     * @param projectName 프로젝트 이름 (외부 주입)
     * @param projectUuid 프로젝트 UUID (외부 주입)
     * @return List<AlertConfigResponse>
     */
    default List<AlertConfigResponse> toResponseList(
            List<AlertConfig> alertConfigs,
            String projectName,
            String projectUuid) {
        return alertConfigs.stream()
                .map(config -> toResponse(config, projectName, projectUuid))
                .toList();
    }
}
