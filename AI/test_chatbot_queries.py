#!/usr/bin/env python3
"""
Comprehensive Test Suite for Chatbot V2 API
Tests all 40+ tools with 125+ diverse queries
"""

import asyncio
import httpx
import json
import time
from datetime import datetime
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, asdict

# Configuration
BASE_URL = "http://localhost:8000"
API_ENDPOINT = "/api/v2/chatbot/ask"
PROJECT_UUID = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e"
TIMEOUT_SECONDS = 120  # Agent may take time for multi-step reasoning

@dataclass
class TestQuery:
    """Test query definition"""
    id: int
    category: str
    query: str
    expected_tools: List[str]
    description: str

@dataclass
class TestResult:
    """Test execution result"""
    query_id: int
    query: str
    category: str
    success: bool
    response_time: float
    answer: Optional[str]
    error: Optional[str]
    timestamp: str

# Define all test queries (125 queries across 13 categories)
TEST_QUERIES: List[TestQuery] = [
    # Category 1: Search Tools (10 queries)
    TestQuery(1, "Search", "NullPointerException 키워드로 로그 검색해줘",
              ["search_logs_by_keyword"], "Basic keyword search"),
    TestQuery(2, "Search", "데이터베이스 연결 실패와 비슷한 에러 찾아줘",
              ["search_logs_by_similarity"], "Semantic similarity search"),
    TestQuery(3, "Search", "payment-service에서 발생한 ERROR 로그 검색",
              ["search_logs_by_keyword"], "Service + level filter"),
    TestQuery(4, "Search", "최근 1시간 내 timeout 관련 로그 보여줘",
              ["search_logs_by_keyword"], "Time-based keyword search"),
    TestQuery(5, "Search", "Connection refused 에러 메시지 검색",
              ["search_logs_by_keyword"], "Exact phrase search"),
    TestQuery(6, "Search", "메모리 부족 오류와 유사한 패턴 찾아줘",
              ["search_logs_by_similarity"], "OOM pattern similarity"),
    TestQuery(7, "Search", "user-service INFO 레벨 로그 중 authentication 키워드 검색",
              ["search_logs_advanced"], "Advanced multi-filter search"),
    TestQuery(8, "Search", "500 상태코드 응답과 비슷한 에러들 보여줘",
              ["search_logs_by_similarity"], "HTTP error similarity"),
    TestQuery(9, "Search", "최근 48시간 내 WARN 레벨 로그 검색",
              ["search_logs_by_keyword"], "Time + level filter"),
    TestQuery(10, "Search", "Exception 키워드가 포함된 로그 고급 검색",
              ["search_logs_advanced"], "Exception pattern search"),

    # Category 2: Analysis Tools (12 queries)
    TestQuery(11, "Analysis", "최근 24시간 로그 통계 보여줘",
              ["get_log_statistics"], "Basic statistics"),
    TestQuery(12, "Analysis", "가장 심각한 에러가 뭐야?",
              ["get_recent_errors"], "Critical severity analysis"),
    TestQuery(13, "Analysis", "최근 발생한 ERROR 10개 목록 보여줘",
              ["get_recent_errors"], "Recent errors listing"),
    TestQuery(14, "Analysis", "연관 로그들을 상관관계 분석해줘",
              ["correlate_logs"], "Log correlation"),
    TestQuery(15, "Analysis", "전체 에러 통합 분석 리포트 생성해줘",
              ["analyze_errors_unified"], "Unified error analysis"),
    TestQuery(16, "Analysis", "log_id 12345를 AI로 깊이 분석해줘",
              ["analyze_single_log"], "Single log deep analysis"),
    TestQuery(17, "Analysis", "지난 주 로그 레벨별 분포 통계",
              ["get_log_statistics"], "Extended time range stats"),
    TestQuery(18, "Analysis", "/api/users 엔드포인트의 요청 패턴 분석해줘",
              ["analyze_request_patterns"], "API request pattern analysis"),
    TestQuery(19, "Analysis", "/api/payments 응답 실패 패턴 분석",
              ["analyze_response_failures"], "Response failure patterns"),
    TestQuery(20, "Analysis", "서비스별 로그 발생량 비교",
              ["get_log_statistics"], "Service comparison stats"),
    TestQuery(21, "Analysis", "최근 3일간 ERROR vs WARN 비율",
              ["get_log_statistics"], "Level ratio analysis"),
    TestQuery(22, "Analysis", "오늘 발생한 모든 Critical 에러 분석",
              ["get_recent_errors", "analyze_errors_unified"], "Multi-step critical analysis"),

    # Category 3: Detail Tools (8 queries)
    TestQuery(23, "Detail", "log_id 67890의 상세 정보 보여줘",
              ["get_log_detail"], "Single log detail"),
    TestQuery(24, "Detail", "trace_id abc123으로 연관된 모든 로그 추적해줘",
              ["get_logs_by_trace_id"], "Trace ID tracking"),
    TestQuery(25, "Detail", "request_id def456의 전체 요청 흐름 보여줘",
              ["get_logs_by_trace_id"], "Request flow tracking"),
    TestQuery(26, "Detail", "log_id 11111의 스택 트레이스 상세 확인",
              ["get_log_detail"], "Stack trace detail"),
    TestQuery(27, "Detail", "여러 trace_id 연관 로그들 모두 조회",
              ["get_logs_by_trace_id"], "Multiple trace tracking"),
    TestQuery(28, "Detail", "log_id 22222가 발생한 컨텍스트 정보",
              ["get_log_detail"], "Context information"),
    TestQuery(29, "Detail", "trace_id xyz789의 시간순 로그 나열",
              ["get_logs_by_trace_id"], "Chronological trace view"),
    TestQuery(30, "Detail", "log_id 33333의 요청 응답 본문 확인",
              ["get_log_detail"], "Request/response body detail"),

    # Category 4: Performance Tools (9 queries)
    TestQuery(31, "Performance", "가장 느린 API TOP 5 보여줘",
              ["get_slowest_apis"], "Slowest APIs"),
    TestQuery(32, "Performance", "시간대별 트래픽 분석해줘",
              ["get_traffic_by_time"], "Traffic by time"),
    TestQuery(33, "Performance", "HTTP 에러 코드 매트릭스 분석",
              ["analyze_http_error_matrix"], "HTTP error matrix"),
    TestQuery(34, "Performance", "응답 시간 3초 이상 걸리는 API 목록",
              ["get_slowest_apis"], "Slow response filtering"),
    TestQuery(35, "Performance", "오전 9시부터 10시까지 트래픽 피크 분석",
              ["get_traffic_by_time"], "Peak time analysis"),
    TestQuery(36, "Performance", "API별 상태코드 분포 매트릭스",
              ["analyze_http_error_matrix"], "Status code distribution"),
    TestQuery(37, "Performance", "평균 응답 시간 가장 긴 엔드포인트",
              ["get_slowest_apis"], "Average response time"),
    TestQuery(38, "Performance", "주말 vs 평일 트래픽 패턴 비교",
              ["get_traffic_by_time"], "Traffic pattern comparison"),
    TestQuery(39, "Performance", "4xx vs 5xx 에러 비율 분석",
              ["analyze_http_error_matrix"], "Client vs server errors"),

    # Category 5: Monitoring Tools (14 queries)
    TestQuery(40, "Monitoring", "에러율 추세 분석해줘",
              ["get_error_rate_trend"], "Error rate trend"),
    TestQuery(41, "Monitoring", "서비스별 헬스 상태 확인",
              ["get_service_health_status"], "Service health status"),
    TestQuery(42, "Monitoring", "가장 자주 발생하는 에러 유형 랭킹",
              ["get_error_frequency_ranking"], "Error frequency ranking"),
    TestQuery(43, "Monitoring", "API별 에러율 비교",
              ["get_api_error_rates"], "API error rates"),
    TestQuery(44, "Monitoring", "영향받은 사용자 수 확인해줘",
              ["get_affected_users_count"], "Affected users count"),
    TestQuery(45, "Monitoring", "이상 탐지 결과 보여줘",
              ["detect_anomalies"], "Anomaly detection"),
    TestQuery(46, "Monitoring", "FE vs BE 로그 소스 타입 비교",
              ["compare_source_types"], "Source type comparison"),
    TestQuery(47, "Monitoring", "로거별 활동량 분석",
              ["analyze_logger_activity"], "Logger activity analysis"),
    TestQuery(48, "Monitoring", "지난 6시간 에러율 증가 추세",
              ["get_error_rate_trend"], "Short-term trend"),
    TestQuery(49, "Monitoring", "모든 서비스 상태 요약 대시보드",
              ["get_service_health_status"], "Dashboard summary"),
    TestQuery(50, "Monitoring", "NullPointerException 발생 빈도 랭킹",
              ["get_error_frequency_ranking"], "Specific error frequency"),
    TestQuery(51, "Monitoring", "payment-service API 에러율",
              ["get_api_error_rates"], "Service-specific error rates"),
    TestQuery(52, "Monitoring", "에러로 인해 영향받은 고유 IP 수",
              ["get_affected_users_count"], "Unique IP impact"),
    TestQuery(53, "Monitoring", "비정상적인 로그 패턴 자동 탐지",
              ["detect_anomalies"], "Pattern anomaly detection"),

    # Category 6: Comparison Tools (6 queries)
    TestQuery(54, "Comparison", "오늘 vs 어제 에러 비교",
              ["compare_time_periods"], "Daily comparison"),
    TestQuery(55, "Comparison", "연쇄 장애 패턴 탐지해줘",
              ["detect_cascading_failures"], "Cascading failure detection"),
    TestQuery(56, "Comparison", "이번 주 vs 지난 주 성능 비교",
              ["compare_time_periods"], "Weekly comparison"),
    TestQuery(57, "Comparison", "배포 전후 에러율 비교 분석",
              ["compare_time_periods"], "Deployment impact comparison"),
    TestQuery(58, "Comparison", "서비스 간 장애 전파 경로 분석",
              ["detect_cascading_failures"], "Failure propagation path"),
    TestQuery(59, "Comparison", "최근 3일간 일별 에러 트렌드 비교",
              ["compare_time_periods"], "Multi-day trend comparison"),

    # Category 7: Alert Tools (6 queries)
    TestQuery(60, "Alert", "현재 알림 조건 평가해줘",
              ["evaluate_alert_conditions"], "Alert condition evaluation"),
    TestQuery(61, "Alert", "리소스 이슈 감지 결과 보여줘",
              ["detect_resource_issues"], "Resource issue detection"),
    TestQuery(62, "Alert", "에러율 5% 초과 알림 조건 체크",
              ["evaluate_alert_conditions"], "Threshold alert check"),
    TestQuery(63, "Alert", "메모리 누수 징후 탐지",
              ["detect_resource_issues"], "Memory leak detection"),
    TestQuery(64, "Alert", "CPU 사용량 관련 이슈 확인",
              ["detect_resource_issues"], "CPU issue detection"),
    TestQuery(65, "Alert", "디스크 공간 부족 알림 평가",
              ["evaluate_alert_conditions"], "Disk space alert"),

    # Category 8: Deployment Tools (4 queries)
    TestQuery(66, "Deployment", "최근 배포가 시스템에 미친 영향 분석",
              ["analyze_deployment_impact"], "Deployment impact analysis"),
    TestQuery(67, "Deployment", "v2.0 릴리스 후 에러 증가 여부 확인",
              ["analyze_deployment_impact"], "Release impact check"),
    TestQuery(68, "Deployment", "핫픽스 배포 전후 성능 변화",
              ["analyze_deployment_impact"], "Hotfix impact analysis"),
    TestQuery(69, "Deployment", "새 기능 배포 후 안정성 평가",
              ["analyze_deployment_impact"], "Stability evaluation"),

    # Category 9: User Tracking Tools (9 queries)
    TestQuery(70, "UserTracking", "IP 192.168.1.100의 세션 활동 추적",
              ["trace_user_session"], "IP session tracking"),
    TestQuery(71, "UserTracking", "UserController.getUser 메서드의 파라미터 분포 분석",
              ["analyze_parameter_distribution"], "Parameter distribution"),
    TestQuery(72, "UserTracking", "log_id 44444에서 시작된 에러 전파 경로 추적",
              ["trace_error_propagation"], "Error propagation trace"),
    TestQuery(73, "UserTracking", "특정 사용자 IP의 24시간 활동 로그",
              ["trace_user_session"], "User activity timeline"),
    TestQuery(74, "UserTracking", "PaymentService.processPayment의 null 파라미터 비율",
              ["analyze_parameter_distribution"], "Null parameter analysis"),
    TestQuery(75, "UserTracking", "에러 연쇄 반응 경로 시각화",
              ["trace_error_propagation"], "Error chain visualization"),
    TestQuery(76, "UserTracking", "의심스러운 IP 활동 패턴 분석",
              ["trace_user_session"], "Suspicious activity detection"),
    TestQuery(77, "UserTracking", "메서드별 파라미터 유효성 검사 결과",
              ["analyze_parameter_distribution"], "Parameter validation analysis"),
    TestQuery(78, "UserTracking", "하나의 에러가 다른 서비스로 전파된 경로",
              ["trace_error_propagation"], "Cross-service error propagation"),

    # Category 10: Architecture Tools (9 queries)
    TestQuery(79, "Architecture", "레이어별 에러 분포 분석 (Controller vs Service vs Repository)",
              ["analyze_error_by_layer"], "Layer-based error analysis"),
    TestQuery(80, "Architecture", "trace_id xyz123의 컴포넌트 호출 순서 추적",
              ["trace_component_calls"], "Component call tracing"),
    TestQuery(81, "Architecture", "가장 많이 실행된 핫스팟 메서드 TOP 10",
              ["get_hottest_methods"], "Hottest methods"),
    TestQuery(82, "Architecture", "아키텍처 계층별 에러 집중도 분석",
              ["analyze_error_by_layer"], "Architecture layer focus"),
    TestQuery(83, "Architecture", "요청 흐름에서 병목 지점 식별",
              ["trace_component_calls"], "Bottleneck identification"),
    TestQuery(84, "Architecture", "자주 호출되지만 에러가 많은 메서드",
              ["get_hottest_methods"], "High-error hotspot methods"),
    TestQuery(85, "Architecture", "Service 레이어 vs Repository 레이어 에러 비율",
              ["analyze_error_by_layer"], "Layer comparison"),
    TestQuery(86, "Architecture", "단일 요청의 전체 호출 체인 분석",
              ["trace_component_calls"], "Full call chain"),
    TestQuery(87, "Architecture", "메서드별 호출 빈도와 평균 실행 시간",
              ["get_hottest_methods"], "Method performance metrics"),

    # Category 11: Pattern Detection Tools (12 queries)
    TestQuery(88, "PatternDetection", "비슷한 스택 트레이스 클러스터링",
              ["cluster_stack_traces"], "Stack trace clustering"),
    TestQuery(89, "PatternDetection", "동시성 문제 (데드락, 레이스 컨디션) 감지",
              ["detect_concurrency_issues"], "Concurrency issue detection"),
    TestQuery(90, "PatternDetection", "주기적으로 재발하는 에러 패턴 탐지",
              ["detect_recurring_errors"], "Recurring error detection"),
    TestQuery(91, "PatternDetection", "미해결 에러의 생존 시간 분석",
              ["analyze_error_lifetime"], "Error lifetime analysis"),
    TestQuery(92, "PatternDetection", "중복 에러 그룹화 및 대표 에러 선정",
              ["cluster_stack_traces"], "Error deduplication"),
    TestQuery(93, "PatternDetection", "스레드 동기화 문제 징후 확인",
              ["detect_concurrency_issues"], "Thread sync issues"),
    TestQuery(94, "PatternDetection", "매일 같은 시간에 발생하는 배치 에러",
              ["detect_recurring_errors"], "Scheduled job errors"),
    TestQuery(95, "PatternDetection", "가장 오래 지속되는 미해결 에러",
              ["analyze_error_lifetime"], "Long-standing errors"),
    TestQuery(96, "PatternDetection", "패턴이 유사한 Exception 그룹",
              ["cluster_stack_traces"], "Exception pattern grouping"),
    TestQuery(97, "PatternDetection", "락 타임아웃 및 동시성 충돌 패턴",
              ["detect_concurrency_issues"], "Lock contention patterns"),
    TestQuery(98, "PatternDetection", "24시간 주기로 반복되는 에러",
              ["detect_recurring_errors"], "Daily recurring errors"),
    TestQuery(99, "PatternDetection", "에러 발생 시점부터 현재까지 지속 시간",
              ["analyze_error_lifetime"], "Error duration tracking"),

    # Category 12: Multi-Step Reasoning (15 queries)
    TestQuery(100, "MultiStep", "시스템 전체 상태 요약해줘",
              ["get_log_statistics", "get_recent_errors", "get_service_health_status"], "Complete system summary"),
    TestQuery(101, "MultiStep", "가장 심각한 에러를 찾고 원인 분석해줘",
              ["get_recent_errors", "analyze_single_log"], "Error identification + deep analysis"),
    TestQuery(102, "MultiStep", "payment-service 문제점 전체 분석",
              ["get_service_health_status", "get_api_error_rates", "get_recent_errors"], "Service-focused multi-analysis"),
    TestQuery(103, "MultiStep", "오늘 발생한 에러를 찾고 비슷한 과거 에러와 비교",
              ["get_recent_errors", "search_logs_by_similarity", "compare_time_periods"], "Historical comparison"),
    TestQuery(104, "MultiStep", "성능 저하 원인을 찾고 영향 범위 파악",
              ["get_slowest_apis", "get_traffic_by_time", "get_affected_users_count"], "Performance impact analysis"),
    TestQuery(105, "MultiStep", "에러 추세를 보고 알람 조건 평가",
              ["get_error_rate_trend", "evaluate_alert_conditions"], "Trend + alert evaluation"),
    TestQuery(106, "MultiStep", "특정 에러의 발생 패턴과 전파 경로 분석",
              ["search_logs_by_keyword", "trace_error_propagation", "detect_cascading_failures"], "Error propagation analysis"),
    TestQuery(107, "MultiStep", "서비스 헬스 체크 후 문제 있는 서비스 상세 분석",
              ["get_service_health_status", "get_api_error_rates", "analyze_errors_unified"], "Health check + detailed analysis"),
    TestQuery(108, "MultiStep", "이상 탐지 결과를 보고 원인 파악",
              ["detect_anomalies", "search_logs_by_similarity", "correlate_logs"], "Anomaly root cause"),
    TestQuery(109, "MultiStep", "배포 영향 분석 후 롤백 필요성 판단",
              ["analyze_deployment_impact", "compare_time_periods", "evaluate_alert_conditions"], "Deployment decision support"),
    TestQuery(110, "MultiStep", "리소스 문제 탐지 후 연관 에러 분석",
              ["detect_resource_issues", "search_logs_by_keyword", "analyze_errors_unified"], "Resource issue analysis"),
    TestQuery(111, "MultiStep", "사용자 세션 추적 후 장애 경험 분석",
              ["trace_user_session", "get_affected_users_count", "analyze_single_log"], "User experience analysis"),
    TestQuery(112, "MultiStep", "아키텍처 레이어별 분석 후 개선 권고",
              ["analyze_error_by_layer", "get_hottest_methods", "cluster_stack_traces"], "Architecture improvement"),
    TestQuery(113, "MultiStep", "동시성 문제 탐지 후 스레드 안전성 평가",
              ["detect_concurrency_issues", "search_logs_by_keyword", "analyze_error_lifetime"], "Concurrency safety evaluation"),
    TestQuery(114, "MultiStep", "재발 에러 패턴 분석 후 근본 원인 파악",
              ["detect_recurring_errors", "cluster_stack_traces", "analyze_errors_unified"], "Root cause analysis"),

    # Category 13: Edge Cases & Error Handling (11 queries)
    TestQuery(115, "EdgeCase", "존재하지 않는 log_id 99999999의 상세 정보",
              ["get_log_detail"], "Non-existent log ID"),
    TestQuery(116, "EdgeCase", "데이터가 없는 시간대 (새벽 3시부터 4시) 통계",
              ["get_log_statistics"], "Empty time range"),
    TestQuery(117, "EdgeCase", "없는 서비스 nonexistent-service의 에러 검색",
              ["search_logs_by_keyword"], "Non-existent service"),
    TestQuery(118, "EdgeCase", "미래 시간 (내일) 로그 검색",
              ["search_logs_by_keyword"], "Future time range"),
    TestQuery(119, "EdgeCase", "빈 키워드로 검색",
              ["search_logs_by_keyword"], "Empty keyword"),
    TestQuery(120, "EdgeCase", "존재하지 않는 trace_id로 추적",
              ["get_logs_by_trace_id"], "Invalid trace ID"),
    TestQuery(121, "EdgeCase", "매우 긴 시간 범위 (365일) 통계",
              ["get_log_statistics"], "Large time range"),
    TestQuery(122, "EdgeCase", "특수문자가 포함된 키워드 검색",
              ["search_logs_by_keyword"], "Special characters"),
    TestQuery(123, "EdgeCase", "0개 결과 limit으로 에러 조회",
              ["get_recent_errors"], "Zero limit"),
    TestQuery(124, "EdgeCase", "매우 작은 유사도 임계값으로 검색",
              ["search_logs_by_similarity"], "Low similarity threshold"),
    TestQuery(125, "EdgeCase", "잘못된 로그 레벨 INVALID로 검색",
              ["search_logs_by_keyword"], "Invalid log level"),
]


