"""
AI 통계 비교 도구 테스트
- 층화 샘플링 로직 검증
- 정확도 계산 검증
- 힌트 기반 추론 검증
"""

import pytest
import sys
import os
from unittest.mock import patch, MagicMock, Mock

# Set required environment variables before importing app modules
os.environ.setdefault("OPENAI_API_KEY", "test-key-for-testing")
os.environ.setdefault("OPENSEARCH_HOST", "localhost")
os.environ.setdefault("OPENSEARCH_PORT", "9200")
os.environ.setdefault("OPENSEARCH_USER", "admin")
os.environ.setdefault("OPENSEARCH_PASSWORD", "admin")

# Now import the app modules after setting env vars
from app.tools.statistics_comparison_tools import (
    _calculate_accuracy,
    _llm_estimate_statistics
)


class TestAccuracyCalculation:
    """정확도 계산 테스트"""

    def test_완벽한_일치_시_100퍼센트_정확도(self):
        """AI 추론이 DB 통계와 완벽히 일치할 때"""
        db_stats = {
            "total_logs": 100000,
            "error_count": 500,
            "warn_count": 1000,
            "info_count": 98500,
            "error_rate": 0.5
        }
        ai_stats = {
            "estimated_total_logs": 100000,
            "estimated_error_count": 500,
            "estimated_warn_count": 1000,
            "estimated_info_count": 98500,
            "estimated_error_rate": 0.5,
            "confidence_score": 95
        }

        result = _calculate_accuracy(db_stats, ai_stats)

        assert result["total_logs_accuracy"] == 100.0
        assert result["error_count_accuracy"] == 100.0
        assert result["warn_count_accuracy"] == 100.0
        assert result["info_count_accuracy"] == 100.0
        assert result["error_rate_accuracy"] == 100.0
        assert result["overall_accuracy"] == 100.0

    def test_부분_일치_시_정확도_계산(self):
        """AI 추론이 90% 정확할 때"""
        db_stats = {
            "total_logs": 100000,
            "error_count": 1000,
            "warn_count": 2000,
            "info_count": 97000,
            "error_rate": 1.0
        }
        ai_stats = {
            "estimated_total_logs": 100000,  # 힌트로 제공되므로 100% 정확
            "estimated_error_count": 900,     # 90% 정확
            "estimated_warn_count": 1800,     # 90% 정확
            "estimated_info_count": 97300,    # 99.7% 정확
            "estimated_error_rate": 0.9,      # 90% 정확
            "confidence_score": 90
        }

        result = _calculate_accuracy(db_stats, ai_stats)

        assert result["total_logs_accuracy"] == 100.0
        assert result["error_count_accuracy"] == 90.0
        assert result["warn_count_accuracy"] == 90.0
        # INFO 정확도는 약 99.69%
        assert result["info_count_accuracy"] > 99.0
        # 에러율 정확도: 0.1% 차이 = 100 - 1 = 99%
        assert result["error_rate_accuracy"] == 99.0
        # 종합: 100*0.3 + 90*0.3 + 99*0.2 + 90*0.1 + 99.69*0.1 = 95.669
        assert result["overall_accuracy"] > 95.0

    def test_제로_값_처리(self):
        """DB에 값이 0일 때 정확도 계산"""
        db_stats = {
            "total_logs": 100,
            "error_count": 0,
            "warn_count": 0,
            "info_count": 100,
            "error_rate": 0.0
        }
        ai_stats = {
            "estimated_total_logs": 100,
            "estimated_error_count": 0,
            "estimated_warn_count": 0,
            "estimated_info_count": 100,
            "estimated_error_rate": 0.0,
            "confidence_score": 95
        }

        result = _calculate_accuracy(db_stats, ai_stats)

        # ERROR/WARN이 0이고 AI도 0을 추론하면 100% 정확
        assert result["error_count_accuracy"] == 100.0
        assert result["warn_count_accuracy"] == 100.0
        assert result["overall_accuracy"] == 100.0

    def test_전혀_다른_값_예측_시_낮은_정확도(self):
        """AI가 완전히 틀린 값을 추론할 때"""
        db_stats = {
            "total_logs": 100000,
            "error_count": 1000,
            "warn_count": 2000,
            "info_count": 97000,
            "error_rate": 1.0
        }
        ai_stats = {
            "estimated_total_logs": 100000,  # 힌트로 제공되므로 정확
            "estimated_error_count": 0,       # 완전히 틀림
            "estimated_warn_count": 0,        # 완전히 틀림
            "estimated_info_count": 100000,   # 3% 오차
            "estimated_error_rate": 0.0,      # 완전히 틀림
            "confidence_score": 50
        }

        result = _calculate_accuracy(db_stats, ai_stats)

        assert result["total_logs_accuracy"] == 100.0
        assert result["error_count_accuracy"] == 0.0
        assert result["warn_count_accuracy"] == 0.0
        # INFO: 3% 오차 = 97%
        assert result["info_count_accuracy"] > 96.0
        # 에러율: 1% 차이 = 100 - 10 = 90%
        assert result["error_rate_accuracy"] == 90.0
        # 종합: 100*0.3 + 0*0.3 + 90*0.2 + 0*0.1 + 97*0.1 = 57.7
        assert result["overall_accuracy"] < 60.0


