# LogLens - AI 기반 로그 분석 플랫폼

<!-- 로고 이미지 위치 -->
![LogLens 로고](https://i.imgur.com/GHemtZz.png)

분산 시스템의 로그를 실시간으로 수집하고, AI(LLM)를 활용한 자동 분석 및 RAG 기반 챗봇 QA 시스템을 제공하는 엔터프라이즈급 로그 관리 플랫폼입니다.


## 소개와 아키텍처 개요

- AI 기반 로그 분석 플랫폼, LogLens
- 팀원
    김건학 : BE 리더 + INFRA
    이석규 : BE + AI
    한종욱 : BE + BE_Lib
    홍혜린 : FE 리더
    김민진 : FE_Lib + BE
    이종현 : FE

데이터 플로우 (텍스트 다이어그램):
```
Spring Boot 앱 (LogLens Starter)
         ↓
      Kafka (메시지 큐)
         ↓
      Logstash (ETL)
         ↓
      OpenSearch (로그 저장 + 벡터 DB)
         ↓
사용자(FE) ←→ BE(Spring Boot) ←→ AI(FastAPI) ←→ OpenSearch
                   ↓
            Redis (캐시), MySQL (메타데이터)
```

### 시스템 아키텍처
![시스템 아키텍처](https://i.imgur.com/IScLo4U.png)

### 인프라 아키텍처
![인프라 아키텍처](https://i.imgur.com/ZHEIZIZ.png)

---

## 로컬 빠른 시작

1) 인프라 (Docker)
```bash
cd BE/BE/infra
docker-compose -f docker-compose-data.yml up -d
```
MySQL: 3306, Redis: 6379

2) FE
```bash
cd FE/S13P31A306
npm install
npm run dev
```
기본 개발 서버: http://localhost:5173

3) BE
```bash
cd BE/BE
./gradlew bootRun
```
기본 포트: 8080, 헬스체크: `GET /health-check`

4) AI
```bash
cd AI/AI
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```
헬스체크: `GET /api/v1/health`

---

## 프론트엔드(FE)

기술 스택
- React 19, TypeScript 5.9, Vite 7
- 상태: Zustand, TanStack React Query
- 라우팅: React Router 7
- UI: Radix UI, Tailwind CSS, Lucide React
- 시각화: Nivo, Recharts, D3, ReactFlow
- 통신: Axios
- 문서: html2canvas + jsPDF, React Markdown

주요 스크립트
```bash
npm run dev          # 개발 서버 실행
npm run build        # 프로덕션 빌드
npm run preview      # 빌드 프리뷰
```

폴더 구조 요약
```
FE/S13P31A306/src/
├─ pages/           # 라우팅 페이지 (13개)
├─ components/      # 재사용 컴포넌트 (30+개)
├─ services/        # API 클라이언트 서비스
├─ stores/          # Zustand 상태 관리
├─ hooks/           # 커스텀 React 훅
├─ types/           # TypeScript 타입 정의
├─ router/          # 라우팅 설정
└─ utils/           # 유틸리티 함수
```

런타임 구성
- API 기본 URL: 환경 변수로 설정
- 인증: Access Token + Refresh Token (JWT)
- 실시간: SSE 기반 알림 구독

---

## 백엔드(BE)

기술 스택
- Spring Boot 3.5.6, Java 21, Gradle
- Web/WebFlux, Security, JPA, Redis, Actuator, Prometheus
- springdoc-openapi (Swagger)

런타임
- 프로필: `dev`(H2 인메모리) / `prod`(MySQL)
- 헬스체크: `GET /health-check`
- Swagger UI: `/swagger-ui/index.html`
- Prometheus: `/actuator/prometheus`

AI 연동
- WebClient를 통해 AI FastAPI 호출
- 로그 분석 요청 → AI 서비스 → 결과 캐싱

데이터베이스
- dev: H2 메모리, `ddl-auto: create`
- prod: MySQL 8.0, Redis 7, OpenSearch 2.17

