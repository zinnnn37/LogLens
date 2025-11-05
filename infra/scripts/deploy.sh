#!/bin/bash

set -e

echo "🚀 BLUE/GREEN 배포 시작"
echo "⏰ 시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"

# 작업 디렉토리 설정
CURRENT_DIR=$(pwd)
echo "📂 작업 디렉토리: $CURRENT_DIR"

# Docker Compose 명령어 감지 (v2: docker compose, v1: docker-compose)
DOCKER_COMPOSE_CMD=""
if docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD="docker compose"
    echo "✅ Docker Compose v2 감지"
elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE_CMD="docker-compose"
    echo "✅ Docker Compose v1 감지"
else
    echo "❌ Docker Compose를 찾을 수 없습니다!"
    exit 1
fi

# 서비스 설정
SERVICE_NAME="loglens"
IMAGE_NAME="${SERVICE_NAME}:latest"
NETWORK_NAME="loglens-network"

# nginx 설정 파일 경로
NGINX_SITES_AVAILABLE="/etc/nginx/sites-available"
NGINX_SITES_ENABLED="/etc/nginx/sites-enabled"
NGINX_SITE_NAME="loglens"

# 디버그 모드 설정 (실패 시 컨테이너 유지)
DEBUG_MODE="${DEBUG_MODE:-false}"

# 현재 활성 환경 확인
CURRENT_ENV=""
BLUE_RUNNING=false
GREEN_RUNNING=false

if docker ps --format "table {{.Names}}" | grep -q "${SERVICE_NAME}-blue"; then
    BLUE_RUNNING=true
fi

if docker ps --format "table {{.Names}}" | grep -q "${SERVICE_NAME}-green"; then
    GREEN_RUNNING=true
fi

# 환경 상태에 따른 처리
if [ "$BLUE_RUNNING" = true ] && [ "$GREEN_RUNNING" = true ]; then
    echo "⚠️ 두 환경 모두 실행 중입니다. green 환경을 중지합니다..."
    cd "${CURRENT_DIR}"
    $DOCKER_COMPOSE_CMD -f "docker-compose-green.yml" down 2>/dev/null || true
    CURRENT_ENV="blue"
elif [ "$BLUE_RUNNING" = true ]; then
    CURRENT_ENV="blue"
elif [ "$GREEN_RUNNING" = true ]; then
    CURRENT_ENV="green"
fi

# 새로운 환경 결정
if [ "$CURRENT_ENV" = "blue" ]; then
    NEW_ENV="green"
    NEW_PORT="8001"
    OLD_ENV="blue"
    OLD_PORT="8000"
else
    NEW_ENV="blue"
    NEW_PORT="8000"
    OLD_ENV="green"
    OLD_PORT="8001"
fi

echo "📋 현재: ${CURRENT_ENV:-없음} → 배포 대상: $NEW_ENV (포트 $NEW_PORT)"

# 시스템 리소스 확인
echo "📊 시스템 리소스 상태:"
free -h
echo ""
docker system df
echo ""

# 네트워크 존재 확인 및 생성
if ! docker network inspect $NETWORK_NAME >/dev/null 2>&1; then
    echo "📡 네트워크 생성: $NETWORK_NAME"
    docker network create $NETWORK_NAME
fi

# 새로운 환경 시작
echo "🎯 $NEW_ENV 환경 시작 중..."

# 기존 컨테이너 정리 (비활성 슬롯)
if docker ps -a -q -f name=${SERVICE_NAME}-${NEW_ENV} | grep -q .; then
    echo "🧹 기존 컨테이너 제거: ${SERVICE_NAME}-${NEW_ENV}"
    cd "${CURRENT_DIR:-$(pwd)}"
    $DOCKER_COMPOSE_CMD -f "docker-compose-${NEW_ENV}.yml" down 2>/dev/null || true
fi

# Docker Compose 파일 경로 확인
COMPOSE_FILE="${CURRENT_DIR:-$(pwd)}/docker-compose-${NEW_ENV}.yml"

if [ ! -f "$COMPOSE_FILE" ]; then
    echo "❌ Docker Compose 파일을 찾을 수 없습니다: $COMPOSE_FILE"
    exit 1
fi

# .env 파일 확인
ENV_FILE="${CURRENT_DIR:-$(pwd)}/.env"
if [ ! -f "$ENV_FILE" ]; then
    echo "⚠️ .env 파일을 찾을 수 없습니다: $ENV_FILE"
    echo "⚠️ Jenkins 환경 변수로 실행됩니다."
fi

