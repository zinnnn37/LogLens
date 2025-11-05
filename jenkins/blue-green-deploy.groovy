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

                            # .env 파일에서 환경변수 export
                            echo "📄 Loading environment variables from .env file"
                            set -a
                            . "$ENV_FILE"
                            set +a

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

                    # 활성 포트 확인
                    if [ -f /etc/nginx/sites-enabled/loglens ]; then
                        ACTIVE_PORT=$(grep "server localhost:" /etc/nginx/sites-enabled/loglens | head -1 | awk -F: '{print $2}' | tr -d '; ')
                        echo "✅ Active port: $ACTIVE_PORT"

                        # 헬스 체크
                        curl -f http://localhost:${ACTIVE_PORT}/health-check || exit 1
                    else
                        echo "⚠️ Nginx configuration not found, skipping health check"
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
