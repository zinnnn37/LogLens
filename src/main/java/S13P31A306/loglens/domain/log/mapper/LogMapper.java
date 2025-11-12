package S13P31A306.loglens.domain.log.mapper;

import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LogMapper {

    /**
     * Log Entity -> LogResponse DTO 변환
     */
    @Mapping(source = "id", target = "logId")
    LogResponse toLogResponse(Log log);

    /**
     * OffsetDateTime → LocalDateTime 변환 헬퍼 (MapStruct가 자동으로 인식해서 사용)
     */
    default LocalDateTime map(OffsetDateTime offsetDateTime) {
        return !Objects.isNull(offsetDateTime) ? offsetDateTime.toLocalDateTime() : null;
    }
}
