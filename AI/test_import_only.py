"""
간단한 import 테스트 (환경변수 불필요)
"""
import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(__file__))

print("=" * 80)
print("성능 분석 도구 Import 테스트")
print("=" * 80)

# 환경변수 설정 (import 오류 방지)
os.environ["OPENAI_API_KEY"] = "dummy-key"
os.environ["OPENAI_BASE_URL"] = "https://api.openai.com/v1"
os.environ["OPENSEARCH_HOST"] = "localhost"
os.environ["OPENSEARCH_PORT"] = "9200"

try:
    print("\n1. performance_tools 모듈 import...")
    from app.tools.performance_tools import get_slowest_apis, get_traffic_by_time
    print("   ✅ performance_tools import 성공")

    print("\n2. 도구 메타데이터 확인...")
    print(f"   - get_slowest_apis.name: {get_slowest_apis.name}")
    print(f"   - get_slowest_apis.description: {get_slowest_apis.description[:100]}...")
    print(f"   - get_traffic_by_time.name: {get_traffic_by_time.name}")
    print(f"   - get_traffic_by_time.description: {get_traffic_by_time.description[:100]}...")
    print("   ✅ 도구 메타데이터 정상")

    print("\n3. chatbot_agent 모듈 import...")
    from app.agents.chatbot_agent import create_log_analysis_agent
    print("   ✅ chatbot_agent import 성공")

    print("\n4. Agent 생성 및 도구 확인...")
    agent = create_log_analysis_agent("test_uuid")
    tool_names = [tool.name for tool in agent.tools]
    print(f"   Agent 도구 목록: {tool_names}")

    print("\n5. 성능 도구 등록 확인...")
    assert "get_slowest_apis" in tool_names, "❌ get_slowest_apis 미등록"
    assert "get_traffic_by_time" in tool_names, "❌ get_traffic_by_time 미등록"
    print("   ✅ 성능 도구가 Agent에 정상 등록됨")

    print("\n" + "=" * 80)
    print("✅ 모든 Import 테스트 통과")
    print("=" * 80)

except Exception as e:
    print(f"\n❌ 테스트 실패: {str(e)}")
    import traceback
    traceback.print_exc()
    sys.exit(1)
