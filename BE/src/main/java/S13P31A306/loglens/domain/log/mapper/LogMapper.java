package S13P31A306.loglens.domain.log.mapper;

import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LogMapper {

    /**
     * Log Entity -> LogResponse DTO 변환 logDetails에서 중복 필드 제거 포함
     */
    @Mapping(target = "logDetails", expression = "java(removeDuplicateFields(log.getLogDetails()))")
    LogResponse toLogResponse(Log log);

    /**
     * logDetails에서 data 레벨과 중복되는 필드 제거
     */
    default Map<String, Object> removeDuplicateFields(Map<String, Object> logDetails) {
        if (Objects.isNull(logDetails) || logDetails.isEmpty()) {
            return logDetails;
        }

        java.util.List<String> duplicateFields = java.util.List.of(
                "execution_time",  // duration과 중복
                "class_name",      // className과 중복
                "method_name",     // methodName과 중복
                "method",          // methodName과 중복
                "http_method",
                "request_uri"
        );

        return logDetails.entrySet().stream()
                .filter(entry -> !duplicateFields.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * OffsetDateTime → LocalDateTime 변환 헬퍼 (MapStruct가 자동으로 인식해서 사용)
     */
    default LocalDateTime map(OffsetDateTime offsetDateTime) {
        return !Objects.isNull(offsetDateTime) ? offsetDateTime.toLocalDateTime() : null;
    }
}
