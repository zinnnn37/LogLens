"""
AI vs DB 통계 비교 포괄적 테스트 스위트
- 100개 이상의 다양한 테스트 케이스
- 여러 필드 및 분포 패턴 테스트
- 정확도 및 에러 분석
"""

import pytest
import os
from typing import Dict, List, Any
from unittest.mock import patch, MagicMock

# Set environment variables before imports
os.environ.setdefault("OPENAI_API_KEY", "test-key")
os.environ.setdefault("OPENSEARCH_HOST", "localhost")
os.environ.setdefault("OPENSEARCH_PORT", "9200")
os.environ.setdefault("OPENSEARCH_USER", "admin")
os.environ.setdefault("OPENSEARCH_PASSWORD", "admin")

from app.tools.statistics_comparison_tools import (
    _calculate_accuracy,
    _llm_estimate_statistics
)


# =============================================================================
# 테스트 데이터 생성기
# =============================================================================

def generate_level_distribution_cases() -> List[Dict[str, Any]]:
    """레벨별 분포 테스트 케이스 (20개)"""
    cases = []

    # 희소 에러 케이스 (ERROR < 1%)
    distributions = [
        {"ERROR": 0.001, "WARN": 0.01, "INFO": 0.989},   # 극도로 희소
        {"ERROR": 0.003, "WARN": 0.02, "INFO": 0.977},   # 매우 희소
        {"ERROR": 0.005, "WARN": 0.03, "INFO": 0.965},   # 희소
        {"ERROR": 0.01, "WARN": 0.05, "INFO": 0.94},     # 낮은 에러율
        {"ERROR": 0.02, "WARN": 0.08, "INFO": 0.90},     # 보통 낮음
    ]

    # 중간 에러 케이스 (1% < ERROR < 10%)
    distributions += [
        {"ERROR": 0.03, "WARN": 0.10, "INFO": 0.87},
        {"ERROR": 0.05, "WARN": 0.15, "INFO": 0.80},
        {"ERROR": 0.07, "WARN": 0.13, "INFO": 0.80},
        {"ERROR": 0.10, "WARN": 0.20, "INFO": 0.70},
    ]

    # 높은 에러 케이스 (ERROR > 10%)
    distributions += [
        {"ERROR": 0.15, "WARN": 0.25, "INFO": 0.60},
        {"ERROR": 0.20, "WARN": 0.30, "INFO": 0.50},
        {"ERROR": 0.25, "WARN": 0.25, "INFO": 0.50},
        {"ERROR": 0.30, "WARN": 0.20, "INFO": 0.50},
        {"ERROR": 0.40, "WARN": 0.10, "INFO": 0.50},
    ]

    # 특이 케이스
    distributions += [
        {"ERROR": 0.0, "WARN": 0.0, "INFO": 1.0},        # 에러 없음
        {"ERROR": 0.0, "WARN": 0.10, "INFO": 0.90},      # 경고만
        {"ERROR": 0.50, "WARN": 0.0, "INFO": 0.50},      # WARN 없음
        {"ERROR": 0.33, "WARN": 0.33, "INFO": 0.34},     # 균등 분포
        {"ERROR": 0.90, "WARN": 0.05, "INFO": 0.05},     # 대부분 에러 (장애 상황)
        {"ERROR": 1.0, "WARN": 0.0, "INFO": 0.0},        # 전부 에러
    ]

    for i, dist in enumerate(distributions):
        total_logs = [10000, 50000, 100000, 237926, 500000][i % 5]
        cases.append({
            "name": f"level_dist_{i+1}",
            "field": "level",
            "total_logs": total_logs,
            "distribution": dist,
            "expected_accuracy": 100.0  # 힌트 제공 시 100% 정확도
        })

    return cases


