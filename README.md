# Log Analysis API

AI 기반 로그 분석 및 챗봇 서비스 - FastAPI + LangChain + OpenSearch

## 📋 개요

Spring Boot 애플리케이션에서 수집된 로그를 AI로 분석하고, 챗봇을 통해 질의할 수 있는 서비스입니다.

### 주요 기능

1. **AI 로그 분석** (`/api/v1/logs/{log_id}/analysis?project_uuid={project_uuid}`)
   - **Multi-tenancy 지원**: project_uuid 기반 데이터 격리
   - **Trace 기반 분석**: trace_id로 연관 로그 수집 (±3초, 최대 100개)
   - **2단계 캐싱**:
     - Trace 캐싱 (97-99% 비용 절감)
     - 유사도 캐싱 (80% 비용 절감)
   - GPT-4o mini를 통한 상세 분석 (요약, 원인, 해결방법)

2. **RAG 기반 챗봇** (`/api/v1/chatbot/ask`)
   - 자연어 질문으로 로그 검색
   - 유사 질문 캐싱 → **80% 비용 절감**
   - 관련 로그를 컨텍스트로 활용한 답변 생성

## 🏗️ 아키텍처

```
┌──────────────────────────────────────┐
│   로그 수집 파이프라인 (별도 관리)    │
│  Spring Boot → Kafka → Logstash     │
│                   ↓                  │
│              OpenSearch              │
│            (로그 + 벡터 저장)         │
└──────────────────────────────────────┘
                   ↓ (읽기)
┌──────────────────────────────────────┐
│      AI 서비스 (FastAPI)              │
│                                      │
│  OpenSearch ← 로그 조회               │
│       ↓                              │
│  ┌─────────────┬─────────────┐      │
│  │ 로그 분석 API │  챗봇 API     │      │
│  │ (유사도 캐시) │ (QA 캐시)    │      │
│  └─────────────┴─────────────┘      │
└──────────────────────────────────────┘
```

**AI 서비스의 역할**:
- OpenSearch에서 로그 읽기 (Logstash가 이미 저장한 로그)
- AI 분석 수행 및 결과 캐싱
- 챗봇 질의 응답 (RAG)

## 📦 기술 스택

- **FastAPI**: 비동기 웹 프레임워크
- **LangChain**: LLM 체인 구성
- **OpenAI**: GPT-4o (분석), text-embedding-3-large (임베딩)
- **OpenSearch**: 로그 저장 + Vector DB (KNN search)

## 🚀 빠른 시작

### 1. 환경 설정

```bash
# 가상환경 생성
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 환경 변수 설정
cp .env.example .env
# .env 파일을 열어서 OPENAI_API_KEY 설정
```

### 2. OpenSearch 인덱스 생성

```bash
python scripts/create_indices.py
```

이 스크립트는 다음을 생성합니다:
- `logs-*` 인덱스 템플릿 (로그 + 벡터)
- `qa-cache` 인덱스 (챗봇 QA 캐시)

### 3. 서버 실행

```bash
uvicorn app.main:app --reload --port 8000
```

**시작 로그**:
```
🚀 Starting log-analysis-api v1.0.0
📊 Environment: development
✅ OpenSearch indices ready
```

## 📚 API 문서

서버 실행 후: http://localhost:8000/docs

### 주요 엔드포인트

#### 1. 로그 분석

```bash
GET /api/v1/logs/{log_id}/analysis?project_uuid={project_uuid}
```

**Query Parameters**:
- `project_uuid` (required): 프로젝트 UUID (multi-tenancy)

**응답 예시**:
```json
{
  "log_id": "550e8400-e29b-41d4-a716-446655440000",
  "analysis": {
    "summary": "NullPointerException in UserService.getUser()",
    "error_cause": "User object was null when accessing getName()",
    "solution": "Add null check before accessing user properties",
    "tags": ["NullPointerException", "UserService", "critical"],
    "analysis_type": "TRACE_BASED",
    "target_type": "LOG",
    "analyzed_at": "2024-01-15T10:35:00.000Z"
  },
  "from_cache": false,
  "similar_log_id": null,
  "similarity_score": null
}
```

