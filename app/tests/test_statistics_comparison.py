"""
AI vs DB 통계 비교 테스트

이 테스트는 LLM이 DB 쿼리를 대체할 수 있는 역량을 검증하는 기능을 테스트합니다.
"""

import pytest
from unittest.mock import patch, MagicMock, AsyncMock
from datetime import datetime
from fastapi.testclient import TestClient

from app.main import app
from app.tools.statistics_comparison_tools import (
    _get_db_statistics,
    _get_log_samples,
    _llm_estimate_statistics,
    _calculate_accuracy
)


@pytest.fixture
def client():
    """FastAPI TestClient 픽스처"""
    return TestClient(app)


@pytest.fixture
def mock_opensearch_stats_response():
    """OpenSearch 통계 쿼리 응답 목업"""
    return {
        "hits": {
            "total": {"value": 15420}
        },
        "aggregations": {
            "by_level": {
                "buckets": [
                    {"key": "INFO", "doc_count": 13873},
                    {"key": "WARN", "doc_count": 1205},
                    {"key": "ERROR", "doc_count": 342}
                ]
            },
            "hourly_distribution": {
                "buckets": [
                    {
                        "key_as_string": "2025-11-14T12:00:00.000+09:00",
                        "doc_count": 892,
                        "by_level": {
                            "buckets": [
                                {"key": "INFO", "doc_count": 750},
                                {"key": "WARN", "doc_count": 100},
                                {"key": "ERROR", "doc_count": 42}
                            ]
                        }
                    },
                    {
                        "key_as_string": "2025-11-14T13:00:00.000+09:00",
                        "doc_count": 650,
                        "by_level": {
                            "buckets": [
                                {"key": "INFO", "doc_count": 550},
                                {"key": "WARN", "doc_count": 70},
                                {"key": "ERROR", "doc_count": 30}
                            ]
                        }
                    }
                ]
            }
        }
    }


@pytest.fixture
def mock_opensearch_samples_response():
    """OpenSearch 샘플 쿼리 응답 목업"""
    return {
        "hits": {
            "hits": [
                {
                    "_source": {
                        "level": "ERROR",
                        "timestamp": "2025-11-14T12:30:00Z",
                        "service_name": "user-service",
                        "message": "NullPointerException occurred"
                    }
                },
                {
                    "_source": {
                        "level": "INFO",
                        "timestamp": "2025-11-14T12:31:00Z",
                        "service_name": "api-gateway",
                        "message": "Request processed successfully"
                    }
                },
                {
                    "_source": {
                        "level": "WARN",
                        "timestamp": "2025-11-14T12:32:00Z",
                        "service_name": "payment-service",
                        "message": "Slow response time detected"
                    }
                }
            ]
        }
    }


@pytest.fixture
def db_statistics():
    """테스트용 DB 통계 데이터"""
    return {
        "total_logs": 15420,
        "error_count": 342,
        "warn_count": 1205,
        "info_count": 13873,
        "error_rate": 2.22,
        "peak_hour": "2025-11-14T12",
        "peak_count": 892,
        "hourly_data": []
    }


@pytest.fixture
def ai_statistics_perfect():
    """완벽히 일치하는 AI 통계"""
    return {
        "estimated_total_logs": 15420,
        "estimated_error_count": 342,
        "estimated_warn_count": 1205,
        "estimated_info_count": 13873,
        "estimated_error_rate": 2.22,
        "confidence_score": 95,
        "reasoning": "샘플 비율을 전체에 적용"
    }


@pytest.fixture
def ai_statistics_with_deviation():
    """약간의 오차가 있는 AI 통계"""
    return {
        "estimated_total_logs": 15380,
        "estimated_error_count": 338,
        "estimated_warn_count": 1198,
        "estimated_info_count": 13844,
        "estimated_error_rate": 2.20,
        "confidence_score": 85,
        "reasoning": "샘플 100개 중 ERROR 2.2% 비율을 전체에 적용"
    }


