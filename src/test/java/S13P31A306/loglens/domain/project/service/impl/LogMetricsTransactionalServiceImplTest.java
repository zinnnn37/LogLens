package S13P31A306.loglens.domain.project.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.CountRequest;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LogMetricsTransactionalServiceImplTest {

    private static final String INDEX_1 = "a07b75f7_de74_3fe5_8039_185d5c59e83f_2025_11";
    private static final String INDEX_2 = "9911573f_8a1d_3b96_98b4_5a0def93513b_2025_11_v2";

    @Autowired
    private OpenSearchClient openSearchClient;

    @Test
    @DisplayName("OpenSearch 연결 테스트 - 인덱스 1")
    void testOpenSearchConnection_Index1() throws Exception {
        System.out.println("\n=== OpenSearch 연결 테스트 - 인덱스 1 ===");

        CountRequest countRequest = CountRequest.of(c -> c.index(INDEX_1));
        CountResponse countResponse = openSearchClient.count(countRequest);

        System.out.println("총 문서 수: " + countResponse.count());

        assertThat(countResponse.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("OpenSearch 연결 테스트 - 인덱스 2")
    void testOpenSearchConnection_Index2() throws Exception {
        System.out.println("\n=== OpenSearch 연결 테스트 - 인덱스 2 ===");

        CountRequest countRequest = CountRequest.of(c -> c.index(INDEX_2));
        CountResponse countResponse = openSearchClient.count(countRequest);

        System.out.println("총 문서 수: " + countResponse.count());

        assertThat(countResponse.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("샘플 데이터 조회 - 인덱스 1")
    void testSampleData_Index1() throws Exception {
        System.out.println("\n=== 샘플 데이터 조회 - 인덱스 1 ===");

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(INDEX_1)
                .size(5)
                .sort(sort -> sort
                        .field(f -> f
                                .field("timestamp")
                                .order(org.opensearch.client.opensearch._types.SortOrder.Desc)
                        )
                )
        );

        SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);

        System.out.println("조회된 문서 수: " + response.hits().hits().size());

        int index = 1;
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            System.out.println("\n--- 문서 " + index++ + " ---");
            System.out.println("log_id: " + source.get("log_id"));
            System.out.println("timestamp: " + source.get("timestamp"));
            System.out.println("log_level: " + source.get("log_level"));
            System.out.println("message: " + source.get("message"));
            System.out.println("service_name: " + source.get("service_name"));
            System.out.println("duration: " + source.get("duration"));
        }

        assertThat(response.hits().hits()).isNotEmpty();
    }

    @Test
    @DisplayName("샘플 데이터 조회 - 인덱스 2")
    void testSampleData_Index2() throws Exception {
        System.out.println("\n=== 샘플 데이터 조회 - 인덱스 2 ===");

        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(INDEX_2)
                .size(5)
                .sort(sort -> sort
                        .field(f -> f
                                .field("timestamp")
                                .order(org.opensearch.client.opensearch._types.SortOrder.Desc)
                        )
                )
        );

        SearchResponse<Map> response = openSearchClient.search(searchRequest, Map.class);

        System.out.println("조회된 문서 수: " + response.hits().hits().size());

        int index = 1;
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            System.out.println("\n--- 문서 " + index++ + " ---");
            System.out.println("log_id: " + source.get("log_id"));
            System.out.println("timestamp: " + source.get("timestamp"));
            System.out.println("log_level: " + source.get("log_level"));
            System.out.println("message: " + source.get("message"));
            System.out.println("service_name: " + source.get("service_name"));
            System.out.println("duration: " + source.get("duration"));
        }

        assertThat(response.hits().hits()).isNotEmpty();
    }

    @Test
    @DisplayName("로그 레벨별 카운트 - 인덱스 1")
    void testLogLevelCount_Index1() throws Exception {
        System.out.println("\n=== 로그 레벨별 카운트 - 인덱스 1 ===");

        SearchRequest errorRequest = SearchRequest.of(s -> s
                .index(INDEX_1)
                .size(0)
                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("ERROR"))))
        );

        SearchRequest warnRequest = SearchRequest.of(s -> s
                .index(INDEX_1)
                .size(0)
                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("WARN"))))
        );

        SearchRequest infoRequest = SearchRequest.of(s -> s
                .index(INDEX_1)
                .size(0)
                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("INFO"))))
        );

        long errorCount = openSearchClient.search(errorRequest, Void.class).hits().total().value();
        long warnCount = openSearchClient.search(warnRequest, Void.class).hits().total().value();
        long infoCount = openSearchClient.search(infoRequest, Void.class).hits().total().value();

        System.out.println("ERROR 로그 수: " + errorCount);
        System.out.println("WARN 로그 수: " + warnCount);
        System.out.println("INFO 로그 수: " + infoCount);
        System.out.println("총합: " + (errorCount + warnCount + infoCount));

        assertThat(errorCount).isGreaterThanOrEqualTo(0);
        assertThat(warnCount).isGreaterThanOrEqualTo(0);
        assertThat(infoCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("로그 레벨별 카운트 - 인덱스 2")
    void testLogLevelCount_Index2() throws Exception {
        System.out.println("\n=== 로그 레벨별 카운트 - 인덱스 2 ===");

        SearchRequest errorRequest = SearchRequest.of(s -> s
                .index(INDEX_2)
                .size(0)
                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("ERROR"))))
        );

        SearchRequest warnRequest = SearchRequest.of(s -> s
                .index(INDEX_2)
                .size(0)
                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("WARN"))))
        );

        SearchRequest infoRequest = SearchRequest.of(s -> s
                .index(INDEX_2)
                .size(0)
                .query(q -> q.term(t -> t.field("log_level").value(FieldValue.of("INFO"))))
        );

        long errorCount = openSearchClient.search(errorRequest, Void.class).hits().total().value();
        long warnCount = openSearchClient.search(warnRequest, Void.class).hits().total().value();
        long infoCount = openSearchClient.search(infoRequest, Void.class).hits().total().value();

        System.out.println("ERROR 로그 수: " + errorCount);
        System.out.println("WARN 로그 수: " + warnCount);
        System.out.println("INFO 로그 수: " + infoCount);
        System.out.println("총합: " + (errorCount + warnCount + infoCount));

        assertThat(errorCount).isGreaterThanOrEqualTo(0);
        assertThat(warnCount).isGreaterThanOrEqualTo(0);
        assertThat(infoCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("duration 필드 집계 테스트")
    void testDurationAggregation() throws Exception {
        System.out.println("\n=== duration 필드 집계 테스트 ===");

        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_1)
                .size(0)
                .aggregations("sum_duration", a -> a
                        .sum(sum -> sum.field("duration"))
                )
                .aggregations("avg_duration", a -> a
                        .avg(avg -> avg.field("duration"))
                )
        );

        SearchResponse<Void> response = openSearchClient.search(request, Void.class);

        double sumDuration = response.aggregations().get("sum_duration").sum().value();
        double avgDuration = response.aggregations().get("avg_duration").avg().value();

        System.out.println("총 duration 합계: " + sumDuration);
        System.out.println("평균 duration: " + avgDuration);

        assertThat(response.aggregations()).isNotNull();
        assertThat(response.aggregations().get("sum_duration")).isNotNull();
        assertThat(response.aggregations().get("avg_duration")).isNotNull();
    }
}
