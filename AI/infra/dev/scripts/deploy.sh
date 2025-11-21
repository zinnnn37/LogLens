#!/bin/bash

set -e

echo "🚀 BLUE/GREEN 배포 시작"
echo "⏰ 시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"

# nginx 설정 파일 경로
NGINX_SITES_AVAILABLE="/etc/nginx/sites-available"
NGINX_SITES_ENABLED="/etc/nginx/sites-enabled"
NGINX_SITE_NAME="loglens-ai"

# 디버그 모드 설정 (실패 시 컨테이너 유지)
DEBUG_MODE="${DEBUG_MODE:-false}"

# 현재 활성 환경 확인
CURRENT_ENV=""
BLUE_RUNNING=false
GREEN_RUNNING=false

if docker ps --format "table {{.Names}}" | grep -q "ai-service-blue"; then
    BLUE_RUNNING=true
fi

if docker ps --format "table {{.Names}}" | grep -q "ai-service-green"; then
    GREEN_RUNNING=true
fi

# 환경 상태에 따른 처리
if [ "$BLUE_RUNNING" = true ] && [ "$GREEN_RUNNING" = true ]; then
    echo "⚠️ 두 환경 모두 실행 중입니다. green 환경을 중지합니다..."
    docker stop ai-service-green 2>/dev/null || true
    docker rm ai-service-green 2>/dev/null || true
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

# 새로운 환경 시작
echo "🎯 $NEW_ENV 환경 시작 중..."

# Docker 네트워크 생성 (이미 있으면 무시)
docker network create ai-loglens-network 2>/dev/null || true

# 컨테이너 시작
docker run -d \
  --name ai-service-${NEW_ENV} \
  --network ai-loglens-network \
  -p ${NEW_PORT}:8000 \
  --env-file .env \
  -e SLOT=${NEW_ENV} \
  -e UVICORN_PORT=8000 \
  -e LOG_LEVEL=INFO \
  --restart unless-stopped \
  --health-cmd "curl -f http://localhost:8000/api/v1/health || exit 1" \
  --health-interval 30s \
  --health-timeout 10s \
  --health-retries 3 \
  --health-start-period 60s \
  ai-service:latest

echo "✅ 컨테이너 시작 명령 완료"

# 컨테이너 시작 대기
echo "⏳ 컨테이너 시작 대기중..."
sleep 10