주요 도메인 (13개)
```
auth        # 인증/인가 (JWT)
project     # 프로젝트 관리 (Multi-Tenancy)
log         # 로그 조회/필터링
analysis    # AI 기반 로그 분석
alert       # 알림 규칙 및 이력
notification # SSE 실시간 알림
dashboard   # 대시보드 통계
statistics  # 로그 통계 분석
component   # 마이크로서비스 컴포넌트
dependency  # 서비스 간 의존성 그래프
flow        # 요청 흐름 추적
jira        # Jira 연동
```

빌드/실행
```bash
./gradlew bootRun        # 개발 실행
./gradlew build          # 빌드
java -jar build/libs/*.jar
```

---

## AI 서비스(AI)

기술 스택
- FastAPI 0.109, Python 3.11, Uvicorn
- LangChain 0.2.16, LangGraph 0.2.28
- OpenAI GPT-4o mini, text-embedding-3-large
- OpenSearch 2.4 (벡터 검색)

기능 모듈
- 로그 분석: 요약, 원인 분석, 해결방법 생성
- 챗봇 QA: RAG 기반 자연어 질의응답
- 임베딩: 로그 텍스트 벡터화
- 2단계 캐싱: Trace 기반 + 유사도 기반 (97-99% AI 비용 절감)

주요 엔드포인트
- 헬스체크: `GET /api/v1/health`
- 로그 분석: `GET /api/v1/logs/{log_id}/analysis`
- 챗봇: `POST /api/v1/chatbot/ask`
- 임베딩: `POST /api/v1/embeddings`
- LangGraph API: `/api/v2_langgraph/*`

API 버전
- v1: 기본 API (LangChain 체인)
- v2: 개선된 API
- v2_langgraph: LangGraph 워크플로우 기반

