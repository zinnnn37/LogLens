# FastAPI 호스팅 설정 가이드

AI 로그 분석 서비스의 환경 구성 및 배포 가이드

---

## 📋 목차

1. [시스템 아키텍처](#-시스템-아키텍처)
2. [환경 구성](#-환경-구성)
3. [로컬 테스트 환경](#-로컬-테스트-환경-wsllinux)
4. [프로덕션 배포](#-프로덕션-배포)
5. [API 엔드포인트](#-api-엔드포인트)
6. [트러블슈팅](#-트러블슈팅)
7. [성능 최적화](#-성능-최적화)

---

## 🏗️ 시스템 아키텍처

### 기술 스택

```
┌─────────────────────────────────────────────────────────┐
│                    Client (React)                        │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP/SSE
┌────────────────────▼────────────────────────────────────┐
│                FastAPI Server (Python 3.12)              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Log API     │  │  Chatbot API │  │  Health API  │  │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘  │
│         │                  │                             │
│  ┌──────▼──────────────────▼───────────────────────┐    │
│  │         Service Layer                           │    │
│  │  - Log Analysis Service (Map-Reduce)            │    │
│  │  - Chatbot Service (RAG + Caching)              │    │
│  │  - Embedding Service (Cache)                    │    │
│  │  - Similarity Service (KNN Search)              │    │
│  └─────────────────────┬───────────────────────────┘    │
└────────────────────────┼────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │OpenAI   │    │OpenSearch│    │  Kafka  │
    │(GPT-4o  │    │(Vector   │    │(Log     │
    │ mini)   │    │ Search)  │    │ Stream) │
    └─────────┘    └──────────┘    └─────────┘
```

### 주요 컴포넌트

| 컴포넌트 | 역할 | 기술 |
|----------|------|------|
| **FastAPI** | REST API 서버 | Python 3.12, Uvicorn |
| **OpenSearch** | 로그 저장 및 벡터 검색 | OpenSearch 2.11.0, KNN |
| **OpenAI** | LLM 및 임베딩 | GPT-4o mini, text-embedding-3-large |
| **LangChain** | LLM 체인 구성 | LangChain 0.2.16 |
| **Kafka** | 로그 스트리밍 | Kafka 7.5.0 (KRaft) |
| **Logstash** | 로그 파이프라인 | Logstash 8.9.0 |

---

## ⚙️ 환경 구성

### 환경변수 파일

프로젝트는 2가지 환경변수 파일을 사용합니다:

| 파일 | 용도 | OpenSearch 호스트 |
|------|------|------------------|
| `.env` | 프로덕션/컨테이너 환경 | `opensearch` (서비스명) |
| `.env.test` | 로컬 테스트 환경 | `localhost` |

### 필수 환경변수

#### 1. OpenAI API (SSAFY GMS)

```bash
OPENAI_API_KEY=S13P32A306-xxxx-xxxx-xxxx-xxxxxxxxxxxx
OPENAI_BASE_URL=https://gms.ssafy.io/gmsapi/api.openai.com/v1
EMBEDDING_MODEL=text-embedding-3-large
LLM_MODEL=gpt-4o-mini
```

**발급 방법**:
1. SSAFY GMS 포털 접속: https://gms.ssafy.io/
2. 프로젝트 등록 후 API 키 발급
3. `.env` 및 `.env.test` 파일에 추가

#### 2. OpenSearch 설정

**프로덕션 (`.env`)**:
```bash
OPENSEARCH_HOST=opensearch  # Docker 컨테이너 이름
OPENSEARCH_PORT=9200
OPENSEARCH_USER=admin
OPENSEARCH_PASSWORD=admin
OPENSEARCH_USE_SSL=false
OPENSEARCH_VERIFY_CERTS=false
```

**로컬 테스트 (`.env.test`)**:
```bash
OPENSEARCH_HOST=localhost  # 호스트에서 접근
OPENSEARCH_PORT=9200
OPENSEARCH_USER=admin
OPENSEARCH_PASSWORD=admin
OPENSEARCH_USE_SSL=false
OPENSEARCH_VERIFY_CERTS=false
```

#### 3. 애플리케이션 설정

```bash
APP_NAME=log-analysis-api
ENVIRONMENT=development  # 또는 production
SERVICE_PORT=8000
LOG_LEVEL=INFO  # DEBUG, INFO, WARN, ERROR

# CORS 설정
CORS_ORIGINS=["http://localhost:3000","http://localhost:5173"]

# 분석 설정
SIMILARITY_THRESHOLD=0.8
MAX_CONTEXT_LOGS=5

# 캐싱 설정
CACHE_CANDIDATE_SIZE=5
DEFAULT_CACHE_TTL=1800
SHORT_CACHE_TTL=600
LONG_CACHE_TTL=86400

# Map-Reduce 설정
ENABLE_MAP_REDUCE=true
MAP_REDUCE_THRESHOLD=10
LOG_CHUNK_SIZE=5

# LLM 타임아웃
LLM_REQUEST_TIMEOUT=120
```

---

## 🧪 로컬 테스트 환경 (WSL/Linux)

### 사전 요구사항

- **Docker Desktop** (WSL 통합 활성화)
- **Python 3.11+** (3.12 권장)
- **pip, venv**
- **최소 4GB RAM** (OpenSearch)

### 1단계: Docker Desktop WSL 통합 활성화

#### Docker Desktop 설정

1. Docker Desktop 실행 (Windows)
2. **Settings (⚙️)** → **Resources** → **WSL Integration**
3. **Enable integration with my default WSL distro** 체크
4. 사용 중인 distro (Ubuntu 등) 활성화
5. **Apply & Restart**

#### 확인

```bash
# WSL 터미널에서
docker --version
docker compose version  # v2 (공백 주의)
```

### 2단계: Docker 권한 설정

```bash
# 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# WSL 재시작 (PowerShell에서)
# wsl --shutdown
# 그 후 WSL 다시 시작

# 그룹 확인
groups  # 출력에 "docker" 포함되어야 함
```

### 3단계: Python 가상환경 구성

```bash
# 프로젝트 디렉토리로 이동
cd /mnt/c/SSAFY/third_project/AI/S13P31A306

# 가상환경 생성
python3 -m venv venv

# 가상환경 활성화
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt
```

**설치되는 주요 패키지**:
- fastapi==0.109.0
- uvicorn[standard]==0.27.0
- langchain==0.2.16
- langchain-openai==0.1.23
- openai==1.40.0
- opensearch-py==2.4.2
- tiktoken==0.7.0

### 4단계: OpenSearch 실행

```bash
# Docker Compose로 OpenSearch 시작
sudo docker compose -f docker-compose.test.yml up -d

# 컨테이너 확인
sudo docker ps

# OpenSearch 준비 대기 (30초 정도)
sleep 30

# OpenSearch 상태 확인
curl http://localhost:9200/_cluster/health?pretty
```

**예상 출력**:
```json
{
  "cluster_name" : "opensearch-cluster",
  "status" : "green",
  "number_of_nodes" : 1
}
```

### 5단계: OpenSearch 인덱스 생성

```bash
# 스크립트가 자동으로 .env.test 사용
python scripts/create_indices.py
```

**예상 출력**:
```
🔧 Using .env.test for local testing
🚀 Creating OpenSearch indices...

✅ Created logs index template
✅ Created qa-cache index

==================================================
📊 Index Creation Results:
==================================================
Logs template        ✅ SUCCESS
QA cache index       ✅ SUCCESS

✨ All indices created successfully!
```

**생성되는 인덱스**:
- `logs-*`: 로그 저장 (KNN 벡터 포함)
- `qa-cache`: QA 캐싱 (KNN 벡터 포함)

### 6단계: FastAPI 서버 실행

```bash
# 가상환경이 활성화된 상태에서
uvicorn app.main:app --reload --env-file .env.test
```

**서버 시작 메시지**:
```
INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
INFO:     Started reloader process [12345] using StatReload
INFO:     Started server process [12346]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
```

### 7단계: API 테스트

#### Swagger UI (권장)

브라우저에서 접속:
```
http://localhost:8000/docs
```

#### Health Check

```bash
curl http://localhost:8000/api/v1/health
```

**응답**:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "services": {
    "opensearch": "healthy",
    "openai": "healthy"
  }
}
```

### 종료

```bash
# FastAPI 서버 종료
# Ctrl+C

# OpenSearch 종료
sudo docker compose -f docker-compose.test.yml down

# 가상환경 비활성화
deactivate
```

---

## 🚀 프로덕션 배포

### Blue-Green 배포 아키텍처

```
                    ┌──────────┐
                    │  Nginx   │
                    │  (8080)  │
                    └────┬─────┘
                         │
          ┌──────────────┴──────────────┐
          │                             │
     ┌────▼────┐                   ┌────▼────┐
     │  Blue   │                   │  Green  │
     │ (8000)  │                   │ (8001)  │
     └────┬────┘                   └────┬────┘
          │                             │
          └──────────────┬──────────────┘
                         │
                   ┌─────▼─────┐
                   │OpenSearch │
                   │  Kafka    │
                   │ Logstash  │
                   └───────────┘
```

### 배포 스크립트 사용

```bash
# Blue 슬롯에 배포
./infra/dev/scripts/deploy.sh blue

# Green 슬롯에 배포
./infra/dev/scripts/deploy.sh green
```

### 수동 배포

#### 1. Docker 이미지 빌드

```bash
# 프로젝트 루트에서
docker build -t ai-service:latest .
```

**Dockerfile 구조**:
```dockerfile
FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

#### 2. 환경변수 설정

프로덕션 `.env` 파일 확인:
- `OPENSEARCH_HOST=opensearch` (컨테이너 이름)
- `KAFKA_BOOTSTRAP_SERVERS=kafka:19092`
- OpenAI API 키 설정

#### 3. Blue 슬롯 배포

```bash
cd infra/dev/docker
docker-compose -f docker-compose-blue.yaml up -d
```

#### 4. Nginx 설정

**Blue로 트래픽 전환** (`nginx-blue.conf`):
```nginx
upstream backend {
    server ai-service-blue:8000;
}

server {
    listen 8080;

    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /api/v1/chatbot/ask/stream {
        proxy_pass http://backend;
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding off;
    }
}
```

#### 5. Health Check

```bash
curl http://localhost:8080/api/v1/health
```

---

## 📡 API 엔드포인트

### 1. Health Check

**Endpoint**: `GET /api/v1/health`

**응답**:
```json
{
  "status": "healthy",
  "timestamp": "2025-01-15T10:30:00.000Z",
  "services": {
    "opensearch": "healthy",
    "openai": "healthy"
  }
}
```

### 2. 로그 분석 (단일)

**Endpoint**: `GET /api/v1/logs/{log_id}/analysis`

**Parameters**:
- `log_id` (path): 로그 ID (integer)
- `project_uuid` (query): 프로젝트 UUID

**예시**:
```bash
curl "http://localhost:8000/api/v1/logs/12345/analysis?project_uuid=550e8400-e29b-41d4-a716-446655440000"
```

**응답**:
```json
{
  "log_id": 12345,
  "summary": "user-service에서 NullPointerException 발생",
  "error_cause": "User 객체가 null인 상태에서 getName() 호출",
  "solution": "null 체크 로직 추가 필요",
  "tags": ["NullPointerException", "user-service", "Critical"],
  "analysis_type": "SINGLE",
  "analyzed_at": "2025-01-15T10:30:00.000Z"
}
```

### 3. 로그 분석 (Trace 기반)

**Endpoint**: `POST /api/v1/logs/trace/analysis`

**Request Body**:
```json
{
  "trace_id": "abc123-def456",
  "center_timestamp": "2025-01-15T10:30:00.000Z",
  "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
  "max_logs": 100,
  "time_window_seconds": 3
}
```

**특징**:
- ±3초 내 같은 trace_id 로그 수집
- 10개 이상 로그 시 **Map-Reduce 패턴** 자동 적용

### 4. 챗봇 (일반)

**Endpoint**: `POST /api/v1/chatbot/ask`

**Request Body**:
```json
{
  "question": "최근 24시간 동안 어떤 에러가 발생했어?",
  "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
  "chat_history": [
    {"role": "user", "content": "최근 에러 알려줘"},
    {"role": "assistant", "content": "NullPointerException 3건 발생했습니다"}
  ],
  "filters": {"level": "ERROR"},
  "time_range": {
    "start": "2025-01-14T00:00:00Z",
    "end": "2025-01-15T00:00:00Z"
  }
}
```

**응답**:
```json
{
  "answer": "최근 24시간 동안 user-service에서 NullPointerException 3건...",
  "from_cache": false,
  "related_logs": [
    {
      "log_id": 12345,
      "timestamp": "2025-01-15T10:30:00Z",
      "level": "ERROR",
      "message": "NullPointerException in UserService",
      "service_name": "user-service",
      "similarity_score": 0.95
    }
  ],
  "answered_at": "2025-01-15T10:35:00.000Z"
}
```

### 5. 챗봇 (스트리밍)

**Endpoint**: `POST /api/v1/chatbot/ask/stream`

**특징**:
- Server-Sent Events (SSE) 형식
- 실시간 타이핑 효과
- `data: [DONE]`으로 완료 신호

**응답 예시**:
```
data: 최근
data:  24시간
data:  동안
data:  user-service에서
data:  NullPointerException
data:  3건이
data:  발생했습니다.
data: [DONE]
```

**에러 시**:
```
data: [ERROR]
data: {"error": "Connection timeout"}
```

---

## 🐛 트러블슈팅

### 1. CRLF/LF 줄바꿈 문제 (WSL)

**증상**:
```bash
./test_local.sh start
# -bash: ./test_local.sh: cannot execute: required file not found
```

**원인**: Windows에서 생성된 파일이 CRLF 줄바꿈 사용

**해결**:
```bash
# 방법 1: sed 변환
sed -i 's/\r$//' test_local.sh
chmod +x test_local.sh

# 방법 2: dos2unix 사용
sudo apt-get install dos2unix
dos2unix test_local.sh
```

---

### 2. Docker 권한 문제

**증상**:
```
permission denied while trying to connect to the Docker daemon socket
```

**해결**:
```bash
# 1. docker 그룹에 추가
sudo usermod -aG docker $USER

# 2. WSL 완전 재시작 (PowerShell에서)
wsl --shutdown
# WSL 다시 시작

# 3. 확인
groups  # "docker" 포함되어야 함
```

**임시 해결** (권장하지 않음):
```bash
sudo docker compose up -d
```

---

### 3. Python 실행 경로 문제

**증상**:
```bash
python scripts/create_indices.py
# -bash: python: cannot execute: required file not found
```

**원인**: WSL에서 Windows pyenv shim 실행 시도

**해결**:
```bash
# python3 사용
python3 scripts/create_indices.py

# 또는 가상환경 생성
python3 -m venv venv
source venv/bin/activate
python scripts/create_indices.py
```

---

### 4. OpenSearch 연결 실패

**증상**:
```
ConnectionError: Failed to resolve 'opensearch'
```

**원인**: 로컬에서 실행 시 `localhost` 사용해야 함

**해결**:
```bash
# .env.test 파일 사용 (자동)
python scripts/create_indices.py

# 또는 환경변수 직접 지정
OPENSEARCH_HOST=localhost python scripts/create_indices.py
```

---

### 5. OpenSearch 인덱스 생성 실패

#### 5-1. space_type 에러

**증상**:
```
'hnsw' configuration does not support space type: 'cosinesimil'
```

**원인**: OpenSearch 2.x는 `cosinesimil` 미지원

**해결**: ✅ 이미 수정됨
- `space_type: "innerproduct"` 사용
- OpenAI 임베딩은 정규화됨 → innerproduct = cosine

#### 5-2. flattened 타입 에러

**증상**:
```
No handler for type [flattened] declared on field
```

**원인**: OpenSearch는 Elasticsearch의 `flattened` 타입 미지원

**해결**: ✅ 이미 수정됨
- `flattened` → `object` 변경
- 동일한 기능 유지

---

### 6. FastAPI 서버 시작 실패

#### 6-1. 포트 충돌

**증상**:
```
[Errno 98] Address already in use
```

**해결**:
```bash
# 포트 사용 프로세스 확인
lsof -i :8000

# 프로세스 종료
kill -9 <PID>

# 또는 다른 포트 사용
uvicorn app.main:app --port 8001
```

#### 6-2. 모듈 Import 에러

**증상**:
```
ModuleNotFoundError: No module named 'opensearchpy'
```

**해결**:
```bash
# 가상환경 활성화 확인
source venv/bin/activate

# 의존성 재설치
pip install -r requirements.txt
```

---

### 7. OpenAI API 에러

**증상**:
```
AuthenticationError: Invalid API key
```

**해결**:
1. `.env.test` 파일의 API 키 확인
2. SSAFY GMS에서 새 키 발급: https://gms.ssafy.io/
3. API 키 앞부분 확인: `S13P32A306-...`

---

### 8. CORS 에러 (프론트엔드 연동)

**증상**:
```
Access to XMLHttpRequest blocked by CORS policy
```

**해결**:
`.env.test` 파일에 프론트엔드 URL 추가:
```bash
CORS_ORIGINS=["http://localhost:3000","http://localhost:5173","http://localhost:8080"]
```

서버 재시작 필요

---

## ⚡ 성능 최적화

### 1. 3단계 캐싱 전략

#### 임베딩 캐시 (embedding_service.py)
- **목적**: 동일한 텍스트의 반복 임베딩 방지
- **방식**: 메모리 캐시 (cachetools)
- **효과**: 97-99% 비용 절감

#### QA 캐시 (chatbot_service.py)
- **목적**: 유사한 질문의 재계산 방지
- **방식**: OpenSearch 벡터 검색 (similarity >= 0.8)
- **검증**: 2단계 (의미 유사도 + 메타데이터 매칭)
- **TTL**: 동적 계산
  - 즉시성 질문 ("방금", "지금"): 10분
  - 일반 질문: 30분
  - 절대 날짜 질문 ("2024-01-15"): 1일

#### Trace 캐싱 (log_analysis_service.py)
- **목적**: 같은 trace_id 로그 재분석 방지
- **방식**: OpenSearch 저장 (ai_analysis 필드)
- **효과**: 동일 trace 재요청 시 즉시 응답

### 2. Map-Reduce 패턴

**트리거 조건**: 로그 개수 > 10개

**Map 단계**:
```python
# 15개 로그 → 3개 청크 (5개씩)
chunks = [logs[0:5], logs[5:10], logs[10:15]]

# 각 청크 요약
summaries = []
for chunk in chunks:
    summary = log_summarization_chain.ainvoke(chunk)
    summaries.append(summary)
```

**Reduce 단계**:
```python
# 요약 결합 및 최종 분석
combined = "\n\n".join(summaries)
result = log_analysis_chain.ainvoke(combined)
```

**효과**:
- LLM 토큰 제한 우회 (4k → 무제한)
- 병렬 처리로 속도 향상
- 대규모 trace 분석 가능

### 3. Chat History Truncation

**목적**: LLM 컨텍스트 관리

**전략**:
```python
def _truncate_history(chat_history, max_tokens=1500):
    """최근 메시지부터 역순으로 추가"""
    truncated = []
    total_tokens = 0

    for msg in reversed(chat_history):
        msg_tokens = len(encoding.encode(msg.content))
        if total_tokens + msg_tokens > max_tokens:
            break
        truncated.append(msg)
        total_tokens += msg_tokens

    return list(reversed(truncated))
```

**효과**:
- 토큰 예산 유지 (1500 tokens)
- 최신 컨텍스트 우선 보존

### 4. Vector Search 최적화

**HNSW 알고리즘 설정**:
```python
"method": {
    "name": "hnsw",
    "space_type": "innerproduct",  # OpenAI 임베딩 최적화
    "engine": "faiss",  # GPU 가속 가능
    "parameters": {
        "ef_construction": 128,  # 인덱스 빌드 품질
        "m": 16  # 연결 수
    }
}
```

**검색 파라미터**:
- `k=5`: 기본 검색 결과 수
- `similarity >= 0.8`: 유사도 임계값

---

## 📊 모니터링

### Prometheus 메트릭

**엔드포인트**: `http://localhost:18000/metrics`

**주요 메트릭**:
- `http_requests_total`: 총 요청 수
- `http_request_duration_seconds`: 요청 처리 시간
- `cache_hits_total`: 캐시 적중 횟수
- `opensearch_queries_total`: OpenSearch 쿼리 수
- `llm_tokens_total`: LLM 토큰 사용량

### Grafana 대시보드

**접속**: `http://localhost:3000`
**계정**: admin / admin123

**주요 패널**:
1. Request Rate (QPS)
2. Response Time (P50, P95, P99)
3. Cache Hit Rate
4. Error Rate
5. LLM Token Usage

---

## 📚 추가 리소스

### 프로젝트 문서

- [README.md](./README.md) - 프로젝트 개요
- [PROJECT_STATUS.md](./PROJECT_STATUS.md) - 현재 상태
- [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) - 배포 가이드
- [LOCAL_TEST_GUIDE.md](./LOCAL_TEST_GUIDE.md) - 로컬 테스트 상세 가이드

### 외부 문서

- [FastAPI 공식 문서](https://fastapi.tiangolo.com/)
- [OpenSearch 공식 문서](https://opensearch.org/docs/latest/)
- [LangChain 공식 문서](https://python.langchain.com/)
- [OpenAI API 문서](https://platform.openai.com/docs/)

---

## 🔄 업데이트 이력

| 날짜 | 버전 | 변경 내역 |
|------|------|----------|
| 2025-01-15 | 1.0.0 | 초기 문서 작성 |

---

**문의**: 프로젝트 팀 (S13P31A306)