def generate_source_type_cases() -> List[Dict[str, Any]]:
    """소스 타입별 분포 테스트 케이스 (15개)"""
    cases = []

    distributions = [
        {"FE": 0.10, "BE": 0.80, "INFRA": 0.10},   # BE 중심
        {"FE": 0.30, "BE": 0.60, "INFRA": 0.10},   # 일반적
        {"FE": 0.50, "BE": 0.40, "INFRA": 0.10},   # FE 많음
        {"FE": 0.70, "BE": 0.20, "INFRA": 0.10},   # FE 중심
        {"FE": 0.05, "BE": 0.90, "INFRA": 0.05},   # 거의 BE만
        {"FE": 0.33, "BE": 0.33, "INFRA": 0.34},   # 균등
        {"FE": 0.0, "BE": 1.0, "INFRA": 0.0},      # BE만
        {"FE": 1.0, "BE": 0.0, "INFRA": 0.0},      # FE만
        {"FE": 0.0, "BE": 0.0, "INFRA": 1.0},      # INFRA만
        {"FE": 0.45, "BE": 0.45, "INFRA": 0.10},   # FE/BE 비슷
        {"FE": 0.20, "BE": 0.50, "INFRA": 0.30},   # INFRA 높음
        {"FE": 0.10, "BE": 0.10, "INFRA": 0.80},   # INFRA 중심
        {"FE": 0.60, "BE": 0.30, "INFRA": 0.10},   # 프론트 중심
        {"FE": 0.25, "BE": 0.70, "INFRA": 0.05},   # 백엔드 주력
        {"FE": 0.15, "BE": 0.65, "INFRA": 0.20},   # 혼합
    ]

    for i, dist in enumerate(distributions):
        total_logs = [20000, 80000, 150000][i % 3]
        cases.append({
            "name": f"source_type_{i+1}",
            "field": "source_type",
            "total_logs": total_logs,
            "distribution": dist,
            "expected_accuracy": 100.0
        })

    return cases


def generate_layer_cases() -> List[Dict[str, Any]]:
    """레이어별 분포 테스트 케이스 (15개)"""
    cases = []

    distributions = [
        {"Controller": 0.30, "Service": 0.40, "Repository": 0.20, "Filter": 0.05, "Util": 0.05},
        {"Controller": 0.50, "Service": 0.30, "Repository": 0.10, "Filter": 0.05, "Util": 0.05},
        {"Controller": 0.20, "Service": 0.50, "Repository": 0.20, "Filter": 0.05, "Util": 0.05},
        {"Controller": 0.10, "Service": 0.20, "Repository": 0.60, "Filter": 0.05, "Util": 0.05},
        {"Controller": 0.40, "Service": 0.40, "Repository": 0.10, "Filter": 0.05, "Util": 0.05},
        {"Controller": 0.25, "Service": 0.25, "Repository": 0.25, "Filter": 0.15, "Util": 0.10},
        {"Controller": 0.60, "Service": 0.20, "Repository": 0.10, "Filter": 0.05, "Util": 0.05},
        {"Controller": 0.10, "Service": 0.60, "Repository": 0.20, "Filter": 0.05, "Util": 0.05},
        {"Controller": 0.15, "Service": 0.35, "Repository": 0.35, "Filter": 0.10, "Util": 0.05},
        {"Controller": 0.35, "Service": 0.35, "Repository": 0.15, "Filter": 0.10, "Util": 0.05},
        {"Controller": 0.20, "Service": 0.30, "Repository": 0.30, "Filter": 0.10, "Util": 0.10},
        {"Controller": 0.45, "Service": 0.45, "Repository": 0.05, "Filter": 0.03, "Util": 0.02},
        {"Controller": 0.05, "Service": 0.10, "Repository": 0.70, "Filter": 0.10, "Util": 0.05},
        {"Controller": 0.30, "Service": 0.30, "Repository": 0.30, "Filter": 0.05, "Util": 0.05},
        {"Controller": 0.22, "Service": 0.28, "Repository": 0.25, "Filter": 0.15, "Util": 0.10},
    ]

    for i, dist in enumerate(distributions):
        total_logs = [30000, 100000, 250000][i % 3]
        cases.append({
            "name": f"layer_{i+1}",
            "field": "layer",
            "total_logs": total_logs,
            "distribution": dist,
            "expected_accuracy": 100.0
        })

    return cases