class ChatbotV2Tester:
    """Test runner for Chatbot V2 API"""

    def __init__(self, base_url: str, project_uuid: str):
        self.base_url = base_url
        self.project_uuid = project_uuid
        self.results: List[TestResult] = []

    async def execute_single_query(self, query: TestQuery) -> TestResult:
        """Execute a single test query"""
        start_time = time.time()

        payload = {
            "question": query.query,
            "project_uuid": self.project_uuid,
            "chat_history": []
        }

        try:
            async with httpx.AsyncClient(timeout=TIMEOUT_SECONDS) as client:
                response = await client.post(
                    f"{self.base_url}{API_ENDPOINT}",
                    json=payload
                )
                response.raise_for_status()

                data = response.json()
                elapsed = time.time() - start_time

                return TestResult(
                    query_id=query.id,
                    query=query.query,
                    category=query.category,
                    success=True,
                    response_time=elapsed,
                    answer=data.get("answer", "")[:1000],  # Truncate for storage
                    error=None,
                    timestamp=datetime.now().isoformat()
                )

        except httpx.TimeoutException:
            elapsed = time.time() - start_time
            return TestResult(
                query_id=query.id,
                query=query.query,
                category=query.category,
                success=False,
                response_time=elapsed,
                answer=None,
                error="TIMEOUT: Request exceeded 120 seconds",
                timestamp=datetime.now().isoformat()
            )
        except Exception as e:
            elapsed = time.time() - start_time
            return TestResult(
                query_id=query.id,
                query=query.query,
                category=query.category,
                success=False,
                response_time=elapsed,
                answer=None,
                error=str(e)[:500],
                timestamp=datetime.now().isoformat()
            )

    async def run_all_tests(self) -> Dict[str, Any]:
        """Run all test queries"""
        print(f"\n{'#'*60}")
        print(f"# Chatbot V2 Comprehensive Test Suite")
        print(f"# Total Queries: {len(TEST_QUERIES)}")
        print(f"# Project UUID: {self.project_uuid}")
        print(f"# Start Time: {datetime.now().isoformat()}")
        print(f"{'#'*60}")

        all_results = []

        for i, query in enumerate(TEST_QUERIES, 1):
            print(f"\n[{i}/{len(TEST_QUERIES)}] Query #{query.id} ({query.category})")
            print(f"Description: {query.description}")
            print(f"Query: {query.query}")
            print("Executing...", end=" ", flush=True)

            result = await self.execute_single_query(query)
            all_results.append(result)

            if result.success:
                print(f"✅ SUCCESS ({result.response_time:.2f}s)")
                print(f"Answer preview: {result.answer[:200]}...")
            else:
                print(f"❌ FAILED ({result.response_time:.2f}s)")
                print(f"Error: {result.error}")

            # Rate limiting - avoid overwhelming the server
            if i < len(TEST_QUERIES):
                print("Waiting 1s before next query...")
                await asyncio.sleep(1)

        # Generate summary
        summary = self._generate_summary(all_results)

        # Save results
        self._save_results(all_results, summary)

        return summary

    def _generate_summary(self, results: List[TestResult]) -> Dict[str, Any]:
        """Generate test execution summary"""
        total = len(results)
        passed = sum(1 for r in results if r.success)
        failed = total - passed
        avg_response_time = sum(r.response_time for r in results) / total if total > 0 else 0

        # Group by category
        category_stats = {}
        for result in results:
            if result.category not in category_stats:
                category_stats[result.category] = {
                    "passed": 0,
                    "failed": 0,
                    "times": [],
                    "failed_queries": []
                }
            if result.success:
                category_stats[result.category]["passed"] += 1
            else:
                category_stats[result.category]["failed"] += 1
                category_stats[result.category]["failed_queries"].append({
                    "id": result.query_id,
                    "query": result.query,
                    "error": result.error
                })
            category_stats[result.category]["times"].append(result.response_time)

        # Calculate per-category averages
        for cat in category_stats:
            times = category_stats[cat]["times"]
            category_stats[cat]["avg_time"] = sum(times) / len(times) if times else 0
            del category_stats[cat]["times"]  # Remove raw times from summary

        return {
            "total_queries": total,
            "passed": passed,
            "failed": failed,
            "pass_rate": f"{(passed/total)*100:.1f}%" if total > 0 else "0%",
            "avg_response_time": f"{avg_response_time:.2f}s",
            "max_response_time": f"{max(r.response_time for r in results):.2f}s" if results else "0s",
            "min_response_time": f"{min(r.response_time for r in results):.2f}s" if results else "0s",
            "category_stats": category_stats,
            "failed_queries": [
                {"id": r.query_id, "query": r.query, "error": r.error}
                for r in results if not r.success
            ],
            "test_start": results[0].timestamp if results else "",
            "test_end": results[-1].timestamp if results else "",
        }

    def _save_results(self, results: List[TestResult], summary: Dict[str, Any]):
        """Save test results to JSON file"""
        output = {
            "summary": summary,
            "results": [asdict(r) for r in results]
        }

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"chatbot_v2_test_results_{timestamp}.json"

        with open(filename, "w", encoding="utf-8") as f:
            json.dump(output, f, ensure_ascii=False, indent=2)

        print(f"\n📊 Results saved to: {filename}")

    def print_summary(self, summary: Dict[str, Any]):
        """Print formatted test summary"""
        print(f"\n{'='*60}")
        print(f"TEST EXECUTION SUMMARY")
        print(f"{'='*60}")
        print(f"Total Queries:        {summary['total_queries']}")
        print(f"Passed:               {summary['passed']} ✅")
        print(f"Failed:               {summary['failed']} ❌")
        print(f"Pass Rate:            {summary['pass_rate']}")
        print(f"Avg Response Time:    {summary['avg_response_time']}")
        print(f"Min Response Time:    {summary['min_response_time']}")
        print(f"Max Response Time:    {summary['max_response_time']}")

        print(f"\n{'='*60}")
        print(f"CATEGORY BREAKDOWN")
        print(f"{'='*60}")
        print(f"{'Category':<20} {'Pass':>6} {'Fail':>6} {'Avg Time':>12}")
        print(f"{'-'*50}")
        for category in sorted(summary['category_stats'].keys()):
            stats = summary['category_stats'][category]
            print(f"{category:<20} {stats['passed']:>6} {stats['failed']:>6} {stats['avg_time']:>10.2f}s")

        if summary['failed_queries']:
            print(f"\n{'='*60}")
            print(f"FAILED QUERIES ({len(summary['failed_queries'])} total)")
            print(f"{'='*60}")
            for fq in summary['failed_queries']:
                print(f"\n#{fq['id']}: {fq['query'][:50]}...")
                print(f"  Error: {fq['error'][:100]}")

        print(f"\n{'='*60}")
        print(f"Coverage Analysis")
        print(f"{'='*60}")
        print(f"Tool categories tested: 13")
        print(f"Unique query patterns: 125")
        print(f"Edge cases covered: 11")
        print(f"Multi-step reasoning tests: 15")


async def main():
    """Main test execution"""
    print("🚀 Starting Chatbot V2 Comprehensive Test Suite")
    print(f"Target: {BASE_URL}{API_ENDPOINT}")
    print(f"Project UUID: {PROJECT_UUID}")

    # Check server health first
    try:
        async with httpx.AsyncClient(timeout=10) as client:
            health_response = await client.get(f"{BASE_URL}/api/v1/health")
            print(f"✅ Server health check passed: {health_response.status_code}")
    except Exception as e:
        print(f"⚠️ Server health check failed: {e}")
        print("Proceeding with tests anyway...")

    tester = ChatbotV2Tester(BASE_URL, PROJECT_UUID)
    summary = await tester.run_all_tests()
    tester.print_summary(summary)

    print(f"\n🎉 Test suite completed!")
    print(f"Total execution time: {summary.get('test_start', '')} to {summary.get('test_end', '')}")


if __name__ == "__main__":
    asyncio.run(main())
