# 🚀 OpenSearch 로그 시스템 배포 가이드

## 📋 목차
1. [문제 요약](#문제-요약)
2. [해결 방안](#해결-방안)
3. [로컬 테스트](#로컬-테스트)
4. [EC2 배포](#ec2-배포)
5. [리인덱싱](#리인덱싱)
6. [검증](#검증)
7. [트러블슈팅](#트러블슈팅)

---

## 문제 요약

### 🔴 증상
AI chatbot이 "응답시간이 가장 느린 API는?" 질의에 "응답 시간 데이터가 없습니다" 응답

### 🔍 근본 원인
1. **Logstash 파싱 문제**: `log_details` 필드가 JSON 객체로 제대로 파싱되지 않음
2. **필드 매핑 부재**: `log_details.http_method`, `log_details.request_uri` 등이 인덱싱되지 않음
3. **AI 쿼리 제한**: HTTP API 필수 필터로 인해 데이터 수집 범위가 너무 좁음

---

## 해결 방안

### ✅ 수정된 파일

1. **Logstash 설정**: `/mnt/c/SSAFY/third_project/AI/infra/infrastructure/logstash.conf`
   - `log_details` JSON 파싱 추가
   - 필드 타입 명시적 변환
   - 폴백 로직 추가

2. **Index Template**: `/mnt/c/SSAFY/third_project/AI/infra/infrastructure/create_opensearch_index.sh`
   - `request_uri`를 text + keyword로 변경
   - `trace_id`를 text + keyword로 변경
   - timestamp format 개선
   - Priority & Version 증가

3. **AI 코드**: `/mnt/c/SSAFY/third_project/AI/app/tools/performance_tools.py`
   - HTTP API 필수 필터 제거
   - 평면 구조 `execution_time` 지원
   - API 식별 폴백 로직 추가
   - 에러 메시지 개선

4. **리인덱싱 스크립트**: `/mnt/c/SSAFY/third_project/AI/infra/infrastructure/reindex_logs.sh`
   - 기존 인덱스를 새 매핑으로 재인덱싱

---

## 로컬 테스트

### 1. 환경 준비

```bash
cd /mnt/c/SSAFY/third_project/AI
```

### 2. Index Template 적용

```bash
cd infra/infrastructure

# OpenSearch가 로컬에서 실행 중이어야 함
# 환경 변수 설정 (필요시)
export OPENSEARCH_HOST="http://localhost:9200"

# Index Template 생성
./create_opensearch_index.sh
```

**예상 출력:**
```
🔍 OpenSearch 연결 확인: http://localhost:9200
✅ OpenSearch 연결 성공
🔍 OpenSearch 버전 확인...
   버전: 2.x.x
📝 인덱스 템플릿 생성 중...
✅ 인덱스 템플릿 생성 완료
```

### 3. Logstash 재시작

```bash
# docker-compose.yml이 있는 디렉토리에서
docker-compose restart logstash

# 로그 확인
docker logs -f logstash
```

**확인 사항:**
- ✅ Logstash가 정상적으로 시작됨
- ✅ Kafka에 연결됨
- ✅ log_details 파싱 에러가 없음

### 4. 테스트 로그 전송

BE 애플리케이션에서 API를 호출하여 로그 생성:

```bash
# 예: 사용자 생성 API 호출
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "테스트 사용자",
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 5. OpenSearch 데이터 확인

```bash
# 인덱스 목록 확인
curl -X GET "http://localhost:9200/_cat/indices/*_2025_*?v"

# 샘플 문서 조회 (프로젝트 UUID 대체 필요)
PROJECT_UUID="9911573f_8a1d_3b96_98b4_5a0def93513b"
curl -X POST "http://localhost:9200/${PROJECT_UUID}_2025_11/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d '{
    "size": 1,
    "query": {
      "exists": {"field": "log_details.execution_time"}
    }
  }'
```

**확인 사항:**
- ✅ `log_details`가 객체로 저장됨
- ✅ `log_details.http_method`가 존재
- ✅ `log_details.request_uri`가 존재
- ✅ `log_details.execution_time`이 integer 타입

### 6. AI Chatbot 테스트

```bash
# AI 서비스 재시작
docker-compose restart ai-service

# Chatbot API 테스트
curl -X POST "http://localhost:8000/api/v2/chatbot/ask/stream" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "응답시간이 가장 느린 API는?",
    "project_uuid": "9911573f-8a1d-3b96-98b4-5a0def93513b"
  }'
```

**예상 결과:**
```
=== 응답 시간이 느린 API 분석 (최근 168시간) ===
총 X건의 요청 분석, 상위 Y개 API 표시

1. POST /users
   📊 요청 수: 150건
   ⏱️  평균 응답 시간: 328ms
   ⏱️  최대 응답 시간: 1200ms
   ⏱️  최소 응답 시간: 50ms
   📈 P50 (중앙값): 280ms
   📈 P95: 850ms
   📈 P99: 1100ms
   등급: 🟢 빠름 (1초 이하)
```

---

## EC2 배포

### 1. 파일 동기화

로컬에서 수정한 파일을 EC2로 전송:

```bash
# SSH 키 경로 설정
SSH_KEY="~/K13A306T.pem"
EC2_HOST="ubuntu@i-0f47c6ea17d39b4ca.ap-northeast-2.compute.amazonaws.com"

# Logstash 설정 복사
scp -i $SSH_KEY \
  /mnt/c/SSAFY/third_project/AI/infra/infrastructure/logstash.conf \
  $EC2_HOST:~/ai-loglens/

# Index Template 스크립트 복사
scp -i $SSH_KEY \
  /mnt/c/SSAFY/third_project/AI/infra/infrastructure/create_opensearch_index.sh \
  $EC2_HOST:~/ai-loglens/

# 리인덱싱 스크립트 복사
scp -i $SSH_KEY \
  /mnt/c/SSAFY/third_project/AI/infra/infrastructure/reindex_logs.sh \
  $EC2_HOST:~/ai-loglens/
```

또는 Git을 통해 동기화:

```bash
# 로컬에서 커밋 & 푸시
cd /mnt/c/SSAFY/third_project/AI
git add .
git commit -m "Fix: OpenSearch log_details 파싱 및 성능 분석 개선"
git push origin main

# EC2에서 풀
ssh -i $SSH_KEY $EC2_HOST
cd ~/ai-loglens
git pull origin main
```

### 2. EC2에서 Index Template 적용

```bash
ssh -i $SSH_KEY $EC2_HOST

cd ~/ai-loglens

# OpenSearch 호스트 설정
export OPENSEARCH_HOST="https://opensearch.loglens.store"

# Index Template 생성
chmod +x create_opensearch_index.sh
./create_opensearch_index.sh
```

### 3. Logstash 재시작

```bash
# Logstash 컨테이너 재시작
docker-compose restart logstash

# 로그 확인 (에러 없는지 확인)
docker logs --tail 100 -f logstash
```

### 4. AI 서비스 재배포

```bash
# AI 서비스 재빌드 및 재시작
docker-compose build ai-service
docker-compose up -d ai-service

# Blue-Green 배포인 경우
docker-compose restart ai-service-blue
# 또는
docker-compose restart ai-service-green

# 로그 확인
docker logs --tail 50 -f ai-service-blue
```

---

## 리인덱싱

### 배경

기존에 저장된 로그 데이터는 새로운 매핑을 적용받지 못하므로, 리인덱싱이 필요합니다.

### ⚠️ 주의사항

- 리인덱싱은 시간이 오래 걸릴 수 있습니다 (데이터 양에 따라 수십 분 ~ 수 시간)
- 다운타임은 발생하지 않지만, OpenSearch 부하가 증가합니다
- 프로덕션 환경에서는 트래픽이 적은 시간대에 수행하는 것이 좋습니다

### 실행 방법

```bash
cd ~/ai-loglens

# 환경 변수 설정
export OPENSEARCH_HOST="https://opensearch.loglens.store"
export OPENSEARCH_USER="admin"
export OPENSEARCH_PASS="your-password"

# 1. 특정 프로젝트의 현재 월 리인덱싱
./reindex_logs.sh 9911573f-8a1d-3b96-98b4-5a0def93513b

# 2. 특정 프로젝트의 특정 월 리인덱싱
./reindex_logs.sh 9911573f-8a1d-3b96-98b4-5a0def93513b 2025_11

# 3. 모든 프로젝트의 현재 월 리인덱싱
./reindex_logs.sh
```

**스크립트 실행 흐름:**
1. 리인덱싱할 인덱스 목록 확인
2. 사용자 확인 프롬프트 (y/N)
3. 각 인덱스에 대해:
   - 새 인덱스 생성 (`원본_reindexed`)
   - 데이터 복사 (비동기)
   - 진행 상황 모니터링
   - 문서 수 검증

### Alias 전환 (선택사항)

리인덱싱 후 다운타임 없이 새 인덱스로 전환:

```bash
# 예: 9911573f_8a1d_3b96_98b4_5a0def93513b_2025_11 인덱스를 전환
curl -X POST "https://opensearch.loglens.store/_aliases" \
  -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
  -H 'Content-Type: application/json' \
  -d '{
    "actions": [
      {
        "remove": {
          "index": "9911573f_8a1d_3b96_98b4_5a0def93513b_2025_11",
          "alias": "9911573f_8a1d_3b96_98b4_5a0def93513b_current"
        }
      },
      {
        "add": {
          "index": "9911573f_8a1d_3b96_98b4_5a0def93513b_2025_11_reindexed",
          "alias": "9911573f_8a1d_3b96_98b4_5a0def93513b_current"
        }
      }
    ]
  }'
```

### 기존 인덱스 삭제

검증 완료 후 기존 인덱스 삭제 (선택):

```bash
# ⚠️ 주의: 되돌릴 수 없습니다!
curl -X DELETE "https://opensearch.loglens.store/9911573f_8a1d_3b96_98b4_5a0def93513b_2025_11" \
  -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}"
```

---

## 검증

### 1. OpenSearch 쿼리 직접 테스트

```bash
PROJECT_UUID="9911573f_8a1d_3b96_98b4_5a0def93513b"
INDEX="${PROJECT_UUID//-/_}_2025_11"

# 1. log_details.execution_time 필드가 있는 문서 확인
curl -X POST "https://opensearch.loglens.store/${INDEX}/_search?pretty" \
  -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
  -H 'Content-Type: application/json' \
  -d '{
    "size": 5,
    "query": {
      "exists": {"field": "log_details.execution_time"}
    },
    "_source": ["log_details", "timestamp"]
  }'

# 2. log_details.http_method로 필터링
curl -X POST "https://opensearch.loglens.store/${INDEX}/_search?pretty" \
  -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
  -H 'Content-Type: application/json' \
  -d '{
    "size": 5,
    "query": {
      "term": {"log_details.http_method": "POST"}
    },
    "_source": ["log_details.http_method", "log_details.request_uri", "log_details.execution_time"]
  }'

# 3. API별 평균 응답 시간 집계 (AI 코드와 동일한 쿼리)
curl -X POST "https://opensearch.loglens.store/${INDEX}/_search?pretty" \
  -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
  -H 'Content-Type: application/json' \
  -d '{
    "size": 0,
    "aggs": {
      "by_api": {
        "terms": {
          "field": "log_details.request_uri",
          "size": 10
        },
        "aggs": {
          "avg_time": {
            "avg": {"field": "log_details.execution_time"}
          }
        }
      }
    }
  }'
