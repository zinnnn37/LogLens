# OpenSearch log_vector 필드 타입 에러 해결 가이드

## 문제 상황

**에러 메시지:**
```
Analysis failed: RequestError(400, 'search_phase_execution_exception',
"failed to create query: Field 'log_vector' is not knn_vector type.")
```

**원인:**
- OpenSearch 인덱스의 `log_vector` 필드가 `knn_vector` 타입으로 매핑되지 않음
- 인덱스가 템플릿 적용 전에 생성되어 구 매핑을 사용 중

---

## 해결 방법 비교

| 방법 | 속도 | 안전성 | 데이터 보존 | 다운타임 | 권장 |
|------|------|--------|-------------|----------|------|
| **옵션 A: 빠른 재생성** | ⚡ 빠름 (초) | ⚠️ 위험 | ❌ 손실 | 있음 | 테스트 환경 |
| **옵션 B: Reindex 마이그레이션** | 🐢 느림 (분) | ✅ 안전 | ✅ 보존 | 거의 없음 | **프로덕션** |
| **옵션 C: 임시 우회 (코드 수정)** | ⚡ 즉시 | ⚠️ 임시 | ✅ 유지 | 없음 | 긴급 복구용 |

---

## 1️⃣ 진단 먼저 실행

서버에 접속하여 현재 상태를 확인하세요:

```bash
cd ~/ai-loglens/scripts  # 또는 스크립트가 있는 위치로 이동
chmod +x diagnose_index_mapping.sh
./diagnose_index_mapping.sh
```

**확인 사항:**
- ✅ logs-template 존재 여부
- ✅ KNN 플러그인 설치 여부
- ❌ log_vector 필드가 knn_vector가 아닌 인덱스 목록

---

## 2️⃣ 해결 방법 선택

### 옵션 A: 빠른 재생성 (테스트/개발 환경 권장)

**장점:**
- 매우 빠름 (수 초)
- 간단한 실행

**단점:**
- 🔴 **모든 데이터 손실**
- Logstash가 다시 데이터를 전송해야 함

**실행 방법:**
```bash
chmod +x fix_index_mapping_fast.sh

# 인덱스 하나씩 재생성
./fix_index_mapping_fast.sh a0b4a1a9_d2ae_3672_a0e1_3a4863922226_2025_11
./fix_index_mapping_fast.sh 9f8c4c75_a936_3ab6_92a5_d1309cd9f87e_2025_11
```

---

### 옵션 B: Reindex 마이그레이션 (프로덕션 권장) ⭐

**장점:**
- ✅ 데이터 100% 보존
- ✅ 백업 자동 생성
- ✅ 안전한 롤백 가능

**단점:**
- 느림 (문서 1000개당 ~1초)
- 인덱스 교체 시 짧은 순간 중단

**실행 방법:**
```bash
chmod +x fix_index_mapping_reindex.sh

# 인덱스 하나씩 마이그레이션
./fix_index_mapping_reindex.sh a0b4a1a9_d2ae_3672_a0e1_3a4863922226_2025_11
./fix_index_mapping_reindex.sh 9f8c4c75_a936_3ab6_92a5_d1309cd9f87e_2025_11
```

**프로세스:**
1. 기존 인덱스 → 새 인덱스로 reindex
2. 문서 개수 검증
3. 사용자 확인 후 인덱스 교체
4. 백업 인덱스 보관

---

### 옵션 C: 임시 우회 (긴급 복구용)

**장점:**
- 즉시 서비스 복구
- 데이터 손실 없음

**단점:**
- 근본 해결 아님 (유사도 검색 비활성화)
- 성능 저하 (캐시 미사용)

**적용 방법:**
코드가 이미 수정되었습니다 (`log_analysis_service.py` Line 141-167).

AI 서버를 재시작하세요:
```bash
# Docker 환경
docker restart ai-loglens

# 또는 직접 실행 환경
sudo systemctl restart ai-loglens
```

---

## 3️⃣ 검증

### API 테스트

Swagger UI에서 테스트:
```
GET /api/v1/logs/{log_id}/analysis?project_uuid={uuid}
```

**이전 에러:**
```json
{
  "detail": "Analysis failed: RequestError(400, 'search_phase_execution_exception'...)"
}
```

**정상 응답:**
```json
{
  "log_id": 388156817,
  "analysis": {
    "summary": "...",
    "error_cause": "...",
    "solution": "..."
  },
  "from_cache": false
}
```

### 로그 확인

AI 서버 로그에서 확인:
```bash
docker logs -f ai-loglens

# 성공 케이스
✅ Found log in index: ...
📊 Validation: ...
INFO: ... 200 OK

# 실패 케이스 (수정 전)
✅ Found log in index: ...
INFO: ... 500 Internal Server Error
```

---

## 4️⃣ 자주 묻는 질문

### Q1: 모든 인덱스를 수정해야 하나요?
A: 진단 스크립트 결과에서 log_vector가 knn_vector가 아닌 인덱스만 수정하면 됩니다.

### Q2: 옵션 C만 적용하면 안 되나요?
A: 긴급 복구로는 가능하지만, 유사도 검색(캐싱)이 작동하지 않아 LLM API 비용이 증가합니다.

### Q3: Reindex 중에 서비스가 중단되나요?
A: Reindex 작업 자체는 백그라운드로 실행되어 서비스에 영향 없습니다. 인덱스 교체 시에만 짧은 순간 중단됩니다.

### Q4: 백업은 언제 삭제하나요?
A: 마이그레이션 후 최소 1주일은 유지하고, 정상 작동 확인 후 삭제하세요.

```bash
# 백업 삭제 (신중하게!)
curl -X DELETE "http://localhost:9200/a0b4a1a9_d2ae_3672_a0e1_3a4863922226_2025_11_backup_20251109_123456"
```

### Q5: 새로운 월 인덱스는 자동으로 템플릿이 적용되나요?
A: 네! `logs-template`이 설정되어 있으면 새로 생성되는 인덱스(예: `*_2025_12`)는 자동으로 올바른 매핑을 갖습니다.

---

## 5️⃣ 트러블슈팅

### KNN 플러그인이 없는 경우

```bash
# OpenSearch 컨테이너 확인
docker exec -it opensearch bash

# 플러그인 설치
bin/opensearch-plugin install opensearch-knn

# 재시작
docker restart opensearch
```

### Reindex가 너무 느린 경우

```bash
# Reindex 설정 조정
curl -X POST "http://localhost:9200/_reindex" \
-H 'Content-Type: application/json' \
-d '{
  "source": {
    "index": "source_index",
    "size": 1000  # 배치 크기 증가
  },
  "dest": {
    "index": "dest_index"
  }
}'
```

---

## 📞 지원

문제가 해결되지 않으면:
1. `diagnose_index_mapping.sh` 결과 공유
2. AI 서버 로그 (`docker logs ai-loglens`) 공유
3. OpenSearch 버전 및 KNN 플러그인 버전 확인

---

**작성:** 2025-11-09
**업데이트:** 2025-11-09
