# Vector AI vs OpenSearch 집계 비교 실험

## 📋 개요

Vector KNN 검색 + LLM 추론이 전통적인 OpenSearch 집계 쿼리를 대체할 수 있는지 검증하는 실험 프레임워크입니다.

## 🎯 실험 목표

1. **정확도 검증**: Vector 샘플링 + LLM 추론이 DB 집계와 얼마나 유사한 결과를 생성하는가?
2. **성능 비교**: 전체 스캔 vs k개 샘플 조회의 성능 차이는?
3. **실용성 평가**: 90% 이상 정확도 달성 시 실제 활용 가능한가?

## 🏗️ 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    실험 비교 프로세스                          │
└─────────────────────────────────────────────────────────────┘

[Ground Truth: OpenSearch 집계]
┌──────────────────────────────────┐
│ OpenSearch Aggregation Query    │
│ - 전체 데이터 스캔               │
│ - 정확도 100%                   │
│ - 처리 시간: ~50-200ms          │
└──────────────────────────────────┘
           ↓
    ┌──────────┐
    │ DB Stats │
    └──────────┘
           ↓
    [비교 & 평가]
           ↑
    ┌──────────┐
    │ AI Stats │
    └──────────┘
           ↑
[Vector AI: KNN + LLM 추론]
┌──────────────────────────────────┐
│ 1. Vector KNN Search (k개)      │
│    - 유사도 기반 샘플링          │
│    - 처리 시간: ~100-500ms      │
├──────────────────────────────────┤
│ 2. LLM Statistical Inference    │
│    - 샘플 비율 계산              │
│    - 전체 통계 추론              │
│    - 처리 시간: ~1000-3000ms    │
└──────────────────────────────────┘
```

## 📁 구현 파일

### 1. 모델 정의
- **파일**: `app/models/experiment.py`
- **역할**: Pydantic 응답 모델 정의
- **주요 클래스**:
  - `VectorAIResult`: AI 추론 결과
  - `AccuracyMetrics`: 정확도 지표
  - `ExperimentConclusion`: 실험 결론
  - `ExperimentComparison`: 전체 비교 결과

### 2. 실험 도구
- **파일**: `app/tools/vector_experiment_tools.py`
- **역할**: 핵심 실험 로직
- **주요 함수**:
  ```python
  def _get_db_statistics_for_experiment(project_uuid, time_hours=24)
      """OpenSearch 집계로 Ground Truth 조회"""

  async def _vector_search_all(project_uuid, k, time_hours=24)
      """Vector KNN으로 k개 샘플 수집"""

  async def _llm_estimate_from_vectors(samples, total_logs_hint, time_hours)
      """LLM이 샘플 비율을 전체에 적용하여 통계 추론"""
      # ⚠️ level_counts를 받지 않음 (진짜 추론!)

  def _calculate_accuracy_for_experiment(db_stats, ai_stats)
      """가중 평균 정확도 계산"""
      # 총 로그 수(30%), ERROR(30%), 에러율(20%), WARN/INFO(각 10%)
  ```

### 3. API 엔드포인트
- **파일**: `app/api/v2_langgraph/experiments.py`
- **엔드포인트**: `GET /api/v2-langgraph/experiments/vector-vs-db`
- **파라미터**:
  - `project_uuid` (required): 프로젝트 UUID
  - `time_hours` (default: 24): 분석 기간
  - `k_values` (default: "100,500,1000"): 테스트할 k 값들 (콤마 구분)

### 4. 라우터 등록
- **파일**: `app/main.py`
- **변경사항**:
  - experiments 라우터 import
  - OpenAPI 태그 추가
  - 라우터 등록

## 🔑 핵심 개선 사항

### 기존 문제: `statistics_comparison_tools.py`
```python
# ❌ 문제: LLM에게 정답을 알려주고 검증
ai_stats = _llm_estimate_statistics(
    log_samples,
    db_stats["total_logs"],
    time_hours,
    level_counts  # ← DB에서 조회한 실제 값을 그대로 전달!
)
# 결과: 100% 정확도 (의미 없는 검증)
```

### 새로운 접근: `vector_experiment_tools.py`
```python
# ✅ 개선: LLM이 샘플만 보고 추론
ai_stats = await _llm_estimate_from_vectors(
    samples,
    db_stats["total_logs"],  # 총 개수만 힌트
    time_hours
    # level_counts 파라미터 없음!
)
# 결과: 진짜 AI 추론 능력 평가
```

## 🧪 테스트 방법

### 방법 1: API 호출 (권장)

#### 1단계: 서버 시작
```bash
cd /mnt/c/SSAFY/third_project/AI/S13P31A306