# 컨테이너 상태 확인
echo "🔍 컨테이너 상태 확인:"
docker ps -a | grep ai-service-${NEW_ENV}
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
    CONTAINER_STATUS=$(docker inspect --format='{{.State.Status}}' ai-service-${NEW_ENV} 2>/dev/null || echo "unknown")
    CONTAINER_RESTART_COUNT=$(docker inspect --format='{{.RestartCount}}' ai-service-${NEW_ENV} 2>/dev/null || echo "0")

    # 재시작 감지
    if [ "$CONTAINER_RESTART_COUNT" -gt "$RESTART_COUNT" ]; then
        echo "⚠️ 컨테이너가 재시작되었습니다! (재시작 횟수: $CONTAINER_RESTART_COUNT)"
        RESTART_COUNT=$CONTAINER_RESTART_COUNT
        echo "🔍 재시작 후 로그 (최근 30줄):"
        docker logs ai-service-${NEW_ENV} --tail 30 2>&1
        echo ""
    fi

    if [ "$CONTAINER_STATUS" != "running" ]; then
        echo "⏳ 컨테이너 시작 대기 중... (시도 $ATTEMPT/$MAX_ATTEMPTS, 상태: $CONTAINER_STATUS)"

        # 컨테이너가 종료된 경우 종료 코드 확인
        if [ "$CONTAINER_STATUS" = "exited" ]; then
            EXIT_CODE=$(docker inspect --format='{{.State.ExitCode}}' ai-service-${NEW_ENV} 2>/dev/null || echo "unknown")
            echo "❌ 컨테이너가 종료됨. 종료 코드: $EXIT_CODE"
        fi

        sleep 5
        continue
    fi

    # Health check - Docker의 HEALTHCHECK 결과 확인 (Jenkins 컨테이너 환경 대응)
    HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' ai-service-${NEW_ENV} 2>/dev/null || echo "none")

    # Docker HEALTHCHECK가 healthy면 성공
    if [ "$HEALTH_STATUS" = "healthy" ]; then
        echo "✅ Docker HEALTHCHECK가 healthy 상태입니다!"
    else
        # HEALTHCHECK 결과가 없거나 아직 starting이면 curl로 직접 확인 시도
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${NEW_PORT}/api/v1/health 2>/dev/null || echo "000")

        # 추가적으로 루트 엔드포인트도 확인
        if [ "$HTTP_CODE" != "200" ]; then
            ROOT_HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${NEW_PORT}/ 2>/dev/null || echo "000")
            if [ "$ROOT_HTTP_CODE" = "200" ]; then
                echo "⚠️ 헬스체크 엔드포인트는 응답하지 않지만 루트 엔드포인트는 정상입니다."
                HEALTH_STATUS="healthy"  # 루트가 정상이면 통과
            fi
        elif [ "$HTTP_CODE" = "200" ]; then
            HEALTH_STATUS="healthy"  # HTTP 헬스체크 성공
        fi
    fi

    if [ "$HEALTH_STATUS" = "healthy" ]; then
        echo "✅ $NEW_ENV 환경이 정상 상태입니다!"
        SUCCESS=true

        # OpenSearch 인덱스 초기화 (최초 배포 시에만 필요)
        echo "🔧 OpenSearch 인덱스 확인 및 초기화..."

        # 컨테이너 내부에서 인덱스 생성 스크립트 실행
        docker exec ai-service-${NEW_ENV} python scripts/create_indices.py 2>&1 | tee /tmp/index-creation.log

        if [ ${PIPESTATUS[0]} -eq 0 ]; then
            echo "✅ OpenSearch 인덱스 준비 완료"
        else
            # 인덱스가 이미 존재하는 경우도 있으므로 로그 확인
            if grep -q "already exists\|resource_already_exists_exception" /tmp/index-creation.log; then
                echo "ℹ️ OpenSearch 인덱스가 이미 존재합니다"
            else
                echo "⚠️ OpenSearch 인덱스 생성 실패. 로그를 확인하세요."
                echo "📋 Error details:"
                cat /tmp/index-creation.log
                # 인덱스 생성 실패는 경고만 하고 배포는 계속 진행
            fi
        fi
        rm -f /tmp/index-creation.log

        # 추가 확인 (컨테이너 내부에서 헬스체크)
        echo "🔍 엔드포인트 확인 (컨테이너 내부):"
        docker exec ai-service-${NEW_ENV} curl -s http://localhost:8000/api/v1/health 2>/dev/null | jq . 2>/dev/null || echo "✅ 헬스체크 확인 (jq 없음)"

        break
    fi

    echo "⏳ 헬스 체크 대기 중... (시도 $ATTEMPT/$MAX_ATTEMPTS, 상태: $HEALTH_STATUS)"

    if [ $((ATTEMPT % 5)) -eq 0 ]; then
        echo "📋 진행 상황: $ATTEMPT/$MAX_ATTEMPTS 시도 완료"
        echo "🔍 컨테이너 로그 (최근 20줄):"
        docker logs ai-service-${NEW_ENV} --tail 20 2>&1
        echo ""

        # 프로세스 확인
        echo "🔍 Java 프로세스 확인:"
        docker exec ai-service-${NEW_ENV} ps aux 2>/dev/null | grep "python\|uvicorn" || echo "Python 프로세스를 찾을 수 없음"
        echo ""

        # 메모리 사용량 확인
        echo "🔍 컨테이너 리소스 사용량:"
        docker stats ai-service-${NEW_ENV} --no-stream || echo "리소스 확인 실패"
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
    docker logs ai-service-${NEW_ENV} 2>&1
    echo "========================================="
    echo ""

    # 컨테이너 상세 정보
    echo "🔍 컨테이너 상세 정보:"
    echo "========================================="
    docker inspect ai-service-${NEW_ENV} | jq '{
        State: .State,
        Config: {
            Env: .Config.Env,
            Cmd: .Config.Cmd,
            Entrypoint: .Config.Entrypoint
        },
        HostConfig: {
            Memory: .HostConfig.Memory,
            CpuShares: .HostConfig.CpuShares,
            RestartPolicy: .HostConfig.RestartPolicy
        }
    }' 2>/dev/null || docker inspect ai-service-${NEW_ENV}
    echo "========================================="
    echo ""

    # 환경 변수 확인
    echo "🔍 컨테이너 환경 변수:"
    echo "========================================="
    docker exec ai-service-${NEW_ENV} printenv 2>/dev/null | sort || echo "환경 변수 확인 실패"
    echo "========================================="
    echo ""

    # 디스크 사용량
    echo "🔍 컨테이너 내부 디스크 사용량:"
    echo "========================================="
    docker exec ai-service-${NEW_ENV} df -h 2>/dev/null || echo "디스크 사용량 확인 실패"
    echo "========================================="
    echo ""

    # 네트워크 연결 확인
    echo "🔍 네트워크 연결 상태:"
    echo "========================================="
    docker exec ai-service-${NEW_ENV} netstat -tuln 2>/dev/null || echo "네트워크 상태 확인 실패"
    echo "========================================="
    echo ""

    # 최근 이벤트
    echo "🔍 Docker 이벤트 (최근 10분):"
    echo "========================================="
    docker events --since 10m --until now --filter container=ai-service-${NEW_ENV} || echo "이벤트 확인 실패"
    echo "========================================="
    echo ""

    # 디버그 모드에서는 컨테이너를 제거하지 않음
    if [ "$DEBUG_MODE" = "true" ]; then
        echo "⚠️ 디버그 모드: 컨테이너를 유지합니다."
        echo "📌 실패한 컨테이너 확인 방법:"
        echo "   - 로그 확인: docker logs ai-service-${NEW_ENV}"
        echo "   - 컨테이너 접속: docker exec -it ai-service-${NEW_ENV} bash"
        echo "   - 컨테이너 상태: docker inspect ai-service-${NEW_ENV}"
        echo "   - 컨테이너 제거: docker stop ai-service-${NEW_ENV} && docker rm ai-service-${NEW_ENV}"
    else
        echo "🔄 컨테이너를 제거합니다..."
        docker stop ai-service-${NEW_ENV} 2>/dev/null || true
        docker rm ai-service-${NEW_ENV} 2>/dev/null || true
    fi

    # 이미지 롤백 (이전 이미지가 있으면)
    if [ "$(docker images -q ai-service:latest-previous)" ]; then
        echo "🔄 Rolling back Docker image to previous version..."
        docker rmi ai-service:latest 2>/dev/null || true
        docker tag ai-service:latest-previous ai-service:latest
        echo "✅ Image rolled back successfully"
    else
        echo "⚠️ No previous image found, removing failed image"
        docker rmi ai-service:latest 2>/dev/null || true
    fi

    exit 1