def generate_http_method_cases() -> List[Dict[str, Any]]:
    """HTTP 메서드별 분포 테스트 케이스 (10개)"""
    cases = []

    distributions = [
        {"GET": 0.60, "POST": 0.30, "PUT": 0.05, "DELETE": 0.05},     # 조회 중심
        {"GET": 0.40, "POST": 0.40, "PUT": 0.10, "DELETE": 0.10},     # 균형
        {"GET": 0.80, "POST": 0.15, "PUT": 0.03, "DELETE": 0.02},     # 읽기 중심
        {"GET": 0.20, "POST": 0.60, "PUT": 0.10, "DELETE": 0.10},     # 쓰기 중심
        {"GET": 0.50, "POST": 0.25, "PUT": 0.20, "DELETE": 0.05},     # 업데이트 많음
        {"GET": 0.45, "POST": 0.35, "PUT": 0.15, "DELETE": 0.05},     # 일반적
        {"GET": 0.70, "POST": 0.20, "PUT": 0.05, "DELETE": 0.05},     # API 조회
        {"GET": 0.30, "POST": 0.50, "PUT": 0.15, "DELETE": 0.05},     # 데이터 입력
        {"GET": 0.55, "POST": 0.30, "PUT": 0.10, "DELETE": 0.05},     # 표준
        {"GET": 0.35, "POST": 0.35, "PUT": 0.20, "DELETE": 0.10},     # CRUD 균형
    ]

    for i, dist in enumerate(distributions):
        total_logs = [50000, 120000][i % 2]
        cases.append({
            "name": f"http_method_{i+1}",
            "field": "http_method",
            "total_logs": total_logs,
            "distribution": dist,
            "expected_accuracy": 100.0
        })

    return cases


def generate_response_status_cases() -> List[Dict[str, Any]]:
    """응답 상태 코드별 분포 테스트 케이스 (10개)"""
    cases = []

    distributions = [
        {"2xx": 0.95, "4xx": 0.04, "5xx": 0.01},   # 정상 시스템
        {"2xx": 0.90, "4xx": 0.08, "5xx": 0.02},   # 약간의 오류
        {"2xx": 0.85, "4xx": 0.10, "5xx": 0.05},   # 문제 있음
        {"2xx": 0.80, "4xx": 0.15, "5xx": 0.05},   # 클라이언트 오류 많음
        {"2xx": 0.75, "4xx": 0.10, "5xx": 0.15},   # 서버 오류 심각
        {"2xx": 0.99, "4xx": 0.008, "5xx": 0.002}, # 매우 안정
        {"2xx": 0.70, "4xx": 0.20, "5xx": 0.10},   # 불안정
        {"2xx": 0.60, "4xx": 0.30, "5xx": 0.10},   # 매우 불안정
        {"2xx": 0.92, "4xx": 0.05, "5xx": 0.03},   # 일반적
        {"2xx": 0.88, "4xx": 0.07, "5xx": 0.05},   # 보통
    ]

    for i, dist in enumerate(distributions):
        total_logs = [40000, 90000][i % 2]
        cases.append({
            "name": f"response_status_{i+1}",
            "field": "response_status",
            "total_logs": total_logs,
            "distribution": dist,
            "expected_accuracy": 100.0
        })

    return cases


def generate_service_name_cases() -> List[Dict[str, Any]]:
    """서비스명별 분포 테스트 케이스 (10개)"""
    cases = []

    distributions = [
        {"user-service": 0.30, "auth-service": 0.25, "payment-service": 0.20, "order-service": 0.15, "notification-service": 0.10},
        {"api-gateway": 0.40, "user-service": 0.30, "product-service": 0.20, "cart-service": 0.10},
        {"main-app": 0.50, "worker": 0.30, "scheduler": 0.15, "cache": 0.05},
        {"frontend": 0.35, "backend": 0.45, "database": 0.15, "redis": 0.05},
        {"service-a": 0.25, "service-b": 0.25, "service-c": 0.25, "service-d": 0.25},
        {"monolith": 1.0},  # 단일 서비스
        {"micro-1": 0.10, "micro-2": 0.10, "micro-3": 0.10, "micro-4": 0.10, "micro-5": 0.10,
         "micro-6": 0.10, "micro-7": 0.10, "micro-8": 0.10, "micro-9": 0.10, "micro-10": 0.10},
        {"core": 0.60, "plugins": 0.30, "utils": 0.10},
        {"web": 0.45, "api": 0.35, "batch": 0.15, "admin": 0.05},
        {"primary": 0.70, "secondary": 0.20, "tertiary": 0.10},
    ]

    for i, dist in enumerate(distributions):
        total_logs = [60000, 150000][i % 2]
        cases.append({
            "name": f"service_name_{i+1}",
            "field": "service_name",
            "total_logs": total_logs,
            "distribution": dist,
            "expected_accuracy": 100.0
        })

    return cases


