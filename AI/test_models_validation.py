#!/usr/bin/env python3
"""
Models Validation Test
V2 필드(sources, validation, analysis_metadata)가 올바르게 정의되었는지 확인
"""

import sys
from dotenv import load_dotenv
load_dotenv()


def test_chat_response_model():
    """ChatResponse에 sources, validation 필드가 있는지 확인"""
    print("=" * 80)
    print("Test 1: ChatResponse Model Validation")
    print("=" * 80)

    from app.models.chat import ChatResponse
    from app.models.experiment import LogSource, ValidationInfo

    # LogSource 생성
    source = LogSource(
        log_id="12345",
        timestamp="2024-01-15T10:30:00Z",
        level="ERROR",
        message="Test NullPointerException in UserService",
        service_name="test-service",
        relevance_score=0.95,
        class_name="UserService",
        method_name="getUser"
    )

    print(f"✅ LogSource 생성 성공")
    print(f"   log_id: {source.log_id}")
    print(f"   level: {source.level}")
    print(f"   relevance_score: {source.relevance_score}")

    # ValidationInfo 생성
    validation = ValidationInfo(
        confidence=85,
        sample_count=10,
        sampling_strategy="proportional_vector_knn",
        coverage="최근 24시간 ERROR 로그 분석",
        data_quality="high",
        limitation="Vector 검색은 ERROR 로그만 지원. WARN/INFO는 기본 필터 사용",
        note="Agent가 자율적으로 도구를 선택하여 분석"
    )

    print(f"✅ ValidationInfo 생성 성공")
    print(f"   confidence: {validation.confidence}/100")
    print(f"   sample_count: {validation.sample_count}개")
    print(f"   sampling_strategy: {validation.sampling_strategy}")
    print(f"   data_quality: {validation.data_quality}")

    # ChatResponse 생성 (V2 필드 포함)
    response = ChatResponse(
        answer="테스트 답변입니다",
        from_cache=False,
        related_logs=[],
        sources=[source],
        validation=validation
    )

    # 검증
    assert response.sources is not None, "sources 필드가 None입니다"
    assert len(response.sources) == 1, f"sources 개수가 잘못됨: {len(response.sources)}"
    assert response.validation is not None, "validation 필드가 None입니다"
    assert response.validation.confidence == 85, f"confidence 값 불일치: {response.validation.confidence}"

    print(f"✅ ChatResponse 모델 검증 성공")
    print(f"   sources: {len(response.sources)}개")
    print(f"   validation.confidence: {response.validation.confidence}")
    print()

    return True


def test_analysis_response_model():
    """LogAnalysisResponse에 sources, validation 필드가 있는지 확인"""
    print("=" * 80)
    print("Test 2: LogAnalysisResponse Model Validation")
    print("=" * 80)

    from app.models.analysis import LogAnalysisResponse, LogAnalysisResult
    from app.models.experiment import LogSource, ValidationInfo

    # LogAnalysisResult 생성
    analysis = LogAnalysisResult(
        summary="UserService의 getUser() 메서드에서 NullPointerException 발생",
        error_cause="user_id에 해당하는 User 객체가 null",
        solution="[즉시] null 체크 추가",
        tags=["SEVERITY_HIGH", "NullPointerException", "UserService"]
    )

    print(f"✅ LogAnalysisResult 생성 성공")
    print(f"   summary: {analysis.summary[:50]}...")

    # LogSource 생성
    source = LogSource(
        log_id="12345",
        timestamp="2024-01-15T10:30:00Z",
        level="ERROR",
        message="NullPointerException in UserService.getUser()",
        service_name="user-service",
        relevance_score=0.98
    )

    # ValidationInfo 생성
    validation = ValidationInfo(
        confidence=90,
        sample_count=5,
        sampling_strategy="trace_id_filter",
        coverage="Trace 기반 5개 로그 분석",
        data_quality="high"
    )

    # LogAnalysisResponse 생성 (V2 필드 포함)
    response = LogAnalysisResponse(
        log_id=12345,
        analysis=analysis,
        from_cache=False,
        similar_log_id=None,
        similarity_score=None,
        sources=[source],
        validation=validation
    )

    # 검증
    assert response.log_id == 12345
    assert response.sources is not None
    assert len(response.sources) == 1
    assert response.validation is not None
    assert response.validation.sampling_strategy == "trace_id_filter"

    print(f"✅ LogAnalysisResponse 모델 검증 성공")
    print(f"   log_id: {response.log_id}")
    print(f"   sources: {len(response.sources)}개")
    print(f"   validation.sampling_strategy: {response.validation.sampling_strategy}")
    print()

    return True