class TestDBStatistics:
    """DB 통계 조회 테스트"""

    @patch("app.tools.statistics_comparison_tools.opensearch_client")
    def test_get_db_statistics_success(self, mock_client, mock_opensearch_stats_response):
        """DB 통계 조회 성공 테스트"""
        # given
        mock_client.search.return_value = mock_opensearch_stats_response

        # when
        result = _get_db_statistics("test-project-uuid", 24)

        # then
        assert result["total_logs"] == 15420
        assert result["error_count"] == 342
        assert result["warn_count"] == 1205
        assert result["info_count"] == 13873
        assert result["error_rate"] == 2.22
        assert result["peak_hour"] == "2025-11-14T12"
        assert result["peak_count"] == 892

    @patch("app.tools.statistics_comparison_tools.opensearch_client")
    def test_get_db_statistics_empty_logs(self, mock_client):
        """로그가 없을 때 테스트"""
        # given
        mock_client.search.return_value = {
            "hits": {"total": {"value": 0}},
            "aggregations": {
                "by_level": {"buckets": []},
                "hourly_distribution": {"buckets": []}
            }
        }

        # when
        result = _get_db_statistics("test-project-uuid", 24)

        # then
        assert result["total_logs"] == 0
        assert result["error_count"] == 0
        assert result["error_rate"] == 0

    @patch("app.tools.statistics_comparison_tools.opensearch_client")
    def test_get_db_statistics_calculates_error_rate(self, mock_client, mock_opensearch_stats_response):
        """에러율 계산 정확성 테스트"""
        # given
        mock_client.search.return_value = mock_opensearch_stats_response

        # when
        result = _get_db_statistics("test-project-uuid", 24)

        # then
        expected_error_rate = round((342 / 15420) * 100, 2)
        assert result["error_rate"] == expected_error_rate


class TestLogSampling:
    """로그 샘플링 테스트"""

    @patch("app.tools.statistics_comparison_tools.opensearch_client")
    def test_get_log_samples_returns_correct_size(self, mock_client, mock_opensearch_samples_response):
        """샘플 크기 검증 테스트"""
        # given
        mock_client.search.return_value = mock_opensearch_samples_response

        # when
        result = _get_log_samples("test-project-uuid", 24, 100)

        # then
        assert len(result) == 3  # 목업에 3개 데이터
        assert result[0]["level"] == "ERROR"
        assert result[1]["level"] == "INFO"
        assert result[2]["level"] == "WARN"

    @patch("app.tools.statistics_comparison_tools.opensearch_client")
    def test_get_log_samples_empty_result(self, mock_client):
        """샘플이 없을 때 테스트"""
        # given
        mock_client.search.return_value = {"hits": {"hits": []}}

        # when
        result = _get_log_samples("test-project-uuid", 24, 100)

        # then
        assert result == []

    @patch("app.tools.statistics_comparison_tools.opensearch_client")
    def test_get_log_samples_truncates_message(self, mock_client):
        """메시지 길이 제한 테스트"""
        # given
        long_message = "A" * 500
        mock_client.search.return_value = {
            "hits": {
                "hits": [{
                    "_source": {
                        "level": "ERROR",
                        "timestamp": "2025-11-14T12:30:00Z",
                        "service_name": "test",
                        "message": long_message
                    }
                }]
            }
        }

        # when
        result = _get_log_samples("test-project-uuid", 24, 100)

        # then
        assert len(result[0]["message"]) == 200  # 200자로 제한


class TestLLMEstimation:
    """LLM 통계 추론 테스트"""

    @patch("app.tools.statistics_comparison_tools.ChatOpenAI")
    def test_llm_estimate_statistics_success(self, mock_llm_class):
        """LLM 추론 성공 테스트"""
        # given
        mock_llm = MagicMock()
        mock_llm_class.return_value = mock_llm
        mock_llm.invoke.return_value.content = '''
        {
            "estimated_total_logs": 15420,
            "estimated_error_count": 342,
            "estimated_warn_count": 1205,
            "estimated_info_count": 13873,
            "estimated_error_rate": 2.22,
            "confidence_score": 90,
            "reasoning": "샘플 비율 기반 추론"
        }
        '''

        log_samples = [
            {"level": "ERROR", "timestamp": "", "service": "test", "message": "error"},
            {"level": "INFO", "timestamp": "", "service": "test", "message": "info"},
        ]

        # when
        result = _llm_estimate_statistics(log_samples, 15420, 24)

        # then
        assert result["estimated_total_logs"] == 15420
        assert result["estimated_error_count"] == 342
        assert result["confidence_score"] == 90

    @patch("app.tools.statistics_comparison_tools.ChatOpenAI")
    def test_llm_estimate_statistics_fallback_on_error(self, mock_llm_class):
        """LLM 오류 시 폴백 테스트"""
        # given
        mock_llm = MagicMock()
        mock_llm_class.return_value = mock_llm
        mock_llm.invoke.side_effect = Exception("LLM API Error")

        log_samples = [
            {"level": "ERROR", "timestamp": "", "service": "test", "message": "error"},
            {"level": "INFO", "timestamp": "", "service": "test", "message": "info"},
            {"level": "INFO", "timestamp": "", "service": "test", "message": "info"},
            {"level": "INFO", "timestamp": "", "service": "test", "message": "info"},
        ]

        # when
        result = _llm_estimate_statistics(log_samples, 1000, 24)

        # then
        # 폴백: 비율 계산 (ERROR 1/4 = 25%)
        assert result["estimated_error_count"] == 250  # 1000 * 0.25
        assert result["confidence_score"] == 50  # 낮은 신뢰도
        assert "LLM 추론 실패" in result["reasoning"]