# 환경 변수 로드 및 서버 시작
bash start_server.sh

# 또는 수동으로:
source venv/bin/activate
set -a && source .env && set +a
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

#### 2단계: API 테스트
```bash
# httpx 테스트 스크립트 실행
./venv/bin/python test_vector_experiment.py
```

또는 curl로 직접 호출:
```bash
curl -X GET "http://localhost:8000/api/v2-langgraph/experiments/vector-vs-db?\
project_uuid=3a73c7d4-8176-3929-b72f-d5b921daae67&\
time_hours=24&\
k_values=100,500,1000" | jq
```

#### 3단계: Swagger UI에서 테스트
1. 브라우저에서 http://localhost:8000/docs 접속
2. `Vector AI Experiments` 섹션 찾기
3. `GET /api/v2-langgraph/experiments/vector-vs-db` 클릭
4. "Try it out" 클릭
5. 파라미터 입력 후 "Execute"

### 방법 2: 직접 함수 호출 (개발 환경)

```bash
# OpenSearch가 localhost:9200으로 접근 가능한 경우
./venv/bin/python test_vector_experiment_direct.py
```

⚠️ **주의**: Docker 내부에서만 "opensearch" 호스트명이 작동합니다.
외부에서 테스트하려면 `app/core/config.py`에서 `OPENSEARCH_HOST`를 임시로 "localhost"로 변경하세요.

## 📊 실험 결과 예시

### API 응답 구조
```json
{
  "project_uuid": "3a73c7d4-8176-3929-b72f-d5b921daae67",
  "experiment_time": "2025-11-18T10:30:00Z",
  "time_range_hours": 24,

  "ground_truth": {
    "total_logs": 10000,
    "error_count": 250,
    "warn_count": 1500,
    "info_count": 8250,
    "error_rate": 2.5,
    "processing_time_ms": 85.2
  },

  "vector_ai_experiments": [
    {
      "k": 100,
      "ai_result": {
        "estimated_total_logs": 10000,
        "estimated_error_count": 245,
        "estimated_error_rate": 2.45,
        "confidence_score": 75,
        "processing_time_ms": 1250.3
      },
      "accuracy_metrics": {
        "overall_accuracy": 92.5
      }
    },
    {
      "k": 500,
      "ai_result": {
        "estimated_error_count": 248,
        "estimated_error_rate": 2.48,
        "confidence_score": 88,
        "processing_time_ms": 2100.7
      },
      "accuracy_metrics": {
        "overall_accuracy": 95.2
      }
    }
  ],

  "conclusion": {
    "best_k": 500,
    "best_accuracy": 95.2,
    "feasible": true,
    "grade": "매우 우수",
    "recommendation": "Vector AI가 DB 집계를 완전히 대체할 수 있습니다.",
    "performance_vs_db": {
      "db_processing_time_ms": 85.2,
      "best_ai_processing_time_ms": 2100.7,
      "speed_ratio": 24.6,
      "accuracy_trade_off": 4.8
    }
  },

  "insights": [
    "k 값이 증가할수록 정확도가 향상됨 (k=100: 92.5% → k=500: 95.2%)",
    "ERROR 로그 개수 추론 정확도 96.8% (이상 탐지에 활용 가능)",
    "전체 로그의 5.00%만 조회하여 95.2% 정확도 달성",
    "DB 집계가 Vector AI 대비 2360% 더 빠름"
  ]
}
```

### 정확도 등급

| 종합 정확도 | 등급 | DB 대체 가능 | 설명 |
|----------|------|----------|------|
| 95% 이상 | 매우 우수 | ✅ 가능 | 프로덕션 환경 즉시 적용 가능 |
| 90-95% | 우수 | ✅ 가능 | 일부 조정 후 프로덕션 적용 |
| 80-90% | 양호 | ❌ 불가 | 보조 도구로 활용 가능 |
| 70-80% | 보통 | ❌ 불가 | 프롬프트 튜닝 필요 |
| 70% 미만 | 미흡 | ❌ 불가 | 근본적인 개선 필요 |

## 💡 실험 시사점

### ✅ 성공 시 (90% 이상 정확도)

**의미**:
- Vector 검색 + LLM이 복잡한 집계 쿼리를 대체할 수 있음
- 전체 데이터 스캔 없이 k개 샘플만으로 통계 추론 가능