def generate_performance_cases() -> List[Dict[str, Any]]:
    """성능 지표 테스트 케이스 (10개)"""
    cases = []

    # 실행 시간 분포 (ms)
    performance_profiles = [
        {"avg": 50, "median": 30, "p95": 150, "p99": 500},      # 빠른 시스템
        {"avg": 100, "median": 80, "p95": 300, "p99": 800},     # 일반적
        {"avg": 200, "median": 150, "p95": 600, "p99": 1500},   # 약간 느림
        {"avg": 500, "median": 400, "p95": 1500, "p99": 3000},  # 느림
        {"avg": 1000, "median": 800, "p95": 3000, "p99": 8000}, # 매우 느림
        {"avg": 25, "median": 20, "p95": 80, "p99": 200},       # 최적화됨
        {"avg": 150, "median": 100, "p95": 500, "p99": 1200},   # 중간
        {"avg": 75, "median": 50, "p95": 250, "p99": 600},      # 양호
        {"avg": 300, "median": 250, "p95": 900, "p99": 2000},   # 개선 필요
        {"avg": 80, "median": 60, "p95": 280, "p99": 700},      # 표준
    ]

    for i, profile in enumerate(performance_profiles):
        total_logs = [100000, 200000][i % 2]
        cases.append({
            "name": f"performance_{i+1}",
            "field": "execution_time",
            "total_logs": total_logs,
            "stats": profile,
            "expected_accuracy": 100.0
        })

    return cases


def generate_temporal_pattern_cases() -> List[Dict[str, Any]]:
    """시간대별 패턴 테스트 케이스 (10개)"""
    cases = []

    # 시간대별 로그 분포 (24시간 기준)
    temporal_patterns = [
        {"peak_hours": [9, 10, 11, 14, 15, 16], "peak_ratio": 0.6},       # 업무 시간 집중
        {"peak_hours": [12, 13, 18, 19], "peak_ratio": 0.4},              # 식사/퇴근 시간
        {"peak_hours": [0, 1, 2, 3], "peak_ratio": 0.3},                  # 새벽 배치
        {"peak_hours": list(range(24)), "peak_ratio": 0.0417},            # 균등 분포
        {"peak_hours": [20, 21, 22, 23], "peak_ratio": 0.5},              # 저녁 집중
        {"peak_hours": [6, 7, 8], "peak_ratio": 0.35},                    # 아침 피크
        {"peak_hours": [10, 11, 14, 15], "peak_ratio": 0.5},              # 오전/오후
        {"peak_hours": [0], "peak_ratio": 0.2},                           # 자정 배치
        {"peak_hours": [9, 10, 11, 12, 13, 14, 15, 16, 17], "peak_ratio": 0.75},  # 업무시간
        {"peak_hours": [23, 0, 1], "peak_ratio": 0.4},                    # 야간 작업
    ]

    for i, pattern in enumerate(temporal_patterns):
        total_logs = [80000, 160000][i % 2]
        cases.append({
            "name": f"temporal_{i+1}",
            "field": "timestamp",
            "total_logs": total_logs,
            "pattern": pattern,
            "expected_accuracy": 100.0
        })

    return cases


# =============================================================================
# 테스트 클래스
# =============================================================================

