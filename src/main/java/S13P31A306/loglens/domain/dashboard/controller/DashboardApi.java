package S13P31A306.loglens.domain.dashboard.controller;

import S13P31A306.loglens.global.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface DashboardApi {

    /**
     * 통계 개요 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 조회 시작 시간 (ISO 8601)
     * @param endTime 조회 종료 시간 (ISO 8601)
     * @return 총 로그 수, 에러율, API 호출 통계 등
     */
    ResponseEntity<? extends BaseResponse> getStatisticsOverview(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    /**
     * 자주 발생하는 에러 Top N 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param limit 조회할 에러 개수 (기본 10개)
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 에러 타입별 발생 횟수 및 비율
     */
    ResponseEntity<? extends BaseResponse> getTopFrequentErrors(
            @RequestParam String projectUuid,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    /**
     * API 호출 통계 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 엔드포인트별 호출 수, 평균 응답 시간, 에러율
     */
    ResponseEntity<? extends BaseResponse> getApiCallStatistics(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    /**
     * 로그 히트맵 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 시간대별 로그 발생 패턴 (레벨별)
     */
    ResponseEntity<? extends BaseResponse> getLogHeatmap(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    /**
     * 의존성 아키텍처 전체 구조 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @return 컴포넌트 노드와 간선 정보
     */
    ResponseEntity<? extends BaseResponse> getDependencyArchitecture(
            @RequestParam String projectUuid
    );

//    /**
//     * 의존성 컴포넌트 목록 조회
//     *
//     * @param projectUuid 프로젝트 UUID
//     * @param layer 계층 필터 (PRESENTATION, SERVICE, REPOSITORY 등)
//     * @param componentType 컴포넌트 타입 필터 (CONTROLLER, SERVICE 등)
//     * @return 컴포넌트 목록
//     */
//    ResponseEntity<? extends BaseResponse> getDependencyComponents(
//            @RequestParam String projectUuid,
//            @RequestParam(required = false) String layer,
//            @RequestParam(required = false) String componentType
//    );
//
//    /**
//     * 특정 컴포넌트 상세 정보 조회
//     *
//     * @param componentId 컴포넌트 ID
//     * @return 컴포넌트 상세 정보 (호출 횟수, 평균 실행 시간 등)
//     */
//    ResponseEntity<? extends BaseResponse> getComponentDetails(
//            @PathVariable Integer componentId
//    );
//
//    /**
//     * 컴포넌트의 의존성 관계 조회
//     *
//     * @param componentId 컴포넌트 ID
//     * @param direction 방향 (from: 호출하는, to: 호출받는, both: 양방향)
//     * @return 의존성 관계 목록
//     */
//    ResponseEntity<? extends BaseResponse> getComponentDependencies(
//            @PathVariable Integer componentId,
//            @RequestParam(required = false, defaultValue = "both") String direction
//    );

    /**
     * 트레이스 플로우 조회
     *
     * @param traceId 트레이스 ID
     * @return 로그 호출 흐름 (시간순 정렬된 컴포넌트 호출 체인)
     */
    ResponseEntity<? extends BaseResponse> getTraceFlow(
            @PathVariable String traceId
    );

    /**
     * 알림 피드 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param severity 심각도 필터 (critical, warning, info)
     * @param isRead 읽음 여부 필터
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 목록 (페이징)
     */
    ResponseEntity<? extends BaseResponse> getAlertFeed(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    );
}
