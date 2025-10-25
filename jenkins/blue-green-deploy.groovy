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
                            if grep -q "MYSQL_DSN" .env; then
                                echo "✅ Database connection configured"
                            else
                                echo "⚠️ MYSQL_DSN not found"
                            fi
                            
                            if grep -q "OPENAI_API_KEY" .env; then
                                echo "✅ OpenAI API key configured"
                            else
                                echo "⚠️ OPENAI_API_KEY not found"
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
                        export SERVICE_DOMAIN=ai.fintech-osm.store
                        
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
                    NGINX_CONFIG="/etc/nginx/sites-enabled/ai.fintech-osm.store"
                    if [ -f "$NGINX_CONFIG" ]; then
                        ACTIVE_PORT=$(grep "server localhost:" $NGINX_CONFIG | awk -F: '{print $2}' | tr -d ';' | xargs)
                        echo "✅ Active AI service port: $ACTIVE_PORT"
                        
                        # AI 서비스 헬스체크
                        echo "🏥 AI service health check:"
                        if curl -f http://localhost:${ACTIVE_PORT}/health; then
                            echo "✅ AI service health check passed"
                        else
                            echo "❌ AI service health check failed"
                            exit 1
                        fi
                        
                        # AI 서비스 기본 엔드포인트 확인
                        echo "🔍 AI service endpoints test:"
                        curl -f http://localhost:${ACTIVE_PORT}/ | head -5 || echo "Root endpoint test completed"
                        
                        # AI 서비스 특화 엔드포인트 확인 (선택사항)
                        echo "🤖 AI service specific endpoints:"
                        curl -f http://localhost:${ACTIVE_PORT}/api/v1/news/health || echo "News service endpoint check completed"
                        curl -f http://localhost:${ACTIVE_PORT}/api/v1/quiz/health || echo "Quiz service endpoint check completed"
                        curl -f http://localhost:${ACTIVE_PORT}/api/v1/filter/health || echo "Filter service endpoint check completed"
                        
                    else
                        echo "⚠️ Nginx config not found at $NGINX_CONFIG"
                        echo "📋 Checking if AI service is running on default ports..."
                        
                        # 기본 포트들 확인
                        for port in 8000 8001; do
                            if curl -f http://localhost:${port}/health 2>/dev/null; then
                                echo "✅ AI service responding on port $port"
                                ACTIVE_PORT=$port
                                break
                            fi
                        done
                        
                        if [ -z "$ACTIVE_PORT" ]; then
                            echo "❌ AI service not responding on any expected port"
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
            echo "🔗 AI service available at: https://ai.fintech-osm.store"
            echo "📋 Available AI services:"
            echo "   - News Crawler: /api/v1/news/"
            echo "   - Quiz Generator: /api/v1/quiz/"
            echo "   - Content Filter: /api/v1/filter/"
        }
        failure {
            echo "❌ AI service deployment failed!"
            echo "📋 Check logs for deployment, health check, or configuration issues"
            
            // 실패 시 디버깅 정보 수집
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
                    '''
                } catch (Exception e) {
                    echo "Failed to collect debugging information: ${e.message}"
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