class TestAccuracyCalculation:
    """정확도 계산 테스트"""

    def test_calculate_accuracy_perfect_match(self, db_statistics, ai_statistics_perfect):
        """완벽 일치 시 100% 정확도"""
        # when
        result = _calculate_accuracy(db_statistics, ai_statistics_perfect)

        # then
        assert result["total_logs_accuracy"] == 100.0
        assert result["error_count_accuracy"] == 100.0
        assert result["overall_accuracy"] == 100.0

    def test_calculate_accuracy_with_deviation(self, db_statistics, ai_statistics_with_deviation):
        """오차가 있을 때 정확도 계산"""
        # when
        result = _calculate_accuracy(db_statistics, ai_statistics_with_deviation)

        # then
        # total_logs: 15420 vs 15380 = 99.74%
        assert result["total_logs_accuracy"] >= 99.0
        # error_count: 342 vs 338 = 98.83%
        assert result["error_count_accuracy"] >= 98.0
        # 종합 정확도 90% 이상
        assert result["overall_accuracy"] >= 90.0

    def test_calculate_accuracy_zero_values(self):
        """0 값 처리 테스트"""
        # given
        db_stats = {
            "total_logs": 0,
            "error_count": 0,
            "warn_count": 0,
            "info_count": 0,
            "error_rate": 0
        }
        ai_stats = {
            "estimated_total_logs": 0,
            "estimated_error_count": 0,
            "estimated_warn_count": 0,
            "estimated_info_count": 0,
            "estimated_error_rate": 0
        }

        # when
        result = _calculate_accuracy(db_stats, ai_stats)

        # then
        assert result["total_logs_accuracy"] == 100.0  # 둘 다 0이면 100%

    def test_calculate_accuracy_large_deviation(self):
        """큰 오차 시 낮은 정확도"""
        # given
        db_stats = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 200,
            "info_count": 700,
            "error_rate": 10.0
        }
        ai_stats = {
            "estimated_total_logs": 500,  # 50% 오차
            "estimated_error_count": 50,
            "estimated_warn_count": 100,
            "estimated_info_count": 350,
            "estimated_error_rate": 10.0,
            "confidence_score": 30
        }

        # when
        result = _calculate_accuracy(db_stats, ai_stats)

        # then
        assert result["total_logs_accuracy"] == 50.0
        assert result["overall_accuracy"] < 70.0  # 미흡 등급

    def test_calculate_accuracy_weighted_average(self):
        """가중 평균 계산 검증"""
        # given
        db_stats = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 100,
            "info_count": 800,
            "error_rate": 10.0
        }
        ai_stats = {
            "estimated_total_logs": 900,  # 90% 일치
            "estimated_error_count": 95,   # 95% 일치
            "estimated_warn_count": 100,   # 100% 일치
            "estimated_info_count": 705,   # 88.125% 일치
            "estimated_error_rate": 10.0,  # 100% 일치
            "confidence_score": 80
        }

        # when
        result = _calculate_accuracy(db_stats, ai_stats)

        # then
        # 가중치: total 30%, error 30%, error_rate 20%, warn 10%, info 10%
        # 90*0.3 + 95*0.3 + 100*0.2 + 100*0.1 + 88.125*0.1 = 94.3125
        expected = 90 * 0.3 + 95 * 0.3 + 100 * 0.2 + 100 * 0.1 + 88.125 * 0.1
        assert abs(result["overall_accuracy"] - expected) < 1.0


