package a306.dependency_logger_starter.dependency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 데이터베이스 타입 감지 유틸리티
 * 여러 데이터베이스를 동시에 감지 가능
 */
@Slf4j
public class DatabaseDetector {

    private final Environment environment;

    public DatabaseDetector(Environment environment) {
        this.environment = environment;
    }

    /**
     * 모든 데이터베이스 감지
     *
     * @return DB 타입 리스트 (H2, MySQL, PostgreSQL, Oracle, MongoDB 등)
     */
    public List<String> detectAllDatabases() {
        Set<String> databases = new HashSet<>();

        // 1. Primary datasource 감지
        detectPrimaryDatasource(databases);

        // 2. Secondary datasources 감지 (spring.datasource.*.url 패턴)
        detectSecondaryDatasources(databases);

        // 3. NoSQL 감지
        detectNoSqlDatabases(databases);

        List<String> result = new ArrayList<>(databases);

        if (result.isEmpty()) {
            log.warn("⚠️ 데이터베이스를 감지할 수 없습니다.");
            result.add("UNKNOWN");
        } else {
            log.info("📊 감지된 데이터베이스: {}", result);
        }

        return result;
    }

    /**
     * Primary datasource 감지 (spring.datasource.url)
     */
    private void detectPrimaryDatasource(Set<String> databases) {
        String url = environment.getProperty("spring.datasource.url");
        if (url != null) {
            String dbType = detectFromUrl(url);
            if (!"UNKNOWN".equals(dbType)) {
                databases.add(dbType);
                log.debug("✅ Primary DB 감지: {} ({})", dbType, url);
            }
            return;
        }

        // URL이 없으면 driver로 시도
        String driver = environment.getProperty("spring.datasource.driver-class-name");
        if (driver != null) {
            String dbType = detectFromDriver(driver);
            if (!"UNKNOWN".equals(dbType)) {
                databases.add(dbType);
                log.debug("✅ Primary DB 감지: {} ({})", dbType, driver);
            }
        }
    }

    /**
     * Secondary datasources 감지
     * 예: spring.datasource.secondary.url, spring.datasource.readonly.url
     */
    private void detectSecondaryDatasources(Set<String> databases) {
        // 일반적인 secondary datasource 패턴들
        String[] prefixes = {
                "spring.datasource.secondary",
                "spring.datasource.readonly",
                "spring.datasource.slave",
                "spring.datasource.replica"
        };

        for (String prefix : prefixes) {
            String url = environment.getProperty(prefix + ".url");
            if (url != null) {
                String dbType = detectFromUrl(url);
                if (!"UNKNOWN".equals(dbType)) {
                    databases.add(dbType);
                    log.debug("✅ Secondary DB 감지: {} ({})", dbType, url);
                }
            }
        }
    }

    /**
     * NoSQL 데이터베이스 감지
     */
    private void detectNoSqlDatabases(Set<String> databases) {
        // MongoDB
        String mongoUri = environment.getProperty("spring.data.mongodb.uri");
        if (mongoUri != null) {
            databases.add("MongoDB");
            log.debug("✅ NoSQL 감지: MongoDB");
        }

        // Redis
        String redisHost = environment.getProperty("spring.data.redis.host");
        String redisUrl = environment.getProperty("spring.data.redis.url");
        if (redisHost != null || redisUrl != null) {
            databases.add("Redis");
            log.debug("✅ NoSQL 감지: Redis");
        }

        // Elasticsearch
        String elasticsearchUris = environment.getProperty("spring.data.elasticsearch.uris");
        if (elasticsearchUris != null) {
            databases.add("Elasticsearch");
            log.debug("✅ NoSQL 감지: Elasticsearch");
        }
    }

    /**
     * JDBC URL로 DB 타입 감지
     */
    private String detectFromUrl(String url) {
        String lowerUrl = url.toLowerCase();

        if (lowerUrl.contains(":h2:")) {
            return "H2";
        }
        if (lowerUrl.contains(":mysql:")) {
            return "MySQL";
        }
        if (lowerUrl.contains(":mariadb:")) {
            return "MariaDB";
        }
        if (lowerUrl.contains(":postgresql:")) {
            return "PostgreSQL";
        }
        if (lowerUrl.contains(":oracle:")) {
            return "Oracle";
        }
        if (lowerUrl.contains(":sqlserver:")) {
            return "SQLServer";
        }

        return "UNKNOWN";
    }

    /**
     * Driver Class Name으로 DB 타입 감지
     */
    private String detectFromDriver(String driver) {
        String lowerDriver = driver.toLowerCase();

        if (lowerDriver.contains("h2")) {
            return "H2";
        }
        if (lowerDriver.contains("mysql")) {
            return "MySQL";
        }
        if (lowerDriver.contains("mariadb")) {
            return "MariaDB";
        }
        if (lowerDriver.contains("postgresql")) {
            return "PostgreSQL";
        }
        if (lowerDriver.contains("oracle")) {
            return "Oracle";
        }
        if (lowerDriver.contains("sqlserver")) {
            return "SQLServer";
        }

        return "UNKNOWN";
    }
}