#### 2. 챗봇 질문

```bash
POST /api/v1/chatbot/ask
Content-Type: application/json

{
  "question": "최근에 어떤 에러가 발생했어?",
  "filters": {
    "level": "ERROR"
  }
}
```

**응답 예시**:
```json
{
  "answer": "최근 24시간 동안 UserService에서 3건의 NullPointerException이 발생했습니다. getUser() 메서드에서 null 체크 누락이 원인입니다.",
  "from_cache": false,
  "related_logs": [
    {
      "log_id": "550e8400-...",
      "timestamp": "2024-01-15T10:30:00Z",
      "level": "ERROR",
      "message": "NullPointerException in UserService.getUser()",
      "service_name": "user-service",
      "similarity_score": 0.95
    }
  ],
  "answered_at": "2024-01-15T10:35:00.000Z"
}
```

## 🎯 캐싱 전략 (2단계)

### 1단계: Trace 기반 캐싱 (우선)

1. trace_id 존재 시, 연관 로그 수집 (±3초, 최대 100개)
2. 수집된 로그 중 **이미 분석된 로그가 있으면** → 분석 결과 재사용
3. 전체 trace에 대해 1회만 분석 → **97-99% 비용 절감**

### 2단계: 유사도 기반 캐싱 (폴백)

1. trace_id가 없거나 캐시 미스 시
2. 임베딩 벡터로 KNN 검색 (k=5)
3. **유사도 >= 0.8인 로그가 있으면** → 분석 결과 재사용
4. 없으면 → GPT-4o mini로 새로 분석

**비용 절감 효과**:
- Trace 캐싱: 97-99% 절감 (같은 요청 흐름)
- 유사도 캐싱: 80% 절감 (유사한 에러)
- **총 97-99% 비용 절감**

### 챗봇 QA 캐싱

1. 질문 임베딩 생성
2. `qa-cache` 인덱스에서 유사 질문 검색
3. **유사도 >= 0.8인 질문이 있으면** → 캐시된 답변 반환
4. 없으면 → RAG로 새로 답변 생성 및 캐싱

## 📂 프로젝트 구조

```
app/
├── __init__.py
├── main.py                      # FastAPI 앱 진입점
├── api/
│   └── v1/
│       ├── health.py            # 헬스체크
│       ├── logs.py              # 로그 분석 API
│       └── chatbot.py           # 챗봇 API
├── chains/
│   ├── log_analysis_chain.py    # 로그 분석 LangChain
│   └── chatbot_chain.py         # 챗봇 LangChain
├── core/
│   ├── config.py                # 설정 (Pydantic Settings)
│   └── opensearch.py            # OpenSearch 클라이언트
├── models/
│   ├── log.py                   # 로그 모델
│   ├── analysis.py              # 분석 결과 모델
│   └── chat.py                  # 챗봇 모델
├── services/
│   ├── embedding_service.py     # OpenAI 임베딩
│   ├── similarity_service.py    # 유사도 검색
│   ├── log_analysis_service.py  # 로그 분석 로직
│   └── chatbot_service.py       # 챗봇 로직
└── repositories/                # (향후 확장용)

scripts/
├── create_indices.py            # OpenSearch 인덱스 생성
└── test_connection.py           # 연결 테스트
```

## 🔧 설정

### 환경 변수 (.env)

#### 기본 설정 (모든 환경)

```bash
# Application
APP_NAME=log-analysis-api
APP_VERSION=1.0.0
ENVIRONMENT=development

# OpenAI (필수)
OPENAI_API_KEY=your-openai-api-key-here
EMBEDDING_MODEL=text-embedding-3-large
LLM_MODEL=gpt-4o-mini

# Analysis
SIMILARITY_THRESHOLD=0.8        # 유사도 임계값
MAX_CONTEXT_LOGS=5              # 챗봇 컨텍스트 로그 수
```

#### 로컬 개발 환경

