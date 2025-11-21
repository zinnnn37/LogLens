"""
EC2의 평면 구조 데이터로 performance_tools.py 테스트
"""
import asyncio
from app.tools.performance_tools import get_slowest_apis, get_traffic_by_time

# EC2에서 사용 중인 project_uuid
PROJECT_UUID = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e"


async def test_slowest_apis():
    """응답 시간이 가장 느린 API 조회 테스트"""
    print("=" * 80)
    print("테스트 1: get_slowest_apis (평면 구조 지원)")
    print("=" * 80)

    result = await get_slowest_apis.ainvoke({
        "project_uuid": PROJECT_UUID,
        "limit": 10,
        "time_hours": 168  # 7일
    })

    print(result)
    print("\n")


async def test_traffic_by_time():
    """시간대별 트래픽 조회 테스트"""
    print("=" * 80)
    print("테스트 2: get_traffic_by_time")
    print("=" * 80)

    result = await get_traffic_by_time.ainvoke({
        "project_uuid": PROJECT_UUID,
        "interval": "1h",
        "time_hours": 24
    })

    print(result)
    print("\n")


async def main():
    """메인 테스트 실행"""
    print("\n🔍 EC2 평면 구조 데이터 테스트 시작\n")

    try:
        await test_slowest_apis()
        await test_traffic_by_time()

        print("\n✅ 모든 테스트 완료!")
        print("\n기대 결과:")
        print("1. get_slowest_apis: CorsFilter.doFilter, UserService.createUser 등 표시")
        print("2. execution_time 필드(9ms 등)를 성공적으로 읽어야 함")
        print("3. API 경로가 아닌 class_name.method_name 형식으로 그룹화")

    except Exception as e:
        print(f"\n❌ 테스트 실패: {str(e)}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    asyncio.run(main())