class TestStatisticsComparisonAPI:
    """API 엔드포인트 테스트"""

    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    @patch("app.api.v2_langgraph.statistics._get_log_samples")
    @patch("app.api.v2_langgraph.statistics._llm_estimate_statistics")
    def test_compare_endpoint_success(
        self, mock_llm, mock_samples, mock_db, client, db_statistics, ai_statistics_with_deviation
    ):
        """비교 API 성공 테스트"""
        # given
        mock_db.return_value = db_statistics
        mock_samples.return_value = [{"level": "ERROR"}] * 100
        mock_llm.return_value = ai_statistics_with_deviation

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/compare",
            params={
                "project_uuid": "test-uuid",
                "time_hours": 24,
                "sample_size": 100
            }
        )

        # then
        assert response.status_code == 200
        data = response.json()
        assert "db_statistics" in data
        assert "ai_statistics" in data
        assert "accuracy_metrics" in data
        assert "verdict" in data
        assert data["accuracy_metrics"]["overall_accuracy"] > 90

    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    def test_compare_endpoint_no_logs_404(self, mock_db, client):
        """로그 없을 때 404 응답"""
        # given
        mock_db.return_value = {
            "total_logs": 0,
            "error_count": 0,
            "warn_count": 0,
            "info_count": 0,
            "error_rate": 0,
            "peak_hour": "",
            "peak_count": 0,
            "hourly_data": []
        }

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/compare",
            params={"project_uuid": "empty-project", "time_hours": 24}
        )

        # then
        assert response.status_code == 404

    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    def test_hourly_endpoint_success(self, mock_db, client, db_statistics):
        """시간대별 통계 API 성공 테스트"""
        # given
        db_statistics["hourly_data"] = [
            {"hour": "2025-11-14T12", "total": 892, "error": 42, "warn": 100, "info": 750},
            {"hour": "2025-11-14T13", "total": 650, "error": 30, "warn": 70, "info": 550}
        ]
        mock_db.return_value = db_statistics

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/hourly",
            params={"project_uuid": "test-uuid", "time_hours": 24}
        )

        # then
        assert response.status_code == 200
        data = response.json()
        assert data["total_logs"] == 15420
        assert len(data["hourly_data"]) == 2


class TestVerdictGrading:
    """검증 등급 테스트"""

    def test_verdict_very_excellent_grade(self):
        """95% 이상: 매우 우수 등급"""
        # given
        db_stats = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 100,
            "info_count": 800,
            "error_rate": 10.0
        }
        ai_stats = {
            "estimated_total_logs": 998,   # 99.8%
            "estimated_error_count": 99,    # 99%
            "estimated_warn_count": 100,    # 100%
            "estimated_info_count": 799,    # 99.875%
            "estimated_error_rate": 10.0,   # 100%
            "confidence_score": 95
        }

        # when
        result = _calculate_accuracy(db_stats, ai_stats)

        # then
        assert result["overall_accuracy"] >= 95.0

    def test_verdict_excellent_grade(self):
        """90-95%: 우수 등급"""
        # given
        db_stats = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 100,
            "info_count": 800,
            "error_rate": 10.0
        }
        ai_stats = {
            "estimated_total_logs": 920,   # 92%
            "estimated_error_count": 93,    # 93%
            "estimated_warn_count": 95,     # 95%
            "estimated_info_count": 732,    # 91.5%
            "estimated_error_rate": 10.1,   # 99%
            "confidence_score": 85
        }

        # when
        result = _calculate_accuracy(db_stats, ai_stats)

        # then
        assert 90.0 <= result["overall_accuracy"] < 95.0

    def test_verdict_poor_grade(self):
        """70% 미만: 미흡 등급"""
        # given
        db_stats = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 100,
            "info_count": 800,
            "error_rate": 10.0
        }
        ai_stats = {
            "estimated_total_logs": 600,   # 60%
            "estimated_error_count": 60,    # 60%
            "estimated_warn_count": 60,     # 60%
            "estimated_info_count": 480,    # 60%
            "estimated_error_rate": 10.0,   # 100%
            "confidence_score": 30
        }

        # when
        result = _calculate_accuracy(db_stats, ai_stats)

        # then
        assert result["overall_accuracy"] < 70.0