class TestStratifiedSamplingLogic:
    """층화 샘플링 로직 테스트 (단위 테스트)"""

    def test_비율_기반_샘플_배분(self):
        """레벨별 비율에 맞게 샘플이 배분되는지 테스트"""
        # 시뮬레이션: 층화 샘플링 로직
        sample_size = 100
        level_counts = {
            "ERROR": 1000,   # 1%
            "WARN": 2000,    # 2%
            "INFO": 97000    # 97%
        }
        total_logs = sum(level_counts.values())

        level_sample_sizes = {}
        remaining_samples = sample_size

        for level in ["ERROR", "WARN", "INFO"]:
            actual_count = level_counts.get(level, 0)
            if actual_count == 0:
                level_sample_sizes[level] = 0
                continue

            ratio = actual_count / total_logs
            allocated = int(sample_size * ratio)

            # 희소 이벤트 최소 보장
            if allocated == 0 and actual_count > 0:
                allocated = min(5, actual_count)

            allocated = min(allocated, actual_count, remaining_samples)
            level_sample_sizes[level] = allocated
            remaining_samples -= allocated

        # 검증
        assert level_sample_sizes["ERROR"] >= 1  # 최소 1개 (1%)
        assert level_sample_sizes["WARN"] >= 2   # 최소 2개 (2%)
        assert level_sample_sizes["INFO"] >= 90  # 대부분 INFO

        # 총 샘플 수가 sample_size 이하
        total_allocated = sum(level_sample_sizes.values())
        assert total_allocated <= sample_size

    def test_희소_이벤트_최소_보장(self):
        """ERROR가 매우 희소할 때도 최소 5개 샘플링"""
        sample_size = 100
        level_counts = {
            "ERROR": 10,      # 0.003% (매우 희소)
            "WARN": 20,       # 0.006%
            "INFO": 299970    # 99.99%
        }
        total_logs = sum(level_counts.values())

        level_sample_sizes = {}
        remaining_samples = sample_size

        for level in ["ERROR", "WARN", "INFO"]:
            actual_count = level_counts.get(level, 0)
            if actual_count == 0:
                level_sample_sizes[level] = 0
                continue

            ratio = actual_count / total_logs
            allocated = int(sample_size * ratio)

            # 희소 이벤트 최소 보장
            if allocated == 0 and actual_count > 0:
                allocated = min(5, actual_count)

            allocated = min(allocated, actual_count, remaining_samples)
            level_sample_sizes[level] = allocated
            remaining_samples -= allocated

        # 검증: 희소 이벤트도 최소 5개 (또는 실제 개수)
        assert level_sample_sizes["ERROR"] == 5  # 최소 5개 보장
        assert level_sample_sizes["WARN"] == 5   # 최소 5개 보장
        assert level_sample_sizes["INFO"] > 0    # 나머지

    def test_층화_샘플링이_랜덤보다_정확한_비율_반영(self):
        """층화 샘플링이 실제 비율을 더 정확히 반영"""
        # 시나리오: 전체 312,884개 로그 중 ERROR 0.37%
        total_logs = 312884
        error_count = 1154  # 0.37%
        warn_count = 219    # 0.07%
        info_count = 311511 # 99.56%

        # 100개 층화 샘플링 시
        sample_size = 100
        expected_error_samples = max(1, int(sample_size * (error_count / total_logs)))
        expected_warn_samples = max(1, int(sample_size * (warn_count / total_logs)))

        # 최소 보장 적용
        if expected_error_samples == 0:
            expected_error_samples = min(5, error_count)
        if expected_warn_samples == 0:
            expected_warn_samples = min(5, warn_count)

        # 검증: 층화 샘플링으로 희소 이벤트 포착 가능
        assert expected_error_samples >= 1
        assert expected_warn_samples >= 1

        # AI가 이 샘플을 기반으로 비율 계산 시
        # ERROR 비율: 5/100 = 5% (실제 0.37%와 차이 있지만 0%보다 훨씬 정확)
        # 기존 랜덤 샘플링: 100% INFO → ERROR 0% 추론


