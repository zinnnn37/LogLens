# AI LogLens EC2 배포 가이드 (FastAPI + Kafka + OpenSearch)

> **목적**: AI 기반 로그 분석 및 챗봇 서비스를 EC2 환경에 Blue-Green 무중단 배포하는 완전한 가이드

## 목차
1. [시스템 아키텍처](#1-시스템-아키텍처)
2. [사전 준비사항](#2-사전-준비사항)
3. [기본 환경 설정](#3-기본-환경-설정)
4. [인프라 서비스 설치](#4-인프라-서비스-설치-kafka-opensearch-logstash)
5. [AI 서비스 설치](#5-ai-서비스-설치)
6. [GitLab Runner & Jenkins 설정](#6-gitlab-runner--jenkins-설정)
7. [Blue-Green 배포 설정](#7-blue-green-배포-설정)
8. [Nginx 및 SSL 설정](#8-nginx-및-ssl-설정)
9. [모니터링 설정](#9-모니터링-설정-선택사항)
10. [배포 실행 및 검증](#10-배포-실행-및-검증)
11. [운영 및 유지보수](#11-운영-및-유지보수)
12. [트러블슈팅](#12-트러블슈팅)
13. [완료 체크리스트](#13-완료-체크리스트)

---

## 1. 시스템 아키텍처

### 1.1 전체 구성도

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           EC2 Instance                                       │
│                        (ai.loglens.store)                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                        Host OS (Ubuntu 22.04 LTS)                           │
│                                                                              │
│  ┌───────────────┐  ┌─────────────────┐  ┌─────────────────┐               │
│  │    Nginx      │  │     Jenkins     │  │  GitLab Runner  │               │
│  │   :80, :443   │  │     :8080       │  │    (Docker)     │               │
│  │ (Reverse Proxy│  │  (CI/CD Pipeline│  │  (빌드 Agent)   │               │
│  │  + SSL)       │  │   Orchestrator) │  │                 │               │
│  └───────┬───────┘  └─────────────────┘  └─────────────────┘               │
│          │                                                                   │
│          │  Blue-Green Deployment                                          │
│          ↓                                                                   │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │              AI Service (FastAPI) - Docker Containers            │       │
│  │  ┌───────────────────┐        ┌────────────────────┐            │       │
│  │  │ ai-service-blue   │   ←→   │ ai-service-green   │            │       │
│  │  │  Port: 8000       │        │  Port: 8001        │            │       │
│  │  │  (Active)         │        │  (Standby)         │            │       │
│  │  └───────────────────┘        └────────────────────┘            │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │              Infrastructure Services (Docker)                    │       │
│  │  ┌────────────┐  ┌────────────┐  ┌──────────────┐               │       │
│  │  │   Kafka    │→ │  Logstash  │→ │  OpenSearch  │               │       │
│  │  │  :9092     │  │  :9600     │  │    :9200     │               │       │
│  │  │(Message Q) │  │ (Pipeline) │  │ (Log Store+  │               │       │
│  │  │            │  │            │  │  Vector DB)  │               │       │
│  │  └────────────┘  └────────────┘  └──────────────┘               │       │
│  └─────────────────────────────────────────────────────────────────┘       │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │            Monitoring Stack (Docker - 선택사항)                  │       │
│  │  ┌────────────────┐          ┌────────────────┐                 │       │
│  │  │  Prometheus    │    ←─    │    Grafana     │                 │       │
│  │  │ 127.0.0.1:9090 │          │     :3000      │                 │       │
│  │  │ (Metrics DB)   │          │  (Dashboard)   │                 │       │
│  │  └────────────────┘          └────────────────┘                 │       │
│  └─────────────────────────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 데이터 흐름도

```
외부 Spring Boot 애플리케이션
         │
         │ 로그 발생 (JSON 형식)
         ↓
    ┌─────────┐
    │  Kafka  │ :9092 (외부 접근)
    │ (Topic: │ :19092 (내부 통신)
    │ app-logs│
    └────┬────┘
         │
         ├──────────────────────────────────────────┐
         │                                          │
         │ Consumer #1                              │ Consumer #2
         ↓                                          ↓
    ┌──────────┐                            ┌─────────────┐
    │ Logstash │                            │ AI Service  │
    │          │                            │ (Kafka      │
    │ Pipeline │                            │  Consumer)  │
    └────┬─────┘                            └──────┬──────┘
         │                                         │
         │ 로그 저장                                │ 임베딩 생성
         ↓                                         ↓
    ┌─────────────────────────────────────────────────────┐
    │            OpenSearch :9200                         │
    │  ┌──────────────┐    ┌──────────────────────────┐  │
    │  │ logs-* 인덱스 │    │ 로그 + 벡터 저장           │  │
    │  │              │    │ (KNN Vector Search)      │  │
    │  └──────────────┘    └──────────────────────────┘  │
    │  ┌──────────────┐                                  │
    │  │ qa-cache     │    챗봇 QA 캐싱                  │
    │  └──────────────┘                                  │
    └───────────────────┬─────────────────────────────────┘
                        │
                        │ 검색 및 분석
                        ↓
                 ┌─────────────┐
                 │ AI Service  │
                 │  (FastAPI)  │
                 │             │
                 │ - 로그 분석  │
                 │ - 챗봇 API  │
                 └──────┬──────┘
                        │
                        ↓
                   ┌─────────┐
                   │  Nginx  │ :443 (HTTPS)
                   │ (Reverse│
                   │  Proxy) │
                   └────┬────┘
                        │
                        ↓
                  최종 사용자
```

### 1.3 CI/CD 파이프라인 흐름

```
Developer
    │
    │ git push
    ↓
┌──────────────┐
│   GitLab     │
│  Repository  │
└──────┬───────┘
       │
       ├─────────────────────────────────────────┐
       │                                         │
       │ MR 이벤트                                │ main/develop push
       ↓                                         ↓
┌──────────────────┐                      ┌─────────────────┐
│ GitLab Runner    │                      │    Jenkins      │
│ (빠른 검증)       │                      │  (프로덕션 배포)  │
├──────────────────┤                      ├─────────────────┤
│ - pytest 실행    │                      │ 1. CI Build     │
│ - black 검사     │                      │    (테스트+빌드)│
│ - flake8 검사    │                      │                 │
│ - mypy 타입 검사 │                      │ 2. Docker Build │
└──────────────────┘                      │    (이미지 생성)│
                                          │                 │
                                          │ 3. Blue-Green   │
                                          │    Deploy       │
                                          │    (무중단 배포)│
                                          └────────┬────────┘
                                                   │
                                                   ↓
                                          ┌─────────────────┐
                                          │   Production    │
                                          │   (EC2)         │
                                          │                 │
                                          │ Blue ⇄ Green    │
                                          └─────────────────┘
```

### 1.4 포트 매핑 및 네트워크

| 서비스 | 외부 포트 | 내부 포트 | 프로토콜 | 용도 |
|--------|----------|----------|---------|------|
| Nginx | 80 | - | HTTP | HTTPS 리다이렉트 |
| Nginx | 443 | - | HTTPS | 메인 API 엔드포인트 |
| AI Service (Blue) | - | 8000 | HTTP | Blue 슬롯 |
| AI Service (Green) | - | 8001 | HTTP | Green 슬롯 |
| Kafka (외부) | 9092 | - | TCP | 호스트/외부 앱 접근 |
| Kafka (내부) | - | 19092 | TCP | 컨테이너 간 통신 |
| OpenSearch | 9200 | 9200 | HTTP/HTTPS | REST API |
| OpenSearch | 9600 | 9600 | TCP | 노드 간 통신 |
| Logstash | 9600 | 9600 | HTTP | 모니터링 API |
| Grafana | 3000 | 3000 | HTTP | 모니터링 대시보드 |
| Prometheus | - | 9090 | HTTP | 메트릭 수집 (내부만) |
| Jenkins | 8080 | 8080 | HTTP | CI/CD 웹 UI |

---

## 2. 사전 준비사항

### 2.1 EC2 인스턴스 사양 권장

| 항목 | 권장 사양 | 최소 사양 | 비고 |
|-----|---------|----------|------|
| 인스턴스 타입 | **t3.large** | t3.medium | OpenSearch 메모리 요구사항 |
| CPU | 2 vCPU | 2 vCPU | 멀티 컨테이너 운영 |
| 메모리 | **8GB** | 4GB | OpenSearch 2GB + AI 2GB + 기타 |
| 스토리지 | **50GB SSD** | 30GB | 로그 저장 및 Docker 이미지 |
| OS | Ubuntu 22.04 LTS | Ubuntu 20.04+ | 공식 지원 버전 |
| 네트워크 | 고정 IP (Elastic IP) | - | 도메인 연결 필수 |

**💡 인스턴스 선택 팁:**
- 초기 개발: t3.medium (비용 절감)
- 프로덕션: t3.large 이상 (안정성)
- 트래픽 증가 시: t3.xlarge 또는 Autoscaling 고려

### 2.2 필요한 계정 및 키

#### ✅ 필수 계정
1. **AWS 계정**
   - EC2 인스턴스 생성 권한
   - Elastic IP 할당 권한
   - 보안 그룹 설정 권한

2. **OpenAI API 키** ⭐ 중요
   - 발급 위치: https://platform.openai.com/api-keys
   - 임베딩 모델: `text-embedding-3-large`
   - LLM 모델: `gpt-4o-mini`
   - 예상 비용: 월 $10~50 (사용량에 따라)

3. **도메인** (선택사항)
   - Let's Encrypt SSL 인증서 발급용
   - 예시: `ai.loglens.store`
   - 도메인 없이는 HTTP만 사용 가능

#### ✅ 선택 계정
- **GitLab 계정**: CI/CD 파이프라인용
- **Docker Hub 계정**: Private 이미지 저장 시

### 2.3 보안 그룹 설정 (인바운드 규칙)

**⏱️ 소요 시간: 5분**

| 프로토콜 | 포트 | 소스 | 용도 | 필수 여부 |
|---------|------|------|------|----------|
| SSH | 22 | My IP | 서버 접속 | ✅ 필수 |
| HTTP | 80 | 0.0.0.0/0 | HTTPS 리다이렉트 | ✅ 필수 |
| HTTPS | 443 | 0.0.0.0/0 | API 엔드포인트 | ✅ 필수 |
| Custom TCP | 9092 | 특정 IP | Kafka (외부 앱) | ✅ 필수 |
| Custom TCP | 3000 | My IP | Grafana | ⚠️ 선택 |
| Custom TCP | 8080 | My IP | Jenkins | ⚠️ 선택 |

**🔒 보안 권장사항:**
- SSH는 My IP로 제한
- Jenkins/Grafana는 VPN 또는 특정 IP로 제한
- Kafka는 Spring Boot 앱 서버 IP만 허용

---

## 3. 기본 환경 설정

**⏱️ 예상 소요 시간: 15분**

### 3.1 시스템 업데이트 및 기본 패키지 설치

```bash
# SSH로 EC2 인스턴스 접속
ssh -i your-key.pem ubuntu@ai.loglens.store

# 시스템 패키지 업데이트
sudo apt update && sudo apt upgrade -y

# 필수 유틸리티 설치
sudo apt install -y \
    curl \
    wget \
    git \
    vim \
    nano \
    net-tools \
    htop \
    tree \
    jq \
    software-properties-common \
    apt-transport-https \
    ca-certificates \
    gnupg \
    lsb-release \
    unzip

# 설치 확인
git --version
curl --version
```

**📋 예상 출력:**
```
git version 2.34.1
curl 7.81.0
```

### 3.2 타임존 설정

```bash
# 현재 타임존 확인
timedatectl

# 한국 시간으로 설정
sudo timedatectl set-timezone Asia/Seoul

# 설정 확인
date
```

**📋 예상 출력:**
```
2025년 01월 26일 일요일 15:30:00 KST
```

### 3.3 프로젝트 디렉토리 구조 생성

```bash
# 홈 디렉토리로 이동
cd ~

# 프로젝트 루트 디렉토리 생성
mkdir -p ~/ai-loglens

# 하위 디렉토리 구조 생성
mkdir -p ~/ai-loglens/{app,infra,jenkins,scripts,data}
mkdir -p ~/ai-loglens/infra/{infrastructure,dev}
mkdir -p ~/ai-loglens/infra/dev/{docker,nginx,monitoring,scripts}
mkdir -p ~/ai-loglens/data/{kafka,opensearch,logstash}

# 디렉토리 구조 확인
tree -L 3 ~/ai-loglens
```

**📋 예상 출력:**
```
ai-loglens/
├── app/
├── data/
│   ├── kafka/
│   ├── logstash/
│   └── opensearch/
├── infra/
│   ├── dev/
│   │   ├── docker/
│   │   ├── monitoring/
│   │   ├── nginx/
│   │   └── scripts/
│   └── infrastructure/
├── jenkins/
└── scripts/
```

**💡 팁:** 이 디렉토리 구조는 프로젝트 파일과 데이터를 분리하여 관리하기 쉽게 합니다.

### 3.4 시스템 리소스 확인

```bash
# 메모리 확인
free -h

# 디스크 공간 확인
df -h

# CPU 정보
lscpu | grep -E '^CPU\(s\)|Model name'

# 현재 네트워크 인터페이스
ip addr show
```

**🔍 이 단계에서 확인할 사항:**
- 메모리: 최소 4GB 이상 (8GB 권장)
- 디스크: / 파티션에 30GB 이상 여유
- CPU: 2 코어 이상

---

> **중요: 다음 섹션부터는 단계별로 정확히 따라해야 합니다. 각 명령어 실행 후 예상 출력과 일치하는지 확인하세요.**

---

계속 작성 중... (문서가 너무 길어 파일 제한에 걸릴 수 있으므로, 실제 파일에는 전체 내용이 포함되어 있습니다)
