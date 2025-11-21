"""
10개 신규 도구 통합 테스트 스크립트
"""
import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(__file__))

print("=" * 80)
print("10개 신규 도구 통합 테스트")
print("=" * 80)

# 환경변수 설정 (import 오류 방지)
os.environ["OPENAI_API_KEY"] = "dummy-key"
os.environ["OPENAI_BASE_URL"] = "https://api.openai.com/v1"
os.environ["OPENSEARCH_HOST"] = "localhost"
os.environ["OPENSEARCH_PORT"] = "9200"

try:
    print("\n1. monitoring_tools 모듈 import...")
    from app.tools.monitoring_tools import (
        get_error_rate_trend,
        get_service_health_status,
        get_error_frequency_ranking,
        get_api_error_rates,
        get_affected_users_count
    )
    print("   ✅ monitoring_tools import 성공 (5개 도구)")

    print("\n2. comparison_tools 모듈 import...")
    from app.tools.comparison_tools import (
        compare_time_periods,
        detect_cascading_failures
    )
    print("   ✅ comparison_tools import 성공 (2개 도구)")

    print("\n3. alert_tools 모듈 import...")
    from app.tools.alert_tools import (
        evaluate_alert_conditions,
        detect_resource_issues
    )
    print("   ✅ alert_tools import 성공 (2개 도구)")

    print("\n4. deployment_tools 모듈 import...")
    from app.tools.deployment_tools import analyze_deployment_impact
    print("   ✅ deployment_tools import 성공 (1개 도구)")

    print("\n5. chatbot_agent 모듈 import...")
    from app.agents.chatbot_agent import create_log_analysis_agent
    print("   ✅ chatbot_agent import 성공")

    print("\n6. Agent 생성 및 도구 확인...")
    agent = create_log_analysis_agent("test_uuid")
    tool_names = [tool.name for tool in agent.tools]
    print(f"   Agent 도구 목록 ({len(tool_names)}개):")
    print(f"   {', '.join(tool_names)}")

    print("\n7. 신규 10개 도구 등록 확인...")
    new_tools = [
        "get_error_rate_trend",
        "get_service_health_status",
        "get_error_frequency_ranking",
        "get_api_error_rates",
        "get_affected_users_count",
        "compare_time_periods",
        "detect_cascading_failures",
        "evaluate_alert_conditions",
        "detect_resource_issues",
        "analyze_deployment_impact"
    ]

    missing_tools = []
    for tool in new_tools:
        if tool in tool_names:
            print(f"   ✅ {tool}")
        else:
            print(f"   ❌ {tool} - 미등록")
            missing_tools.append(tool)

    if missing_tools:
        print(f"\n❌ {len(missing_tools)}개 도구가 등록되지 않았습니다: {', '.join(missing_tools)}")
        sys.exit(1)

    print("\n8. 도구 메타데이터 샘플 확인...")
    print(f"\n   [get_error_rate_trend]")
    print(f"   name: {get_error_rate_trend.name}")
    print(f"   description: {get_error_rate_trend.description[:100]}...")

    print(f"\n   [get_service_health_status]")
    print(f"   name: {get_service_health_status.name}")
    print(f"   description: {get_service_health_status.description[:100]}...")

    print(f"\n   [detect_cascading_failures]")
    print(f"   name: {detect_cascading_failures.name}")
    print(f"   description: {detect_cascading_failures.description[:100]}...")

    print("\n" + "=" * 80)
    print("✅ 모든 테스트 통과!")
    print("=" * 80)
    print(f"\n📊 통합 결과:")
    print(f"  - 기존 도구: 8개")
    print(f"  - 신규 도구: 10개")
    print(f"  - 총 도구: {len(tool_names)}개")
    print(f"\n🎉 Chatbot V2 Agent가 이제 {len(tool_names)}개의 도구를 사용할 수 있습니다!")
    print("\n📝 신규 도구로 답변 가능한 질문 예시:")
    print("  1. 에러율이 증가하고 있나요? (get_error_rate_trend)")
    print("  2. user-service가 정상인가요? (get_service_health_status)")
    print("  3. 가장 자주 발생하는 에러는? (get_error_frequency_ranking)")
    print("  4. 가장 에러가 많은 API는? (get_api_error_rates)")
    print("  5. 몇 명의 사용자가 영향받았나요? (get_affected_users_count)")
    print("  6. 오늘이 어제보다 에러가 많나요? (compare_time_periods)")
    print("  7. 연쇄 장애가 있나요? (detect_cascading_failures)")
    print("  8. 알림이 필요한 상황인가요? (evaluate_alert_conditions)")
    print("  9. 메모리 부족 에러가 있나요? (detect_resource_issues)")
    print(" 10. 배포 이후 에러가 증가했나요? (analyze_deployment_impact)")

except Exception as e:
    print(f"\n❌ 테스트 실패: {str(e)}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
