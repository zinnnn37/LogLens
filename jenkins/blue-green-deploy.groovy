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
                    withCredentials([
                        string(credentialsId: 'spring-profiles-active', variable: 'SPRING_PROFILES_ACTIVE'),
                        string(credentialsId: 'spring-datasource-url', variable: 'SPRING_DATASOURCE_URL'),
                        string(credentialsId: 'spring-datasource-username', variable: 'SPRING_DATASOURCE_USERNAME'),
                        string(credentialsId: 'spring-datasource-password', variable: 'SPRING_DATASOURCE_PASSWORD'),
                        string(credentialsId: 'spring-redis-host', variable: 'SPRING_REDIS_HOST'),
                        string(credentialsId: 'spring-redis-port', variable: 'SPRING_REDIS_PORT'),
                        string(credentialsId: 'spring-redis-password', variable: 'SPRING_REDIS_PASSWORD')
                    ]) {
                        sh '''
                            chmod +x scripts/deploy.sh

                            # 환경 변수 export (스크립트에서 사용 가능하도록)
                            export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE}"
                            export SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}"
                            export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME}"
                            export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD}"
                            export SPRING_REDIS_HOST="${SPRING_REDIS_HOST}"
                            export SPRING_REDIS_PORT="${SPRING_REDIS_PORT}"
                            export SPRING_REDIS_PASSWORD="${SPRING_REDIS_PASSWORD}"

                            # 배포 스크립트 실행
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
