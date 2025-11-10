package S13P31A306.loglens.domain.flow.service.impl;

import S13P31A306.loglens.domain.component.entity.Component;
import S13P31A306.loglens.domain.component.repository.ComponentRepository;
import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;
import S13P31A306.loglens.domain.dependency.dto.response.Edge;
import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;
import S13P31A306.loglens.domain.dependency.repository.DependencyGraphRepository;
import S13P31A306.loglens.domain.flow.dto.response.*;
import S13P31A306.loglens.domain.flow.service.FlowService;
import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, noRollbackFor = Exception.class)
public class FlowServiceImpl implements FlowService {
    private static final String LOG_PREFIX = "[FlowService]";
    private static final String UNKNOWN_COMPONENT = "Unknown Component";

    private final LogService logService;
    private final ComponentRepository componentRepository;
    private final DependencyGraphRepository dependencyGraphRepository;
    private final ProjectService projectService;

    @Override
    public TraceLogsResponse getTraceLogsById(String traceId, String projectUuid) {
        log.info("{} Trace 로그 조회 시작: traceId={}, projectUuid={}", LOG_PREFIX, traceId, projectUuid);

        // LogService를 이용하여 trace_id로 로그 검색
        LogSearchRequest searchRequest = LogSearchRequest.builder()
                .projectUuid(projectUuid)
                .traceId(traceId)
                .sort("TIMESTAMP,ASC")
                .size(1000)
                .build();

        TraceLogResponse traceLogResponse = logService.getLogsByTraceId(searchRequest);
        List<LogResponse> logs = traceLogResponse.getLogs();

        if (logs.isEmpty()) {
            log.warn("{} Trace ID에 해당하는 로그를 찾을 수 없음: traceId={}", LOG_PREFIX, traceId);
            throw new BusinessException(LogErrorCode.LOG_NOT_FOUND);
        }

        // request: 가장 빠른 로그 (첫 번째)
        LogResponse request = logs.getFirst();

        // response: 가장 느린 로그 (마지막)
        LogResponse response = logs.getLast();

        // duration 계산 (밀리초)
        Long duration = Duration.between(
                request.getTimestamp(),
                response.getTimestamp()
        ).toMillis();

        // status 판단: ERROR 로그가 하나라도 있으면 ERROR, 없으면 SUCCESS
        String status = logs.stream()
                .anyMatch(log -> "ERROR".equals(log.getLogLevel()))
                ? "ERROR"
                : "SUCCESS";

        log.info("{} Trace 로그 조회 완료: traceId={}, 로그 수={}, duration={}ms, status={}",
                LOG_PREFIX, traceId, logs.size(), duration, status);

        return new TraceLogsResponse(
                traceId,
                projectUuid,
                request,
                response,
                duration,
                status,
                logs
        );
    }