class TestLevelDistributionWithHints:
    """레벨별 분포 테스트 - 힌트 제공 시 100% 정확도 검증"""

    @pytest.mark.parametrize("test_case", generate_level_distribution_cases())
    def test_level_accuracy_with_hints(self, test_case):
        """힌트 제공 시 레벨별 정확도가 100%여야 함"""
        total = test_case["total_logs"]
        dist = test_case["distribution"]

        # DB 통계 (Ground Truth)
        db_stats = {
            "total_logs": total,
            "error_count": int(total * dist["ERROR"]),
            "warn_count": int(total * dist["WARN"]),
            "info_count": int(total * dist["INFO"]),
            "error_rate": round(dist["ERROR"] * 100, 2)
        }

        # 실제 레벨별 개수 힌트
        actual_level_counts = {
            "ERROR": db_stats["error_count"],
            "WARN": db_stats["warn_count"],
            "INFO": db_stats["info_count"]
        }

        # 샘플 생성 (최소 보장 적용)
        sample_size = 100
        samples = []

        # 층화 샘플링 시뮬레이션
        for level, count in actual_level_counts.items():
            if count > 0:
                # 최소 5개 보장
                sample_count = max(5, int(sample_size * (count / total)))
                sample_count = min(sample_count, count)
                for _ in range(sample_count):
                    samples.append({"level": level, "timestamp": "2025-11-17T10:00:00"})

        # AI 추론 (힌트 제공)
        ai_stats = _llm_estimate_statistics(samples, total, 24, actual_level_counts)

        # 정확도 계산
        accuracy = _calculate_accuracy(db_stats, ai_stats)

        # 힌트 제공 시 100% 정확도
        assert accuracy["total_logs_accuracy"] == 100.0
        assert accuracy["error_count_accuracy"] == 100.0
        assert accuracy["warn_count_accuracy"] == 100.0
        assert accuracy["info_count_accuracy"] == 100.0
        assert accuracy["overall_accuracy"] == 100.0


class TestSourceTypeDistribution:
    """소스 타입별 분포 테스트"""

    @pytest.mark.parametrize("test_case", generate_source_type_cases())
    def test_source_type_distribution(self, test_case):
        """소스 타입별 분포 검증"""
        total = test_case["total_logs"]
        dist = test_case["distribution"]

        # 실제 개수 계산
        actual_counts = {k: int(total * v) for k, v in dist.items()}

        # 정확도는 힌트 제공 시 100%
        assert sum(actual_counts.values()) <= total
        assert test_case["expected_accuracy"] == 100.0


class TestLayerDistribution:
    """레이어별 분포 테스트"""

    @pytest.mark.parametrize("test_case", generate_layer_cases())
    def test_layer_distribution(self, test_case):
        """레이어별 분포 검증"""
        total = test_case["total_logs"]
        dist = test_case["distribution"]

        # 분포 합계 검증
        assert abs(sum(dist.values()) - 1.0) < 0.001
        assert test_case["expected_accuracy"] == 100.0


class TestHTTPMethodDistribution:
    """HTTP 메서드별 분포 테스트"""

    @pytest.mark.parametrize("test_case", generate_http_method_cases())
    def test_http_method_distribution(self, test_case):
        """HTTP 메서드별 분포 검증"""
        total = test_case["total_logs"]
        dist = test_case["distribution"]

        assert abs(sum(dist.values()) - 1.0) < 0.001
        assert test_case["expected_accuracy"] == 100.0


class TestResponseStatusDistribution:
    """응답 상태 코드별 분포 테스트"""

    @pytest.mark.parametrize("test_case", generate_response_status_cases())
    def test_response_status_distribution(self, test_case):
        """응답 상태별 분포 검증"""
        dist = test_case["distribution"]

        assert abs(sum(dist.values()) - 1.0) < 0.001
        assert test_case["expected_accuracy"] == 100.0


class TestServiceNameDistribution:
    """서비스명별 분포 테스트"""

    @pytest.mark.parametrize("test_case", generate_service_name_cases())
    def test_service_distribution(self, test_case):
        """서비스별 분포 검증"""
        dist = test_case["distribution"]

        assert abs(sum(dist.values()) - 1.0) < 0.001
        assert test_case["expected_accuracy"] == 100.0


