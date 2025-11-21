# 🧪 로컬 테스트 가이드

AI 로그 분석 서비스를 로컬 환경에서 테스트하는 방법을 안내합니다.

## 📋 목차

1. [빠른 시작](#-빠른-시작)
2. [사전 요구사항](#-사전-요구사항)
3. [테스트 환경 구성](#-테스트-환경-구성)
4. [API 테스트 방법](#-api-테스트-방법)
5. [통합 테스트 노트북 실행](#-통합-테스트-노트북-실행)
6. [트러블슈팅](#-트러블슈팅)

---

## 🚀 빠른 시작

### 1. 테스트 환경 시작

```bash
# 1. 테스트 환경 시작 (OpenSearch + 인덱스 생성)
./test_local.sh start

# 2. FastAPI 서버 실행
uvicorn app.main:app --reload --env-file .env.test

# 3. 브라우저에서 Swagger UI 접속
# http://localhost:8000/docs
```

### 2. 테스트

#### Health Check
```bash
curl http://localhost:8000/api/v1/health
```

#### Swagger UI
브라우저에서 http://localhost:8000/docs 접속

### 3. 종료

```bash
# FastAPI 서버 종료 (Ctrl+C)

# 테스트 환경 종료
./test_local.sh stop
```

---

## 📦 사전 요구사항

### 필수 소프트웨어

- **Docker Desktop** 또는 **Docker Engine** (최신 버전)
- **Python 3.11+**
- **pip** (Python 패키지 관리자)

### 메모리 요구사항

- **최소**: 4GB RAM
- **권장**: 8GB RAM (OpenSearch 최적 성능)

---

## 🛠️ 테스트 환경 구성

### 구성 요소

| 구성 요소 | 용도 | 포트 |
|----------|------|------|
| **OpenSearch** | 로그 저장 및 벡터 검색 | 9200 |
| **OpenSearch Dashboards** | 데이터 시각화 (선택사항) | 5601 |
| **FastAPI** | REST API 서버 | 8000 |

### 파일 구조

```
S13P31A306/
├── .env.test                  # 테스트용 환경변수 ⭐ NEW
├── docker-compose.test.yml    # 테스트용 OpenSearch ⭐ NEW
├── test_local.sh              # 테스트 자동화 스크립트 ⭐ NEW
├── LOCAL_TEST_GUIDE.md        # 이 문서 ⭐ NEW
├── .env                       # 프로덕션용 환경변수 (수정 안함)
├── requirements.txt
├── scripts/
│   ├── create_indices.py     # OpenSearch 인덱스 생성
│   └── test_connection.py    # 연결 테스트
└── app/
    ├── main.py
    └── ...
```

---

## 🎯 테스트 환경 명령어

### `test_local.sh` 스크립트 사용법

```bash
# 1. 시작 (OpenSearch 컨테이너 + 인덱스 생성)
./test_local.sh start

# 2. 종료
./test_local.sh stop

# 3. 재시작
./test_local.sh restart

# 4. 상태 확인
./test_local.sh status

# 5. 로그 확인
./test_local.sh logs

# 6. 완전 초기화 (모든 데이터 삭제)
./test_local.sh clean
```

### 수동 실행 (스크립트 없이)

```bash
# 1. OpenSearch 시작
docker-compose -f docker-compose.test.yml up -d

# 2. OpenSearch 준비 대기 (약 10-30초)
curl http://localhost:9200/_cluster/health

# 3. 인덱스 생성
python scripts/create_indices.py

# 4. FastAPI 서버 실행
uvicorn app.main:app --reload --env-file .env.test
```

---

## 🧪 API 테스트 방법

### 1. Swagger UI 사용 (권장)

**접속**: http://localhost:8000/docs

**장점**:
- ✅ 시각적 인터페이스
- ✅ 요청/응답 자동 검증
- ✅ 예제 데이터 자동 생성

**사용 방법**:
1. Swagger UI 접속
2. 원하는 엔드포인트 클릭 (예: `POST /api/v1/chatbot/ask`)
3. **Try it out** 버튼 클릭
4. 요청 바디 수정
5. **Execute** 버튼 클릭

### 2. curl 사용

#### Health Check
```bash
curl http://localhost:8000/api/v1/health
```

**예상 응답**:
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

#### 챗봇 질문 (기본)
```bash
curl -X POST http://localhost:8000/api/v1/chatbot/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "최근에 발생한 에러를 요약해줘",
    "project_uuid": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

#### 챗봇 질문 (히스토리 포함)
```bash
curl -X POST http://localhost:8000/api/v1/chatbot/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "그 중 가장 심각한 건?",
    "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
    "chat_history": [
      {"role": "user", "content": "최근 에러를 알려줘"},
      {"role": "assistant", "content": "NullPointerException 3건 발생했습니다"}
    ]
  }'
```

#### 챗봇 질문 (스트리밍)
```bash
curl -X POST http://localhost:8000/api/v1/chatbot/ask/stream \
  -H "Content-Type: application/json" \
  -d '{
    "question": "최근 에러 알려줘",
    "project_uuid": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

**예상 응답** (Server-Sent Events):
```
data: 최근
data:  24시간
data:  동안
data:  NullPointerException
data:  3건
data: ...
data: [DONE]
```

#### 로그 분석 (단일 로그)
```bash
curl "http://localhost:8000/api/v1/logs/12345/analysis?project_uuid=550e8400-e29b-41d4-a716-446655440000"
```

#### 로그 분석 (Trace 기반)
```bash
curl -X POST http://localhost:8000/api/v1/logs/trace/analysis \
  -H "Content-Type: application/json" \
  -d '{
    "trace_id": "abc123-def456",
    "center_timestamp": "2024-01-15T10:30:00.000Z",
    "project_uuid": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

### 3. Python requests 사용

```python
import requests

# Health Check
response = requests.get("http://localhost:8000/api/v1/health")
print(response.json())

# 챗봇 질문
response = requests.post(
    "http://localhost:8000/api/v1/chatbot/ask",
    json={
        "question": "최근 에러를 요약해줘",
        "project_uuid": "550e8400-e29b-41d4-a716-446655440000"
    }
)
print(response.json())
```

---

## 📓 통합 테스트 노트북 실행

### 1. Jupyter 설치

```bash
pip install jupyter
```

### 2. 노트북 실행

```bash
jupyter notebook test_llm_log_analysis.ipynb
```

### 3. 셀 실행

노트북에서 **Run All** 클릭

**테스트 항목**:
- ✅ 단일 로그 분석
- ✅ **Trace 기반 분석 (15개 로그 → Map-Reduce 트리거)**
- ✅ 챗봇 기본 질문 (캐시 미스)
- ✅ 챗봇 히스토리 포함 (캐시 스킵)
- ✅ 임베딩 캐시 검증

---

## 🔍 OpenSearch 데이터 확인

### 1. OpenSearch Dashboards 사용

**접속**: http://localhost:5601

**Dev Tools 사용**:
```json
// 인덱스 목록 확인
GET _cat/indices?v

// 로그 검색
GET logs-*/_search
{
  "query": {
    "match_all": {}
  },
  "size": 10
}

// QA 캐시 확인
GET qa-cache/_search
{
  "query": {
    "match_all": {}
  }
}
```

### 2. curl 사용

```bash
# 클러스터 상태
curl http://localhost:9200/_cluster/health?pretty

# 인덱스 목록
curl http://localhost:9200/_cat/indices?v

# 로그 검색
curl -X GET "http://localhost:9200/logs-*/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{"query": {"match_all": {}}, "size": 10}'
```

---

## 🐛 트러블슈팅

### 문제 1: OpenSearch 컨테이너가 시작되지 않음

**증상**:
```
ERROR: opensearch-test exited with code 137
```

**원인**: 메모리 부족

**해결**:
```bash
# Docker Desktop 설정에서 메모리 증가 (최소 4GB)
# 또는 OpenSearch 메모리 줄이기 (docker-compose.test.yml 수정)
OPENSEARCH_JAVA_OPTS=-Xms1g -Xmx1g
```

### 문제 2: 포트 충돌

**증상**:
```
Error starting userland proxy: listen tcp4 0.0.0.0:9200: bind: address already in use
```

**해결**:
```bash
# 기존 포트 사용 프로세스 확인
lsof -i :9200  # macOS/Linux
netstat -ano | findstr :9200  # Windows

# 기존 OpenSearch 종료
docker stop opensearch  # 프로덕션 컨테이너
docker stop opensearch-test
```

### 문제 3: 인덱스 생성 실패

**증상**:
```
ConnectionError: [Errno 111] Connection refused
```

**원인**: OpenSearch가 아직 준비되지 않음

**해결**:
```bash
# OpenSearch 준비 대기 (최대 30초)
for i in {1..30}; do
  curl -s http://localhost:9200/_cluster/health && break
  sleep 1
done

# 그 후 인덱스 생성 재시도
python scripts/create_indices.py
```

### 문제 4: OpenAI API 키 오류

**증상**:
```
AuthenticationError: Invalid API key
```

**해결**:
```bash
# .env.test 파일에서 API 키 확인
cat .env.test | grep OPENAI_API_KEY

# SSAFY GMS에서 새 키 발급
# https://gms.ssafy.io/
```

### 문제 5: ModuleNotFoundError

**증상**:
```
ModuleNotFoundError: No module named 'opensearchpy'
```

**해결**:
```bash
# 의존성 재설치
pip install -r requirements.txt

# 또는 개별 설치
pip install opensearch-py==2.4.2
```

### 문제 6: CORS 에러 (프론트엔드 연동 시)

**증상**:
```
Access to XMLHttpRequest blocked by CORS policy
```

**해결**:
`.env.test` 파일에서 CORS_ORIGINS 확인:
```bash
CORS_ORIGINS=["http://localhost:3000","http://localhost:5173"]
```

프론트엔드 URL을 추가하고 서버 재시작

---

## 📊 성능 모니터링

### FastAPI 로그 레벨 조정

`.env.test`:
```bash
LOG_LEVEL=DEBUG  # 상세 로그
LOG_LEVEL=INFO   # 일반 로그 (기본)
LOG_LEVEL=ERROR  # 에러만
```

### OpenSearch 메트릭 확인

```bash
# 노드 통계
curl http://localhost:9200/_nodes/stats?pretty

# 인덱스 통계
curl http://localhost:9200/logs-*/_stats?pretty
```

---

## 🧹 정리

### 테스트 데이터 유지 종료
```bash
./test_local.sh stop
```

### 완전 초기화 (모든 데이터 삭제)
```bash
./test_local.sh clean
```

### Docker 이미지 삭제 (디스크 공간 확보)
```bash
docker rmi opensearchproject/opensearch:2.11.0
docker rmi opensearchproject/opensearch-dashboards:2.11.0
```

---

## 💡 팁

### 1. 빠른 재시작

```bash
./test_local.sh restart
```

### 2. 백그라운드 실행

```bash
# FastAPI 서버를 백그라운드에서 실행
nohup uvicorn app.main:app --env-file .env.test &> fastapi.log &

# 프로세스 확인
ps aux | grep uvicorn

# 종료
pkill -f "uvicorn app.main:app"
```

### 3. Hot Reload 활성화

FastAPI 서버를 `--reload` 옵션으로 실행하면 코드 변경 시 자동 재시작:
```bash
uvicorn app.main:app --reload --env-file .env.test
```

### 4. VSCode에서 디버깅

`.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "FastAPI (Test)",
      "type": "python",
      "request": "launch",
      "module": "uvicorn",
      "args": [
        "app.main:app",
        "--reload",
        "--env-file",
        ".env.test"
      ],
      "jinja": true
    }
  ]
}
```

---

## 📚 추가 리소스

- [FastAPI 공식 문서](https://fastapi.tiangolo.com/)
- [OpenSearch 공식 문서](https://opensearch.org/docs/latest/)
- [LangChain 공식 문서](https://python.langchain.com/)
- [프로젝트 README.md](./README.md)

---

## ❓ 도움말

문제가 해결되지 않으면:

1. **로그 확인**: `./test_local.sh logs`
2. **상태 확인**: `./test_local.sh status`
3. **완전 초기화**: `./test_local.sh clean` → `./test_local.sh start`

---

**마지막 업데이트**: 2025-01-15
