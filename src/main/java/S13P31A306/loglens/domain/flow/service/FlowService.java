package S13P31A306.loglens.domain.flow.service;

import S13P31A306.loglens.domain.flow.dto.response.TraceFlowResponse;
import S13P31A306.loglens.domain.flow.dto.response.TraceLogsResponse;

public interface FlowService {
    /**
     * TraceID로 로그 목록 조회
     * @param traceId 추적 ID
     * @param projectUuid 프로젝트 UUID
     * @return Trace 로그 목록
     */
    TraceLogsResponse getTraceLogsById(String traceId, String projectUuid);

    /**
     * TraceID로 요청 흐름 조회
     * @param traceId 추적 ID
     * @param projectUuid 프로젝트 UUID
     * @return Trace 요청 흐름
     */
    TraceFlowResponse getTraceFlowById(String traceId, String projectUuid);
}