```bash
# OpenSearch (로컬)
OPENSEARCH_HOST=localhost
OPENSEARCH_PORT=9200
OPENSEARCH_USER=admin
OPENSEARCH_PASSWORD=Admin123!@#
OPENSEARCH_USE_SSL=false
```

#### 컨테이너/프로덕션 환경

컨테이너 환경에서는 `docker-compose.yml`에서 자동으로 오버라이드됩니다:

```yaml
environment:
  # OpenSearch - 서비스명으로 접근
  - OPENSEARCH_HOST=opensearch
  - OPENSEARCH_PORT=9200
  - OPENSEARCH_USER=admin
  - OPENSEARCH_PASSWORD=Admin123!@#
  - OPENSEARCH_USE_SSL=false
```

**중요 사항**:
- **로컬 개발**: `localhost` 사용
- **컨테이너**: 서비스명 (`opensearch`) 사용
- **보안**: 프로덕션에서는 비밀번호 변경 및 SSL 활성화 필수

## 🧪 테스트 시나리오

### 1. 로그 분석 테스트

```bash
# 1. Spring Boot 앱에서 에러 로그 생성
curl "http://localhost:8080/api/test/error"

# 2. OpenSearch에서 로그 ID 확인
curl "http://localhost:9200/logs-*/_search?q=level:ERROR&pretty" | grep log_id

# 3. AI 분석 요청 (project_uuid 포함)
LOG_ID="..."  # 위에서 확인한 ID
PROJECT_UUID="550e8400-e29b-41d4-a716-446655440000"
curl "http://localhost:8000/api/v1/logs/$LOG_ID/analysis?project_uuid=$PROJECT_UUID" | jq
```

### 2. Trace 캐싱 확인

```bash
# trace_id가 같은 다른 로그로 분석 요청
# → from_cache: true, similarity_score: 1.0 반환 (같은 trace)
```

### 3. 유사도 캐싱 확인

```bash
# 같은 에러를 다시 발생시킴 (다른 trace_id)
curl "http://localhost:8080/api/test/error"

# 새 로그 ID로 분석 요청 → from_cache: true, similarity_score: 0.9x 반환
```

### 3. 챗봇 테스트

```bash
curl -X POST "http://localhost:8000/api/v1/chatbot/ask" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "최근에 발생한 에러 로그를 요약해줘"
  }' | jq
```

## 📊 성능 최적화

### 벡터 검색 최적화

- **HNSW 알고리즘**: 빠른 근사 최근접 이웃 검색
- **Cosine similarity**: 임베딩 벡터 간 유사도 측정
- **인덱스 샤딩**: 대용량 로그 처리

### 비용 최적화

1. **유사도 임계값 조정**: 0.8 → 0.85 (더 엄격하게)
2. **LLM 모델 변경**: GPT-4o → GPT-3.5-turbo
3. **컨텍스트 로그 수 제한**: MAX_CONTEXT_LOGS

## 🐛 트러블슈팅

### OpenSearch 연결 실패

```bash
curl http://localhost:9200/_cluster/health?pretty
# status가 "green" 또는 "yellow"인지 확인
```

### OpenAI API 에러

```bash
# API 키 확인
cat .env | grep OPENAI_API_KEY

# 간단한 테스트
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

## 🚀 프로덕션 배포

### 보안

- [ ] OpenSearch 인증 활성화
- [ ] API 인증 추가 (JWT)
- [ ] HTTPS 적용
- [ ] API Rate Limiting

### 확장성

- [ ] OpenSearch 샤드/레플리카 조정
- [ ] FastAPI workers 증가 (`--workers 4`)
- [ ] Redis 캐시 추가

### 모니터링

- [ ] Prometheus 메트릭
- [ ] Grafana 대시보드
- [ ] OpenSearch 모니터링
- [ ] 에러 알림 (Slack, PagerDuty)

## 📝 라이센스

MIT License

## 🙋 문의

프로젝트 관련 문의사항이 있으시면 이슈를 등록해주세요.
"# open_ai_langchain_log_analyze" 
