pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'loglens', description: '배포할 서비스 이름')
    }

    environment {
        IMAGE_NAME = "${params.SERVICE_NAME}:latest"
    }

    stages {
        stage('Blue-Green Deploy') {
            steps {
                dir('infra') {
                    withCredentials([file(credentialsId: 'dev-env', variable: 'ENV_FILE')]) {
                        sh '''
                            # .env 파일 줄바꿈 변환 (CRLF → LF)
                            # dos2unix가 없을 경우 sed 사용
                            if command -v dos2unix >/dev/null 2>&1; then
                                dos2unix "$ENV_FILE" 2>/dev/null || sed -i 's/\r$//' "$ENV_FILE"
                            else
                                sed -i 's/\r$//' "$ENV_FILE"
                            fi

                            # .env 파일에서 환경변수 export (안전한 파싱)
                            echo "📄 Loading environment variables from .env file"

                            while IFS='=' read -r key value || [ -n "$key" ]; do
                                # 빈 줄이나 주석 무시
                                case "$key" in
                                    ''|'#'*) continue ;;
                                esac

                                # 앞뒤 공백 제거
                                key=$(echo "$key" | xargs)
                                value=$(echo "$value" | xargs)

                                # 값이 따옴표로 감싸져 있으면 제거
                                value=$(echo "$value" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")

                                # export
                                export "$key=$value"
                                echo "✅ Loaded: $key"
                            done < "$ENV_FILE"

                            # 배포 스크립트 실행
                            chmod +x scripts/deploy.sh
                            scripts/deploy.sh
                        '''
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    echo "🔍 Final deployment status:"
                    docker ps --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}" | grep loglens || true

                    # 활성 컨테이너 확인
                    ACTIVE_CONTAINER=$(docker ps --format "{{.Names}}" | grep "loglens-" | head -1)

                    if [ -n "$ACTIVE_CONTAINER" ]; then
                        echo "✅ Active container: $ACTIVE_CONTAINER"

                        # Docker health status 확인
                        HEALTH_STATUS=$(docker inspect --format='{{.State.Health.Status}}' $ACTIVE_CONTAINER 2>/dev/null || echo "none")
                        echo "🏥 Health status: $HEALTH_STATUS"

                        if [ "$HEALTH_STATUS" = "healthy" ] || [ "$HEALTH_STATUS" = "none" ]; then
                            # 컨테이너 내부에서 헬스 체크 실행
                            docker exec $ACTIVE_CONTAINER curl -f http://localhost:8080/health-check || exit 1
                            echo "✅ Health check passed!"
                        else
                            echo "❌ Container is not healthy: $HEALTH_STATUS"
                            exit 1
                        fi
                    else
                        echo "⚠️ No active loglens container found"
                        exit 1
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo "🎉 Deployment completed successfully!"
        }
        failure {
            echo "❌ Deployment failed!"
        }
    }
}
