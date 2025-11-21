"""
성능 분석 도구 테스트 스크립트
"""
import asyncio
import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(__file__))

from app.tools.performance_tools import get_slowest_apis, get_traffic_by_time
from app.core.config import settings

# 테스트용 프로젝트 UUID (실제 사용 중인 프로젝트 UUID로 변경 필요)
TEST_PROJECT_UUID = "test_project_uuid"


async def test_slowest_apis():
    """get_slowest_apis 도구 테스트"""
    print("=" * 80)
    print("TEST 1: get_slowest_apis")
    print("=" * 80)

    try:
        result = await get_slowest_apis.ainvoke({
            "project_uuid": TEST_PROJECT_UUID,
            "limit": 5,
            "time_hours": 168
        })
        print(result)
        print("\n✅ get_slowest_apis 테스트 성공\n")
    except Exception as e:
        print(f"\n❌ get_slowest_apis 테스트 실패: {str(e)}\n")


async def test_traffic_by_time():
    """get_traffic_by_time 도구 테스트"""
    print("=" * 80)
    print("TEST 2: get_traffic_by_time")
    print("=" * 80)

    try:
        result = await get_traffic_by_time.ainvoke({
            "project_uuid": TEST_PROJECT_UUID,
            "interval": "1h",
            "time_hours": 24
        })
        print(result)
        print("\n✅ get_traffic_by_time 테스트 성공\n")
    except Exception as e:
        print(f"\n❌ get_traffic_by_time 테스트 실패: {str(e)}\n")


async def test_agent_integration():
    """Agent 통합 테스트"""
    print("=" * 80)
    print("TEST 3: Agent Integration")
    print("=" * 80)

    try:
        from app.agents.chatbot_agent import create_log_analysis_agent

        # Agent 생성
        agent = create_log_analysis_agent(TEST_PROJECT_UUID)

        # Agent에 등록된 도구 확인
        tool_names = [tool.name for tool in agent.tools]
        print(f"Agent에 등록된 도구: {tool_names}")

        # 성능 도구가 포함되어 있는지 확인
        assert "get_slowest_apis" in tool_names, "get_slowest_apis가 Agent에 등록되지 않음"
        assert "get_traffic_by_time" in tool_names, "get_traffic_by_time가 Agent에 등록되지 않음"

        print("✅ Agent 통합 테스트 성공 - 성능 도구가 정상적으로 등록됨\n")

    except Exception as e:
        print(f"❌ Agent 통합 테스트 실패: {str(e)}\n")


async def main():
    """메인 테스트 함수"""
    print("\n")
    print("🔧 성능 분석 도구 테스트 시작")
    print(f"OpenSearch Host: {settings.OPENSEARCH_HOST}")
    print(f"OpenSearch Port: {settings.OPENSEARCH_PORT}")
    print(f"Test Project UUID: {TEST_PROJECT_UUID}")
    print("\n")

    # 통합 테스트 (Agent 도구 등록 확인)
    await test_agent_integration()

    # 개별 도구 테스트 (실제 데이터가 있는 경우에만 성공)
    print("\n⚠️  참고: 아래 테스트는 실제 OpenSearch 데이터가 필요합니다.")
    print("   데이터가 없으면 '데이터가 없습니다' 메시지가 정상 응답입니다.\n")

    await test_slowest_apis()
    await test_traffic_by_time()

    print("=" * 80)
    print("✅ 모든 테스트 완료")
    print("=" * 80)


if __name__ == "__main__":
    asyncio.run(main())
