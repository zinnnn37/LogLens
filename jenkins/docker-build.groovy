// docker-build-job.groovy
pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'loglens', description: '서비스 이름')
        string(name: 'BUILD_NUMBER', defaultValue: '1', description: '빌드 번호')
    }

    environment {
        IMAGE_NAME = "${params.SERVICE_NAME}:latest"
        IMAGE_TAG = "${params.SERVICE_NAME}:build-${params.BUILD_NUMBER}"
    }

    stages {
        stage('Copy Artifacts') {
            steps {
                echo "📥 Copying artifacts from previous job"
                copyArtifacts projectName: 'ci-build-job',
                        filter: 'build/libs/*.jar',
                        target: '.'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "🐳 Building Docker image: ${IMAGE_NAME}"
                sh '''
                    docker build -t ${IMAGE_NAME} -t ${IMAGE_TAG} .
                    echo "✅ Docker image built successfully"
                    docker images | grep ${SERVICE_NAME}
                '''
            }
        }
    }

    post {
        success {
            echo "🎉 Docker build completed successfully!"
            // Job 3 트리거
            build job: 'deploy-job',
                    parameters: [
                            string(name: 'SERVICE_NAME', value: "${params.SERVICE_NAME}")
                    ],
                    wait: false
        }
        failure {
            echo "❌ Docker build failed!"
        }
        always {
            cleanWs()
        }
    }
}
