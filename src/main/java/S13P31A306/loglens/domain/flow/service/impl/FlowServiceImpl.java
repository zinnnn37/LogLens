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

        LogResponse request = logs.getFirst();
        LogResponse response = logs.getLast();
        Long duration = Duration.between(request.getTimestamp(), response.getTimestamp()).toMillis();

        String status = logs.stream()
                .anyMatch(l -> "ERROR".equals(l.getLogLevel())) ? "ERROR" : "SUCCESS";

        log.info("{} Trace 로그 조회 완료: traceId={}, 로그 수={}, duration={}ms, status={}",
                LOG_PREFIX, traceId, logs.size(), duration, status);

        return new TraceLogsResponse(traceId, projectUuid, request, response, duration, status, logs);
    }

    @Override
    public TraceFlowResponse getTraceFlowById(String traceId, String projectUuid) {
        log.info("{} Trace 흐름 조회 시작: traceId={}, projectUuid={}", LOG_PREFIX, traceId, projectUuid);

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

        List<TimelineEntry> timeline = buildTimeline(logs, projectUuid);

        List<FlowComponentInfo> components = timeline.stream()
                .map(entry -> new FlowComponentInfo(entry.componentId(), entry.componentName(), entry.layer()))
                .distinct()
                .toList();

        Set<Integer> componentIds = timeline.stream()
                .map(TimelineEntry::componentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        DependencyGraphResponse graph = buildDependencyGraph(componentIds, projectUuid);
        FlowSummary summary = buildSummary(logs);

        log.info("{} Trace 흐름 조회 완료: traceId={}, timeline 개수={}, component 개수={}",
                LOG_PREFIX, traceId, timeline.size(), components.size());

        return new TraceFlowResponse(traceId, projectUuid, summary, timeline, components, graph);
    }

    private List<TimelineEntry> buildTimeline(List<LogResponse> logs, String projectUuid) {
        List<TimelineEntry> timeline = new ArrayList<>();

        Integer currentComponentId = null;
        String currentComponentName = null;
        String currentLayer = null;
        List<LogResponse> currentLogs = new ArrayList<>();
        int sequence = 1;

        for (LogResponse logResponse : logs) {
            FlowComponentInfo componentInfo = extractComponentInfo(logResponse.getLogger(), projectUuid);

            // Filter 제외
            if ("Other".equals(componentInfo.layer())) {
                continue;
            }

            // Component 테이블에 없는 것 제외
            if (componentInfo.id() == null) {
                continue;
            }

            if (currentComponentId != null && !Objects.equals(currentComponentId, componentInfo.id())) {
                timeline.add(createTimelineEntry(sequence++, currentComponentId, currentComponentName, currentLayer, currentLogs));
                currentLogs = new ArrayList<>();
            }

            currentComponentId = componentInfo.id();
            currentComponentName = componentInfo.name();
            currentLayer = componentInfo.layer();
            currentLogs.add(logResponse);
        }

        if (!currentLogs.isEmpty()) {
            timeline.add(createTimelineEntry(sequence, currentComponentId, currentComponentName, currentLayer, currentLogs));
        }

        return timeline;
    }

    private TimelineEntry createTimelineEntry(int sequence, Integer componentId, String componentName, String layer, List<LogResponse> logs) {
        LocalDateTime startTime = logs.get(0).getTimestamp();
        LocalDateTime endTime = logs.get(logs.size() - 1).getTimestamp();
        Long duration = Duration.between(startTime, endTime).toMillis();

        return new TimelineEntry(sequence, componentId, componentName, layer, startTime, endTime, duration, new ArrayList<>(logs));
    }

    private FlowComponentInfo extractComponentInfo(String logger, String projectUuid) {
        if (logger == null || logger.isEmpty()) {
            return new FlowComponentInfo(null, UNKNOWN_COMPONENT, null);
        }

        try {
            Integer projectId = projectService.getProjectIdByUuid(projectUuid);

            int lastDot = logger.lastIndexOf('.');
            if (lastDot == -1) {
                return new FlowComponentInfo(null, UNKNOWN_COMPONENT, null);
            }

            String packageName = logger.substring(0, lastDot);
            String className = logger.substring(lastDot + 1);

            Optional<Component> componentOpt = componentRepository.findByProjectIdAndPackageNameAndName(projectId, packageName, className);

            if (componentOpt.isPresent()) {
                Component component = componentOpt.get();
                return new FlowComponentInfo(component.getId(), component.getName(), component.getLayer() != null ? component.getLayer().name() : null);
            } else {
                return new FlowComponentInfo(null, UNKNOWN_COMPONENT, null);
            }
        } catch (Exception e) {
            log.error("{} Component 정보 추출 실패: logger={}", LOG_PREFIX, logger, e);
            return new FlowComponentInfo(null, UNKNOWN_COMPONENT, null);
        }
    }

    private DependencyGraphResponse buildDependencyGraph(Set<Integer> componentIds, String projectUuid) {
        if (componentIds.isEmpty()) {
            return new DependencyGraphResponse(List.of());
        }

        try {
            Integer projectId = projectService.getProjectIdByUuid(projectUuid);
            List<DependencyGraph> dependencies = dependencyGraphRepository.findAllByProjectIdAndComponentIds(projectId, componentIds);
            List<Edge> edges = dependencies.stream().map(dep -> new Edge(dep.getFrom(), dep.getTo())).distinct().toList();
            return new DependencyGraphResponse(edges);
        } catch (Exception e) {
            log.error("{} Dependency graph 조회 실패: projectUuid={}", LOG_PREFIX, projectUuid, e);
            return new DependencyGraphResponse(List.of());
        }
    }

    private FlowSummary buildSummary(List<LogResponse> logs) {
        LocalDateTime startTime = logs.get(0).getTimestamp();
        LocalDateTime endTime = logs.get(logs.size() - 1).getTimestamp();
        Long totalDuration = Duration.between(startTime, endTime).toMillis();

        String status = logs.stream().anyMatch(l -> "ERROR".equals(l.getLogLevel())) ? "ERROR" : "SUCCESS";

        return new FlowSummary(totalDuration, status, startTime, endTime);
    }
}