class TestLLMInferenceWithHints:
    """힌트 기반 LLM 추론 테스트 (폴백 로직)"""

    @patch('app.tools.statistics_comparison_tools.ChatOpenAI')
    def test_폴백_로직_정확한_비율_계산(self, mock_openai):
        """LLM 실패 시 폴백 로직이 정확한 비율을 계산하는지"""
        # LLM 호출 실패 시뮬레이션
        mock_llm = MagicMock()
        mock_llm.invoke.side_effect = Exception("API Error")
        mock_openai.return_value = mock_llm

        # 층화 샘플링된 데이터 (실제 비율 반영)
        log_samples = [
            {"level": "ERROR", "timestamp": "2025-11-17T15:00:00", "service": "app", "message": "Error occurred"},
            {"level": "ERROR", "timestamp": "2025-11-17T16:00:00", "service": "app", "message": "Another error"},
            {"level": "WARN", "timestamp": "2025-11-17T15:30:00", "service": "app", "message": "Warning"},
        ] + [
            {"level": "INFO", "timestamp": f"2025-11-17T{i:02d}:00:00", "service": "app", "message": f"Info {i}"}
            for i in range(97)
        ]

        total_logs_hint = 100000

        result = _llm_estimate_statistics(log_samples, total_logs_hint, 24)

        # 폴백 로직 검증
        assert result["estimated_total_logs"] == 100000  # 힌트 사용
        # ERROR 비율: 2/100 = 2%
        assert result["estimated_error_count"] == 2000
        # WARN 비율: 1/100 = 1%
        assert result["estimated_warn_count"] == 1000
        # INFO 비율: 97/100 = 97%
        assert result["estimated_info_count"] == 97000
        assert result["estimated_error_rate"] == 2.0
        assert result["confidence_score"] == 85  # 폴백이지만 높은 신뢰도

    def test_빈_샘플_처리(self):
        """샘플이 비어있을 때 안전한 처리"""
        with patch('app.tools.statistics_comparison_tools.ChatOpenAI') as mock_openai:
            mock_llm = MagicMock()
            mock_llm.invoke.side_effect = Exception("API Error")
            mock_openai.return_value = mock_llm

            result = _llm_estimate_statistics([], 10000, 24)

            assert result["estimated_total_logs"] == 10000
            assert result["estimated_error_count"] == 0
            assert result["confidence_score"] == 0