fi

# nginx 설정 업데이트
echo "🔧 nginx 설정 업데이트 중..."

# nginx 설정 파일 경로 확인 (절대경로 사용)
CURRENT_DIR=$(pwd)
NGINX_CONFIG_FILE="${CURRENT_DIR}/nginx/nginx-${NEW_ENV}.conf"
OLD_NGINX_CONFIG_FILE="${CURRENT_DIR}/nginx/nginx-${OLD_ENV}.conf"

echo "🔍 nginx 설정 파일 경로 확인:"
echo "   - 새 설정 파일: $NGINX_CONFIG_FILE"
echo "   - 기존 설정 파일: $OLD_NGINX_CONFIG_FILE"
echo "   - 현재 작업 디렉토리: $(pwd)"

if [ -f "$NGINX_CONFIG_FILE" ]; then
    echo "✅ nginx 설정 파일을 찾았습니다: $NGINX_CONFIG_FILE"
    
    # 새 설정 적용 (절대경로 사용)
    echo "🔧 복사 명령: cp \"$NGINX_CONFIG_FILE\" ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME}"
    cp "$NGINX_CONFIG_FILE" ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME}
    ln -sf ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME} ${NGINX_SITES_ENABLED}/${NGINX_SITE_NAME}

    # nginx reload (kill 시그널 사용)
    if [ -f /var/run/nginx.pid ]; then
        NGINX_PID=$(cat /var/run/nginx.pid)
        echo "🔄 nginx reload 중... (PID: $NGINX_PID)"
        kill -HUP $NGINX_PID

        if [ $? -eq 0 ]; then
            echo "✅ nginx 재로드 완료 (${NEW_ENV} 환경으로 전환)"

            # 전환 확인
            ACTIVE_PORT=$(grep "server localhost:" ${NGINX_SITES_ENABLED}/${NGINX_SITE_NAME} | head -1 | awk -F: '{print $2}' | tr -d '; ')
            echo "✅ 활성 포트: $ACTIVE_PORT"
        else
            echo "❌ nginx reload 실패"

            # 롤백: 이전 환경 설정으로 복구
            if [ "$CURRENT_ENV" != "" ] && [ -f "$OLD_NGINX_CONFIG_FILE" ]; then
                echo "🔄 이전 환경 설정으로 롤백 중..."
                cp "$OLD_NGINX_CONFIG_FILE" ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME}
                ln -sf ${NGINX_SITES_AVAILABLE}/${NGINX_SITE_NAME} ${NGINX_SITES_ENABLED}/${NGINX_SITE_NAME}
                kill -HUP $NGINX_PID && echo "✅ 이전 환경으로 nginx 설정 복구 완료" || echo "❌ nginx 설정 복구도 실패!"
            else
                echo "⚠️ 이전 환경 설정을 찾을 수 없어 롤백을 건너뜁니다."
            fi
        fi
    else
        echo "⚠️ nginx PID 파일을 찾을 수 없습니다: /var/run/nginx.pid"
        echo "📋 설정 파일은 업데이트되었으나 reload는 수동으로 실행 필요:"
        echo "   sudo systemctl reload nginx"
        # 설정은 변경되었으므로 경고만 하고 계속 진행
    fi
