package S13P31A306.loglens.domain.log.service;

import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    /**
     * 로그 상세 정보 조회 (AI 분석 포함) OpenSearch에서 로그를 조회하고, AI 분석이 없는 경우 AI 서비스를 호출하여 분석 결과를 포함합니다.
     *
     * @param logId       로그 ID
     * @param projectUuid 프로젝트 UUID
     * @return 로그 상세 정보 및 AI 분석 결과
     */
    LogDetailResponse getLogDetail(Long logId, String projectUuid);

    /**
     * 실시간 로그 스트리밍 (SSE) 5초 간격으로 새로운 로그를 조회하여 클라이언트에 전송합니다.
     *
     * @param request 로그 검색 조건
     * @return SseEmitter 객체
     */
    SseEmitter streamLogs(LogSearchRequest request);
}