# Docker Compose로 새 컨테이너 실행
echo "🐳 컨테이너 실행: ${SERVICE_NAME}-${NEW_ENV}"
echo "📄 사용할 Compose 파일: $COMPOSE_FILE"

# Working directory를 infra로 변경하여 docker compose 실행
cd "${CURRENT_DIR:-$(pwd)}"
$DOCKER_COMPOSE_CMD -f "docker-compose-${NEW_ENV}.yml" up -d

# 컨테이너 시작 대기
echo "⏳ 컨테이너 시작 대기중..."
sleep 10

# 컨테이너 상태 확인
echo "🔍 컨테이너 상태 확인:"
docker ps -a | grep ${SERVICE_NAME}-${NEW_ENV}
echo ""

# Health check
echo "🏥 헬스 체크 시작..."
MAX_ATTEMPTS=40
ATTEMPT=0
RESTART_COUNT=0
SUCCESS=false

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))

    # Container status check
    CONTAINER_STATUS=$(docker inspect --format='{{.State.Status}}' ${SERVICE_NAME}-${NEW_ENV} 2>/dev/null || echo "unknown")
    CONTAINER_RESTART_COUNT=$(docker inspect --format='{{.RestartCount}}' ${SERVICE_NAME}-${NEW_ENV} 2>/dev/null || echo "0")

    # 재시작 감지
    if [ "$CONTAINER_RESTART_COUNT" -gt "$RESTART_COUNT" ]; then
        echo "⚠️ 컨테이너가 재시작되었습니다! (재시작 횟수: $CONTAINER_RESTART_COUNT)"
        RESTART_COUNT=$CONTAINER_RESTART_COUNT
        echo "🔍 재시작 후 로그 (최근 30줄):"
        docker logs ${SERVICE_NAME}-${NEW_ENV} --tail 30 2>&1
        echo ""
    fi

    if [ "$CONTAINER_STATUS" != "running" ]; then
        echo "⏳ 컨테이너 시작 대기 중... (시도 $ATTEMPT/$MAX_ATTEMPTS, 상태: $CONTAINER_STATUS)"

        # 컨테이너가 종료된 경우 종료 코드 확인
        if [ "$CONTAINER_STATUS" = "exited" ]; then
            EXIT_CODE=$(docker inspect --format='{{.State.ExitCode}}' ${SERVICE_NAME}-${NEW_ENV} 2>/dev/null || echo "unknown")
            echo "❌ 컨테이너가 종료됨. 종료 코드: $EXIT_CODE"
        fi

        sleep 5
        continue
    fi

    # Docker 컨테이너의 health status 확인
    HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' ${SERVICE_NAME}-${NEW_ENV} 2>/dev/null || echo "none")

    # Docker healthcheck가 없는 경우 컨테이너 내부에서 curl 실행
    if [ "$HEALTH_STATUS" = "none" ]; then
        HTTP_CODE=$(docker exec ${SERVICE_NAME}-${NEW_ENV} curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health-check 2>/dev/null || echo "000")
    elif [ "$HEALTH_STATUS" = "healthy" ]; then
        HTTP_CODE="200"
    else
        # starting, unhealthy 등의 상태
        HTTP_CODE="000"
    fi

    if [ "$HTTP_CODE" = "200" ]; then
        echo "✅ $NEW_ENV 환경이 정상 상태입니다! (Health status: $HEALTH_STATUS)"
        SUCCESS=true

        # 추가 확인 - 컨테이너 내부에서 실행
        echo "🔍 엔드포인트 확인:"
        docker exec ${SERVICE_NAME}-${NEW_ENV} curl -s http://localhost:8080/health-check 2>/dev/null || echo "헬스체크 응답 실패"
        echo ""

        break
    fi

    echo "⏳ 헬스 체크 대기 중... (시도 $ATTEMPT/$MAX_ATTEMPTS, Health: $HEALTH_STATUS, HTTP: $HTTP_CODE)"

    if [ $((ATTEMPT % 5)) -eq 0 ]; then
        echo "📋 진행 상황: $ATTEMPT/$MAX_ATTEMPTS 시도 완료"
        echo "🔍 컨테이너 로그 (최근 20줄):"
        docker logs ${SERVICE_NAME}-${NEW_ENV} --tail 20 2>&1
        echo ""

        # 프로세스 확인
        echo "🔍 Java 프로세스 확인:"
        docker exec ${SERVICE_NAME}-${NEW_ENV} ps aux 2>/dev/null | grep java || echo "Java 프로세스를 찾을 수 없음"
        echo ""

        # 메모리 사용량 확인
        echo "🔍 컨테이너 리소스 사용량:"
        docker stats ${SERVICE_NAME}-${NEW_ENV} --no-stream || echo "리소스 확인 실패"
        echo ""
    fi

    sleep 5
