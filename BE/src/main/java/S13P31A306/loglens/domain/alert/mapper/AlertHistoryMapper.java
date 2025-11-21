package S13P31A306.loglens.domain.alert.mapper;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.domain.alert.entity.AlertHistory;
import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.log.mapper.LogMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * AlertHistory 매핑 인터페이스
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {LogMapper.class}
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

    /**
     * AlertHistory와 관련 로그를 함께 변환
     *
     * @param alertHistory 알림 이력 엔티티
     * @param projectUuid 프로젝트 UUID
     * @param relatedLogs 관련 로그 목록
     * @param logMapper LogMapper 인스턴스
     * @return AlertHistoryResponse
     */
    default AlertHistoryResponse toResponseWithLogs(
            AlertHistory alertHistory,
            String projectUuid,
            List<Log> relatedLogs,
            LogMapper logMapper) {

        // 관련 로그를 LogResponse로 변환
        List<LogResponse> logResponses = Objects.isNull(relatedLogs) || relatedLogs.isEmpty()
                ? Collections.emptyList()
                : relatedLogs.stream()
                        .map(logMapper::toLogResponse)
                        .toList();

        // Trace ID 목록 추출 (중복 제거)
        List<String> traceIds = logResponses.stream()
                .map(LogResponse::getTraceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new AlertHistoryResponse(
                alertHistory.getId(),
                alertHistory.getAlertMessage(),
                alertHistory.getAlertTime(),
                alertHistory.getResolvedYN(),
                alertHistory.getLogReference(),
                alertHistory.getAlertLevel(),
                alertHistory.getTraceId(),
                projectUuid,
                logResponses,
                traceIds
        );
    }
}
