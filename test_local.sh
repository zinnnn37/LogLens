#!/bin/bash

# ============================================
# AI 로그 분석 서비스 - 로컬 테스트 자동화 스크립트
# ============================================

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수: 헤더 출력
print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 함수: 성공 메시지
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# 함수: 경고 메시지
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 함수: 에러 메시지
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 함수: OpenSearch 상태 확인
check_opensearch() {
    echo -n "OpenSearch 상태 확인 중..."
    if curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; then
        print_success "OpenSearch 실행 중 ✓"
        return 0
    else
        print_warning "OpenSearch 미실행"
        return 1
    fi
}

# 함수: 테스트 환경 시작
start_test() {
    print_header "테스트 환경 시작"

    # 1. Docker Compose로 OpenSearch 시작
    echo "1️⃣  OpenSearch 컨테이너 시작..."
    docker-compose -f docker-compose.test.yml up -d

    # 2. OpenSearch 준비 대기
    echo "2️⃣  OpenSearch 준비 대기 중..."
    MAX_RETRIES=30
    RETRY_COUNT=0

    while ! check_opensearch; do
        RETRY_COUNT=$((RETRY_COUNT + 1))
        if [ $RETRY_COUNT -gt $MAX_RETRIES ]; then
            print_error "OpenSearch 시작 타임아웃 (30초 초과)"
            echo "로그 확인: docker logs opensearch-test"
            exit 1
        fi
        echo -n "."
        sleep 1
    done
    echo ""

    # 3. 인덱스 생성
    echo "3️⃣  OpenSearch 인덱스 생성..."
    if python scripts/create_indices.py; then
        print_success "인덱스 생성 완료"
    else
        print_error "인덱스 생성 실패"
        exit 1
    fi

    echo ""
    print_success "테스트 환경 시작 완료!"
    echo ""
    echo "📌 다음 명령어로 FastAPI 서버를 실행하세요:"
    echo -e "${YELLOW}   uvicorn app.main:app --reload --env-file .env.test${NC}"
    echo ""
    echo "🌐 접속 URL:"
    echo "   - Swagger UI: http://localhost:8000/docs"
    echo "   - Health Check: http://localhost:8000/api/v1/health"
    echo "   - OpenSearch: http://localhost:9200"
    echo "   - Dashboards: http://localhost:5601"
    echo ""
}

# 함수: 테스트 환경 종료
stop_test() {
    print_header "테스트 환경 종료"

    echo "1️⃣  OpenSearch 컨테이너 종료..."
    docker-compose -f docker-compose.test.yml down

    print_success "테스트 환경 종료 완료"
    echo ""
    echo "💡 데이터를 유지하려면 볼륨을 삭제하지 마세요."
    echo "   완전 초기화: docker-compose -f docker-compose.test.yml down -v"
}

# 함수: 테스트 환경 재시작
restart_test() {
    print_header "테스트 환경 재시작"
    stop_test
    echo ""
    start_test
}

# 함수: 상태 확인
status_test() {
    print_header "테스트 환경 상태"

    echo "📦 Docker 컨테이너 상태:"
    docker-compose -f docker-compose.test.yml ps

    echo ""
    echo "🔍 OpenSearch 클러스터 상태:"
    if check_opensearch; then
        curl -s http://localhost:9200/_cluster/health?pretty
    fi

    echo ""
    echo "📊 인덱스 목록:"
    if check_opensearch; then
        curl -s http://localhost:9200/_cat/indices?v
    fi
}

# 함수: 로그 확인
logs_test() {
    print_header "OpenSearch 로그"
    docker logs -f opensearch-test
}

# 함수: 완전 초기화
clean_test() {
    print_header "테스트 환경 완전 초기화"

    print_warning "모든 테스트 데이터가 삭제됩니다!"
    read -p "계속하시겠습니까? (y/N): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "1️⃣  컨테이너 및 볼륨 삭제..."
        docker-compose -f docker-compose.test.yml down -v

        print_success "완전 초기화 완료"
    else
        print_warning "취소됨"
    fi
}

# 함수: 도움말
show_help() {
    echo "사용법: ./test_local.sh [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start      테스트 환경 시작 (OpenSearch + 인덱스 생성)"
    echo "  stop       테스트 환경 종료"
    echo "  restart    테스트 환경 재시작"
    echo "  status     현재 상태 확인"
    echo "  logs       OpenSearch 로그 확인"
    echo "  clean      완전 초기화 (데이터 삭제)"
    echo "  help       이 도움말 표시"
    echo ""
    echo "예시:"
    echo "  ./test_local.sh start"
    echo "  uvicorn app.main:app --reload --env-file .env.test"
    echo ""
}

# 메인 로직
case "${1:-help}" in
    start)
        start_test
        ;;
    stop)
        stop_test
        ;;
    restart)
        restart_test
        ;;
    status)
        status_test
        ;;
    logs)
        logs_test
        ;;
    clean)
        clean_test
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "알 수 없는 명령어: $1"
        echo ""
        show_help
        exit 1
        ;;
esac
