//package S13P31A306.loglens.domain.project.service.impl;
//
//import S13P31A306.loglens.domain.project.entity.HeatmapMetrics;
//import S13P31A306.loglens.domain.project.entity.LogMetrics;
//import S13P31A306.loglens.domain.project.entity.Project;
//import S13P31A306.loglens.domain.project.repository.HeatmapMetricsRepository;
//import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
//import S13P31A306.loglens.domain.project.repository.ProjectRepository;
//import S13P31A306.loglens.domain.project.service.LogMetricsTransactionalService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.opensearch.client.json.JsonData;
//import org.opensearch.client.opensearch.OpenSearchClient;
//import org.opensearch.client.opensearch._types.FieldValue;
//import org.opensearch.client.opensearch._types.aggregations.Aggregate;
//import org.opensearch.client.opensearch.core.CountRequest;
//import org.opensearch.client.opensearch.core.CountResponse;
//import org.opensearch.client.opensearch.core.SearchRequest;
//import org.opensearch.client.opensearch.core.SearchResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//
//import javax.sql.DataSource;
//import java.sql.Connection;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//class LogMetricsTransactionalServiceImplTest {
//
//    private static final String TEST_PROJECT_UUID = "a07b75f7-de74-3fe5-8039-185d5c59e83f";
//    private static final String TEST_INDEX = "a07b75f7_de74_3fe5_8039_185d5c59e83f_2025_11";
//
//    @Autowired
//    private DataSource dataSource;
//
//    @Autowired
//    private OpenSearchClient openSearchClient;
//
//    @Autowired
//    private ProjectRepository projectRepository;
//
//    @Autowired
//    private LogMetricsRepository logMetricsRepository;
//
//    @Autowired
//    private HeatmapMetricsRepository heatmapMetricsRepository;
//
//    @Autowired
//    private LogMetricsTransactionalService transactionalService;
//
//    private Project testProject;
//
//    @BeforeEach
//    void setUp() throws Exception {
//        // 1. DB 연결 확인
//        try (Connection conn = dataSource.getConnection()) {
//            assertThat(conn.isValid(1)).isTrue();
//            System.out.println("\n✅ H2 DB 연결 성공: " + conn.getMetaData().getURL());
//        }
//
//        // 2. 있으면 사용, 없으면 생성
//        testProject = projectRepository.findByProjectUuid(TEST_PROJECT_UUID)
//                .orElseGet(() -> {
//                    System.out.println("⚠️ 프로젝트가 없어서 생성합니다.");
//                    return projectRepository.save(
//                            Project.builder()
//                                    .projectName("테스트 프로젝트")
//                                    .projectUuid(TEST_PROJECT_UUID)
//                                    .description("통합 테스트용 프로젝트")
//                                    .build()
//                    );
//                });
//
//        System.out.println("✅ 테스트 프로젝트: id=" + testProject.getId() +
//                ", uuid=" + testProject.getProjectUuid());
//    }
//
//    @Test
//    @DisplayName("1단계: OpenSearch 연결 및 데이터 확인")
//    void step1_testOpenSearchConnection() throws Exception {
//        System.out.println("\n=== 1단계: OpenSearch 연결 테스트 ===");
//
//        CountRequest countRequest = CountRequest.of(c -> c.index(TEST_INDEX));
//        CountResponse countResponse = openSearchClient.count(countRequest);
//
//        System.out.println("OpenSearch 총 문서 수: " + countResponse.count());
//        assertThat(countResponse.count()).isGreaterThan(0);
//    }
//
//    @Test
//    @DisplayName("2단계: OpenSearch 로그 레벨별 카운트 확인")
//    void step2_testLogLevelCount() throws Exception {
//        System.out.println("\n=== 2단계: 로그 레벨별 카운트 ===");
//
//        SearchRequest errorRequest = SearchRequest.of(s -> s
//                .index(TEST_INDEX)
//                .size(0)
//                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("ERROR"))))
//        );
//
//        SearchRequest warnRequest = SearchRequest.of(s -> s
//                .index(TEST_INDEX)
//                .size(0)
//                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("WARN"))))
//        );
//
//        SearchRequest infoRequest = SearchRequest.of(s -> s
//                .index(TEST_INDEX)
//                .size(0)
//                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("INFO"))))
//        );
//
//        long errorCount = openSearchClient.search(errorRequest, Void.class).hits().total().value();
//        long warnCount = openSearchClient.search(warnRequest, Void.class).hits().total().value();
//        long infoCount = openSearchClient.search(infoRequest, Void.class).hits().total().value();
//
//        System.out.println("ERROR: " + errorCount);
//        System.out.println("WARN: " + warnCount);
//        System.out.println("INFO: " + infoCount);
//        System.out.println("총합: " + (errorCount + warnCount + infoCount));
//
//        assertThat(errorCount + warnCount + infoCount).isGreaterThan(0);
//    }
//
//    @Test
//    @DisplayName("3단계: LogMetrics 집계 및 저장 테스트")
//    @Transactional
//    void step3_testAggregateProjectMetricsIncremental_LogMetrics() throws Exception {
//        System.out.println("\n=== 3단계: LogMetrics 집계 테스트 ===");
//
//        // OpenSearch 데이터 확인
//        CountResponse countResponse = openSearchClient.count(
//                CountRequest.of(c -> c.index(TEST_INDEX))
//        );
//        long expectedTotal = countResponse.count();
//        System.out.println("OpenSearch 총 문서 수: " + expectedTotal);
//
//        // 집계 실행
//        LocalDateTime from = LocalDateTime.now().minusDays(30);
//        LocalDateTime to = LocalDateTime.now();
//
//        System.out.println("집계 기간: " + from + " ~ " + to);
//        System.out.println("집계 시작...");
//
//        transactionalService.aggregateProjectMetricsIncremental(testProject, from, to, null);
//
//        System.out.println("집계 완료!");
//
//        // 결과 확인
//        LogMetrics result = logMetricsRepository
//                .findTopByProjectIdOrderByAggregatedAtDesc(testProject.getId())
//                .orElseThrow(() -> new RuntimeException("❌ LogMetrics 저장 실패!"));
//
//        System.out.println("\n=== LogMetrics 저장 결과 ===");
//        System.out.println("ID: " + result.getId());
//        System.out.println("총 로그 수: " + result.getTotalLogs());
//        System.out.println("에러 로그: " + result.getErrorLogs());
//        System.out.println("경고 로그: " + result.getWarnLogs());
//        System.out.println("정보 로그: " + result.getInfoLogs());
//        System.out.println("평균 응답시간: " + result.getAvgResponseTime() + "ms");
//        System.out.println("집계 시각: " + result.getAggregatedAt());
//
//        // 검증
//        assertThat(result.getTotalLogs()).isGreaterThan(0);
//        assertThat(result.getErrorLogs()).isGreaterThanOrEqualTo(0);
//        assertThat(result.getWarnLogs()).isGreaterThanOrEqualTo(0);
//        assertThat(result.getInfoLogs()).isGreaterThanOrEqualTo(0);
//
//        // OpenSearch 카운트와 비교
//        System.out.println("\n=== 정확성 검증 ===");
//        System.out.println("OpenSearch 문서 수: " + expectedTotal);
//        System.out.println("집계된 로그 수: " + result.getTotalLogs());
//
//        if (result.getTotalLogs() > 0) {
//            System.out.println("✅ LogMetrics 저장 성공!");
//        } else {
//            System.out.println("⚠️ 로그 수가 0입니다. 집계 로직 확인 필요");
//        }
//    }
//
//    @Test
//    @DisplayName("4단계: HeatmapMetrics 집계 및 저장 테스트")
//    @Transactional
//    void step4_testAggregateProjectMetricsIncremental_HeatmapMetrics() throws Exception {
//        System.out.println("\n=== 4단계: HeatmapMetrics 집계 테스트 ===");
//
//        LocalDateTime from = LocalDateTime.now().minusDays(7);
//        LocalDateTime to = LocalDateTime.now();
//
//        System.out.println("집계 기간: " + from + " ~ " + to);
//
//        // 직접 OpenSearch 쿼리 테스트
//        String indexPattern = getProjectIndexPattern(testProject.getProjectUuid());
//        SearchRequest testRequest = SearchRequest.of(s -> s
//                .index(indexPattern)
//                .size(0)
//                .query(q -> q
//                        .range(r -> r
//                                .field("timestamp")
//                                .gte(JsonData.of(from.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
//                                .lt(JsonData.of(to.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
//                        )
//                )
//                .aggregations("by_hour", a -> a
//                        .dateHistogram(dh -> dh
//                                .field("timestamp")
//                                .fixedInterval(fi -> fi.time("1h"))
//                                .timeZone("Asia/Seoul")
//                        )
//                        .aggregations("by_level", agg -> agg
//                                .terms(t -> t.field("log_level"))
//                        )
//                )
//        );
//
//        SearchResponse<Void> testResponse = openSearchClient.search(testRequest, Void.class);
//
//        System.out.println("\n=== OpenSearch 응답 확인 ===");
//        Aggregate byHour = testResponse.aggregations().get("by_hour");
//        if (byHour != null && byHour.dateHistogram() != null) {
//            System.out.println("Bucket 개수: " + byHour.dateHistogram().buckets().array().size());
//
//            // 첫 몇 개만 출력
//            byHour.dateHistogram().buckets().array().stream()
//                    .limit(5)
//                    .forEach(bucket -> {
//                        System.out.println("  - key: " + bucket.key() +
//                                ", keyAsString: " + bucket.keyAsString() +
//                                ", docCount: " + bucket.docCount());
//                    });
//        } else {
//            System.out.println("⚠️ by_hour aggregation이 null입니다!");
//        }
//
//        System.out.println("\n집계 시작...");
//        transactionalService.aggregateProjectMetricsIncremental(testProject, from, to, null);
//
//        System.out.println("집계 완료!");
//
//        List<HeatmapMetrics> results = heatmapMetricsRepository.findByProjectIdAndDateBetween(
//                testProject.getId(),
//                from.toLocalDate(),
//                to.toLocalDate()
//        );
//
//        System.out.println("\n=== HeatmapMetrics 저장 결과 ===");
//        System.out.println("총 셀 개수: " + results.size());
//
//        if (!results.isEmpty()) {
//            System.out.println("\n샘플 데이터 (최대 10개):");
//            results.stream().limit(10).forEach(h -> {
//                System.out.println(String.format("  - %s %02d시: total=%d, error=%d, warn=%d, info=%d",
//                        h.getDate(), h.getHour(), h.getTotalCount(),
//                        h.getErrorCount(), h.getWarnCount(), h.getInfoCount()));
//            });
//        }
//
//        assertThat(results).isNotEmpty();
//    }
//
//    private String getProjectIndexPattern(String projectUuid) {
//        return projectUuid.replace("-", "_") + "_*";
//    }
//
//}