done

if [ "$SUCCESS" = false ]; then
    echo "❌ 헬스 체크 실패!"
    echo ""
    echo "========================================="
    echo "📋 디버깅을 위한 상세 정보"
    echo "========================================="

    # 전체 로그
    echo "📋 전체 애플리케이션 로그:"
    echo "========================================="
    docker logs ${SERVICE_NAME}-${NEW_ENV} 2>&1
    echo "========================================="
    echo ""

    # 컨테이너 상세 정보
    echo "🔍 컨테이너 상세 정보:"
    echo "========================================="
    docker inspect ${SERVICE_NAME}-${NEW_ENV}
    echo "========================================="
    echo ""

    # 환경 변수 확인
    echo "🔍 컨테이너 환경 변수:"
    echo "========================================="
    docker exec ${SERVICE_NAME}-${NEW_ENV} printenv 2>/dev/null | sort || echo "환경 변수 확인 실패"
    echo "========================================="
    echo ""

    # 디버그 모드에서는 컨테이너를 제거하지 않음
    if [ "$DEBUG_MODE" = "true" ]; then
        echo "⚠️ 디버그 모드: 컨테이너를 유지합니다."
        echo "📌 실패한 컨테이너 확인 방법:"
        echo "   - 로그 확인: docker logs ${SERVICE_NAME}-${NEW_ENV}"
        echo "   - 컨테이너 접속: docker exec -it ${SERVICE_NAME}-${NEW_ENV} bash"
        echo "   - 컨테이너 상태: docker inspect ${SERVICE_NAME}-${NEW_ENV}"
        echo "   - 컨테이너 제거: cd ${CURRENT_DIR:-$(pwd)} && $DOCKER_COMPOSE_CMD -f docker-compose-${NEW_ENV}.yml down"
    else
        echo "🔄 컨테이너를 제거합니다..."
        cd "${CURRENT_DIR:-$(pwd)}"
        $DOCKER_COMPOSE_CMD -f "docker-compose-${NEW_ENV}.yml" down 2>/dev/null || true
    fi

    exit 1
fi

# nginx 설정 업데이트
echo "🔧 nginx 설정 업데이트 중..."

# sudo와 nginx 명령어 존재 확인
HAS_SUDO=false
HAS_NGINX=false

if command -v sudo >/dev/null 2>&1; then
    HAS_SUDO=true
fi

if command -v nginx >/dev/null 2>&1; then
    HAS_NGINX=true
fi

# nginx 설정이 불가능한 경우 건너뛰기 (예: Jenkins 컨테이너 환경)
if [ "$HAS_SUDO" = false ] || [ "$HAS_NGINX" = false ]; then
    echo "⚠️ nginx 설정을 건너뜁니다 (sudo: $HAS_SUDO, nginx: $HAS_NGINX)"
    echo "   - AWS ALB나 외부 로드밸런서를 사용하는 경우 정상입니다."
    echo "   - 컨테이너가 정상적으로 실행되었으므로 배포를 계속합니다."

    # 이전 환경 정리
    if [ "$CURRENT_ENV" != "" ]; then
        echo "🧹 이전 환경 정리 중: ${SERVICE_NAME}-${CURRENT_ENV}"
        cd "${CURRENT_DIR:-$(pwd)}"
        $DOCKER_COMPOSE_CMD -f "docker-compose-${CURRENT_ENV}.yml" down 2>/dev/null || true
        echo "✅ 이전 환경 제거 완료"
    fi

    echo "🎉 배포 완료!"
    echo "⏰ 완료 시간: $(date '+%Y-%m-%d %H:%M:%S')"
    exit 0
fi

# nginx 설정 파일 경로 확인 (절대경로 사용)
NGINX_CONFIG_FILE="${CURRENT_DIR}/nginx/nginx-${NEW_ENV}.conf"
OLD_NGINX_CONFIG_FILE="${CURRENT_DIR}/nginx/nginx-${OLD_ENV}.conf"

echo "🔍 nginx 설정 파일 경로 확인:"
echo "   - 새 설정 파일: $NGINX_CONFIG_FILE"
echo "   - 기존 설정 파일: $OLD_NGINX_CONFIG_FILE"
echo "   - 현재 작업 디렉토리: $(pwd)"

