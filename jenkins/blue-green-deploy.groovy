pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'loglens', description: '배포할 서비스 이름')
    }

    environment {
        IMAGE_NAME = "${params.SERVICE_NAME}:latest"
    }

    stages {
        stage('Prepare Environment') {
            steps {
                dir('infra') {
                    withCredentials([file(credentialsId: 'dev-env', variable: 'ENV_FILE')]) {
                        sh '''
                            cp "${ENV_FILE}" .env
                            chmod 600 .env
                            echo "✅ Environment file prepared"
                        '''
                    }
                }
            }
        }

        stage('Blue-Green Deploy') {
            steps {
                dir('infra') {
                    sh '''
                        chmod +x scripts/deploy.sh
                        scripts/deploy.sh
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    echo "🔍 Final deployment status:"
                    docker ps --format "table {{.Names}}\\t{{.Status}}\\t{{.Ports}}" | grep loglens

                    # 활성 포트 확인
                    ACTIVE_PORT=$(grep "server localhost:" /etc/nginx/sites-enabled/loglens | awk -F: '{print $2}' | tr -d ';')
                    echo "✅ Active port: $ACTIVE_PORT"

                    # 헬스 체크
                    curl -f http://localhost:${ACTIVE_PORT}/health-check || exit 1
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
        always {
            sh 'rm -f infra/.env || true'
        }
    }
}
