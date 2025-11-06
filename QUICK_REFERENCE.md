# 프로젝트 빠른 참조 - Log Analysis API

## 기본 정보
- **위치**: `/mnt/c/Project/prototype/log-analysis-api`
- **타입**: FastAPI + LangChain 기반 AI 로그 분석 서버
- **파일**: 30개 | **코드**: 1,535줄

## 디렉터리 구조 (요약)
```
app/
├── main.py              # FastAPI 앱
├── api/v1/              # REST API (health, logs, chatbot)
├── chains/              # LangChain 체인 (분석, 챗봇)
├── core/                # 설정 (config.py, opensearch.py)
├── models/              # Pydantic 모델 (log, analysis, chat)
├── services/            # 비즈니스 로직 (embedding, similarity, analysis, chatbot)
└── repositories/        # (빈 폴더 - 향후 확장)

scripts/
├── create_indices.py    # OpenSearch 인덱스 생성
└── test_connection.py   # 연결 테스트
```

## 주요 기능

### 1. AI 로그 분석 API
- `GET /api/v1/logs/{log_id}/analysis?project_uuid={project_uuid}`
- **Multi-tenancy**: project_uuid 기반 데이터 격리
- **Trace 기반 분석**: trace_id로 연관 로그 수집 (±3초, 최대 100개)
- **2단계 캐싱**: Trace 캐싱 (97-99%) + 유사도 캐싱 (80%)
- GPT-4o mini로 분석 (summary, error_cause, solution)

### 2. RAG 챗봇 API
- `POST /api/v1/chatbot/ask`
- 질문 → 유사 질문 캐시 확인 → 없으면 RAG로 답변 생성

## 기술 스택
- FastAPI 0.109, Uvicorn 0.27
- LangChain 0.2.16, OpenAI 1.40.0
- OpenSearch 2.4.2 (로그 + Vector DB)

## 핵심 설정 (.env)
```bash
OPENAI_API_KEY=...
LLM_MODEL=gpt-4o-mini              # GPT-4o 대비 94% 저렴
EMBEDDING_MODEL=text-embedding-3-large
OPENSEARCH_HOST=localhost:9200
SIMILARITY_THRESHOLD=0.8           # 유사도 80% 이상이면 캐시 사용
```

## OpenSearch 인덱스
1. **logs-YYYY-MM**: 통합 인덱스 (logs + log_details + ai_analysis)
   - project_uuid (multi-tenancy)
   - logger, source_type, layer (ERD 필드)
   - log_details (nested object): HTTP 요청/응답, 예외 상세
   - ai_analysis: 분석 결과 + analysis_type (SINGLE/TRACE_BASED)
   - log_vector: 1536차원 임베딩 (cosine similarity)
2. **qa-cache**: 챗봇 질문 + 벡터 + 답변

## 실행 방법
```bash
# 1. 환경 설정
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
cp .env.example .env  # OPENAI_API_KEY 설정

# 2. 연결 테스트
python scripts/test_connection.py

# 3. 인덱스 생성
python scripts/create_indices.py

# 4. 서버 실행
uvicorn app.main:app --reload --port 8000
```

## API 엔드포인트
- `GET /api/v1/health` - 헬스체크
- `GET /api/v1/logs/{log_id}/analysis?project_uuid={project_uuid}` - 로그 AI 분석
- `POST /api/v1/chatbot/ask` - 챗봇 질의

## 핵심 파일 (라인수)
| 파일 | 라인 | 설명 |
|------|------|------|
| `services/chatbot_service.py` | 195 | RAG 챗봇 로직 + QA 캐싱 |
| `services/log_analysis_service.py` | 156 | 로그 분석 + 유사도 캐싱 |
| `services/similarity_service.py` | 120 | OpenSearch KNN 검색 |
| `chains/log_analysis_chain.py` | 61 | LangChain 분석 체인 |
| `chains/chatbot_chain.py` | 47 | LangChain 챗봇 체인 |
| `services/embedding_service.py` | 47 | OpenAI 임베딩 생성 |

## 비용 절감 전략
1. **Trace 캐싱**: 같은 trace_id → 1회만 분석 → 97-99% 절감
2. **유사도 캐싱**: 80% 이상 유사 → LLM 호출 안함 → 80% 절감
3. **GPT-4o mini**: GPT-4o 대비 94% 저렴
4. **총 효과**: 97-99% 비용 절감

## 의존성
- **Infrastructure**: OpenSearch(9200) 필요 (로그는 Logstash가 저장)
- **API Key**: OpenAI API 키 필수
- **Python**: 3.8+

## 완료 상태
✅ 전체 구조 완성 | ✅ 모든 기능 구현 | ✅ 문서화 완료