```

### 2. AI Chatbot 통합 테스트

```bash
# 1. 응답시간이 가장 느린 API
curl -X POST "https://ai.loglens.store/api/v2/chatbot/ask/stream" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "응답시간이 가장 느린 API는?",
    "project_uuid": "9911573f-8a1d-3b96-98b4-5a0def93513b"
  }'

# 2. 특정 API의 평균 응답시간
curl -X POST "https://ai.loglens.store/api/v2/chatbot/ask/stream" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "POST /users API의 평균 응답시간은?",
    "project_uuid": "9911573f-8a1d-3b96-98b4-5a0def93513b"
  }'

# 3. 최근 24시간 느린 API
curl -X POST "https://ai.loglens.store/api/v2/chatbot/ask/stream" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "최근 24시간 동안 응답시간이 가장 느린 API 3개는?",
    "project_uuid": "9911573f-8a1d-3b96-98b4-5a0def93513b"
  }'
```

### 3. 모니터링

```bash
# 로그 확인
docker logs --tail 100 -f ai-service-blue

# OpenSearch 클러스터 상태
curl -X GET "https://opensearch.loglens.store/_cluster/health?pretty" \
  -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}"

# 인덱스 통계
curl -X GET "https://opensearch.loglens.store/${INDEX}/_stats?pretty" \
  -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}"
