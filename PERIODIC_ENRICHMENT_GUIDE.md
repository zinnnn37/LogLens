# Periodic Enrichment Scheduler 사용 가이드

## 개요

Periodic Enrichment Scheduler는 OpenSearch에 저장된 ERROR 로그 중 벡터화되지 않은 로그를 주기적으로 찾아 embedding을 생성하고 업데이트하는 서비스입니다.

### 주요 기능

- **주기적 실행**: 10분마다 자동 실행
- **배치 처리**: 한 번에 100개의 ERROR 로그 처리
- **자동 필터링**: log_vector 필드가 없는 ERROR 로그만 선택
- **고성능 embedding**: text-embedding-3-large 모델 사용 (1536차원)
- **캐싱**: 동일한 메시지는 캐시에서 재사용하여 API 호출 절감

### 기존 enrichment_consumer.py와의 차이점

| 특징 | enrichment_consumer.py | periodic_enrichment_scheduler.py |
|------|------------------------|----------------------------------|
| 트리거 | Kafka 이벤트 기반 | 10분 주기 스케줄러 |
| 대상 | Kafka에서 실시간 수신하는 신규 ERROR 로그 | OpenSearch에 이미 저장된 벡터 없는 ERROR 로그 |
| 처리 방식 | 50개 배치 또는 5초 타임아웃 | 100개 배치, 10분마다 |
| 용도 | 실시간 벡터화 (새 로그) | 백필 벡터화 (기존 로그) |

두 서비스를 함께 사용하면:
- **enrichment_consumer**: 실시간으로 들어오는 ERROR 로그를 즉시 벡터화
- **periodic_scheduler**: 누락된 벡터를 주기적으로 보완 (백필)

---

## 설치 및 설정

### 1. 의존성 설치

```bash
cd /mnt/c/SSAFY/third_project/AI/AI
pip install -r requirements.txt
```

**새로 추가된 패키지**:
- `apscheduler==3.10.4`: 주기적 작업 스케줄링

### 2. 환경 변수 설정

`.env` 파일에 다음 설정이 필요합니다:

```env
# OpenAI API (SSAFY GMS)
OPENAI_API_KEY=your-api-key
OPENAI_BASE_URL=https://api.openai.com/v1

# OpenSearch
OPENSEARCH_HOST=localhost
OPENSEARCH_PORT=9200
OPENSEARCH_USER=admin
OPENSEARCH_PASSWORD=Admin123!@#
OPENSEARCH_USE_SSL=false

# Embedding Model
EMBEDDING_MODEL=text-embedding-3-large
```

### 3. OpenSearch 연결 확인

스케줄러를 실행하기 전에 OpenSearch가 실행 중이고 접근 가능한지 확인하세요:

```bash
curl -u admin:Admin123!@# http://localhost:9200
```

---

## 실행 방법

### 방법 1: 직접 실행

```bash
cd /mnt/c/SSAFY/third_project/AI/AI
python3 periodic_enrichment_scheduler.py
```

**출력 예시**:
```
🚀 Starting Periodic Enrichment Scheduler
   Schedule: Every 10 minutes
   Batch size: 100 logs per cycle
   Model: text-embedding-3-large

✅ Scheduler started successfully
   Next run: 2025-11-27 15:20:00+09:00

⚡ Running first cycle immediately...
================================================================================
🔄 Periodic Enrichment Cycle #1 Started
   Time: 2025-11-27 15:10:00
================================================================================
[1/3] Querying OpenSearch for ERROR logs without vectors...
  Found 237 ERROR logs without vectors

[2/3] Generating embeddings for 100 logs...
  ✅ Generated 100 embeddings

[3/3] Updating OpenSearch documents...
  ✅ OpenSearch updated: 100 success, 0 failed

📊 Cycle #1 Summary:
   Duration: 12.45s
   Vectors created this cycle: 100
   Total vectors created: 100
   Average rate: 8.03 vectors/sec
   Total errors: 0
================================================================================
```

### 방법 2: 백그라운드 실행 (nohup)

```bash
nohup python3 periodic_enrichment_scheduler.py > periodic_enrichment.out 2>&1 &

# 로그 확인
tail -f periodic_enrichment.out
tail -f periodic_enrichment.log
```

### 방법 3: systemd 서비스로 등록 (Linux)

