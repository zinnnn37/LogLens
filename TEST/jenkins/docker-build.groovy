pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'loglens', description: '빌드할 서비스 이름')
        string(name: 'SHARED_WORKSPACE', defaultValue: '/tmp/shared-workspace', description: '공유 작업 공간 디렉토리')
    }

    environment {
        IMAGE_NAME = "${params.SERVICE_NAME}:latest"
        SHARED_DIR = "${params.SHARED_WORKSPACE}/${params.SERVICE_NAME}"
    }

    stages {
        stage('Copy Artifacts from Shared Directory') {
            steps {
                sh '''
                    if [ ! -d "${SHARED_DIR}/libs" ]; then
                        echo "❌ No libs directory found in shared workspace"
                        exit 1
                    fi

                    mkdir -p build/libs
                    cp ${SHARED_DIR}/libs/*.jar build/libs/
                    echo "✅ JAR files copied to workspace"
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                        # 이전 이미지 백업 (존재할 때만)
                        if [ "$(docker images -q ${IMAGE_NAME})" ]; then
                            echo "ℹ️ Found existing image: ${IMAGE_NAME}, tagging as previous"
                            docker tag ${IMAGE_NAME} ${IMAGE_NAME}-previous
                        else
                            echo "ℹ️ No existing image found, skipping backup"
                        fi

                        # 새 이미지 빌드
                        docker build -t ${IMAGE_NAME} .
                        echo "✅ Docker image built successfully: ${IMAGE_NAME}"

                        # 이미지 확인
                        docker images | grep ${SERVICE_NAME}
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "🎉 Docker build completed successfully!"
            echo "🚀 Blue-Green deployment will be triggered automatically by Jenkins pipeline configuration"
        }
        failure {
            echo "❌ Docker build failed!"
        }
    }
}