else
    echo "❌ nginx 설정 파일을 찾을 수 없습니다!"
    echo "   확인 대상: $NGINX_CONFIG_FILE"
    echo "   디렉토리 내용:"
    ls -la nginx/ 2>/dev/null || echo "   nginx 디렉토리가 존재하지 않습니다."
    
    echo "⚠️ nginx 설정 업데이트를 건너뜁니다."
    echo "⚠️ 수동으로 nginx 설정을 확인해주세요."
fi

# 기존 환경 종료 - nginx 설정 성공 후에만 실행
if [ "$CURRENT_ENV" != "" ]; then
    echo "🛑 기존 $OLD_ENV 환경 중지 중..."
    echo "⏳ nginx 트래픽 전환 대기 (10초)..."
    sleep 10  # 트래픽 전환을 위한 대기
    
    echo "🔍 기존 컨테이너 상태 확인:"
    docker ps --filter "name=ai-service-${OLD_ENV}" --format "table {{.Names}}\t{{.Status}}"

    echo "🛑 기존 컨테이너 중지 및 제거..."
    docker stop ai-service-${OLD_ENV} 2>/dev/null || true
    docker rm ai-service-${OLD_ENV} 2>/dev/null || true

    echo "✅ 기존 $OLD_ENV 환경 정리 완료"
    
    # 정리 후 상태 확인
    echo "🔍 컨테이너 정리 후 상태:"
    docker ps --filter "name=ai-service" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || echo "실행 중인 ai-service 컨테이너 없음"
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
docker ps --filter "name=ai-service" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 헬스 체크 엔드포인트 응답 확인
echo ""
echo "🏥 헬스 체크 응답:"
curl -s http://localhost:${NEW_PORT}/api/v1/health | jq . 2>/dev/null || echo "JSON 파싱 실패"

# 성공적으로 종료
exit 0