    @Override
    public TraceFlowResponse getTraceFlowById(String traceId, String projectUuid) {
        log.info("{} Trace 흐름 조회 시작: traceId={}, projectUuid={}", LOG_PREFIX, traceId, projectUuid);

        // 1. 로그 조회 (시간순)
        LogSearchRequest searchRequest = LogSearchRequest.builder()
                .projectUuid(projectUuid)
                .traceId(traceId)
                .sort("TIMESTAMP,ASC")
                .size(1000)
                .build();

        TraceLogResponse traceLogResponse = logService.getLogsByTraceId(searchRequest);
        List<LogResponse> logs = traceLogResponse.getLogs();

        if (logs.isEmpty()) {
            log.warn("{} Trace ID에 해당하는 로그를 찾을 수 없음: traceId={}", LOG_PREFIX, traceId);
            throw new BusinessException(LogErrorCode.LOG_NOT_FOUND);
        }

        // 2. Timeline 생성 (컴포넌트가 바뀔 때마다 새 항목)
        List<TimelineEntry> timeline = buildTimeline(logs, projectUuid);

        // 3. 사용된 컴포넌트 정보 수집 (중복 제거)
        List<FlowComponentInfo> components = timeline.stream()
                .map(entry -> new FlowComponentInfo(
                        entry.componentId(),
                        entry.componentName(),
                        entry.layer()
                ))
                .distinct()
                .toList();

        // 4. 사용된 컴포넌트 ID 추출
        Set<Integer> componentIds = timeline.stream()
                .map(TimelineEntry::componentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 5. Dependency Graph 구성
        DependencyGraphResponse graph = buildDependencyGraph(componentIds, projectUuid);

        // 6. Summary 생성
        FlowSummary summary = buildSummary(logs);

        log.info("{} Trace 흐름 조회 완료: traceId={}, timeline 개수={}, component 개수={}",
                LOG_PREFIX, traceId, timeline.size(), components.size());

        return new TraceFlowResponse(
                traceId,
                projectUuid,
                summary,
                timeline,
                components,
                graph
        );
    }

    /**
     * 로그를 시간순으로 순회하면서 컴포넌트가 바뀔 때마다 TimelineEntry 생성
     */
    private List<TimelineEntry> buildTimeline(List<LogResponse> logs, String projectUuid) {
        List<TimelineEntry> timeline = new ArrayList<>();

        Integer currentComponentId = null;
        String currentComponentName = null;
        String currentLayer = null;
        List<LogResponse> currentLogs = new ArrayList<>();
        int sequence = 1;

        for (LogResponse log : logs) {
            // logger에서 component 정보 추출
            FlowComponentInfo componentInfo = extractComponentInfo(log.getLogger(), projectUuid);

            // 컴포넌트가 바뀌면 이전 항목을 timeline에 추가
            if (currentComponentId != null &&
                    !Objects.equals(currentComponentId, componentInfo.id())) {

                timeline.add(createTimelineEntry(
                        sequence++,
                        currentComponentId,
                        currentComponentName,
                        currentLayer,
                        currentLogs
                ));

                currentLogs = new ArrayList<>();
            }

            currentComponentId = componentInfo.id();
            currentComponentName = componentInfo.name();
            currentLayer = componentInfo.layer();
            currentLogs.add(log);
        }

        // 마지막 항목 추가
        if (!currentLogs.isEmpty()) {
            timeline.add(createTimelineEntry(
                    sequence,
                    currentComponentId,
                    currentComponentName,
                    currentLayer,
                    currentLogs
            ));
        }

        return timeline;
    }

    /**
     * TimelineEntry 생성
     */
    private TimelineEntry createTimelineEntry(
            int sequence,
            Integer componentId,
            String componentName,
            String layer,
            List<LogResponse> logs
    ) {
        LocalDateTime startTime = logs.get(0).getTimestamp();
        LocalDateTime endTime = logs.get(logs.size() - 1).getTimestamp();
        Long duration = Duration.between(startTime, endTime).toMillis();

        return new TimelineEntry(
                sequence,
                componentId,
                componentName,
                layer,
                startTime,
                endTime,
                duration,
                new ArrayList<>(logs)
        );
    }

    /**
     * logger 문자열에서 component 정보 추출
     */
    private FlowComponentInfo extractComponentInfo(String logger, String projectUuid) {
        if (logger == null || logger.isEmpty()) {
            return new FlowComponentInfo(null, UNKNOWN_COMPONENT, null);
        }

        try {
            // projectUuid를 projectId로 변환
            Integer projectId = projectService.getProjectIdByUuid(projectUuid);

            int lastDot = logger.lastIndexOf('.');
            if (lastDot == -1) {
                return new FlowComponentInfo(null, UNKNOWN_COMPONENT, null);
            }

            String packageName = logger.substring(0, lastDot);
            String className = logger.substring(lastDot + 1);

            // Component 테이블에서 조회
            Optional<Component> componentOpt = componentRepository
                    .findByProjectIdAndPackageNameAndName(projectId, packageName, className);

            if (componentOpt.isPresent()) {
                Component component = componentOpt.get();
                return new FlowComponentInfo(
                        component.getId(),
                        component.getName(),
                        component.getLayer() != null ? component.getLayer().name() : null
                );
            } else {
                log.debug("{} Component를 찾을 수 없음, Unknown으로 표시: logger={}", LOG_PREFIX, logger);
                return new FlowComponentInfo(null, UNKNOWN_COMPONENT + " (" + className + ")", null);
            }
        } catch (Exception e) {
            log.error("{} Component 정보 추출 실패: logger={}", LOG_PREFIX, logger, e);
            return new FlowComponentInfo(null, UNKNOWN_COMPONENT, null);
        }
    }

    /**
     * 사용된 컴포넌트들의 dependency graph(edges만) 구성
     */
    private DependencyGraphResponse buildDependencyGraph(Set<Integer> componentIds, String projectUuid) {
        if (componentIds.isEmpty()) {
            return new DependencyGraphResponse(List.of());
        }

        try {
            // projectUuid -> projectId 변환
            Integer projectId = projectService.getProjectIdByUuid(projectUuid);

            // Repository에서 직접 조회
            List<DependencyGraph> dependencies = dependencyGraphRepository
                    .findAllByProjectIdAndComponentIds(projectId, componentIds);

            List<Edge> edges = dependencies.stream()
                    .map(dep -> new Edge(dep.getFrom(), dep.getTo()))
                    .distinct()
                    .toList();

            return new DependencyGraphResponse(edges);
        } catch (Exception e) {
            log.error("{} Dependency graph 조회 실패: projectUuid={}", LOG_PREFIX, projectUuid, e);
            return new DependencyGraphResponse(List.of());
        }
    }

    /**
     * FlowSummary 생성
     */
    private FlowSummary buildSummary(List<LogResponse> logs) {
        LocalDateTime startTime = logs.get(0).getTimestamp();
        LocalDateTime endTime = logs.get(logs.size() - 1).getTimestamp();
        Long totalDuration = Duration.between(startTime, endTime).toMillis();

        String status = logs.stream()
                .anyMatch(log -> "ERROR".equals(log.getLogLevel()))
                ? "ERROR"
                : "SUCCESS";

        return new FlowSummary(totalDuration, status, startTime, endTime);
    }
}