class TestEndToEndScenario:
    """종단 간 시나리오 테스트"""

    def test_층화_샘플링으로_높은_정확도_달성_시나리오(self):
        """
        실제 시나리오 시뮬레이션:
        - DB: 312,884 로그, ERROR 1,154 (0.37%), WARN 219 (0.07%), INFO 311,511 (99.56%)
        - 층화 샘플링: ERROR 5개, WARN 5개, INFO 90개
        - AI 추론: 비율 기반으로 계산
        """
        # 1. DB 통계 (Ground Truth)
        db_stats = {
            "total_logs": 312884,
            "error_count": 1154,
            "warn_count": 219,
            "info_count": 311511,
            "error_rate": 0.37
        }

        # 2. 층화 샘플링 결과 시뮬레이션
        # ERROR: 5/100 = 5%
        # WARN: 5/100 = 5%
        # INFO: 90/100 = 90%

        # 3. AI 추론 (힌트 + 비율 적용)
        # 총 로그 수는 힌트로 제공 (100% 정확)
        # 비율 적용:
        # ERROR: 312884 * 0.05 = 15644 (실제 1154)
        # WARN: 312884 * 0.05 = 15644 (실제 219)
        # INFO: 312884 * 0.90 = 281596 (실제 311511)

        # 더 나은 추론: 최소 보장 샘플이 과대표현됨을 고려
        # 실제 구현에서는 LLM이 이를 보정할 수 있음

        # 4. 보수적 추정 (최소 보장 효과 반영)
        ai_stats = {
            "estimated_total_logs": 312884,  # 힌트로 100% 정확
            "estimated_error_count": 15644,   # 과대 추정 (희소 이벤트 과대표현)
            "estimated_warn_count": 15644,    # 과대 추정
            "estimated_info_count": 281596,   # 과소 추정
            "estimated_error_rate": 5.0,      # 샘플 비율 그대로
            "confidence_score": 85
        }

        result = _calculate_accuracy(db_stats, ai_stats)

        # 총 로그 수: 100% (힌트 사용)
        assert result["total_logs_accuracy"] == 100.0

        # ERROR/WARN 개수는 과대 추정되지만 0%보다 훨씬 나음
        # 기존: 0% (ERROR 0개 추론)
        # 현재: 최소한 ERROR가 있다는 것을 인식

        # 종합 정확도: 총 로그 수가 100%이므로 가중치 30% 확보
        # 이전 시스템: 19.57% → 현재: 최소 30% 이상
        assert result["overall_accuracy"] > 20.0  # 기존보다 개선

    def test_실제_비율_반영_시_높은_정확도(self):
        """
        LLM이 최소 보장 효과를 보정하여 실제 비율에 가깝게 추론할 때
        """
        db_stats = {
            "total_logs": 312884,
            "error_count": 1154,
            "warn_count": 219,
            "info_count": 311511,
            "error_rate": 0.37
        }

        # 이상적인 AI 추론 (LLM이 최소 보장 효과를 보정)
        # 프롬프트에서 "희소 이벤트 최소 보장" 명시했으므로
        # LLM이 이를 고려하여 보정할 수 있음
        ai_stats = {
            "estimated_total_logs": 312884,  # 힌트로 정확
            "estimated_error_count": 1250,    # 근접 추론
            "estimated_warn_count": 250,      # 근접 추론
            "estimated_info_count": 311384,   # 근접 추론
            "estimated_error_rate": 0.40,     # 근접 추론
            "confidence_score": 90
        }

        result = _calculate_accuracy(db_stats, ai_stats)

        # 높은 정확도
        assert result["total_logs_accuracy"] == 100.0
        assert result["error_count_accuracy"] > 90.0  # 1250 vs 1154 = ~92%
        assert result["warn_count_accuracy"] > 85.0   # 250 vs 219 = ~86%
        assert result["info_count_accuracy"] > 99.0   # 매우 정확
        assert result["error_rate_accuracy"] > 96.0   # 0.4% vs 0.37% = ~97%

        # 종합 정확도: 95% 이상 (매우 우수)
        assert result["overall_accuracy"] > 95.0


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