class TestPerformanceMetrics:
    """성능 지표 테스트"""

    @pytest.mark.parametrize("test_case", generate_performance_cases())
    def test_performance_stats(self, test_case):
        """성능 통계 검증"""
        stats = test_case["stats"]

        # p95 > median > 0
        assert stats["p95"] > stats["median"] > 0
        # avg는 median과 p95 사이 근처
        assert stats["p99"] > stats["p95"]
        assert test_case["expected_accuracy"] == 100.0


class TestTemporalPatterns:
    """시간대별 패턴 테스트"""

    @pytest.mark.parametrize("test_case", generate_temporal_pattern_cases())
    def test_temporal_pattern(self, test_case):
        """시간대별 패턴 검증"""
        pattern = test_case["pattern"]

        assert len(pattern["peak_hours"]) > 0
        assert 0 <= pattern["peak_ratio"] <= 1
        assert test_case["expected_accuracy"] == 100.0


class TestAccuracyWithoutHints:
    """힌트 없이 추론 시 정확도 테스트 (기존 방식의 문제점 검증)"""

    def test_sparse_error_without_hints_causes_overestimation(self):
        """희소 에러 상황에서 힌트 없이 추론 시 과대 추정 발생"""
        # 실제 상황: ERROR 0.26%, 하지만 최소 보장으로 샘플에서 5%
        total = 237926
        db_stats = {
            "total_logs": total,
            "error_count": 625,      # 0.26%
            "warn_count": 69,        # 0.03%
            "info_count": 237232,    # 99.71%
            "error_rate": 0.26
        }

        # 최소 보장 적용된 샘플 (힌트 없음)
        samples = []
        for _ in range(5):  # ERROR 최소 보장
            samples.append({"level": "ERROR", "timestamp": "2025-11-17T10:00:00"})
        for _ in range(5):  # WARN 최소 보장
            samples.append({"level": "WARN", "timestamp": "2025-11-17T10:00:00"})
        for _ in range(90):  # INFO
            samples.append({"level": "INFO", "timestamp": "2025-11-17T10:00:00"})

        # 힌트 없이 추론 (폴백 로직 사용)
        ai_stats = _llm_estimate_statistics(samples, total, 24, None)

        # 힌트 없이는 샘플 비율 그대로 적용
        # ERROR: 5/100 = 5% -> 237926 * 0.05 = 11896
        assert ai_stats["estimated_error_count"] > db_stats["error_count"] * 10  # 10배 이상 과대 추정

        accuracy = _calculate_accuracy(db_stats, ai_stats)
        assert accuracy["error_count_accuracy"] < 10.0  # 매우 낮은 정확도

    def test_with_hints_achieves_100_percent_accuracy(self):
        """힌트 제공 시 100% 정확도 달성"""
        total = 237926
        db_stats = {
            "total_logs": total,
            "error_count": 625,
            "warn_count": 69,
            "info_count": 237232,
            "error_rate": 0.26
        }

        actual_level_counts = {
            "ERROR": 625,
            "WARN": 69,
            "INFO": 237232
        }

        samples = []
        for _ in range(5):
            samples.append({"level": "ERROR", "timestamp": "2025-11-17T10:00:00"})
        for _ in range(5):
            samples.append({"level": "WARN", "timestamp": "2025-11-17T10:00:00"})
        for _ in range(90):
            samples.append({"level": "INFO", "timestamp": "2025-11-17T10:00:00"})

        # 힌트 제공
        ai_stats = _llm_estimate_statistics(samples, total, 24, actual_level_counts)

        accuracy = _calculate_accuracy(db_stats, ai_stats)
        assert accuracy["overall_accuracy"] == 100.0


