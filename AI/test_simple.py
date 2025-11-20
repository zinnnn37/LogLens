"""
Simple verification script - checks files exist and templates are valid
No external dependencies required except jinja2
"""

from pathlib import Path
import sys

def check_files_exist():
    """Check that all required files exist"""
    print("=" * 80)
    print("파일 존재 여부 확인")
    print("=" * 80)

    base_path = Path(__file__).parent
    required_files = [
        "app/services/html_document_service.py",
        "app/models/document.py",
        "app/api/v2_langgraph/analysis.py",
        "app/templates/analysis/project_analysis.html",
        "app/templates/analysis/error_analysis.html",
        "app/tests/__init__.py",
        "app/tests/test_html_document_service.py",
        "app/tests/test_analysis_api.py",
        "app/tests/test_templates.py",
        "requirements.txt",
    ]

    all_exist = True
    for file_path in required_files:
        full_path = base_path / file_path
        exists = full_path.exists()
        status = "✅" if exists else "❌"
        print(f"{status} {file_path}")
        if not exists:
            all_exist = False

    return all_exist


def check_jinja2_templates():
    """Check that Jinja2 templates are valid"""
    print("\n" + "=" * 80)
    print("Jinja2 템플릿 검증")
    print("=" * 80)

    try:
        from jinja2 import Environment, FileSystemLoader, TemplateSyntaxError

        template_dir = Path(__file__).parent / "app" / "templates"
        env = Environment(
            loader=FileSystemLoader(template_dir),
            autoescape=True,
        )

        # Register custom filters (same as in HtmlDocumentService)
        def format_number(value):
            if isinstance(value, (int, float)):
                return f"{value:,}"
            return str(value)

        def format_percentage(value, total):
            if total == 0:
                return "0%"
            percentage = (value / total) * 100
            return f"{percentage:.1f}%"

        env.filters["format_number"] = format_number
        env.filters["format_percentage"] = format_percentage

        # Test project analysis template
        try:
            project_template = env.get_template("analysis/project_analysis.html")
            print("✅ project_analysis.html - 템플릿 로딩 성공")

            # Try rendering with minimal context
            html = project_template.render(
                project_info={"name": "Test", "uuid": "test", "description": "Test"},
                time_range={},
                metrics={"totalLogs": 1000, "errorCount": 10, "warnCount": 50, "infoCount": 940, "avgResponseTime": 100.5},
                top_errors=[],
                options={},
                style={"dark_mode": False, "css_framework": "tailwind", "chart_library": "chartjs", "color_scheme": "blue"},
            )
            print(f"   - 렌더링 성공 ({len(html):,} 문자)")

            # Check for required tags
            if all(tag in html.lower() for tag in ["<!doctype html>", "<html", "<body"]):
                print(f"   - 필수 HTML 태그 확인 ✓")

            # Check Tailwind CSS
            if "cdn.tailwindcss.com" in html:
                print(f"   - Tailwind CSS CDN 확인 ✓")

        except TemplateSyntaxError as e:
            print(f"❌ project_analysis.html - 템플릿 문법 오류: {e}")
            return False
        except Exception as e:
            print(f"❌ project_analysis.html - 렌더링 오류: {e}")
            import traceback
            traceback.print_exc()
            return False

        # Test error analysis template
        try:
            error_template = env.get_template("analysis/error_analysis.html")
            print("✅ error_analysis.html - 템플릿 로딩 성공")

            # Try rendering with minimal context
            html = error_template.render(
                log_id=123,
                error_log={"level": "ERROR", "message": "Test error", "timestamp": "2024-01-01T00:00:00"},
                existing_analysis={},
                related_logs=[],
                options={},
                style={"dark_mode": False, "css_framework": "tailwind", "chart_library": "chartjs", "color_scheme": "blue"},
            )
            print(f"   - 렌더링 성공 ({len(html):,} 문자)")

            # Check for required tags
            if all(tag in html.lower() for tag in ["<!doctype html>", "<html", "<body"]):
                print(f"   - 필수 HTML 태그 확인 ✓")

            # Check error report title
            if "에러 분석 리포트" in html:
                print(f"   - 에러 분석 제목 확인 ✓")

        except TemplateSyntaxError as e:
            print(f"❌ error_analysis.html - 템플릿 문법 오류: {e}")
            return False
        except Exception as e:
            print(f"❌ error_analysis.html - 렌더링 오류: {e}")
            import traceback
            traceback.print_exc()
            return False

        return True

    except ImportError:
        print("❌ jinja2 모듈이 설치되어 있지 않습니다")
        print("   설치 방법: pip install jinja2")
        return False


def check_requirements():
    """Check requirements.txt includes jinja2"""
    print("\n" + "=" * 80)
    print("의존성 확인 (requirements.txt)")
    print("=" * 80)

    req_file = Path(__file__).parent / "requirements.txt"
    if req_file.exists():
        content = req_file.read_text()
        if "jinja2" in content.lower():
            print("✅ jinja2 의존성 포함됨")
            return True
        else:
            print("❌ jinja2 의존성 누락")
            return False
    else:
        print("❌ requirements.txt 파일 없음")
        return False


def check_test_files():
    """Check test file structure"""
    print("\n" + "=" * 80)
    print("테스트 파일 분석")
    print("=" * 80)

    test_files = {
        "test_html_document_service.py": "서비스 단위 테스트",
        "test_analysis_api.py": "API 엔드포인트 테스트",
        "test_templates.py": "템플릿 렌더링 테스트",
    }

    base_path = Path(__file__).parent / "app" / "tests"

    for filename, description in test_files.items():
        file_path = base_path / filename
        if file_path.exists():
            content = file_path.read_text()
            test_count = content.count("def test_")
            print(f"✅ {filename}")
            print(f"   - {description}")
            print(f"   - 테스트 함수 {test_count}개")
        else:
            print(f"❌ {filename} - 파일 없음")


def main():
    """Run all checks"""
    print("\n🔍 HTML 문서 생성 기능 - 파일 및 구조 검증\n")

    results = []

    results.append(("파일 존재 확인", check_files_exist()))
    results.append(("Jinja2 템플릿 검증", check_jinja2_templates()))
    results.append(("requirements.txt 확인", check_requirements()))

    # This doesn't return a result
    check_test_files()

    # Summary
    print("\n" + "=" * 80)
    print("검증 결과 요약")
    print("=" * 80)

    passed = sum(1 for _, result in results if result)
    total = len(results)

    for name, result in results:
        status = "✅ PASS" if result else "❌ FAIL"
        print(f"{status} - {name}")

    print("\n" + "=" * 80)
    print(f"총 {total}개 검증 중 {passed}개 통과 ({passed/total*100:.1f}%)")
    print("=" * 80)

    if passed == total:
        print("\n🎉 모든 검증 통과!")
        print("\n📝 다음 단계:")
        print("   1. 의존성 설치: pip install -r requirements.txt")
        print("   2. pytest 설치: pip install pytest pytest-asyncio httpx")
        print("   3. 테스트 실행: pytest app/tests/ -v")
        print("   4. 또는 서버 실행: uvicorn app.main:app --reload")
        print("   5. API 문서 확인: http://localhost:8000/docs")
    else:
        print(f"\n⚠️  {total - passed}개 검증 실패")


if __name__ == "__main__":
    main()