`/etc/systemd/system/periodic-enrichment.service`:

```ini
[Unit]
Description=Periodic Enrichment Scheduler for ERROR Logs
After=network.target opensearch.service

[Service]
Type=simple
User=your-username
WorkingDirectory=/path/to/AI/AI
Environment="PATH=/usr/bin:/usr/local/bin"
ExecStart=/usr/bin/python3 periodic_enrichment_scheduler.py
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**서비스 시작**:
```bash
sudo systemctl daemon-reload
sudo systemctl enable periodic-enrichment
sudo systemctl start periodic-enrichment
sudo systemctl status periodic-enrichment
```

### 방법 4: Docker Compose에 추가

`docker-compose.enrichment.yml`:

```yaml
version: '3.8'

services:
  periodic-enrichment:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: periodic-enrichment-scheduler
    command: python3 periodic_enrichment_scheduler.py
    env_file:
      - .env
    depends_on:
      - opensearch
      - ai-service
    restart: unless-stopped
    networks:
      - loglens-network
```

---

## 테스트

스케줄러를 실제 운영 환경에 배포하기 전에 테스트를 실행하세요.

### 컴포넌트 테스트 (안전)

```bash
python3 test_periodic_scheduler.py
```

이 테스트는:
1. OpenSearch에서 벡터 없는 ERROR 로그 조회
2. 샘플 로그 3개로 embedding 생성 테스트
3. OpenSearch 업데이트 가능 여부 확인 (실제 업데이트 안 함)

### 전체 사이클 테스트 (주의: 실제 업데이트)

```bash
python3 test_periodic_scheduler.py
# 프롬프트에서 'y' 입력
```

이 테스트는 실제로 OpenSearch를 업데이트합니다.

---

## 모니터링

### 로그 파일

스케줄러는 두 가지 로그를 생성합니다:

1. **콘솔 출력** (stdout)
   - 주요 이벤트 및 통계

2. **파일 로그** (`periodic_enrichment.log`)
   - 모든 상세 로그 기록
   - 에러 및 경고 메시지

**로그 확인**:
```bash
tail -f periodic_enrichment.log
grep ERROR periodic_enrichment.log
grep "Cycle #" periodic_enrichment.log
```

### 주요 메트릭

스케줄러는 다음 통계를 추적합니다:

- **Total runs**: 총 실행 횟수
- **Total vectors created**: 생성된 총 벡터 수
- **Total errors**: 발생한 오류 수
- **Average rate**: 초당 평균 벡터 생성 속도

### OpenSearch에서 진행 상황 확인

**벡터화된 ERROR 로그 개수 확인**:
```bash
curl -u admin:Admin123!@# -X GET "localhost:9200/*_*/_count" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        {"term": {"level": "ERROR"}},
        {"exists": {"field": "log_vector"}}
      ]
    }
  }
}
'
```

**벡터 없는 ERROR 로그 개수 확인**:
```bash
curl -u admin:Admin123!@# -X GET "localhost:9200/*_*/_count" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        {"term": {"level": "ERROR"}}
      ],
      "must_not": [
        {"exists": {"field": "log_vector"}}
      ]
    }
  }
}
'
```

---

## 설정 커스터마이징

스케줄러의 동작을 변경하려면 `periodic_enrichment_scheduler.py`의 `__init__` 메서드를 수정하세요:

```python
def __init__(self):
    self.batch_size = 100  # 한 번에 처리할 로그 개수 (기본: 100)
    self.interval_minutes = 10  # 실행 간격 (분 단위, 기본: 10)
