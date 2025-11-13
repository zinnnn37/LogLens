package S13P31A306.loglens.domain.alert.mapper;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.domain.alert.entity.AlertHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * AlertHistory 매핑 인터페이스
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AlertHistoryMapper {

    /**
     * AlertHistory Entity를 AlertHistoryResponse DTO로 변환
     *
     * @param alertHistory 알림 이력 엔티티
     * @param projectUuid 프로젝트 UUID (외부 주입)
     * @return AlertHistoryResponse
     */
    @Mapping(source = "projectUuid", target = "projectUuid")
    AlertHistoryResponse toResponse(AlertHistory alertHistory, String projectUuid);

    /**
     * AlertHistory Entity 리스트를 AlertHistoryResponse DTO 리스트로 변환
     *
     * @param alertHistories 알림 이력 엔티티 리스트
     * @param projectUuid 프로젝트 UUID (외부 주입)
     * @return List<AlertHistoryResponse>
     */
    default List<AlertHistoryResponse> toResponseList(
            List<AlertHistory> alertHistories,
            String projectUuid) {
        return alertHistories.stream()
                .map(history -> toResponse(history, projectUuid))
                .toList();
    }
}
