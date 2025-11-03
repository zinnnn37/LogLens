package S13P31A306.loglens.domain.log.service;

import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;

/**
 * 로그 관련 비즈니스 로직을 처리하는 서비스
 */
public interface LogService {

    /**
     * 로그 목록 조회 (페이지네이션)
     *
     * @param request 로그 검색 조건
     * @return 로그 목록 및 페이지네이션 정보
     */
    LogPageResponse getLogs(LogSearchRequest request);

    /**
     * Trace ID로 로그 조회
     *
     * @param request 로그 검색 조건 (traceId 포함)
     * @return Trace ID에 해당하는 로그 목록 및 요약 정보
     */
    TraceLogResponse getLogsByTraceId(LogSearchRequest request);
}
