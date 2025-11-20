# AI 프로젝트 아키텍처 문서 (V2 API)

## 개요

이 문서는 로그 분석 AI 프로젝트의 V2 API 아키텍처와 주요 기능 플로우를 설명합니다.

**기술 스택:**
- FastAPI (비동기 웹 프레임워크)
- LangChain (LLM 오케스트레이션)
- LangGraph (상태 기반 워크플로우)
- OpenSearch (벡터 DB + 로그 저장소)
- OpenAI/GMS (임베딩 + LLM)

---

## 1. API 엔드포인트 구조

```
/ (root)
├── /api/v2
│   └── /chatbot/
│       ├── /ask                              # ReAct Agent 챗봇
│       └── /ask/stream                       # Agent 스트리밍
│
└── /api/v2-langgraph
    ├── /logs/{log_id}/analysis               # LangGraph 기반 로그 분석
    ├── /analysis/
    │   ├── /projects/html-document           # 프로젝트 분석 HTML 생성
    │   └── /errors/html-document             # 에러 분석 HTML 생성
    └── /statistics/
        ├── /compare                          # AI vs DB 통계 비교
        └── /hourly                           # 시간대별 통계
```

---

## 2. V2 Chatbot (ReAct Agent)

### 주요 파일
- API: `app/api/v2/chatbot.py`
- Service: `app/services/chatbot_service_v2.py`
- Agent: `app/agents/chatbot_agent.py`
- Tools: `app/tools/*.py`

### 전체 플로우

```
[POST /api/v2/chatbot/ask]
    │
    ▼
[1. Off-Topic 필터링] ─── _is_off_topic()
    │                      - 로그 관련 키워드 확인
    │                      - 인사말/날씨/요리 등 거부
    │
    ├── [Off-Topic] → 안내 메시지 즉시 반환
    │
    ▼
[2. 질문 유형 분류] ─── _classify_query_type()
    │                   - error_analysis: 에러/오류/장애
    │                   - performance_analysis: 느린/성능/지연
    │                   - monitoring: 통계/추세/헬스
    │                   - search: 검색/찾기/조회
    ▼
[3. ReAct Agent 생성] ─── create_log_analysis_agent()
    │                      - 41개 도구 바인딩
    │                      - max_iterations: 12
    │                      - timeout: 60초
    ▼
[4. ReAct Loop 실행]
    │
    ┌─────────────────────────┐
    │ Thought: 무엇을 해야 할지 │
    └─────────────────────────┘
                │
                ▼
    ┌─────────────────────────┐
    │ Action: 도구 선택         │
    │ Action Input: JSON 파라미터│
    └─────────────────────────┘
                │
                ▼
    ┌─────────────────────────┐
    │ [도구 실행]               │
    │ - OpenSearch 쿼리        │
    │ - 데이터 집계/분석        │
    └─────────────────────────┘
                │
                ▼
    ┌─────────────────────────┐
    │ Observation: 도구 결과    │
    └─────────────────────────┘
                │
                ▼
           [반복 또는]
                │
                ▼
    ┌─────────────────────────┐
    │ Final Answer: 최종 답변   │
    └─────────────────────────┘
    │
    ▼
[5. 응답 검증] ─── _validate_and_enhance_response()
                   - 보안: XSS/SQL 인젝션 방지
                   - 최소 길이 검증 (유형별)
                   - 구조 체크 (마크다운 헤더, 코드 블록)
```

### 사용 가능한 도구 (41개)

| 카테고리 | 도구 | 설명 |
|----------|------|------|
| **검색** | search_logs_by_keyword | 키워드 기반 로그 검색 |
| | search_logs_advanced | 고급 검색 (다중 필터) |
| **분석** | get_log_statistics | 로그 통계 집계 |
| | get_recent_errors | 최근 에러 목록 |
| | analyze_single_log | 단일 로그 AI 분석 |
| **성능** | get_slowest_apis | 가장 느린 API |
| | get_traffic_by_time | 시간대별 트래픽 |
| **모니터링** | get_error_rate_trend | 에러율 추이 |
| | get_service_health_status | 서비스 헬스 상태 |
| | detect_anomalies | 이상 탐지 |
| **아키텍처** | analyze_error_by_layer | 레이어별 에러 분석 |
| | trace_component_calls | 컴포넌트 호출 추적 |
| **패턴** | cluster_stack_traces | 스택 트레이스 클러스터링 |
| | detect_recurring_errors | 반복 에러 탐지 |

---

## 3. V2-LangGraph API

### 3.1 로그 분석 (StateGraph 기반)