**활용 방안**:
1. **DB 부하 감소**: 전체 스캔 → k개 샘플 조회 (5-10% 데이터만 사용)
2. **자연어 통계 질의**: "오늘 에러가 어제보다 많아?" 같은 질문에 즉시 응답
3. **유연한 지표 계산**: 새로운 통계도 LLM이 자동으로 계산
4. **실시간 대시보드**: AI 기반 통계 캐싱 레이어 구축

**트레이드오프**:
- 정확도: 95% (5% 오차 허용)
- 속도: DB 집계보다 10-25배 느림 (Vector 검색 + LLM 호출 비용)

### ❌ 실패 시 (90% 미만)

**원인 분석**:
1. **샘플링 정확도 부족**: k 값 증가 또는 층화 샘플링 개선 필요
2. **LLM 추론 능력 한계**: 프롬프트 엔지니어링 또는 Few-shot 예제 추가
3. **데이터 분포 편향**: 희소 이벤트(ERROR) 샘플링 부족

**개선 방안**:
1. k 값 증가 (1000 → 5000 → 10000)
2. 층화 샘플링 강화 (ERROR/WARN 우선 샘플링)
3. LLM 모델 업그레이드 (gpt-4o-mini → gpt-4o)
4. Few-shot 예제 추가 (과거 성공 사례 제공)

## 🔍 기술적 특징

### 1. 진짜 AI 추론
- 기존: LLM에게 정답(level_counts) 제공 → 100% 정확도 (의미 없음)
- 신규: 샘플만 제공 → LLM이 비율 계산 → 진짜 추론 능력 평가

### 2. Vector DB 활용
- 별도 ChromaDB 불필요 (OpenSearch가 Vector DB 역할)
- text-embedding-3-large (1536차원) 이미 구축됨
- KNN 알고리즘으로 유사 로그 샘플링

### 3. 가중 정확도 계산
```python
종합 정확도 =
    총 로그 수 정확도 * 0.3 +
    ERROR 수 정확도 * 0.3 +
    에러율 정확도 * 0.2 +
    WARN 수 정확도 * 0.1 +
    INFO 수 정확도 * 0.1
```

### 4. k 값별 비교
- k=100: 빠르지만 부정확 (샘플 부족)
- k=500: 균형잡힌 성능/정확도
- k=1000: 느리지만 정확 (충분한 샘플)
- k=5000: DB 집계와 유사한 수준

## 📝 TODO

- [ ] OpenSearch가 Docker 외부에서 localhost:9200으로 접근 가능하도록 config 개선
- [ ] 실제 데이터로 실험 실행 및 결과 문서화
- [ ] k=5000, k=10000 등 대규모 샘플 테스트
- [ ] 층화 샘플링 vs 랜덤 Vector 샘플링 비교 실험
- [ ] LLM 모델별 비교 (gpt-4o-mini vs gpt-4o)
- [ ] 프롬프트 튜닝 (Few-shot 예제 추가)

## 🚀 다음 단계

1. **프로덕션 환경 테스트**: EC2에서 실제 데이터로 실험
2. **결과 분석**: 어떤 k 값이 최적인지 판단
3. **적용 여부 결정**:
   - 90% 이상 → 챗봇 V2에 Vector AI 통계 기능 추가
   - 90% 미만 → 개선 작업 또는 DB 집계 유지
4. **문서화**: 실험 결과를 README에 추가

## 📚 참고 자료

- **기존 통계 API**: `app/api/v2_langgraph/statistics.py`
- **기존 통계 도구**: `app/tools/statistics_comparison_tools.py`
- **Vector 검색 구현**: `app/services/embedding_service.py`
- **OpenSearch 설정**: `app/core/opensearch.py`

## ❓ FAQ

**Q: Vector AI가 DB 집계보다 느린데 왜 사용하나요?**
A: 속도가 아닌 **유연성**이 목표입니다. DB 집계는 쿼리를 미리 정의해야 하지만, Vector AI는 자연어 질문에 즉시 대응할 수 있습니다.

**Q: 정확도 95%면 5% 오차인데 괜찮나요?**
A: 트렌드 분석, 이상 탐지, 대략적 통계에는 충분합니다. 정확한 숫자가 필요한 경우에만 DB 집계를 사용하면 됩니다.

**Q: k=10000이면 거의 전체 데이터인데 의미가 있나요?**
A: 전체 데이터가 100만개라면 k=10000은 1%입니다. 작은 데이터셋에서는 의미가 적지만, 대규모 데이터에서는 여전히 효율적입니다.

**Q: 왜 level_counts를 안 주나요?**
A: 기존 방식은 LLM에게 정답을 주고 검증하는 것이라 의미가 없었습니다. 이번 실험은 **LLM이 샘플만 보고 추론하는 능력**을 평가하는 것입니다.