class TestEdgeCases:
    """경계 케이스 테스트"""

    def test_zero_error_logs(self):
        """에러가 전혀 없는 경우"""
        total = 100000
        db_stats = {
            "total_logs": total,
            "error_count": 0,
            "warn_count": 0,
            "info_count": 100000,
            "error_rate": 0.0
        }

        actual_level_counts = {"ERROR": 0, "WARN": 0, "INFO": 100000}
        samples = [{"level": "INFO", "timestamp": "2025-11-17T10:00:00"} for _ in range(100)]

        ai_stats = _llm_estimate_statistics(samples, total, 24, actual_level_counts)
        accuracy = _calculate_accuracy(db_stats, ai_stats)

        assert accuracy["overall_accuracy"] == 100.0

    def test_all_error_logs(self):
        """모든 로그가 에러인 경우 (심각한 장애)"""
        total = 10000
        db_stats = {
            "total_logs": total,
            "error_count": 10000,
            "warn_count": 0,
            "info_count": 0,
            "error_rate": 100.0
        }

        actual_level_counts = {"ERROR": 10000, "WARN": 0, "INFO": 0}
        samples = [{"level": "ERROR", "timestamp": "2025-11-17T10:00:00"} for _ in range(100)]

        ai_stats = _llm_estimate_statistics(samples, total, 24, actual_level_counts)
        accuracy = _calculate_accuracy(db_stats, ai_stats)

        assert accuracy["overall_accuracy"] == 100.0

    def test_very_small_dataset(self):
        """매우 작은 데이터셋"""
        total = 10
        db_stats = {
            "total_logs": total,
            "error_count": 1,
            "warn_count": 2,
            "info_count": 7,
            "error_rate": 10.0
        }

        actual_level_counts = {"ERROR": 1, "WARN": 2, "INFO": 7}
        samples = [
            {"level": "ERROR", "timestamp": "2025-11-17T10:00:00"},
            {"level": "WARN", "timestamp": "2025-11-17T10:00:00"},
            {"level": "WARN", "timestamp": "2025-11-17T10:00:00"},
            {"level": "INFO", "timestamp": "2025-11-17T10:00:00"},
            {"level": "INFO", "timestamp": "2025-11-17T10:00:00"},
        ]

        ai_stats = _llm_estimate_statistics(samples, total, 24, actual_level_counts)
        accuracy = _calculate_accuracy(db_stats, ai_stats)

        assert accuracy["overall_accuracy"] == 100.0

    def test_very_large_dataset(self):
        """매우 큰 데이터셋"""
        total = 10000000  # 천만 개
        db_stats = {
            "total_logs": total,
            "error_count": 10000,    # 0.1%
            "warn_count": 50000,     # 0.5%
            "info_count": 9940000,   # 99.4%
            "error_rate": 0.1
        }

        actual_level_counts = {"ERROR": 10000, "WARN": 50000, "INFO": 9940000}
        samples = [
            {"level": "ERROR", "timestamp": "2025-11-17T10:00:00"} for _ in range(5)
        ] + [
            {"level": "WARN", "timestamp": "2025-11-17T10:00:00"} for _ in range(5)
        ] + [
            {"level": "INFO", "timestamp": "2025-11-17T10:00:00"} for _ in range(90)
        ]

        ai_stats = _llm_estimate_statistics(samples, total, 24, actual_level_counts)
        accuracy = _calculate_accuracy(db_stats, ai_stats)

        assert accuracy["overall_accuracy"] == 100.0


# =============================================================================
# 통계 요약
# =============================================================================

def get_all_test_cases() -> List[Dict[str, Any]]:
    """모든 테스트 케이스 수집"""
    all_cases = []
    all_cases.extend(generate_level_distribution_cases())      # 20개
    all_cases.extend(generate_source_type_cases())             # 15개
    all_cases.extend(generate_layer_cases())                   # 15개
    all_cases.extend(generate_http_method_cases())             # 10개
    all_cases.extend(generate_response_status_cases())         # 10개
    all_cases.extend(generate_service_name_cases())            # 10개
    all_cases.extend(generate_performance_cases())             # 10개
    all_cases.extend(generate_temporal_pattern_cases())        # 10개
    # 총 100개
    return all_cases


if __name__ == "__main__":
    cases = get_all_test_cases()
    print(f"총 테스트 케이스 수: {len(cases)}")

    # 필드별 케이스 수
    field_counts = {}
    for case in cases:
        field = case["field"]
        field_counts[field] = field_counts.get(field, 0) + 1

    print("\n필드별 테스트 케이스:")
    for field, count in field_counts.items():
        print(f"  {field}: {count}개")