**엔드포인트:** `GET /api/v2-langgraph/logs/{log_id}/analysis`

**주요 파일:**
- API: `app/api/v2_langgraph/logs.py`
- Service: `app/services/log_analysis_service_v2.py`
- Graph: `app/graphs/log_analysis_graph.py`
- State: `app/graphs/state/log_analysis_state.py`

**플로우:**

```
fetch_log
    │
    ▼
check_direct_cache ─── 직접 캐시 확인
    │                   비용 절감: 100%
    ├── [HIT] ────────────────────► save_result → END
    │
    ▼ [MISS]
check_trace_cache ─── Trace 캐시 확인
    │                  비용 절감: 97-99%
    ├── [HIT] ────────────────────► save_result → END
    │
    ▼ [MISS]
check_similarity_cache ─── 유사도 캐시 확인
    │                       비용 절감: 80%
    ├── [HIT] ────────────────────► save_result → END
    │
    ▼ [MISS]
collect_logs ─── Trace 기반 관련 로그 수집
    │
    ▼
decide_strategy ─── 분석 전략 결정
    │                - single: 1개 로그
    │                - direct: 2-30개 로그
    │                - map_reduce: 31-100개 로그
    ▼
analyze ─── LLM 기반 분석
    │
    ▼
validate ─── 한국어 + 품질 검증
    │         - 한국어 검증 실패: 최대 2회 재시도
    │         - 품질 검증 실패: 최대 1회 재시도
    ├── [PASSED] → save_result → END
    ├── [FAILED] → analyze (재시도)
    └── [MAX_RETRIES] → save_result → END
```

**그래프 노드 (9개):**

| 노드 | 함수 | 기능 |
|------|------|------|
| fetch_log | `_fetch_log_node()` | OpenSearch에서 로그 조회 |
| check_direct_cache | `_check_direct_cache_node()` | 직접 캐시 체크 |
| check_trace_cache | `_check_trace_cache_node()` | Trace 캐시 체크 |
| check_similarity_cache | `_check_similarity_cache_node()` | 유사도 캐시 체크 |
| collect_logs | `_collect_logs_node()` | 관련 로그 수집 |
| decide_strategy | `_decide_strategy_node()` | 분석 전략 결정 |
| analyze | `_analyze_node()` | LLM 분석 수행 |
| validate | `_validate_node()` | 검증 및 재시도 |
| save_result | `_save_result_node()` | 결과 저장 |

### 3.2 HTML 문서 생성

**프로젝트 분석:**
- 엔드포인트: `POST /api/v2-langgraph/analysis/projects/html-document`
- 기능: 프로젝트 통계 시각화, Chart.js 차트, 건강 점수 계산

**에러 분석:**
- 엔드포인트: `POST /api/v2-langgraph/analysis/errors/html-document`
- 기능: 에러 로그 상세 정보, AI 분석 결과, 스택 트레이스 포맷팅

**주요 파일:**
- API: `app/api/v2_langgraph/analysis.py`
- Service: `app/services/html_document_service.py`
- Templates: Jinja2 기반 HTML 템플릿

### 3.3 AI vs DB 통계 비교

**엔드포인트:** `GET /api/v2-langgraph/statistics/compare`

**파라미터:**
- `project_uuid`: 프로젝트 UUID
- `time_hours`: 시간 범위
- `sample_size`: 샘플 크기

**플로우:**

```
1. DB 직접 통계 조회 ─── _get_db_statistics()
    │
    ▼
2. 로그 샘플 추출 ─── _get_log_samples()
    │
    ▼
3. LLM 기반 통계 추론 ─── _llm_estimate_statistics()
    │                       temperature: 0.1 (일관성)
    │                       Structured Output
    ▼
4. 정확도 계산 ─── _calculate_accuracy()
    │              - 총 로그 수
    │              - ERROR/WARN/INFO 수
    │              - 에러율
    ▼
5. 검증 결론
    - 95%+: 매우 우수 (프로덕션 사용 가능)
    - 90%+: 우수
    - 80%+: 양호
    - 70%+: 보통
    - 70%-: 미흡
```

### 3.4 시간대별 통계

**엔드포인트:** `GET /api/v2-langgraph/statistics/hourly`

**파라미터:**
- `project_uuid`: 프로젝트 UUID
- `time_hours`: 시간 범위

**기능:** DB에서 시간대별 통계를 조회하여 반환

---

## 4. V2 Agent vs V2-LangGraph 비교