```

**예시**:
- 5분마다 50개씩 처리: `self.interval_minutes = 5`, `self.batch_size = 50`
- 30분마다 200개씩 처리: `self.interval_minutes = 30`, `self.batch_size = 200`

---

## 문제 해결

### 1. "ModuleNotFoundError: No module named 'apscheduler'"

**원인**: APScheduler가 설치되지 않음

**해결**:
```bash
pip install apscheduler==3.10.4
```

### 2. "Failed to connect to OpenSearch"

**원인**: OpenSearch가 실행 중이 아니거나 연결 정보가 잘못됨

**해결**:
1. OpenSearch 실행 상태 확인:
   ```bash
   curl http://localhost:9200
   ```
2. `.env` 파일의 OpenSearch 설정 확인
3. 방화벽/포트 확인

### 3. "OpenAI API Error: 401 Unauthorized"

**원인**: OpenAI API 키가 잘못되었거나 만료됨

**해결**:
1. `.env` 파일의 `OPENAI_API_KEY` 확인
2. SSAFY GMS에서 API 키 상태 확인

### 4. 벡터가 생성되지 않음

**원인**: 모든 ERROR 로그가 이미 벡터화되었거나 ERROR 로그가 없음

**해결**:
1. OpenSearch에서 확인:
   ```bash
   # 벡터 없는 ERROR 로그 개수
   curl -u admin:Admin123!@# "localhost:9200/*_*/_count?q=level:ERROR AND NOT _exists_:log_vector"
   ```
2. 로그가 정상적으로 수집되고 있는지 확인

### 5. 메모리 부족 에러

**원인**: 배치 크기가 너무 큼

**해결**:
- `self.batch_size`를 50 또는 25로 줄임
- OpenAI API rate limit 확인

---

## 성능 최적화

### 1. 캐싱 활용

Embedding Service는 자동으로 캐싱을 사용합니다:
- 캐시 크기: 1000개
- TTL: 1시간
- 동일한 메시지는 캐시에서 즉시 반환

### 2. 배치 크기 조정

- **작은 배치 (50개)**: 메모리 절약, 빠른 피드백
- **큰 배치 (200개)**: 처리량 증가, 메모리 사용 증가

권장: 100개 (기본값)

### 3. 실행 간격 조정

- **짧은 간격 (5분)**: 빠른 백필, 리소스 사용 증가
- **긴 간격 (30분)**: 리소스 절약, 백필 속도 느림

권장: 10분 (기본값)

### 4. enrichment_consumer와 병행 사용

최적의 시스템 구성:
```
실시간 벡터화 (enrichment_consumer.py):
├─ Kafka에서 새 ERROR 로그 수신
├─ 50개 배치 또는 5초 타임아웃
└─ 즉시 벡터화

백필 벡터화 (periodic_enrichment_scheduler.py):
├─ 10분마다 실행
├─ 누락된 벡터 찾기
└─ 100개씩 보완
```

---

## 통계 비교 API 테스트 상태

### 테스트 파일 위치

1. **`app/tests/test_statistics_comparison.py`**
   - 단위 테스트 (pytest)
   - DB vs AI 비교 API의 모든 컴포넌트 테스트
   - Mock을 사용하여 독립적으로 테스트

2. **`test_ai_db_comprehensive.py`**
   - 100개 이상의 포괄적인 테스트 케이스
   - 다양한 분포 패턴 검증
   - 경계 케이스 및 에지 케이스 테스트

### 테스트 실행

**단위 테스트**:
```bash
cd /mnt/c/SSAFY/third_project/AI/AI
pytest app/tests/test_statistics_comparison.py -v
```

**종합 테스트**:
```bash
pytest test_ai_db_comprehensive.py -v
```

### API 엔드포인트

**DB vs AI 통계 비교 API**:
```
GET /api/v2-langgraph/statistics/compare?project_uuid={uuid}&time_hours=24&sample_size=100
```

**응답 예시**:
```json
{
  "project_uuid": "test-project",
  "db_statistics": {
    "total_logs": 50000,
    "error_count": 250,
    "error_rate": 0.5
  },
  "ai_statistics": {
    "estimated_total_logs": 49500,
    "estimated_error_count": 245,
    "confidence_score": 92
  },
  "accuracy_metrics": {
    "overall_accuracy": 98.5
  },
  "verdict": {
    "grade": "매우 우수",
    "can_replace_db": true
  }
}
```

---

## 추가 리소스

- **enrichment_consumer.py**: 실시간 이벤트 기반 벡터화
- **ENRICHMENT_CONSUMER.md**: Enrichment Consumer 상세 문서
- **VECTOR_EXPERIMENT_README.md**: Vector AI 실험 결과
- **app/services/embedding_service.py**: Embedding 서비스 구현
- **app/api/v1/embedding.py**: Batch Embedding API

---

## 라이선스

이 프로젝트는 SSAFY 특화 프로젝트의 일부입니다.

---

## 문의

문제가 발생하면 로그 파일 (`periodic_enrichment.log`)과 함께 이슈를 등록해주세요.
