pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'ai-service', description: '빌드할 서비스 이름')
        string(name: 'SHARED_WORKSPACE', defaultValue: '/tmp/shared-workspace', description: '공유 작업 공간 디렉토리')
    }

    environment {
        IMAGE_NAME = "${params.SERVICE_NAME}:latest"
        SHARED_DIR = "${params.SHARED_WORKSPACE}/${params.SERVICE_NAME}"
    }

    stages {
        stage('Copy AI Service Artifacts') {
            steps {
                sh '''
                    echo "🔍 Debugging shared workspace:"
                    echo "Expected path: ${SHARED_DIR}"
                    echo "Current user: $(whoami)"
                    echo "Current working directory: $(pwd)"
                    
                    echo "📂 Checking shared workspace root:"
                    ls -la ${SHARED_WORKSPACE} || echo "Shared workspace root not found"
                    
                    echo "📂 Checking service directory:"
                    ls -la ${SHARED_DIR} || echo "Service directory not found"
                    
                    echo "📂 Checking /tmp structure:"
                    ls -la /tmp/ | grep shared || echo "No shared directory in /tmp"
                    
                    if [ ! -d "${SHARED_DIR}/app" ]; then
                        echo "❌ No app directory found in shared workspace"
                        echo "📋 Expected AI service artifacts: app/, requirements.txt, Dockerfile"
                        echo "🔍 Full system debug:"
                        find /tmp -name "*ai-service*" -type d 2>/dev/null || echo "No ai-service directories found anywhere in /tmp"
                        find /tmp -name "app" -type d 2>/dev/null | head -5 || echo "No app directories found in /tmp"
                        exit 1
                    fi
                    
                    # Python 소스코드 및 설정 파일 복사
                    cp -r ${SHARED_DIR}/app/ .
                    cp ${SHARED_DIR}/requirements.txt .
                    cp ${SHARED_DIR}/Dockerfile .
                    
                    # 빌드 정보 확인
                    if [ -f "${SHARED_DIR}/build-info.txt" ]; then
                        cp ${SHARED_DIR}/build-info.txt .
                        echo "📋 Build info:"
                        cat build-info.txt
                    fi
                    
                    echo "✅ AI service files copied to workspace"
                    echo "📁 Workspace contents:"
                    ls -la
                '''
            }
        }

        stage('Build AI Service Docker Image') {
            steps {
                script {
                    sh """
                        # 이전 이미지 백업 (존재할 때만)
                        if [ "\$(docker images -q ${IMAGE_NAME})" ]; then
                            echo "ℹ️ Found existing AI service image: ${IMAGE_NAME}, tagging as previous"
                            docker tag ${IMAGE_NAME} ${IMAGE_NAME}-previous
                        else
                            echo "ℹ️ No existing AI service image found, skipping backup"
                        fi

                        # AI 서비스 Docker 이미지 빌드
                        echo "🐳 Building AI service image: ${IMAGE_NAME} (no cache)"
                        docker build --no-cache -t ${IMAGE_NAME} .
                        echo "✅ AI service Docker image built successfully: ${IMAGE_NAME}"

                        # 이미지 확인
                        echo "📋 AI service images:"
                        docker images | grep "${SERVICE_NAME}" || echo "Image built but grep found no match (this is OK)"
                    """
                }
            }
        }

        stage('Verify AI Service Image') {
            steps {
                script {
                    sh """
                        # Docker 이미지 빌드 성공 여부만 확인
                        echo "🔍 Verifying AI service image build"

                        if [ "\$(docker images -q ${IMAGE_NAME})" ]; then
                            echo "✅ AI service image built successfully: ${IMAGE_NAME}"
                            echo "📋 AI service images:"
                            docker images | grep "${SERVICE_NAME}" || echo "Image exists but grep found no match"
                            echo "🎯 Docker image is ready for deployment"
                        else
                            echo "❌ AI service image not found"
                            exit 1
                        fi
                    """
                }
            }
        }
    }

    post {
        success {
            echo "🎉 AI service Docker image built successfully!"
            echo "🚀 Image ready for Blue-Green deployment: ${IMAGE_NAME}"
            echo "📋 Next step: Deploy pipeline will verify service health"
        }
        failure {
            echo "❌ AI service Docker build failed!"
            echo "📋 Check logs for Docker build issues"
            echo "🔄 Rolling back to previous image..."
            script {
                sh """
                    # 실패한 이미지 제거
                    if [ "\$(docker images -q ${IMAGE_NAME})" ]; then
                        echo "🗑️ Removing failed image: ${IMAGE_NAME}"
                        docker rmi ${IMAGE_NAME} || true
                    fi

                    # 이전 이미지가 있으면 복원
                    if [ "\$(docker images -q ${IMAGE_NAME}-previous)" ]; then
                        echo "🔄 Restoring previous image..."
                        docker tag ${IMAGE_NAME}-previous ${IMAGE_NAME}
                        echo "✅ Rolled back to previous image"
                    else
                        echo "⚠️ No previous image found to restore"
                    fi
                """
            }
        }
        always {
            // 실패 시 디버깅을 위한 정보 수집
            script {
                try {
                    sh """
                        echo "📊 Final Docker status:"
                        docker images | grep "${SERVICE_NAME}" || echo "No AI service images found"
                        docker ps -a | grep "${SERVICE_NAME}" || echo "No AI service containers found"
                    """
                } catch (Exception e) {
                    echo "Failed to collect Docker status: ${e.message}"
                }
            }

            // Docker 이미지 자동 정리
            script {
                try {
                    sh '''
                        echo ""
                        echo "🧹 Docker 이미지 정리 시작..."
                        echo "========================================="

                        # 정리 전 상태
                        echo "📋 정리 전 디스크 사용량:"
                        docker system df
                        echo ""

                        # 1. ai-service-test 이미지 중 오래된 것 삭제 (최근 5개만 유지)
                        echo "📋 현재 테스트 이미지 목록:"
                        docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | grep "ai-service-test" || echo "테스트 이미지 없음"
                        echo ""

                        # 빌드 번호 기준으로 정렬하여 오래된 이미지 찾기
                        OLD_TEST_IMAGES=$(docker images --format "{{.Repository}}:{{.Tag}}" | \
                            grep "^ai-service-test:" | \
                            sort -t: -k2 -n | \
                            head -n -5)

                        if [ ! -z "$OLD_TEST_IMAGES" ]; then
                            echo "🗑️ 오래된 테스트 이미지 삭제 중..."
                            echo "$OLD_TEST_IMAGES" | while read img; do
                                echo "  - 삭제: $img"
                                docker rmi "$img" 2>/dev/null || echo "  ⚠️ 삭제 실패 (사용 중일 수 있음)"
                            done
                        else
                            echo "ℹ️ 삭제할 오래된 테스트 이미지가 없습니다 (5개 이하 유지 중)"
                        fi
                        echo ""

                        # 2. Dangling 이미지 삭제 (태그 없는 이미지)
                        echo "🗑️ Dangling 이미지 삭제 중..."
                        DANGLING_COUNT=$(docker images -f "dangling=true" -q | wc -l)
                        if [ "$DANGLING_COUNT" -gt 0 ]; then
                            echo "  찾은 dangling 이미지: ${DANGLING_COUNT}개"
                            docker image prune -f
                        else
                            echo "  Dangling 이미지 없음"
                        fi
                        echo ""

                        # 3. 정리 후 상태
                        echo "✅ 정리 완료!"
                        echo "📋 정리 후 디스크 사용량:"
                        docker system df
                        echo ""

                        echo "📋 현재 ai-service 이미지 목록:"
                        docker images | grep "ai-service" || echo "ai-service 이미지 없음"
                        echo "========================================="
                    '''
                } catch (Exception e) {
                    echo "⚠️ Docker 이미지 정리 중 오류 발생: ${e.message}"
                    echo "ℹ️ 정리 실패는 배포에 영향을 주지 않습니다"
                }
            }
        }
    }
}