실행
```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

테스트
```bash
python -m pytest tests -v
```

---

## 도커/인프라/배포 파이프라인

- 각 모듈별 `Dockerfile` 존재
- Docker Compose: MySQL, Redis, OpenSearch, BE, AI
- Blue/Green 배포: `infra/dev/docker/docker-compose-blue.yaml`, `docker-compose-green.yaml`
- Jenkins: CI/CD 파이프라인 (`docker-compose-jenkins.yml`)
- 로그 파이프라인: Kafka → Logstash → OpenSearch
- BE Actuator + Prometheus 모니터링

주요 포트
```
3306  - MySQL
6379  - Redis
8080  - BE (Spring Boot)
8000  - AI (FastAPI)
9200  - OpenSearch
5601  - OpenSearch Dashboards
```

---

## 서비스 소개

### 1. 실시간 로그 대시보드
![대시보드](https://i.imgur.com/bEcCy2t.png)
- 로그 트렌드 그래프, 시간대별 히트맵
- 에러 통계, 시스템 상태 모니터링
- 실시간 SSE 기반 데이터 업데이트

### 2. AI 로그 분석
![AI 분석](https://i.imgur.com/FoCSpeq.gif)
- GPT-4o mini 기반 자동 로그 분석
- 요약, 원인 분석, 해결방법 제시
- 2단계 캐싱으로 97-99% 비용 절감

### 3. 챗봇 QA
![챗봇-1](https://i.imgur.com/7lp5FrD.gif)
![챗봇-2](https://i.imgur.com/dXRc5Fr.gif)
- RAG, LangGraph 기반 자연어 질의응답
- 로그 데이터를 자연어로 검색 또는 질의 유형 판별 후 AI Agent(ReAct Agent)가 tool을 활용해 질의 처리
- 관련 로그와 함께 답변 제공

### 4. 의존성 그래프
![의존성 그래프](https://i.imgur.com/e3vgvSU.gif)
- 마이크로서비스 간 호출 관계 시각화
- ReactFlow 기반 인터랙티브 그래프
- 서비스 상태 및 연결 확인

### 5. 요청 흐름 추적
![요청 흐름 - 1](https://i.imgur.com/2ZixeSs.png)
![요청 흐름 - 2](https://i.imgur.com/jmdHja0.png)
- trace_id 기반 분산 트레이싱
- 요청 경로 시각화
- 병목 지점 파악

### 6. 로그 검색 및 필터링
![로그 검색-1](https://i.imgur.com/vTWrW97.png)
![로그 검색-2](https://i.imgur.com/TyW7Rty.png)
- 상세 필터링 (레벨, 서비스, 시간 등)
- OpenSearch 기반 고속 검색
- 로그 내보내기 기능

### 7. 사용자 가이드
![사용자 가이드](https://i.imgur.com/ioarA89.gif)
- 초기 설정 가이드

### 8. 분석 리포트
![분석 리포트 - 1](https://i.imgur.com/eo0GA0t.gif)
![분석 리포트 - 2](https://i.imgur.com/sJtkTZt.png)
![분석 리포트 - jira1](https://i.imgur.com/qyfiuBh.gif)
![분석 리포트 - jira2](https://i.imgur.com/DYsMpYl.gif)
- AI 분석 결과 문서화
- PDF/Markdown 내보내기
- Jira 이슈 연동

---

## 핵심 기술 특징

### FE (프론트엔드)
- **React 19 + TypeScript 5.9 + Vite 7**: 최신 프론트엔드 스택, HMR(Hot Module Replacement)로 개발 생산성 향상, 번들 최적화로 빌드 시간 단축
- **Zustand 상태 관리**: sessionStorage 기반 인증 상태 저장으로 탭 닫으면 자동 로그아웃, `persist` 미들웨어로 새로고침 시에도 상태 유지
- **TanStack React Query**: staleTime 5분 설정으로 불필요한 API 재호출 방지, 4xx 에러는 UI 처리 / 5xx 에러만 throw하여 에러 바운더리로 전파
- **SSE 실시간 알림**: EventSource API로 서버 푸시 이벤트 구독, 재연결 로직 내장으로 네트워크 끊김 시 자동 복구
- **Radix UI + Tailwind CSS**: WAI-ARIA 접근성 가이드라인 준수 헤드리스 컴포넌트 + 유틸리티 퍼스트 스타일링으로 일관된 디자인 시스템 구축
- **데이터 시각화**: ReactFlow로 노드/엣지 기반 의존성 그래프 렌더링 (줌/팬/드래그 지원), Recharts/Nivo/D3로 시계열 차트 및 히트맵 구현
- **문서 내보내기**: html2canvas로 DOM을 캔버스로 캡처 후 jsPDF로 PDF 생성, React Markdown + remark/rehype 플러그인으로 마크다운 렌더링
- **React Router 7**: createBrowserRouter로 선언적 라우팅, Outlet 기반 중첩 라우트, ProtectedRoute 컴포넌트로 인증되지 않은 사용자 리다이렉트
- **Framer Motion**: AnimatePresence로 컴포넌트 마운트/언마운트 애니메이션, variants로 복잡한 시퀀스 애니메이션 구현
- **dnd-kit**: DndContext + useDraggable/useDroppable 훅으로 접근성 지원 드래그 앤 드롭, 키보드 네비게이션 완벽 지원
- **Sonner**: toast() API로 성공/에러/로딩 상태 알림, 자동 dismiss 및 액션 버튼 지원

### BE (백엔드)
- **Spring Boot 3.5.6 + Java 21**: Virtual Threads 지원으로 높은 동시성 처리, Record 클래스로 불변 DTO 정의, Pattern Matching으로 타입 안전한 분기 처리
- **JWT 인증**: Access Token 24시간 유효 (Authorization 헤더), Refresh Token 7일 유효 (HttpOnly 쿠키), 토큰 탈취 시 Refresh Token Rotation으로 보안 강화
- **SSE 실시간 알림**: SseEmitter로 50개 동시 연결 지원, 60분 타임아웃 설정, 5초 간격 heartbeat로 연결 유지, 클라이언트별 emitter 관리
- **HikariCP 커넥션 풀**: maximumPoolSize=50, minimumIdle=10, connectionTimeout=30초, idleTimeout=10분, maxLifetime=30분으로 최적화
- **JPA 배치 처리**: hibernate.jdbc.batch_size=20으로 INSERT/UPDATE 배치 처리, order_inserts/order_updates=true로 쿼리 그룹화하여 DB 왕복 최소화
- **Multi-Tenancy**: 모든 엔티티에 project_uuid 필드 추가, @Where 또는 Hibernate Filter로 자동 필터링, OpenSearch 인덱스도 `{uuid}_{YYYY}_{MM}` 형식으로 분리
- **Prometheus + Actuator**: /actuator/prometheus 엔드포인트로 JVM 메트릭, HTTP 요청 통계, DB 커넥션 풀 상태, 커스텀 비즈니스 메트릭 노출
- **WebFlux WebClient**: 논블로킹 I/O로 AI 서비스 호출, timeout 30초, retry 3회 설정, Mono/Flux로 리액티브 스트림 처리
- **Hibernate Validator**: @Valid로 요청 DTO 검증, @NotNull/@NotBlank/@Size/@Pattern 등 Bean Validation 어노테이션, ControllerAdvice로 MethodArgumentNotValidException 글로벌 처리
- **스케줄러**: @Scheduled(cron)으로 알림 규칙 체크 (1분 간격), 통계 집계 (1시간 간격), @Async와 조합하여 비동기 배치 처리
- **OpenSearch 연동**: RestHighLevelClient로 BoolQuery 기반 동적 쿼리 생성, must/should/filter 조합으로 복합 검색, KNN 쿼리로 벡터 유사도 검색

### AI (인공지능)
- **FastAPI + LangChain 0.2.16 + LangGraph 0.2.28**: Pydantic v2 기반 요청/응답 검증, 의존성 주입으로 서비스 계층 분리, LangGraph StateGraph로 복잡한 워크플로우 구현
- **OpenAI GPT-4o mini**: temperature=0.3으로 일관된 분석 결과, max_tokens=2000으로 상세한 응답 생성, 로그 분석 시 요약/원인/해결방법/태그 구조화된 JSON 출력
- **text-embedding-3-large**: 3072차원 벡터 생성, 배치 처리로 여러 로그 동시 임베딩, 코사인 유사도로 관련 로그 검색
- **2단계 캐싱 전략**:
  ```
  1단계: Trace 기반 캐싱
  - trace_id 존재 시 ±3초 범위 관련 로그 최대 100개 수집
  - 수집된 로그 중 이미 분석된 로그가 있으면 해당 결과 재사용
  - 같은 요청 흐름의 에러는 동일 원인일 확률 높음
  - API 호출 없이 즉시 응답 → 비용 절감 97-99%

  2단계: 유사도 기반 캐싱
  - 로그 메시지 임베딩 후 OpenSearch KNN 검색 (k=5)
  - 유사도 0.8 이상인 기존 분석 결과 발견 시 재사용
  - "NullPointerException at UserService.java:42" 같은 반복 에러 패턴 감지
  - 임베딩 비용만 발생 → GPT 호출 비용 80% 절감
  ```
- **RAG 챗봇**: 질문 임베딩 → OpenSearch 벡터 검색으로 관련 로그 top-5 추출 → 로그 컨텍스트와 함께 GPT에 전달 → 근거 기반 답변 생성
- **ReAct Agent**: LangGraph의 Tool 노드로 구현, 질의 분석 → 적절한 tool 선택 (search_logs, get_statistics, analyze_error 등) → 결과 종합 → 최종 답변 생성
- **LangGraph 워크플로우**: StateGraph로 노드(분석 단계) 정의, add_conditional_edges로 분기 조건 설정, 캐시 히트/미스에 따른 다른 경로 실행
- **프롬프트 엔지니어링**: 시스템 프롬프트에 로그 분석 전문가 역할 부여, Few-shot 예시로 출력 포맷 가이드, Chain-of-Thought로 단계별 추론 유도
- **토큰 최적화**: tiktoken으로 입력 토큰 수 사전 계산, 컨텍스트 윈도우(128K) 초과 시 오래된 로그부터 제거, 요약 후 재입력하는 Recursive Summarization 적용
- **비동기 처리**: httpx.AsyncClient로 OpenAI API 비동기 호출, asyncio.gather로 여러 분석 요청 병렬 처리, 응답 스트리밍으로 UX 개선
- **에러 핸들링**: OpenAI API 429 에러 시 exponential backoff (1s → 2s → 4s) 재시도, 타임아웃 30초, 3회 실패 시 "분석 실패" 폴백 응답 반환

### INFRA (인프라)
- **Docker + Docker Compose**: 멀티 스테이지 빌드로 이미지 크기 최소화 (JDK 빌드 → JRE 런타임), docker-compose.yml로 서비스 간 의존성 및 네트워크 정의
- **Blue/Green 무중단 배포**: Blue(8080)/Green(8081) 두 환경 운영, 새 버전 Green에 배포 → 헬스체크 통과 → ALB 타겟 그룹 전환 → Blue 종료, 롤백 시 즉시 Blue로 전환
- **Jenkins CI/CD**: Jenkinsfile로 파이프라인 정의, Git push 시 웹훅 트리거, 빌드 → 테스트 → Docker 이미지 빌드/푸시 → 배포 자동화
- **로그 파이프라인**: Spring Boot 앱 → Kafka Producer로 로그 전송 → Kafka Topic(application-logs) → Logstash Consumer → 필터/변환 → OpenSearch Bulk Indexing
- **OpenSearch 2.17**: HNSW 알고리즘 기반 KNN 인덱스로 벡터 검색, 샤드 5개 / 레플리카 1개 설정, ILM(Index Lifecycle Management)으로 30일 이후 자동 삭제
- **MySQL 8.0 + Redis 7**: MySQL은 사용자/프로젝트/분석결과 메타데이터 저장, Redis는 세션/토큰 캐시 (TTL 설정), Redis Pub/Sub으로 서버 간 이벤트 브로드캐스트
- **Nginx 리버스 프록시**: upstream 블록으로 BE 서버 로드 밸런싱, SSL 인증서 적용 (Let's Encrypt), gzip 압축 활성화, /static 경로는 직접 서빙
- **Fluent Bit**: 메모리 사용량 ~450KB의 경량 로그 수집기, tail 입력으로 로그 파일 모니터링, kafka 출력으로 메시지 큐 전송, Logstash 대비 리소스 90% 절감
- **볼륨 마운트**: MySQL(/var/lib/mysql), Redis(/data), OpenSearch(/usr/share/opensearch/data) 호스트 볼륨 마운트로 컨테이너 재시작 시에도 데이터 유지
- **헬스체크**: Docker HEALTHCHECK로 30초 간격 상태 확인, curl로 /health-check 엔드포인트 호출, unhealthy 3회 시 컨테이너 자동 재시작
- **환경 분리**: application-dev.yml (H2, 로컬 AI), application-prod.yml (MySQL, 원격 AI/OpenSearch), Spring Profile로 환경별 설정 자동 로드
- **시크릿 관리**: DB 비밀번호, API 키 등 민감정보는 환경 변수로 주입, docker-compose에서 env_file로 .env 파일 참조, 버전 관리에서 제외

### FE_Lib (프론트엔드 공용 라이브러리)
- **공용 API 클라이언트**: Axios 인스턴스 싱글톤 패턴, 요청 인터셉터에서 Authorization 헤더 자동 주입, 응답 인터셉터에서 에러 정규화
- **토큰 자동 갱신**: 401 응답 시 Refresh Token으로 /api/auth/refresh 호출, 새 Access Token 저장 후 원래 요청 재시도, Refresh Token도 만료 시 로그인 페이지로 리다이렉트
- **useFormValidation Hook**: 필드별 검증 규칙 정의, touched 상태 관리로 blur 시점에 에러 표시, isValid 플래그로 submit 버튼 활성화 제어
- **Zustand persist**: createJSONStorage(sessionStorage)로 인증 상태 저장, 새로고침 시에도 로그인 유지, 브라우저 탭 닫으면 자동 로그아웃
- **공용 Layout**: Sidebar + Header + Outlet 구조, 프로젝트 연결 상태 확인하여 미연결 시 안내 페이지 표시, 반응형 사이드바 (모바일에서 오버레이)
- **에러 바운더리**: React.ErrorBoundary로 컴포넌트 트리 에러 캐치, 에러 발생 시 폴백 UI 표시, componentDidCatch에서 에러 로깅
- **타입 안전성**: API 응답 타입 (ProjectDTO, LogDTO 등) 중앙 정의, Generic으로 ApiResponse<T> 래퍼 타입, strict 모드로 암묵적 any 방지
- **SSE 클라이언트**: new EventSource(url)로 서버 연결, onmessage 핸들러로 알림 수신, onerror에서 재연결 로직, 컴포넌트 언마운트 시 .close() 정리
- **날짜 포맷터**: date-fns의 format/formatDistance 활용, 로그 타임스탬프를 "2분 전", "오늘 14:30" 등 사용자 친화적 형식으로 변환

### BE_Lib (LogLens Spring Boot Starter)
- **@LogCollect 어노테이션**: @Around 어드바이스로 메서드 실행 전후 가로채기, ProceedingJoinPoint에서 메서드명/파라미터/반환값 추출, description으로 비즈니스 의미 부여
  ```java
  @LogCollect(description = "사용자 조회", logParameters = true, logResult = false)
  public User getUser(String userId) {
      return userRepository.findById(userId).orElseThrow();
  }
  // → {"method": "getUser", "description": "사용자 조회", "parameters": {"userId": "123"}, "duration": 45}
  ```
- **Kafka Producer 통합**: ProducerConfig에 Snappy 압축 활성화 (CPU 사용량 증가 대비 대역폭 50% 절감), batch.size=16KB로 메시지 모아서 전송, linger.ms=10으로 10ms 대기 후 배치 전송
- **Spring Boot Auto Configuration**: spring.factories에 AutoConfiguration 클래스 등록, @ConditionalOnProperty(log.collector.enabled=true)로 조건부 활성화, 설정 없이 의존성 추가만으로 동작
- **민감 정보 마스킹**: 파라미터명에 "password", "secret", "token" 포함 시 값을 "***"로 치환, 정규식 패턴으로 신용카드 번호/주민번호 자동 감지 및 마스킹
- **자동 수집 정보**: 메서드 실행 시간(ms), 예외 발생 시 스택 트레이스 (최대 3000자), MDC의 trace_id, 메서드 파라미터 (최대 200자), 호출 클래스/메서드명
- **MDC 통합**: 요청 진입 시 Filter에서 UUID 생성하여 MDC.put("traceId"), @LogCollect에서 MDC.get("traceId")로 추출, 응답 헤더에 X-Trace-Id로 반환
- **비동기 전송**: KafkaTemplate.send() 기본 비동기, ListenableFuture로 전송 결과 콜백 등록, 전송 실패 시 로컬 파일에 백업 후 재시도 큐에 추가
- **로그 레벨 분류**: 정상 완료 → INFO, 예외 발생 → ERROR, 실행 시간 1초 초과 → WARN으로 자동 분류, level 필드에 저장되어 OpenSearch 필터링 가능
- **컨텍스트 정보**: spring.application.name → serviceName, InetAddress.getLocalHost() → hostName, Thread.currentThread().getName() → threadName 자동 수집
- **성능 최적화**: ObjectMapper 싱글톤으로 재사용 (매번 생성 시 ~1ms 오버헤드), Kafka Producer 커넥션 풀링, StringBuilder로 문자열 조합 최적화