```

---

## 트러블슈팅

### ❌ "응답 시간 데이터가 없습니다" 여전히 발생

**원인**: 데이터가 아직 수집되지 않았거나, 쿼리 시간 범위 밖

**해결**:
```bash
# 1. 인덱스에 데이터가 있는지 확인
curl -X GET "https://opensearch.loglens.store/${INDEX}/_count?pretty"

# 2. execution_time 필드가 있는 문서 수 확인
curl -X POST "https://opensearch.loglens.store/${INDEX}/_count?pretty" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "bool": {
        "should": [
          {"exists": {"field": "log_details.execution_time"}},
          {"exists": {"field": "execution_time"}},
          {"exists": {"field": "duration"}}
        ]
      }
    }
  }'

# 3. 최근 로그의 timestamp 확인
curl -X POST "https://opensearch.loglens.store/${INDEX}/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d '{
    "size": 1,
    "sort": [{"timestamp": "desc"}],
    "_source": ["timestamp"]
  }'
```

### ❌ Logstash에서 log_details 파싱 에러

**증상**: Logstash 로그에 JSON 파싱 실패 메시지

**해결**:
```bash
# Logstash 로그 확인
docker logs --tail 100 logstash | grep -i "parse\|error"

# BE 애플리케이션에서 보내는 원본 데이터 확인 (Kafka)
# Kafka 메시지 확인 도구 사용

