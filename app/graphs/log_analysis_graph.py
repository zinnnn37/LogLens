"""
Log Analysis LangGraph Implementation

LangGraph를 사용한 로그 분석 워크플로우:
1. 캐시 체크 (3-tier)
2. 로그 수집
3. 분석 전략 결정
4. 분석 실행
5. 검증 (한국어 + 품질)
6. 결과 저장
"""

from typing import Dict, Any, List
from datetime import datetime
import time

from langgraph.graph import StateGraph, END
from langchain_core.runnables import RunnableConfig

from app.graphs.state.log_analysis_state import LogAnalysisState, create_initial_state
from app.graphs.tools.cache_tools import (
    create_check_direct_cache_tool,
    create_check_trace_cache_tool,
    create_check_similarity_cache_tool,
)
from app.graphs.tools.analysis_tools import (
    create_collect_trace_logs_tool,
    create_analyze_single_log_tool,
    create_analyze_logs_direct_tool,
    create_analyze_with_map_reduce_tool,
)
from app.graphs.tools.validation_tools import (
    create_validate_korean_tool,
    create_validate_quality_tool,
)

from app.core.opensearch import opensearch_client
from app.core.config import settings


class LogAnalysisGraph:
    """
    LangGraph 기반 로그 분석 워크플로우
    """

    def __init__(self, project_uuid: str):
        """
        Args:
            project_uuid: 프로젝트 UUID
        """
        self.project_uuid = project_uuid

        # 도구 생성 (project_uuid 바인딩)
        self.check_direct_cache = create_check_direct_cache_tool(project_uuid)
        self.check_trace_cache = create_check_trace_cache_tool(project_uuid)
        # Note: check_similarity_cache is created dynamically in _check_similarity_cache_node with metadata
        self.collect_trace_logs = create_collect_trace_logs_tool(project_uuid)
        self.analyze_single_log = create_analyze_single_log_tool(project_uuid)
        self.analyze_logs_direct = create_analyze_logs_direct_tool(project_uuid)
        self.analyze_with_map_reduce = create_analyze_with_map_reduce_tool(project_uuid)
        self.validate_korean = create_validate_korean_tool(project_uuid)
        self.validate_quality = create_validate_quality_tool(project_uuid)

        # StateGraph 생성
        self.graph = self._build_graph()

    def _build_graph(self) -> StateGraph:
        """
        LangGraph StateGraph 구성
        """
        # StateGraph 초기화
        workflow = StateGraph(LogAnalysisState)

        # 노드 추가
        workflow.add_node("fetch_log", self._fetch_log_node)
        workflow.add_node("check_direct_cache", self._check_direct_cache_node)
        workflow.add_node("check_trace_cache", self._check_trace_cache_node)
        workflow.add_node("check_similarity_cache", self._check_similarity_cache_node)
        workflow.add_node("collect_logs", self._collect_logs_node)
        workflow.add_node("decide_strategy", self._decide_strategy_node)
        workflow.add_node("analyze", self._analyze_node)
        workflow.add_node("validate", self._validate_node)
        workflow.add_node("save_result", self._save_result_node)

        # 시작점
        workflow.set_entry_point("fetch_log")

        # 엣지 정의
        workflow.add_edge("fetch_log", "check_direct_cache")

        # Direct cache 체크 후
        workflow.add_conditional_edges(
            "check_direct_cache",
            self._direct_cache_router,
            {
                "cache_hit": "save_result",  # Direct cache hit → 바로 저장
                "cache_miss": "check_trace_cache",  # Miss → Trace cache 체크
            }
        )

        # Trace cache 체크 후
        workflow.add_conditional_edges(
            "check_trace_cache",
            self._trace_cache_router,
            {
                "cache_hit": "save_result",  # Trace cache hit → 바로 저장
                "cache_miss": "check_similarity_cache",  # Miss → Similarity cache 체크
            }
        )

        # Similarity cache 체크 후
        workflow.add_conditional_edges(
            "check_similarity_cache",
            self._similarity_cache_router,
            {
                "cache_hit": "save_result",  # Similarity cache hit → 바로 저장
                "cache_miss": "collect_logs",  # Miss → 로그 수집
            }
        )

        # 로그 수집 후 전략 결정
        workflow.add_edge("collect_logs", "decide_strategy")

        # 전략 결정 후 분석
        workflow.add_edge("decide_strategy", "analyze")

        # 분석 후 검증
        workflow.add_edge("analyze", "validate")

        # 검증 후 처리
        workflow.add_conditional_edges(
            "validate",
            self._validation_router,
            {
                "passed": "save_result",  # 검증 통과 → 저장
                "failed": "analyze",  # 검증 실패 → 재분석 (최대 재시도 횟수까지)
                "max_retries": "save_result",  # 최대 재시도 도달 → 저장 (경고와 함께)
            }
        )

        # 저장 후 종료
        workflow.add_edge("save_result", END)

        return workflow.compile()

    # ===== 노드 구현 =====

    async def _fetch_log_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        1. 로그 데이터 조회
        """
        log_id = state["log_id"]
        project_uuid = state["project_uuid"]

        # OpenSearch에서 로그 조회
        index_pattern = f"{project_uuid.replace('-', '_')}_*"

        try:
            response = opensearch_client.search(
                index=index_pattern,
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"term": {"log_id": log_id}},
                                {"term": {"project_uuid.keyword": project_uuid}},
                            ]
                        }
                    },
                    "size": 1
                },
            )

            if response["hits"]["total"]["value"] == 0:
                return {
                    "error": f"Log not found: log_id={log_id}, project_uuid={project_uuid}",
                    "finished_at": datetime.utcnow().isoformat() + "Z"
                }

            log_data = response["hits"]["hits"][0]["_source"]

            return {
                "log_data": log_data,
                "trace_id": log_data.get("trace_id"),
                "timestamp": log_data.get("timestamp"),
                "log_message": log_data.get("message"),
                "log_level": log_data.get("log_level") or log_data.get("level"),
                "service_name": log_data.get("service_name"),
            }

        except Exception as e:
            return {
                "error": f"Error fetching log: {str(e)}",
                "finished_at": datetime.utcnow().isoformat() + "Z"
            }

    async def _check_direct_cache_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        2. Direct Cache 체크
        """
        if state.get("error"):
            return {"direct_cache_result": None}

        start_time = time.time()

        log_id = state["log_id"]
        result_str = await self.check_direct_cache.ainvoke({"log_id": log_id})

        duration_ms = int((time.time() - start_time) * 1000)

        # 캐시 히트 여부 파싱
        import json
        if result_str.startswith("CACHE_MISS") or result_str.startswith("ERROR:"):
            return {
                "direct_cache_result": None,
                "cache_check_duration_ms": duration_ms,
            }

        try:
            result = json.loads(result_str)
            if result.get("cache_hit"):
                return {
                    "direct_cache_result": result,
                    "from_cache": True,
                    "cache_type": "direct",
                    "final_analysis": result["analysis"],
                    "cache_check_duration_ms": duration_ms,
                }
        except json.JSONDecodeError:
            pass

        return {"direct_cache_result": None, "cache_check_duration_ms": duration_ms}

    async def _check_trace_cache_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        3. Trace Cache 체크
        """
        if state.get("error") or not state.get("trace_id"):
            return {"trace_cache_result": None}

        start_time = time.time()

        trace_id = state["trace_id"]
        result_str = await self.check_trace_cache.ainvoke({"trace_id": trace_id})

        duration_ms = int((time.time() - start_time) * 1000)

        import json
        if result_str.startswith("CACHE_MISS") or result_str.startswith("ERROR:"):
            return {"trace_cache_result": None}

        try:
            result = json.loads(result_str)
            if result.get("cache_hit"):
                return {
                    "trace_cache_result": result,
                    "from_cache": True,
                    "cache_type": "trace",
                    "final_analysis": result["analysis"],
                    "cache_check_duration_ms": state.get("cache_check_duration_ms", 0) + duration_ms,
                }
        except json.JSONDecodeError:
            pass

        return {"trace_cache_result": None}

    async def _check_similarity_cache_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        4. Similarity Cache 체크 (메타데이터 필터링 포함)
        """
        if state.get("error") or not state.get("log_message"):
            return {"similarity_cache_result": None}

        start_time = time.time()

        log_id = state["log_id"]
        log_message = state["log_message"]
        log_data = state.get("log_data", {})

        # 메타데이터 추출
        log_metadata = {
            "level": log_data.get("level") or state.get("log_level"),
            "service_name": log_data.get("service_name") or state.get("service_name"),
            "source_type": log_data.get("source_type")
        }

        # Similarity cache 도구 생성 (log_id와 메타데이터 전달)
        check_similarity_cache_with_metadata = create_check_similarity_cache_tool(
            self.project_uuid,
            requesting_log_id=log_id,
            log_metadata=log_metadata
        )

        result_str = await check_similarity_cache_with_metadata.ainvoke({
            "log_message": log_message,
            "threshold": settings.SIMILARITY_THRESHOLD
        })

        duration_ms = int((time.time() - start_time) * 1000)

        import json
        if result_str.startswith("CACHE_MISS") or result_str.startswith("ERROR:"):
            return {"similarity_cache_result": None}

        try:
            result = json.loads(result_str)
            if result.get("cache_hit"):
                similar_log_id = result.get("similar_log_id")

                # 로그 ID 검증: 캐시된 log_id가 요청한 log_id와 다른지 확인
                if similar_log_id != log_id:
                    print(f"[Similarity Cache Validation] ⚠️ Cache returned different log - Requested: {log_id}, Got: {similar_log_id}, Score: {result.get('similarity_score', 0):.4f}")
                else:
                    print(f"[Similarity Cache Validation] ⚠️ WARNING: Cache returned same log_id (should have been excluded)")

                return {
                    "similarity_cache_result": result,
                    "from_cache": True,
                    "cache_type": "similarity",
                    "similar_log_id": similar_log_id,
                    "similarity_score": result.get("similarity_score"),
                    "final_analysis": result["analysis"],
                    "cache_check_duration_ms": state.get("cache_check_duration_ms", 0) + duration_ms,
                }
        except json.JSONDecodeError:
            pass

        return {"similarity_cache_result": None}

    async def _collect_logs_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        5. Trace 로그 수집
        """
        if state.get("error"):
            return {"related_logs": None, "log_count": 1}

        trace_id = state.get("trace_id")
        timestamp = state.get("timestamp")

        if not trace_id or not timestamp:
            # Trace가 없으면 단일 로그 분석
            return {
                "related_logs": None,
                "log_count": 1
            }

        # Trace 로그 수집
        import json
        result_str = await self.collect_trace_logs.ainvoke({
            "trace_id": trace_id,
            "center_timestamp": timestamp,
            "time_window_seconds": 3,
            "max_logs": 100
        })

        try:
            result = json.loads(result_str)
            if result.get("success"):
                logs = result.get("logs", [])
                return {
                    "related_logs": logs if logs else None,
                    "log_count": len(logs) if logs else 1
                }
        except json.JSONDecodeError:
            pass

        return {"related_logs": None, "log_count": 1}

    async def _decide_strategy_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        6. 분석 전략 결정
        """
        log_count = state.get("log_count", 1)
        related_logs = state.get("related_logs")

        if not related_logs or log_count == 1:
            return {"analysis_method": "single"}
        elif log_count <= settings.MAP_REDUCE_THRESHOLD:
            return {"analysis_method": "direct"}
        else:
            return {
                "analysis_method": "map_reduce",
                "chunk_size": settings.LOG_CHUNK_SIZE
            }

    async def _analyze_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        7. 분석 실행
        """
        if state.get("error"):
            return {"analysis_result": None}

        start_time = time.time()
        import json

        analysis_method = state.get("analysis_method")
        log_data = state.get("log_data")

        try:
            if analysis_method == "single":
                # 단일 로그 분석
                result_str = await self.analyze_single_log.ainvoke({
                    "log_data_json": json.dumps(log_data, ensure_ascii=False)
                })

            elif analysis_method == "direct":
                # 직접 분석 (2-30개)
                related_logs = state.get("related_logs", [])
                result_str = await self.analyze_logs_direct.ainvoke({
                    "related_logs_json": json.dumps(related_logs, ensure_ascii=False),
                    "center_log_json": json.dumps(log_data, ensure_ascii=False)
                })

            else:  # map_reduce
                # Map-Reduce 분석 (31-100개)
                related_logs = state.get("related_logs", [])
                chunk_size = state.get("chunk_size", settings.LOG_CHUNK_SIZE)
                result_str = await self.analyze_with_map_reduce.ainvoke({
                    "related_logs_json": json.dumps(related_logs, ensure_ascii=False),
                    "center_log_json": json.dumps(log_data, ensure_ascii=False),
                    "chunk_size": chunk_size
                })

            duration_ms = int((time.time() - start_time) * 1000)

            # 분석 결과 파싱
            result = json.loads(result_str)
            if result.get("success"):
                return {
                    "analysis_result": result["analysis"],
                    "analysis_duration_ms": duration_ms,
                    "llm_call_count": state.get("llm_call_count", 0) + 1
                }
            else:
                return {"error": result_str}

        except Exception as e:
            return {"error": f"Analysis failed: {str(e)}"}

    async def _validate_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        8. 검증 (한국어 + 품질)
        """
        if state.get("error"):
            # 에러 발생 시에도 재시도 카운터 증가 (무한 루프 방지)
            current_korean_retry = state.get("korean_retry_count", 0)
            current_validation_retry = state.get("validation_retry_count", 0)
            return {
                "korean_valid": False,
                "quality_score": 0.0,
                "korean_retry_count": current_korean_retry + 1,
                "validation_retry_count": current_validation_retry + 1,
            }

        start_time = time.time()
        import json

        analysis_result = state.get("analysis_result")
        log_data = state.get("log_data")

        # 1. 한국어 검증
        korean_result_str = await self.validate_korean.ainvoke({
            "analysis_json": json.dumps(analysis_result, ensure_ascii=False)
        })

        korean_result = json.loads(korean_result_str)
        korean_valid = korean_result.get("valid", False)

        # 2. 품질 검증
        quality_result_str = await self.validate_quality.ainvoke({
            "analysis_json": json.dumps(analysis_result, ensure_ascii=False),
            "log_data_json": json.dumps(log_data, ensure_ascii=False)
        })

        quality_result = json.loads(quality_result_str)
        quality_passed = quality_result.get("passed", False)
        quality_score = quality_result.get("overall_score", 0.0)
        quality_suggestions = quality_result.get("suggestions", [])

        duration_ms = int((time.time() - start_time) * 1000)

        # 재시도 카운터 증가
        current_korean_retry = state.get("korean_retry_count", 0)
        current_validation_retry = state.get("validation_retry_count", 0)

        # 검증 실패 시 카운터 증가
        new_korean_retry = current_korean_retry + 1 if not korean_valid else current_korean_retry
        new_validation_retry = current_validation_retry + 1 if not quality_passed else current_validation_retry

        return {
            "korean_valid": korean_valid,
            "quality_score": quality_score,
            "quality_suggestions": quality_suggestions,
            "validation_duration_ms": duration_ms,
            "korean_retry_count": new_korean_retry,
            "validation_retry_count": new_validation_retry,
            "error": None,  # 재시도 시 에러 초기화
        }

    async def _save_result_node(self, state: LogAnalysisState) -> Dict[str, Any]:
        """
        9. 결과 저장
        """
        if state.get("error"):
            return {"finished_at": datetime.utcnow().isoformat() + "Z"}

        # 최종 분석 결과 결정
        final_analysis = state.get("final_analysis") or state.get("analysis_result")

        if not final_analysis:
            return {
                "error": "No analysis result available",
                "finished_at": datetime.utcnow().isoformat() + "Z"
            }

        # OpenSearch에 저장
        log_id = state["log_id"]
        project_uuid = state["project_uuid"]
        index_pattern = f"{project_uuid.replace('-', '_')}_*"

        try:
            opensearch_client.update_by_query(
                index=index_pattern,
                body={
                    "script": {
                        "source": "ctx._source.ai_analysis = params.analysis",
                        "params": {"analysis": final_analysis},
                    },
                    "query": {
                        "bool": {
                            "must": [
                                {"term": {"log_id": log_id}},
                                {"term": {"project_uuid.keyword": project_uuid}},
                            ]
                        }
                    },
                },
            )

            # Trace 전파 (from_cache가 아니고 related_logs가 있는 경우)
            if not state.get("from_cache") and state.get("related_logs"):
                related_logs = state["related_logs"]
                for log in related_logs:
                    if log.get("log_id") != log_id:
                        opensearch_client.update_by_query(
                            index=index_pattern,
                            body={
                                "script": {
                                    "source": "ctx._source.ai_analysis = params.analysis",
                                    "params": {"analysis": final_analysis},
                                },
                                "query": {
                                    "bool": {
                                        "must": [
                                            {"term": {"log_id": log["log_id"]}},
                                            {"term": {"project_uuid.keyword": project_uuid}},
                                        ]
                                    }
                                },
                            },
                        )

        except Exception as e:
            print(f"Error saving analysis: {e}")

        # 최종 상태 업데이트
        finished_at = datetime.utcnow().isoformat() + "Z"
        started_at = datetime.fromisoformat(state["started_at"].replace("Z", "+00:00"))
        finished_time = datetime.fromisoformat(finished_at.replace("Z", "+00:00"))
        total_duration_ms = int((finished_time - started_at).total_seconds() * 1000)

        return {
            "final_analysis": final_analysis,
            "finished_at": finished_at,
            "total_duration_ms": total_duration_ms
        }

    # ===== 라우터 함수 =====

    def _direct_cache_router(self, state: LogAnalysisState) -> str:
        """Direct cache 체크 결과 라우팅"""
        if state.get("direct_cache_result"):
            return "cache_hit"
        return "cache_miss"

    def _trace_cache_router(self, state: LogAnalysisState) -> str:
        """Trace cache 체크 결과 라우팅"""
        if state.get("trace_cache_result"):
            return "cache_hit"
        return "cache_miss"

    def _similarity_cache_router(self, state: LogAnalysisState) -> str:
        """Similarity cache 체크 결과 라우팅"""
        if state.get("similarity_cache_result"):
            return "cache_hit"
        return "cache_miss"

    def _validation_router(self, state: LogAnalysisState) -> str:
        """검증 결과 라우팅"""
        # 에러 상태가 있으면 재시도 중단 (무한 루프 방지)
        if state.get("error"):
            return "max_retries"

        korean_valid = state.get("korean_valid", False)
        quality_score = state.get("quality_score", 0.0)
        korean_retry_count = state.get("korean_retry_count", 0)
        validation_retry_count = state.get("validation_retry_count", 0)
        max_korean_retries = state.get("max_korean_retries", 2)
        max_validation_retries = state.get("max_validation_retries", 1)

        # 최대 재시도 도달
        if (korean_retry_count >= max_korean_retries or
            validation_retry_count >= max_validation_retries):
            return "max_retries"

        # 검증 통과
        if korean_valid and quality_score >= settings.VALIDATION_OVERALL_THRESHOLD:
            return "passed"

        # 검증 실패 → 재시도
        return "failed"

    # ===== 공개 메서드 =====

    async def analyze(self, log_id: int) -> Dict[str, Any]:
        """
        로그 분석 실행

        Args:
            log_id: 로그 ID

        Returns:
            분석 결과 딕셔너리
        """
        # 초기 상태 생성
        initial_state = create_initial_state(log_id=log_id, project_uuid=self.project_uuid)

        # 그래프 실행
        final_state = await self.graph.ainvoke(initial_state)

        # 결과 반환
        return {
            "log_id": log_id,
            "analysis": final_state.get("final_analysis"),
            "from_cache": final_state.get("from_cache", False),
            "cache_type": final_state.get("cache_type"),
            "similar_log_id": final_state.get("similar_log_id"),
            "similarity_score": final_state.get("similarity_score"),
            "analysis_method": final_state.get("analysis_method"),
            "log_count": final_state.get("log_count", 1),
            "metadata": {
                "started_at": final_state.get("started_at"),
                "finished_at": final_state.get("finished_at"),
                "total_duration_ms": final_state.get("total_duration_ms"),
                "llm_call_count": final_state.get("llm_call_count", 0),
                "cache_check_duration_ms": final_state.get("cache_check_duration_ms"),
                "analysis_duration_ms": final_state.get("analysis_duration_ms"),
                "validation_duration_ms": final_state.get("validation_duration_ms"),
                "korean_valid": final_state.get("korean_valid"),
                "quality_score": final_state.get("quality_score"),
            },
            "error": final_state.get("error"),
        }


def create_log_analysis_graph(project_uuid: str) -> LogAnalysisGraph:
    """
    로그 분석 그래프 생성

    Args:
        project_uuid: 프로젝트 UUID

    Returns:
        LogAnalysisGraph 인스턴스
    """
    return LogAnalysisGraph(project_uuid)