if [ -f "$NGINX_CONFIG_FILE" ]; then
    echo "✅ nginx 설정 파일을 찾았습니다: $NGINX_CONFIG_FILE"

    # 새 설정 적용 (절대경로 사용)
    echo "🔧 복사 명령: sudo cp \"$NGINX_CONFIG_FILE\" ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME}"
    sudo cp "$NGINX_CONFIG_FILE" ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME}
    sudo ln -sf ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME} ${NGINX_SITES_ENABLED}/${NGINX_SITE_NAME}

    # nginx 설정 테스트
    if sudo nginx -t; then
        sudo systemctl reload nginx
        echo "✅ nginx 재로드 완료 (${NEW_ENV} 환경으로 전환)"

        # 전환 확인
        ACTIVE_PORT=$(grep "server localhost:" ${NGINX_SITES_ENABLED}/${NGINX_SITE_NAME} | head -1 | awk -F: '{print $2}' | tr -d '; ')
        echo "✅ 활성 포트: $ACTIVE_PORT"
    else
        echo "❌ nginx 설정 테스트 실패"

        # 롤백: 이전 환경 설정으로 복구
        if [ "$CURRENT_ENV" != "" ] && [ -f "$OLD_NGINX_CONFIG_FILE" ]; then
            echo "🔄 이전 환경 설정으로 롤백 중..."
            sudo cp "$OLD_NGINX_CONFIG_FILE" ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME}
            sudo ln -sf ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME} ${NGINX_SITES_ENABLED}/${NGINX_SITE_NAME}

            if sudo nginx -t && sudo systemctl reload nginx; then
                echo "✅ 이전 환경으로 nginx 설정 복구 완료"
            else
                echo "❌ nginx 설정 복구도 실패!"
            fi
        else
            echo "⚠️ 이전 환경 설정을 찾을 수 없어 롤백을 건너뜁니다."
        fi

        # 디버그 모드에서는 컨테이너를 유지
        if [ "$DEBUG_MODE" != "true" ]; then
            echo "🔄 실패한 컨테이너 제거 중..."
            cd "${CURRENT_DIR:-$(pwd)}"
            $DOCKER_COMPOSE_CMD -f "docker-compose-${NEW_ENV}.yml" down 2>/dev/null || true
        fi
        exit 1
    fi
else
    echo "❌ nginx 설정 파일을 찾을 수 없습니다!"
    echo "   확인 대상: $NGINX_CONFIG_FILE"
    echo "   디렉토리 내용:"
    ls -la nginx/ 2>/dev/null || echo "   nginx 디렉토리가 존재하지 않습니다."

    echo "⚠️ nginx 설정 업데이트를 건너뜁니다."
    echo "⚠️ 수동으로 nginx 설정을 확인해주세요."
fi

# 최종 확인
echo "🔍 새 환경 최종 확인..."
FINAL_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${NEW_PORT}/health-check)
if [ "$FINAL_CHECK" != "200" ]; then
    echo "❌ 최종 확인 실패!"
    exit 1
fi

# 기존 환경 종료 - nginx 설정 성공 후에만 실행
if [ "$CURRENT_ENV" != "" ]; then
    echo "🛑 기존 $OLD_ENV 환경 중지 중..."
    echo "⏳ nginx 트래픽 전환 대기 (10초)..."
    sleep 10  # 트래픽 전환을 위한 대기

    echo "🔍 기존 컨테이너 상태 확인:"
    docker ps --filter "name=${SERVICE_NAME}-${OLD_ENV}" --format "table {{.Names}}\t{{.Status}}"

    cd "${CURRENT_DIR:-$(pwd)}"
    $DOCKER_COMPOSE_CMD -f "docker-compose-${OLD_ENV}.yml" down 2>/dev/null || true

    echo "✅ 기존 $OLD_ENV 환경 정리 완료"

    # 정리 후 상태 확인
    echo "🔍 컨테이너 정리 후 상태:"
    docker ps --filter "name=${SERVICE_NAME}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || echo "실행 중인 ${SERVICE_NAME} 컨테이너 없음"
else
    echo "ℹ️ 이전 환경이 없어 정리할 컨테이너가 없습니다."
fi

echo ""
echo "🎉 배포가 성공적으로 완료되었습니다!"
echo "   이전: ${CURRENT_ENV:-없음} → 신규: $NEW_ENV (포트 $NEW_PORT)"
echo "   완료 시간: $(date '+%Y-%m-%d %H:%M:%S')"

# Final verification
echo ""
echo "📊 최종 상태:"
docker ps --filter "name=${SERVICE_NAME}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 헬스 체크 엔드포인트 응답 확인
echo ""
echo "🏥 헬스 체크 응답:"
curl -s http://localhost:${NEW_PORT}/health-check 2>/dev/null || echo "헬스체크 응답 확인 실패"

# 성공적으로 종료
exit 0
