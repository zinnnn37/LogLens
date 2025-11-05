#!/bin/bash

set -e

# ============================================
# Blue-Green Deployment Script
# ============================================

echo "=========================================="
echo "Starting Blue-Green Deployment"
echo "=========================================="

# 설정
SERVICE_NAME="loglens"
IMAGE_NAME="${SERVICE_NAME}:latest"
NETWORK_NAME="loglens-network"
BLUE_PORT=8000
GREEN_PORT=8001
NGINX_CONF_DIR="/etc/nginx/sites-enabled"
NGINX_BLUE_CONF="../nginx/nginx-blue.conf"
NGINX_GREEN_CONF="../nginx/nginx-green.conf"

# ============================================
# 1. 현재 활성 슬롯 감지
# ============================================

echo "🔍 Detecting current active slot..."

# 실행 중인 컨테이너 확인
BLUE_RUNNING=$(docker ps -q -f name=${SERVICE_NAME}-blue 2>/dev/null || echo "")
GREEN_RUNNING=$(docker ps -q -f name=${SERVICE_NAME}-green 2>/dev/null || echo "")

if [ -n "$BLUE_RUNNING" ]; then
    CURRENT_SLOT="blue"
    CURRENT_PORT=$BLUE_PORT
    NEW_SLOT="green"
    NEW_PORT=$GREEN_PORT
    NEW_NGINX_CONF=$NGINX_GREEN_CONF
    echo "✅ Current active slot: BLUE (port $CURRENT_PORT)"
    echo "📦 Deploying to: GREEN (port $NEW_PORT)"
elif [ -n "$GREEN_RUNNING" ]; then
    CURRENT_SLOT="green"
    CURRENT_PORT=$GREEN_PORT
    NEW_SLOT="blue"
    NEW_PORT=$BLUE_PORT
    NEW_NGINX_CONF=$NGINX_BLUE_CONF
    echo "✅ Current active slot: GREEN (port $CURRENT_PORT)"
    echo "📦 Deploying to: BLUE (port $NEW_PORT)"
else
    # 최초 배포: Blue 슬롯부터 시작
    CURRENT_SLOT="none"
    CURRENT_PORT="none"
    NEW_SLOT="blue"
    NEW_PORT=$BLUE_PORT
    NEW_NGINX_CONF=$NGINX_BLUE_CONF
    echo "ℹ️  No active slot detected (first deployment)"
    echo "📦 Deploying to: BLUE (port $NEW_PORT)"
fi

# ============================================
# 2. 새 컨테이너 배포
# ============================================

echo ""
echo "🚀 Deploying new container to $NEW_SLOT slot..."

# 기존 컨테이너 정리 (비활성 슬롯)
if docker ps -a -q -f name=${SERVICE_NAME}-${NEW_SLOT} | grep -q .; then
    echo "🧹 Removing old container: ${SERVICE_NAME}-${NEW_SLOT}"
    docker stop ${SERVICE_NAME}-${NEW_SLOT} 2>/dev/null || true
    docker rm ${SERVICE_NAME}-${NEW_SLOT} 2>/dev/null || true
fi

# 네트워크 존재 확인 및 생성
if ! docker network inspect $NETWORK_NAME >/dev/null 2>&1; then
    echo "📡 Creating network: $NETWORK_NAME"
    docker network create $NETWORK_NAME
fi

# .env 파일 로드
if [ -f ".env" ]; then
    echo "📄 Loading environment variables from .env"
    set -a
    source .env
    set +a
else
    echo "⚠️  Warning: .env file not found, using default values"
fi

# 새 컨테이너 실행
echo "🐳 Starting new container: ${SERVICE_NAME}-${NEW_SLOT}"
docker run -d \
    --name ${SERVICE_NAME}-${NEW_SLOT} \
    --network $NETWORK_NAME \
    -p ${NEW_PORT}:8080 \
    -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod} \
    -e SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:-jdbc:h2:mem:testdb} \
    -e SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME:-sa} \
    -e SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD:-} \
    -e SPRING_REDIS_HOST=${SPRING_REDIS_HOST:-loglens-redis} \
    -e SPRING_REDIS_PORT=${SPRING_REDIS_PORT:-6379} \
    -e SPRING_REDIS_PASSWORD=${SPRING_REDIS_PASSWORD:-} \
    --restart unless-stopped \
    $IMAGE_NAME

echo "✅ Container started successfully"

# ============================================
# 3. 헬스체크
# ============================================

echo ""
echo "🔍 Performing health check on new container..."

MAX_ATTEMPTS=30
ATTEMPT=0
HEALTH_CHECK_URL="http://localhost:${NEW_PORT}/health-check"

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))
    echo "Attempt $ATTEMPT/$MAX_ATTEMPTS: Checking $HEALTH_CHECK_URL"

    if curl -f -s -o /dev/null $HEALTH_CHECK_URL; then
        echo "✅ Health check passed!"
        break
    fi

    if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
        echo "❌ Health check failed after $MAX_ATTEMPTS attempts"
        echo "🔄 Rolling back: Stopping new container"
        docker stop ${SERVICE_NAME}-${NEW_SLOT}
        docker rm ${SERVICE_NAME}-${NEW_SLOT}
        exit 1
    fi

    sleep 2
done

# ============================================
# 4. Nginx 설정 전환
# ============================================

echo ""
echo "🔄 Switching Nginx configuration to $NEW_SLOT slot..."

# Nginx 설정 파일 교체
if [ -d "$NGINX_CONF_DIR" ] && [ -w "$NGINX_CONF_DIR" ]; then
    echo "📝 Updating Nginx configuration in $NGINX_CONF_DIR"
    sudo cp $NEW_NGINX_CONF $NGINX_CONF_DIR/loglens.conf

    # Nginx 설정 검증
    if sudo nginx -t; then
        echo "✅ Nginx configuration is valid"
        # Nginx 리로드
        sudo nginx -s reload
        echo "✅ Nginx reloaded successfully"
    else
        echo "❌ Nginx configuration is invalid"
        exit 1
    fi
else
    echo "⚠️  Warning: Cannot access $NGINX_CONF_DIR"
    echo "ℹ️  Skipping Nginx configuration update (may require manual intervention)"
    echo "ℹ️  To manually update: sudo cp $NEW_NGINX_CONF $NGINX_CONF_DIR/loglens.conf && sudo nginx -s reload"
fi

# ============================================
# 5. 이전 컨테이너 정리
# ============================================

if [ "$CURRENT_SLOT" != "none" ]; then
    echo ""
    echo "🧹 Cleaning up old container ($CURRENT_SLOT slot)..."

    # 잠시 대기 (새 컨테이너 안정화)
    sleep 5

    echo "🛑 Stopping old container: ${SERVICE_NAME}-${CURRENT_SLOT}"
    docker stop ${SERVICE_NAME}-${CURRENT_SLOT} 2>/dev/null || true

    echo "🗑️  Removing old container: ${SERVICE_NAME}-${CURRENT_SLOT}"
    docker rm ${SERVICE_NAME}-${CURRENT_SLOT} 2>/dev/null || true

    echo "✅ Old container removed"
fi

# ============================================
# 완료
# ============================================

echo ""
echo "=========================================="
echo "🎉 Blue-Green Deployment Completed!"
echo "=========================================="
echo "Active slot: $NEW_SLOT (port $NEW_PORT)"
echo "Container: ${SERVICE_NAME}-${NEW_SLOT}"
echo "Image: $IMAGE_NAME"
echo ""
echo "📊 Current container status:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep $SERVICE_NAME || echo "No containers found"
echo "=========================================="
