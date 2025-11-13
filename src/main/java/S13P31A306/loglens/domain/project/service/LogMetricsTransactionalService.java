package S13P31A306.loglens.domain.project.service;

import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;

import java.time.LocalDateTime;

public interface LogMetricsTransactionalService {

    /**
     * 프로젝트의 증분 로그 메트릭을 집계하고 저장합니다.
     * 독립된 트랜잭션에서 실행됩니다.
     *
     * @param project 집계할 프로젝트
     * @param from 집계 시작 시간
     * @param to 집계 종료 시간
     * @param previous 이전 누적 메트릭 (null 가능)
     */
    void aggregateProjectMetricsIncremental(
            Project project,
            LocalDateTime from,
            LocalDateTime to,
            LogMetrics previous
    );

}