class TestLogging:
    """로깅 테스트"""

    @patch("app.api.v2_langgraph.statistics.logger")
    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    @patch("app.api.v2_langgraph.statistics._get_log_samples")
    @patch("app.api.v2_langgraph.statistics._llm_estimate_statistics")
    @patch("app.api.v2_langgraph.statistics._calculate_accuracy")
    def test_logs_info_on_successful_comparison(
        self, mock_accuracy, mock_llm, mock_samples, mock_db, mock_logger, client
    ):
        """성공적인 비교 시 모든 단계에서 로깅"""
        # given
        mock_db.return_value = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 100,
            "info_count": 800,
            "error_rate": 10.0,
            "peak_hour": "2025-11-14T12",
            "peak_count": 500,
            "hourly_data": []
        }
        mock_samples.return_value = [{"level": "ERROR"}] * 100
        mock_llm.return_value = {
            "estimated_total_logs": 1000,
            "estimated_error_count": 100,
            "estimated_warn_count": 100,
            "estimated_info_count": 800,
            "estimated_error_rate": 10.0,
            "confidence_score": 90,
            "reasoning": "테스트"
        }
        mock_accuracy.return_value = {
            "total_logs_accuracy": 100.0,
            "error_count_accuracy": 100.0,
            "warn_count_accuracy": 100.0,
            "info_count_accuracy": 100.0,
            "error_rate_accuracy": 100.0,
            "overall_accuracy": 100.0,
            "ai_confidence": 90
        }

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/compare",
            params={"project_uuid": "test-uuid", "time_hours": 24, "sample_size": 100}
        )

        # then
        assert response.status_code == 200

        # 로깅 호출 검증
        info_calls = [call for call in mock_logger.info.call_args_list]
        assert len(info_calls) >= 4  # 시작, DB 조회, 샘플 추출, LLM 추론, 정확도 계산

        # 첫 번째 로그: 시작 메시지
        start_log = str(info_calls[0])
        assert "AI vs DB 통계 비교 시작" in start_log
        assert "test-uuid" in start_log

    @patch("app.api.v2_langgraph.statistics.logger")
    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    def test_logs_warning_when_no_data(self, mock_db, mock_logger, client):
        """로그 데이터가 없을 때 경고 로깅"""
        # given
        mock_db.return_value = {
            "total_logs": 0,
            "error_count": 0,
            "warn_count": 0,
            "info_count": 0,
            "error_rate": 0,
            "peak_hour": "",
            "peak_count": 0,
            "hourly_data": []
        }

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/compare",
            params={"project_uuid": "empty-uuid", "time_hours": 24}
        )

        # then
        assert response.status_code == 404

        # 경고 로깅 검증
        warning_calls = mock_logger.warning.call_args_list
        assert len(warning_calls) >= 1
        warning_log = str(warning_calls[0])
        assert "로그 데이터 없음" in warning_log
        assert "empty-uuid" in warning_log

    @patch("app.api.v2_langgraph.statistics.logger")
    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    def test_logs_error_on_exception(self, mock_db, mock_logger, client):
        """예외 발생 시 에러 로깅"""
        # given
        mock_db.side_effect = Exception("OpenSearch connection failed")

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/compare",
            params={"project_uuid": "fail-uuid", "time_hours": 24}
        )

        # then
        assert response.status_code == 500

        # 에러 로깅 검증
        error_calls = mock_logger.error.call_args_list
        assert len(error_calls) >= 1
        error_log = str(error_calls[0])
        assert "AI vs DB 통계 비교 중 예외 발생" in error_log
        assert "fail-uuid" in error_log

    @patch("app.api.v2_langgraph.statistics.logger")
    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    @patch("app.api.v2_langgraph.statistics._get_log_samples")
    def test_logs_error_when_samples_empty(self, mock_samples, mock_db, mock_logger, client):
        """샘플 추출 실패 시 에러 로깅"""
        # given
        mock_db.return_value = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 100,
            "info_count": 800,
            "error_rate": 10.0,
            "peak_hour": "2025-11-14T12",
            "peak_count": 500,
            "hourly_data": []
        }
        mock_samples.return_value = []  # 빈 샘플

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/compare",
            params={"project_uuid": "no-samples-uuid", "time_hours": 24}
        )

        # then
        assert response.status_code == 500

        # 에러 로깅 검증
        error_calls = mock_logger.error.call_args_list
        assert len(error_calls) >= 1
        error_log = str(error_calls[0])
        assert "로그 샘플 추출 실패" in error_log

    @patch("app.api.v2_langgraph.statistics.logger")
    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    @patch("app.api.v2_langgraph.statistics._get_log_samples")
    @patch("app.api.v2_langgraph.statistics._llm_estimate_statistics")
    @patch("app.api.v2_langgraph.statistics._calculate_accuracy")
    def test_logs_include_accuracy_results(
        self, mock_accuracy, mock_llm, mock_samples, mock_db, mock_logger, client
    ):
        """정확도 결과가 로그에 포함됨"""
        # given
        mock_db.return_value = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 100,
            "info_count": 800,
            "error_rate": 10.0,
            "peak_hour": "2025-11-14T12",
            "peak_count": 500,
            "hourly_data": []
        }
        mock_samples.return_value = [{"level": "ERROR"}] * 50
        mock_llm.return_value = {
            "estimated_total_logs": 950,
            "estimated_error_count": 95,
            "estimated_warn_count": 95,
            "estimated_info_count": 760,
            "estimated_error_rate": 10.0,
            "confidence_score": 85,
            "reasoning": "테스트"
        }
        mock_accuracy.return_value = {
            "total_logs_accuracy": 95.0,
            "error_count_accuracy": 95.0,
            "warn_count_accuracy": 95.0,
            "info_count_accuracy": 95.0,
            "error_rate_accuracy": 100.0,
            "overall_accuracy": 96.0,
            "ai_confidence": 85
        }

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/compare",
            params={"project_uuid": "test-uuid", "time_hours": 24}
        )

        # then
        assert response.status_code == 200

        # 정확도 로깅 검증
        info_calls = [str(call) for call in mock_logger.info.call_args_list]
        accuracy_logged = any("96" in call or "overall_accuracy" in call for call in info_calls)
        assert accuracy_logged

    @patch("app.api.v2_langgraph.statistics.logger")
    @patch("app.api.v2_langgraph.statistics._get_db_statistics")
    @patch("app.api.v2_langgraph.statistics._get_log_samples")
    @patch("app.api.v2_langgraph.statistics._llm_estimate_statistics")
    @patch("app.api.v2_langgraph.statistics._calculate_accuracy")
    def test_debug_logs_for_each_step(
        self, mock_accuracy, mock_llm, mock_samples, mock_db, mock_logger, client
    ):
        """각 단계별 디버그 로그 확인"""
        # given
        mock_db.return_value = {
            "total_logs": 1000,
            "error_count": 100,
            "warn_count": 100,
            "info_count": 800,
            "error_rate": 10.0,
            "peak_hour": "2025-11-14T12",
            "peak_count": 500,
            "hourly_data": []
        }
        mock_samples.return_value = [{"level": "ERROR"}] * 100
        mock_llm.return_value = {
            "estimated_total_logs": 1000,
            "estimated_error_count": 100,
            "estimated_warn_count": 100,
            "estimated_info_count": 800,
            "estimated_error_rate": 10.0,
            "confidence_score": 90,
            "reasoning": "테스트"
        }
        mock_accuracy.return_value = {
            "total_logs_accuracy": 100.0,
            "error_count_accuracy": 100.0,
            "warn_count_accuracy": 100.0,
            "info_count_accuracy": 100.0,
            "error_rate_accuracy": 100.0,
            "overall_accuracy": 100.0,
            "ai_confidence": 90
        }

        # when
        response = client.get(
            "/api/v2-langgraph/statistics/compare",
            params={"project_uuid": "test-uuid", "time_hours": 24}
        )

        # then
        assert response.status_code == 200

        # 디버그 로그 검증
        debug_calls = [str(call) for call in mock_logger.debug.call_args_list]
        assert any("1단계" in call or "DB 통계 조회" in call for call in debug_calls)
        assert any("2단계" in call or "로그 샘플 추출" in call for call in debug_calls)
        assert any("3단계" in call or "LLM 통계 추론" in call for call in debug_calls)
        assert any("4단계" in call or "정확도 계산" in call for call in debug_calls)