def test_document_metadata_model():
    """AiDocumentMetadata에 analysis_metadata 필드가 있는지 확인"""
    print("=" * 80)
    print("Test 3: AiDocumentMetadata Model Validation")
    print("=" * 80)

    from app.models.document import AiDocumentMetadata
    from app.models.experiment import AnalysisMetadata
    from datetime import datetime

    # AnalysisMetadata 생성
    analysis_metadata = AnalysisMetadata(
        generated_at=datetime.utcnow(),
        data_range="2025-11-17 ~ 2025-11-18",
        total_logs_analyzed=1000,
        error_logs=50,
        warn_logs=150,
        info_logs=800,
        sample_strategy={
            "error_strategy": "proportional_vector_knn",
            "warn_strategy": "random_filter",
            "info_strategy": "random_filter"
        },
        limitations=[
            "Vector 검색은 ERROR 로그만 지원",
            "WARN/INFO는 벡터화되지 않음"
        ]
    )

    print(f"✅ AnalysisMetadata 생성 성공")
    print(f"   total_logs_analyzed: {analysis_metadata.total_logs_analyzed:,}")
    print(f"   error_logs: {analysis_metadata.error_logs}")

    # AiDocumentMetadata 생성 (V2 필드 포함)
    metadata = AiDocumentMetadata(
        word_count=1500,
        estimated_reading_time="7 minutes",
        sections_generated=["overview", "errors", "recommendations"],
        generation_time=3.5,
        health_score=85,
        critical_issues=5,
        analysis_metadata=analysis_metadata
    )

    # 검증
    assert metadata.word_count == 1500
    assert metadata.analysis_metadata is not None
    assert metadata.analysis_metadata.total_logs_analyzed == 1000
    assert metadata.analysis_metadata.error_logs == 50

    print(f"✅ AiDocumentMetadata 모델 검증 성공")
    print(f"   word_count: {metadata.word_count:,}")
    print(f"   analysis_metadata.error_logs: {metadata.analysis_metadata.error_logs}")
    print(f"   analysis_metadata.limitations: {len(metadata.analysis_metadata.limitations)}개")
    print()

    return True


