pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'ai-service', description: '배포할 서비스 이름')
    }

    environment {
        IMAGE_NAME = "${params.SERVICE_NAME}:latest"
    }

    stages {
        stage('Prepare AI Service Environment') {
            steps {
                dir('infra/dev') {
                    withCredentials([file(credentialsId: 'ai-env-file', variable: 'ENV_FILE')]) {
                        sh '''
                            cp "${ENV_FILE}" .env
                            chmod 600 .env
                            
                            echo "✅ AI service environment file prepared"
                            echo "📋 Checking required AI environment variables:"

                            # AI 서비스 필수 환경변수 확인
                            if grep -q "OPENAI_API_KEY" .env; then
                                echo "✅ OpenAI API key configured"
                            else
                                echo "⚠️ OPENAI_API_KEY not found"
                            fi

                            if grep -q "OPENSEARCH_HOST" .env; then
                                echo "✅ OpenSearch connection configured"
                            else
                                echo "⚠️ OPENSEARCH_HOST not found"
                            fi

                            if grep -q "KAFKA_BOOTSTRAP_SERVERS" .env; then
                                echo "✅ Kafka connection configured"
                            else
                                echo "⚠️ KAFKA_BOOTSTRAP_SERVERS not found"
                            fi
                            
                            if grep -q "SERVICE_NAME" .env; then
                                echo "✅ Service name configured"
                            else
                                echo "📋 Adding SERVICE_NAME to environment"
                                echo "SERVICE_NAME=ai-service" >> .env
                            fi
                        '''
                    }
                }
            }
        }

        stage('AI Service Blue-Green Deploy') {
            steps {
                dir('infra/dev') {
                    sh '''
                        chmod +x scripts/deploy.sh
                        
                        echo "🚀 Starting AI service Blue-Green deployment"
                        
                        # AI 서비스용 환경변수 설정
                        export SERVICE_TYPE=ai-service
                        export BASE_PORT=8000
                        export SERVICE_DOMAIN=ai.loglens.store
                        
                        # 배포 스크립트 실행
                        scripts/deploy.sh
                    '''
                }
            }
        }

        stage('AI Service Health Check') {
            steps {
                sh '''
                    echo "🔍 Final AI service deployment status:"
                    docker ps --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}" | grep ai-service

                    # AI 서비스 활성 포트 확인 (8000 또는 8001)
                    NGINX_CONFIG="/etc/nginx/sites-enabled/loglens-ai"
                    if [ -f "$NGINX_CONFIG" ]; then
                        ACTIVE_PORT=$(grep "server localhost:" $NGINX_CONFIG | awk -F: '{print $2}' | tr -d ';' | xargs)
                        echo "✅ Active AI service port: $ACTIVE_PORT"

                        # AI 서비스 헬스체크 (Docker HEALTHCHECK 상태 확인)
                        echo "🏥 AI service health check:"
                        if docker ps --filter "name=ai-service" --filter "health=healthy" --format "{{.Names}}" | grep -q ai-service; then
                            echo "✅ AI service health check passed"

                            # 컨테이너 내부에서 health endpoint 확인
                            CONTAINER=$(docker ps --filter "name=ai-service" --format "{{.Names}}" | head -1)
                            echo "🔍 Testing health endpoint in container: $CONTAINER"
                            docker exec $CONTAINER curl -f http://localhost:8000/api/v1/health > /dev/null 2>&1 && echo "✅ Health endpoint responding" || echo "⚠️ Health endpoint test failed"
                        else
                            echo "❌ AI service health check failed"
                            exit 1
                        fi

                        # AI 서비스 API 엔드포인트 확인
                        echo "🤖 AI service API endpoints:"
                        echo "✅ Health endpoint: /api/v1/health"
                        echo "✅ Log analysis endpoint: /api/v1/logs/{log_id}/analysis"
                        echo "✅ Chatbot endpoint: /api/v1/chatbot/ask"
                        
                    else
                        echo "⚠️ Nginx config not found at $NGINX_CONFIG"
                        echo "📋 Checking AI service container health status..."

                        # Docker HEALTHCHECK 상태로 확인
                        if docker ps --filter "name=ai-service" --filter "health=healthy" --format "{{.Names}}" | grep -q ai-service; then
                            CONTAINER=$(docker ps --filter "name=ai-service" --format "{{.Names}}" | head -1)
                            echo "✅ AI service container $CONTAINER is healthy"
                        else
                            echo "❌ AI service container not healthy"
                            exit 1
                        fi
                    fi
                    
                    echo "🎯 AI service deployment verification completed successfully"
                '''
            }
        }
    }

    post {
        success {
            echo "🎉 AI service deployment completed successfully!"
            echo "🔗 AI service available at: https://ai.loglens.store"
            echo "📋 Available AI API endpoints:"
            echo "   - Health Check: GET /api/v1/health"
            echo "   - Log Analysis: GET /api/v1/logs/{log_id}/analysis"
            echo "   - Chatbot QA: POST /api/v1/chatbot/ask"
            echo "   - API Docs: https://ai.loglens.store/docs"
        }
        failure {
            echo "❌ AI service deployment failed!"
            echo "📋 Check logs for deployment, health check, or configuration issues"
            echo "🔄 Rolling back Docker image to previous version..."

            // 실패 시 디버깅 정보 수집 및 이미지 롤백
            script {
                try {
                    sh '''
                        echo "🔍 AI service debugging information:"
                        echo "📋 Running containers:"
                        docker ps | grep ai-service || echo "No ai-service containers running"

                        echo "📋 Available images:"
                        docker images | grep ai-service || echo "No ai-service images found"

                        echo "📋 Recent container logs:"
                        for container in $(docker ps -a --filter "name=ai-service" --format "{{.Names}}"); do
                            echo "--- Logs for $container ---"
                            docker logs $container --tail 10 2>&1 || echo "Failed to get logs for $container"
                        done

                        echo ""
                        echo "🔄 Attempting to rollback Docker image..."

                        # 배포 실패 시 이미지 롤백
                        if [ "$(docker images -q ai-service:latest-previous)" ]; then
                            echo "🗑️ Removing failed image..."
                            docker rmi ai-service:latest 2>/dev/null || true

                            echo "🔄 Restoring previous image..."
                            docker tag ai-service:latest-previous ai-service:latest

                            echo "✅ Docker image rolled back successfully"
                            echo "ℹ️ Previous version restored as ai-service:latest"
                        else
                            echo "⚠️ No previous image found (ai-service:latest-previous)"
                            echo "ℹ️ Failed image remains for debugging"
                        fi
                    '''
                } catch (Exception e) {
                    echo "Failed to collect debugging information or rollback: ${e.message}"
                }
            }
        }
        always {
            sh 'rm -f infra/dev/.env || true'
            
            // 최종 상태 로그
            script {
                try {
                    sh '''
                        echo "📊 Final AI service status:"
                        docker ps --filter "name=ai-service" --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}" || echo "No AI service containers found"
                    '''
                } catch (Exception e) {
                    echo "Failed to collect final status: ${e.message}"
                }
            }
        }
    }
}
