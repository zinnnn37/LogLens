"""
Manual test script for HTML document generation
Run without pytest dependency
"""

import asyncio
import sys
from pathlib import Path

# Add app to path
sys.path.insert(0, str(Path(__file__).parent))

# Import directly to avoid loading all services
from app.models.document import (
    AiHtmlDocumentRequest,
    DocumentType,
    DocumentFormat,
    StylePreferences,
)

# Import service class directly
import importlib.util
spec = importlib.util.spec_from_file_location(
    "html_document_service",
    Path(__file__).parent / "app" / "services" / "html_document_service.py"
)
html_doc_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(html_doc_module)
html_document_service = html_doc_module.HtmlDocumentService()


async def test_project_analysis():
    """Test project analysis HTML generation"""
    print("=" * 80)
    print("TEST 1: Project Analysis HTML Generation")
    print("=" * 80)

    request = AiHtmlDocumentRequest(
        project_uuid="550e8400-e29b-41d4-a716-446655440000",
        document_type=DocumentType.PROJECT_ANALYSIS,
        format=DocumentFormat.HTML,
        data={
            "projectInfo": {
                "name": "LogLens Test Project",
                "uuid": "550e8400-e29b-41d4-a716-446655440000",
                "description": "실시간 로그 분석 플랫폼 테스트",
            },
            "timeRange": {
                "startTime": "2024-01-01T00:00:00",
                "endTime": "2024-01-31T23:59:59",
            },
            "metrics": {
                "totalLogs": 15000,
                "errorCount": 250,
                "warnCount": 1200,
                "infoCount": 13550,
                "avgResponseTime": 125.5,
            },
            "topErrors": [
                {
                    "logId": 1,
                    "message": "NullPointerException in UserService.getUser()",
                    "timestamp": "2024-01-15T10:30:00",
                    "componentName": "UserService",
                    "logLevel": "ERROR",
                    "traceId": "abc123",
                },
                {
                    "logId": 2,
                    "message": "Database connection timeout",
                    "timestamp": "2024-01-16T14:20:00",
                    "componentName": "DatabasePool",
                    "logLevel": "ERROR",
                    "traceId": "def456",
                },
            ],
        },
        options={"includeCharts": True, "darkMode": False},
        style_preferences=StylePreferences(
            css_framework="tailwind", chart_library="chartjs", color_scheme="blue"
        ),
    )

    try:
        response = await html_document_service.generate_html_document(request)

        print(f"✅ HTML 생성 성공")
        print(f"   - HTML 길이: {len(response.html_content):,} 문자")
        print(f"   - 단어 수: {response.metadata.word_count:,}")
        print(f"   - 예상 읽기 시간: {response.metadata.estimated_reading_time}")
        print(f"   - 건강 점수: {response.metadata.health_score}/100")
        print(f"   - 생성 시간: {response.metadata.generation_time}초")
        print(f"   - HTML 유효성: {response.validation_status.is_valid_html}")
        print(f"   - 필수 섹션 포함: {response.validation_status.has_required_sections}")

        # Check required HTML tags
        html_lower = response.html_content.lower()
        required_tags = ["<!doctype html>", "<html", "<head", "<body", "</body>", "</html>"]
        for tag in required_tags:
            if tag in html_lower:
                print(f"   ✓ {tag} 태그 포함")
            else:
                print(f"   ✗ {tag} 태그 누락!")

        # Check project name
        if "LogLens Test Project" in response.html_content:
            print(f"   ✓ 프로젝트명 바인딩 성공")

        # Check metrics
        if "15,000" in response.html_content or "15000" in response.html_content:
            print(f"   ✓ 메트릭 바인딩 성공")

        # Save to file
        output_file = Path(__file__).parent / "test_output_project.html"
        output_file.write_text(response.html_content, encoding="utf-8")
        print(f"\n📄 HTML 파일 저장: {output_file}")

        return True

    except Exception as e:
        print(f"❌ 테스트 실패: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


async def test_error_analysis():
    """Test error analysis HTML generation"""
    print("\n" + "=" * 80)
    print("TEST 2: Error Analysis HTML Generation")
    print("=" * 80)

    request = AiHtmlDocumentRequest(
        log_id=12345,
        project_uuid="550e8400-e29b-41d4-a716-446655440000",
        document_type=DocumentType.ERROR_ANALYSIS,
        format=DocumentFormat.HTML,
        data={
            "errorLog": {
                "logId": 12345,
                "level": "ERROR",
                "message": "NullPointerException in UserService.getUser()",
                "stackTrace": "java.lang.NullPointerException: Cannot invoke method getEmail() on null object\n\tat com.example.service.UserService.getUser(UserService.java:45)\n\tat com.example.controller.UserController.getUserInfo(UserController.java:28)",
                "timestamp": "2024-01-15T10:35:00.123Z",
                "componentName": "UserService",
                "traceId": "abc123-def456-ghi789",
            },
            "existingAnalysis": {
                "summary": "UserService의 getUser() 메서드에서 NullPointerException이 발생했습니다.",
                "errorCause": "user_id=12345에 해당하는 User 객체가 null입니다.",
                "solution": "[즉시] null 체크 추가\n[단기] Optional 사용\n[장기] 검증 로직 추가",
                "tags": ["SEVERITY_HIGH", "NullPointerException", "UserService"],
            },
            "relatedLogs": [
                {
                    "logId": 12346,
                    "message": "User not found in database",
                    "timestamp": "2024-01-15T10:34:59.998Z",
                    "componentName": "UserRepository",
                    "logLevel": "WARN",
                }
            ],
        },
        options={
            "includeRelatedLogs": True,
            "includeImpactAnalysis": True,
        },
    )

    try:
        response = await html_document_service.generate_html_document(request)

        print(f"✅ HTML 생성 성공")
        print(f"   - HTML 길이: {len(response.html_content):,} 문자")
        print(f"   - 단어 수: {response.metadata.word_count:,}")
        print(f"   - 예상 읽기 시간: {response.metadata.estimated_reading_time}")
        print(f"   - 심각도: {response.metadata.severity}")
        print(f"   - 근본 원인: {response.metadata.root_cause[:50]}...")
        print(f"   - 생성 시간: {response.metadata.generation_time}초")
        print(f"   - HTML 유효성: {response.validation_status.is_valid_html}")

        # Check required tags
        html_lower = response.html_content.lower()
        if "<!doctype html>" in html_lower:
            print(f"   ✓ DOCTYPE 선언 포함")
        if "에러 분석 리포트" in response.html_content:
            print(f"   ✓ 제목 바인딩 성공")
        if "12345" in response.html_content:
            print(f"   ✓ 로그 ID 바인딩 성공")
        if "NullPointerException" in response.html_content:
            print(f"   ✓ 에러 메시지 바인딩 성공")

        # Save to file
        output_file = Path(__file__).parent / "test_output_error.html"
        output_file.write_text(response.html_content, encoding="utf-8")
        print(f"\n📄 HTML 파일 저장: {output_file}")

        return True

    except Exception as e:
        print(f"❌ 테스트 실패: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


async def test_health_score_calculation():
    """Test health score calculation algorithm"""
    print("\n" + "=" * 80)
    print("TEST 3: Health Score Calculation")
    print("=" * 80)

    service = html_document_service

    # Good health
    score1 = service._calculate_health_score(10000, 10, 100)
    print(f"양호 (10/10000 errors): {score1}/100 {'✓' if score1 > 80 else '✗'}")

    # Medium health
    score2 = service._calculate_health_score(10000, 200, 1000)
    print(f"보통 (200/10000 errors): {score2}/100 {'✓' if 40 < score2 < 80 else '✗'}")

    # Poor health
    score3 = service._calculate_health_score(1000, 500, 300)
    print(f"불량 (500/1000 errors): {score3}/100 {'✓' if score3 < 40 else '✗'}")

    # Edge case: no logs
    score4 = service._calculate_health_score(0, 0, 0)
    print(f"로그 없음: {score4}/100 {'✓' if score4 == 100 else '✗'}")

    return True


async def test_dark_mode():
    """Test dark mode option"""
    print("\n" + "=" * 80)
    print("TEST 4: Dark Mode Option")
    print("=" * 80)

    request = AiHtmlDocumentRequest(
        project_uuid="test-uuid",
        document_type=DocumentType.PROJECT_ANALYSIS,
        format=DocumentFormat.HTML,
        data={
            "projectInfo": {"name": "Test", "uuid": "test", "description": "Test"},
            "metrics": {"totalLogs": 100, "errorCount": 10, "warnCount": 20, "infoCount": 70},
        },
        options={"darkMode": True, "includeCharts": False},
    )

    try:
        response = await html_document_service.generate_html_document(request)

        if "#1a1a1a" in response.html_content:
            print(f"✅ 다크모드 스타일 적용 확인")
            return True
        else:
            print(f"❌ 다크모드 스타일 미적용")
            return False

    except Exception as e:
        print(f"❌ 테스트 실패: {str(e)}")
        return False


async def main():
    """Run all tests"""
    print("\n🧪 HTML 문서 생성 기능 테스트 시작\n")

    results = []

    # Run tests
    results.append(("프로젝트 분석 HTML 생성", await test_project_analysis()))
    results.append(("에러 분석 HTML 생성", await test_error_analysis()))
    results.append(("건강 점수 계산", await test_health_score_calculation()))
    results.append(("다크모드 옵션", await test_dark_mode()))

    # Summary
    print("\n" + "=" * 80)
    print("테스트 결과 요약")
    print("=" * 80)

    passed = sum(1 for _, result in results if result)
    total = len(results)

    for name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{status} - {name}")

    print("\n" + "=" * 80)
    print(f"총 {total}개 테스트 중 {passed}개 통과 ({passed/total*100:.1f}%)")
    print("=" * 80)

    if passed == total:
        print("\n🎉 모든 테스트 통과!")
    else:
        print(f"\n⚠️  {total - passed}개 테스트 실패")

    print("\n📄 생성된 파일:")
    print("   - test_output_project.html (프로젝트 분석)")
    print("   - test_output_error.html (에러 분석)")
    print("\n브라우저에서 열어서 HTML 렌더링을 확인하세요!")


if __name__ == "__main__":
    asyncio.run(main())