def test_sources_tracker():
    """SourcesTracker 유틸리티 테스트"""
    print("=" * 80)
    print("Test 4: SourcesTracker Utility")
    print("=" * 80)

    from app.utils.sources_tracker import SourcesTracker

    tracker = SourcesTracker()

    # 로그 추가
    logs = [
        {
            "log_id": 1,
            "level": "ERROR",
            "message": "NullPointerException in UserService",
            "timestamp": "2024-01-15T10:00:00Z",
            "service_name": "user-service",
            "class_name": "UserService",
            "method_name": "getUser"
        },
        {
            "log_id": 2,
            "level": "ERROR",
            "message": "TimeoutException in PaymentService",
            "timestamp": "2024-01-15T10:01:00Z",
            "service_name": "payment-service",
            "class_name": "PaymentService",
            "method_name": "processPayment"
        },
        {
            "log_id": 3,
            "level": "WARN",
            "message": "Slow query detected",
            "timestamp": "2024-01-15T10:02:00Z",
            "service_name": "db-service"
        }
    ]

    relevance_scores = [0.95, 0.88, 0.72]

    tracker.add_sources(logs, search_type="vector_knn", relevance_scores=relevance_scores)

    # 검증
    assert len(tracker.sources) == 3, f"sources 개수 불일치: {len(tracker.sources)}"
    assert tracker.error_logs_count == 2, f"ERROR 개수 불일치: {tracker.error_logs_count}"
    assert tracker.warn_logs_count == 1, f"WARN 개수 불일치: {tracker.warn_logs_count}"
    assert tracker.used_vector_search is True, "Vector 검색 사용 플래그 미설정"

    print(f"✅ 출처 추적 성공")
    print(f"   총 sources: {len(tracker.sources)}개")
    print(f"   ERROR 로그: {tracker.error_logs_count}개")
    print(f"   WARN 로그: {tracker.warn_logs_count}개")
    print(f"   Vector 검색 사용: {tracker.used_vector_search}")

    # ValidationInfo 생성
    validation = tracker.get_validation_info()

    assert validation.sample_count == 3
    assert validation.confidence > 50
    assert validation.data_quality in ["high", "medium", "low"]

    print(f"✅ ValidationInfo 생성 성공")
    print(f"   confidence: {validation.confidence}/100")
    print(f"   sample_count: {validation.sample_count}")
    print(f"   sampling_strategy: {validation.sampling_strategy}")
    print(f"   data_quality: {validation.data_quality}")
    print(f"   coverage: {validation.coverage}")

    # 상위 출처 가져오기
    top_sources = tracker.get_top_sources(limit=2)

    assert len(top_sources) == 2
    # 첫 번째가 가장 높은 점수여야 함
    assert top_sources[0].relevance_score >= top_sources[1].relevance_score

    print(f"✅ 상위 출처 정렬 성공")
    print(f"   Top 1: log_id={top_sources[0].log_id}, score={top_sources[0].relevance_score}")
    print(f"   Top 2: log_id={top_sources[1].log_id}, score={top_sources[1].relevance_score}")
    print()

    return True


def main():
    """모든 테스트 실행"""
    print()
    print("🧪 V2 Models & Utilities Validation Test Suite")
    print()

    results = {}

    try:
        results["ChatResponse Model"] = test_chat_response_model()
    except Exception as e:
        print(f"❌ ChatResponse 테스트 실패: {e}")
        import traceback
        traceback.print_exc()
        results["ChatResponse Model"] = False

    try:
        results["LogAnalysisResponse Model"] = test_analysis_response_model()
    except Exception as e:
        print(f"❌ LogAnalysisResponse 테스트 실패: {e}")
        import traceback
        traceback.print_exc()
        results["LogAnalysisResponse Model"] = False

    try:
        results["AiDocumentMetadata Model"] = test_document_metadata_model()
    except Exception as e:
        print(f"❌ AiDocumentMetadata 테스트 실패: {e}")
        import traceback
        traceback.print_exc()
        results["AiDocumentMetadata Model"] = False

    try:
        results["SourcesTracker Utility"] = test_sources_tracker()
    except Exception as e:
        print(f"❌ SourcesTracker 테스트 실패: {e}")
        import traceback
        traceback.print_exc()
        results["SourcesTracker Utility"] = False

    # 결과 요약
    print("=" * 80)
    print("📊 Test Results Summary")
    print("=" * 80)
    print()

    for test_name, passed in results.items():
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"  {status}  {test_name}")

    print()

    all_passed = all(results.values())
    if all_passed:
        print("✅ All tests passed!")
        print()
        print("📌 다음 단계:")
        print("   1. AI Service 시작 (.env에 OPENAI_API_KEY 필요)")
        print("   2. Chatbot V2 API 통합 테스트")
        print("   3. Log 분석 V2 API 통합 테스트")
        print("   4. ERROR 패턴 분석 실험 실행")
    else:
        print("❌ Some tests failed")
        failed_tests = [name for name, passed in results.items() if not passed]
        print(f"   Failed: {', '.join(failed_tests)}")

    print()

    return 0 if all_passed else 1


if __name__ == "__main__":
    sys.exit(main())