# log_details가 이미 JSON 객체인지, 문자열인지 확인
```

### ❌ OpenSearch 스크립트 실행 에러

**증상**: "script compilation error" 또는 "field doesn't exist"

**원인**: 필드가 존재하지 않거나 타입이 맞지 않음

**해결**:
```bash
# 매핑 확인
curl -X GET "https://opensearch.loglens.store/${INDEX}/_mapping?pretty"

# 특정 필드 매핑 확인
curl -X GET "https://opensearch.loglens.store/${INDEX}/_mapping/field/log_details.http_method?pretty"

# 샘플 문서에서 실제 필드 구조 확인
curl -X POST "https://opensearch.loglens.store/${INDEX}/_search?pretty" \
  -H 'Content-Type: application/json' \
  -d '{
    "size": 1,
    "_source": ["log_details"]
  }'
```

### ❌ 리인덱싱 중 타임아웃

**원인**: 데이터가 너무 많거나 클러스터 리소스 부족

**해결**:
```bash
# 배치 크기 줄이기 (스크립트 수정 필요)
# reindex_logs.sh에서:
# "size": 1000  # 기본값에서 줄이기

# 또는 수동으로 작은 단위로 리인덱싱
curl -X POST "https://opensearch.loglens.store/_reindex" \
  -H 'Content-Type: application/json' \
  -d '{
    "source": {
      "index": "원본_인덱스",
      "size": 500
    },
    "dest": {
      "index": "대상_인덱스"
    }
  }'
```

---

## 📞 지원

문제가 계속되면:
1. OpenSearch 로그 확인: `docker logs opensearch`
2. Logstash 로그 확인: `docker logs logstash`
3. AI 서비스 로그 확인: `docker logs ai-service-blue`
4. GitHub Issue 생성 또는 팀원에게 문의

---

## 📝 체크리스트

배포 전:
- [ ] 로컬에서 모든 변경사항 테스트 완료
- [ ] OpenSearch Index Template 생성 확인
- [ ] Logstash log_details 파싱 테스트
- [ ] AI chatbot 응답 확인

배포 후:
- [ ] EC2에서 Index Template 적용 완료
- [ ] Logstash 재시작 및 로그 확인
- [ ] AI 서비스 재시작 및 로그 확인
- [ ] 리인덱싱 완료 (필요시)
- [ ] 통합 테스트 완료
- [ ] 모니터링 대시보드 확인

---

**마지막 업데이트**: 2025-11-12
**작성자**: Claude Code
**버전**: 1.0