| 특성 | V2 Agent (ReAct) | V2-LangGraph |
|------|------------------|--------------|
| **아키텍처** | ReAct Agent (LangChain) | StateGraph (LangGraph) |
| **실행 흐름** | 동적 (LLM이 도구 선택) | 정적 워크플로우 (미리 정의된 노드/엣지) |
| **도구 사용** | 41개 도구 자율 선택 | 9개 고정 노드 |
| **주요 용도** | 대화형 질의응답 | 로그 분석 자동화 |
| **캐싱** | 미지원 | 3-tier 캐시 (Direct/Trace/Similarity) |
| **검증** | 기본 검증 | 한국어 + 품질 검증 |
| **재시도** | 파싱 에러만 | 분석 품질 기반 재시도 |
| **Temperature** | 0 (결정적) | 분석별 설정 |
| **상태 관리** | Agent Scratchpad | TypedDict State (70+ 필드) |
| **결과 저장** | 즉시 응답 | OpenSearch 저장 + Trace 전파 |

---

## 5. 핵심 서비스 상호작용

### OpenSearch 통신

**파일:** `app/core/opensearch.py`

```python
OPENSEARCH_HOST = "localhost"  # Docker: "opensearch"
OPENSEARCH_PORT = 9200
OPENSEARCH_USER = "admin"
OPENSEARCH_PASSWORD = "Admin123!@#"
```

**인덱스 패턴:**
```
프로젝트 로그: {uuid_underscores}_{YYYY}_{MM}
```

**주요 쿼리 유형:**
1. **KNN 벡터 검색** - 코사인 유사도
2. **Term 쿼리** - 정확한 값 매칭
3. **Range 쿼리** - 시간 범위 필터
4. **Aggregation** - 통계 집계

### OpenAI/GMS API

**파일:** `app/core/config.py`

```python
OPENAI_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"
EMBEDDING_MODEL = "text-embedding-3-large"  # 1536차원
LLM_MODEL = "gpt-4o-mini"
AGENT_MODEL = "gpt-4o-mini"
```

**API 호출 패턴:**
- Agent: `AgentExecutor.ainvoke()` (temperature=0)
- LangGraph 분석: 노드별 LLM 호출
- 통계 비교: Structured Output (temperature=0.1)

---

## 6. 설정 파라미터

**파일:** `app/core/config.py`

| 파라미터 | 값 | 설명 |
|----------|-----|------|
| `SIMILARITY_THRESHOLD` | 0.92 | 캐시 히트 기준 |
| `AGENT_MAX_ITERATIONS` | 12 | Agent 최대 반복 횟수 |
| `MAP_REDUCE_THRESHOLD` | 10 | Map-Reduce 적용 기준 |
| `LOG_CHUNK_SIZE` | 5 | Map 단계 청크 크기 |
| `VALIDATION_MAX_RETRIES` | 2 | 검증 실패 시 최대 재시도 |
| `VALIDATION_STRUCTURAL_THRESHOLD` | 0.7 | 구조적 검증 임계값 |
| `VALIDATION_CONTENT_THRESHOLD` | 0.6 | 내용 검증 임계값 |
| `VALIDATION_OVERALL_THRESHOLD` | 0.65 | 종합 통과 기준 |

---

## 7. 보안 및 검증

### 보안 기능
- **XSS 방지**: 사용자 입력 위생처리
- **SQL 인젝션 방지**: 파라미터화된 쿼리
- **멀티테넌시**: project_uuid 기반 데이터 격리

### V2 Agent 응답 검증
- **구조적 검증**: 마크다운 형식 확인
- **내용 검증**: 숫자 일관성, 근거 확인
- **최소 길이**: 질문 유형별 검증

### V2-LangGraph 검증 파이프라인
- **한국어 검증**: 정규식 기반 한글 포함 여부
- **품질 검증**: ValidationPipeline 사용
- **재시도 로직**: 검증 실패 시 자동 재분석

---

## 8. 성능 최적화

### 3-Tier 캐싱 전략 (V2-LangGraph)
- **Direct Cache**: 100% 비용 절감 (이미 분석된 로그)
- **Trace Cache**: 97-99% 비용 절감 (동일 trace 로그)
- **Similarity Cache**: 80% 비용 절감 (유사 로그)

### Agent 최적화 (V2)
- **Off-Topic 필터링**: 불필요한 LLM 호출 방지
- **최대 반복 제한**: 12회 (무한 루프 방지)
- **타임아웃**: 60초 (응답 보장)

### 분석 전략 최적화
- **Single**: 1개 로그 (즉시 분석)
- **Direct**: 2-30개 로그 (한 번에 분석)
- **Map-Reduce**: 31-100개 로그 (청크 분할)

---

*마지막 업데이트: 2025-11-17*
