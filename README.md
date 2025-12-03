# LogLens - AI 기반 로그 분석 플랫폼

<!-- 로고 이미지 위치 -->

![LogLens 로고](https://i.imgur.com/GHemtZz.png)

분산 시스템의 로그를 실시간으로 수집하고, AI(LLM)를 활용한 자동 분석 및 RAG 기반 챗봇 QA 시스템을 제공하는 엔터프라이즈급 로그 관리 플랫폼입니다.

## 소개와 아키텍처 개요

- AI 기반 로그 분석 플랫폼, LogLens
- [발표 자료](https://drive.google.com/file/d/1xdyG942JTshnqAE9WcN1g9H3czU-5nrc/view?usp=sharing)
- [영상 포트폴리오](https://youtu.be/vu8shzigjy4)
- 팀원
  김건학 : BE 리더 + INFRA
  이석규 : BE + AI
  한종욱 : BE + BE_Lib
  홍혜린 : FE 리더
  김민진 : FE_Lib + BE
  이종현 : FE

데이터 플로우 (텍스트 다이어그램):

```text
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

1. 인프라 (Docker)

```bash
cd BE/BE/infra
docker-compose -f docker-compose-data.yml up -d
```

MySQL: 3306, Redis: 6379

2. FE

```bash
cd FE/S13P31A306
npm install
npm run dev
```

기본 개발 서버: http://localhost:5173

3. BE

```bash
cd BE/BE
./gradlew bootRun
```

기본 포트: 8080, 헬스체크: `GET /health-check`

4. AI

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

```text
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
- Web/WebFlux, Security, JPA, Redis, Actuator
- springdoc-openapi (Swagger)

런타임

- 프로필: `dev`(H2 인메모리) / `prod`(MySQL)
- 헬스체크: `GET /health-check`
- Swagger UI: `/swagger-ui/index.html`

AI 연동

- WebClient를 통해 AI FastAPI 호출
- 로그 분석 요청 → AI 서비스 → 결과 캐싱

데이터베이스

- dev: H2 메모리, `ddl-auto: create`
- prod: MySQL 8.0, Redis 7, OpenSearch 2.17

주요 도메인 (13개)

```text
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

주요 포트

```text
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

![대시보드](assets/gif/대시보드.gif)

- 로그 트렌드 그래프, 시간대별 히트맵
- 에러 통계, 시스템 상태 모니터링
- 실시간 SSE 기반 데이터 업데이트

### 2. AI 로그 분석

![AI 분석](assets/gif/로그_상세_정보-에러.gif)

- GPT-4o mini 기반 자동 로그 분석
- 요약, 원인 분석, 해결방법 제시
- 2단계 캐싱으로 97-99% 비용 절감

### 3. 챗봇 QA

![챗봇-1](assets/gif/AI_챗봇.gif)

- RAG, LangGraph 기반 자연어 질의응답
- 로그 데이터를 자연어로 검색 또는 질의 유형 판별 후 AI Agent(ReAct Agent)가 tool을 활용해 질의 처리
- 관련 로그와 함께 답변 제공

### 4. 의존성 그래프

![의존성 그래프](assets/gif/의존성_그래프.gif)

- 마이크로서비스 간 호출 관계 시각화
- ReactFlow 기반 인터랙티브 그래프
- 서비스 상태 및 연결 확인

### 5. 요청 흐름 추적

![요청 흐름 - 에러](assets/gif/요청_흐름-에러.gif)
![요청 흐름 - 정상](assets/gif/요청_흐름-정상.gif)

- trace_id 기반 분산 트레이싱
- 요청 경로 시각화
- 병목 지점 파악

### 6. 로그 검색 및 필터링

![로그 검색-1](assets/gif/로그_내역.gif)

- 상세 필터링 (레벨, 서비스, 시간 등)
- OpenSearch 기반 고속 검색
- 로그 내보내기 기능

### 7. 사용자 가이드

![사용자 가이드](assets/gif/사용자_가이드.gif)

- 초기 설정 가이드

### 8. 분석 리포트

![분석 리포트 - 1](https://i.imgur.com/eo0GA0t.gif)
![분석 리포트 - 2](assets/gif/프로젝트_분석.gif)
![분석 리포트 - jira](https://i.imgur.com/DYsMpYl.gif)

- AI 분석 결과 문서화
- PDF/Markdown 내보내기
- Jira 이슈 연동

---

## 핵심 기술 특징

### Frontend

- **React 19 + TypeScript + Vite**: 최신 프론트엔드 스택으로 빠른 개발과 최적화된 번들링
- **상태 관리**: Zustand + TanStack Query로 효율적인 상태 관리 및 서버 데이터 캐싱
- **실시간 알림**: SSE(Server-Sent Events)로 서버 푸시 알림, 자동 재연결 지원
- **UI/UX**: Radix UI + Tailwind CSS로 접근성 준수, Framer Motion으로 부드러운 애니메이션
- **데이터 시각화**: ReactFlow(의존성 그래프), Recharts/D3(차트), html2canvas+jsPDF(문서 생성)

### Backend

- **Spring Boot 3.5.6 + Java 21**: Virtual Threads로 높은 동시성 처리
- **인증**: JWT 기반 (Access 24h + Refresh 7d), Refresh Token Rotation으로 보안 강화
- **실시간 통신**: SSE로 50개 동시 연결 지원, heartbeat로 연결 유지
- **데이터베이스**: HikariCP 커넥션 풀 최적화, JPA 배치 처리로 DB 부하 최소화
- **Multi-Tenancy**: project_uuid 기반 자동 필터링, OpenSearch 인덱스 분리
- **모니터링**: Prometheus + Actuator로 JVM/HTTP/DB 메트릭 수집

### AI

- **FastAPI + LangChain + LangGraph**: GPT-4o mini 기반 로그 분석 시스템
- **2단계 캐싱 전략**으로 AI 비용 최적화
  - **1단계 (Trace 기반)**: 같은 요청 흐름 로그 재사용 → 비용 97-99% 절감
  - **2단계 (유사도 기반)**: 벡터 검색으로 유사 에러 패턴 감지 → 비용 80% 절감
- **RAG 챗봇**: 벡터 검색 기반 근거 있는 답변 생성
- **ReAct Agent**: LangGraph Tool로 질의 분석 → 도구 선택 → 결과 종합

### Infrastructure

- **컨테이너화**: Docker + Docker Compose, 멀티 스테이지 빌드로 이미지 최적화
- **CI/CD**: Jenkins 파이프라인, Blue/Green 무중단 배포
- **로그 파이프라인**: Fluent Bit → Kafka → Logstash → OpenSearch (Logstash 대비 리소스 90% 절감)
- **데이터 저장**: MySQL(메타데이터), Redis(캐시/세션), OpenSearch(로그 검색, 30일 자동 삭제)
- **헬스체크**: 30초 간격 상태 확인, 자동 재시작

### 공용 라이브러리

**Frontend Lib (NPM Package)**

주요 기능

- 자동 TraceID 전파: 최상위 함수만 래핑하면 내부 호출 체인에 자동으로 traceId 전파
- React Hook 지원: `useLogLens` Hook으로 React 컴포넌트에서 간편하게 사용
- 비동기 처리 자동 지원: Promise, async/await 함수 자동 추적
- 성능 측정: 함수 실행 시간 자동 측정 및 콘솔 표시
- 민감 데이터 마스킹: 환경변수로 마스킹 필드 설정 (password, token 등)
- 직접 로깅: `loglens.info()`, `loglens.warn()`, `loglens.error()` 메서드 제공
- 자동 로그 전송: 설정된 주기(기본 30초)마다 백엔드로 HTTP 전송
- 에러 자동 수집: `captureErrors: true` 옵션으로 에러 자동 수집

**Backend Lib (LogLens Spring Boot Starter)**

주요 기능

- 자동 로그 수집: @RestController, @Service, @Repository, @Component 클래스의 public 메서드 자동 추적
- 선택적 로깅: @LogMethodExecution 어노테이션으로 특정 메서드만 로깅 가능
- 로깅 제외: @NoLogging 어노테이션으로 특정 메서드/클래스 제외
- 보안:
  - @Sensitive 어노테이션: 파라미터/필드를 \*\*\*\*로 마스킹
  - @ExcludeValue 어노테이션: 파라미터를 <excluded>로 처리
- MDC 통합: TraceID 기반 분산 추적 지원
- 비동기 처리: 로그 전송이 비즈니스 로직을 차단하지 않음

---

## 인프라 아키텍처

### 네트워크 구성

```text
VPC (10.0.0.0/16)
├── Public Subnet (10.0.0.0/20, 10.0.16.0/20)
│   ├── Internet Gateway → 외부 트래픽 수신
│   ├── ALB → HTTPS 트래픽 분산
│   ├── NAT Gateway → Private 서브넷 아웃바운드
│   └── Bastion Host → 안전한 SSH 접근
└── Private Subnet (10.0.64.0/20)
    └── Application Server (Spring Boot, MySQL, Redis)
```

### 보안 설계

- **Security Group**: 리소스별 최소 권한 원칙 적용
- **IAM Role**: EC2가 S3에 안전하게 접근 (Access Key 불필요)
- **HTTPS 강제**: ACM 인증서로 모든 트래픽 암호화
- **Bastion Host**: Private 서버 접근 단일 진입점

### 배포 전략

```text
User → Route53 → CloudFront → S3 (React)
     → Route53 → ALB → Target Group (Spring Boot)
                      ├── Blue (8080)
                      └── Green (8081) ← 무중단 전환
```

### 도메인 구성

| 도메인                   | 용도       | 기술              |
| ------------------------ | ---------- | ----------------- |
| www.loglens.co.kr        | 프론트엔드 | CloudFront + S3   |
| api.loglens.co.kr        | 백엔드 API | ALB → Spring Boot |
| jenkins_be.loglens.co.kr | CI/CD      | ALB → Jenkins     |
| bastion.loglens.co.kr    | 관리 접속  | Bastion Host      |

### 데이터 수집 서버 (외부 제공)

**인스턴스**: t2.xlarge (4 vCPU, 16GB RAM)

**구성 요소**

- **Kafka**: 로그 스트리밍, Snappy 압축
- **Logstash**: 로그 정규화 및 OpenSearch 인덱싱
- **OpenSearch 2.17**: KNN 벡터 검색, ILM 자동 삭제
- **FastAPI**: AI 분석 API, 2단계 캐싱

---

## 데이터 흐름

### 사용자 요청

```text
React App → api.loglens.co.kr → Route53 → ALB → Spring Boot
```

### 로그 수집

```text
Application → Fluent Bit → Kafka → Logstash → OpenSearch
                                              ↓
                                         AI 분석 (캐싱)
```

### 관리자 접근

```text
Admin → bastion.loglens.co.kr → Bastion Host → Private Servers
```

---

## 기술적 성과

### 성능 최적화

- Fluent Bit 도입으로 **리소스 사용량 90% 절감**
- JPA 배치 처리로 **DB 왕복 최소화**
- CloudFront 캐싱으로 **응답 속도 개선**

### 비용 최적화

- AI 2단계 캐싱으로 **GPT 비용 97-99% 절감**
- Kafka Snappy 압축으로 **대역폭 50% 절감**

### 안정성

- Blue/Green 배포로 **무중단 서비스**
- 자동 헬스체크 및 재시작
- 분산 추적(TraceID)으로 **에러 원인 빠른 파악**

### 보안

- Private 서브넷으로 서버 보호
- JWT Rotation으로 토큰 탈취 방어
- 민감정보 자동 마스킹
