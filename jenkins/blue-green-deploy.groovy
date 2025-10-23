// deploy-job.groovy
pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'loglens', description: '서비스 이름')
        choice(name: 'TRAFFIC_SWITCH_MODE', choices: ['auto', 'manual'],
                description: '트래픽 전환 모드')
    }

    environment {
        IMAGE_NAME = "${params.SERVICE_NAME}:latest"
        BLUE_PORT = '8080'
        GREEN_PORT = '8081'
    }

    stages {
        stage('Prepare Environment') {
            steps {
                echo "📋 Preparing environment file from credentials"
                withCredentials([
                        file(credentialsId: 'dev-env', variable: 'ENV_FILE')
                ]) {
                    sh '''#!/bin/bash
                        # Jenkins workspace에 .env 파일 복사
                        cp ${ENV_FILE} ${WORKSPACE}/.env
                        echo "✅ Environment file prepared"
                        
                        # 환경변수 확인 (민감정보는 마스킹됨)
                        echo "📊 Environment variables loaded"
                        grep "^AWS_REGION=" ${WORKSPACE}/.env || echo "AWS_REGION not found"
                        grep "^BLUE_TG=" ${WORKSPACE}/.env || echo "BLUE_TG not found"
                        grep "^GREEN_TG=" ${WORKSPACE}/.env || echo "GREEN_TG not found"
                    '''
                }
            }
        }

        stage('Start Data Services') {
            steps {
                echo "🐠 Starting MySQL & Redis with environment file"
                sh '''#!/bin/bash
                    # .env 파일을 infra 디렉토리로 복사
                    mkdir -p infra
                    cp ${WORKSPACE}/.env infra/.env
                    
                    cd infra/
                    
                    # 기존 컨테이너가 실행 중인지 확인
                    if docker ps | grep -q "loglens-mysql\\|loglens-redis"; then
                        echo "ℹ️ Data services already running, checking if restart needed..."
                        
                        # 환경 변수 해시 비교 (변경 감지)
                        NEW_HASH=$(md5sum .env | awk '{print $1}')
                        OLD_HASH=""
                        
                        if [ -f /tmp/loglens-data-env.hash ]; then
                            OLD_HASH=$(cat /tmp/loglens-data-env.hash)
                        fi
                        
                        if [ "$NEW_HASH" != "$OLD_HASH" ]; then
                            echo "⚠️ Environment variables changed, restarting data services..."
                            docker compose -f docker-compose-data.yml down
                            docker compose -f docker-compose-data.yml up -d
                            echo "$NEW_HASH" > /tmp/loglens-data-env.hash
                        else
                            echo "✅ No environment changes, skipping restart"
                        fi
                    else
                        echo "🚀 Starting data services for the first time..."
                        docker compose -f docker-compose-data.yml up -d
                        md5sum .env | awk '{print $1}' > /tmp/loglens-data-env.hash
                    fi
                    
                    echo "✅ Data services ready"
                    docker ps | grep loglens
                '''
            }
        }

        stage('Determine Target Environment') {
            steps {
                script {
                    sh '''#!/bin/bash
                        BLUE_RUNNING=$(docker ps -q -f name=loglens-app-blue -f status=running)
                        GREEN_RUNNING=$(docker ps -q -f name=loglens-app-green -f status=running)
                        
                        if [ ! -z "$BLUE_RUNNING" ] && [ -z "$GREEN_RUNNING" ]; then
                            echo "DEPLOY_TARGET=green" > deploy-target.env
                            echo "🔵 Blue is active → Deploying to Green"
                        elif [ ! -z "$GREEN_RUNNING" ] && [ -z "$BLUE_RUNNING" ]; then
                            echo "DEPLOY_TARGET=blue" > deploy-target.env
                            echo "🟢 Green is active → Deploying to Blue"
                        else
                            echo "DEPLOY_TARGET=blue" > deploy-target.env
                            echo "⚪ Initial deployment → Deploying to Blue"
                        fi
                    '''
                    def props = readProperties file: 'deploy-target.env'
                    env.DEPLOY_TARGET = props.DEPLOY_TARGET
                    echo "🎯 Target environment: ${env.DEPLOY_TARGET}"
                }
            }
        }

        stage('Deploy New Version') {
            steps {
                script {
                    def containerName = "loglens-app-${env.DEPLOY_TARGET}"
                    def port = env.DEPLOY_TARGET == 'blue' ? env.BLUE_PORT : env.GREEN_PORT

                    sh """#!/bin/bash
                        # 기존 컨테이너 정리
                        if [ \$(docker ps -aq -f name=${containerName}) ]; then
                            echo "🗑️ Removing old container: ${containerName}"
                            docker stop ${containerName} || true
                            docker rm ${containerName} || true
                        fi
                        
                        # 새 컨테이너 배포 (env-file 사용)
                        echo "🚀 Deploying ${containerName} on port ${port}"
                        docker run -d \
                            --name ${containerName} \
                            --network loglens-network \
                            -p ${port}:8080 \
                            --env-file ${WORKSPACE}/.env \
                            --restart unless-stopped \
                            ${IMAGE_NAME}
                        
                        echo "✅ ${containerName} deployed successfully"
                        docker ps | grep ${containerName}
                    """
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def port = env.DEPLOY_TARGET == 'blue' ? env.BLUE_PORT : env.GREEN_PORT

                    echo "🏥 Running health check on port ${port}"
                    timeout(time: 5, unit: 'MINUTES') {
                        sh """#!/bin/bash
                            for i in {1..30}; do
                                echo "Health check attempt \$i/30..."
                                
                                if curl -sf http://localhost:${port}/actuator/health; then
                                    echo "✅ Health check passed!"
                                    exit 0
                                fi
                                
                                echo "⏳ Waiting... (\$i/30)"
                                sleep 10
                            done
                            
                            echo "❌ Health check failed after 30 attempts"
                            exit 1
                        """
                    }
                }
            }
        }

        stage('Switch Traffic') {
            steps {
                script {
                    if (params.TRAFFIC_SWITCH_MODE == 'manual') {
                        input message: '새 버전으로 트래픽을 전환하시겠습니까?', ok: '전환'
                    }

                    echo "🔄 Switching traffic to ${env.DEPLOY_TARGET}"

                    // .env 파일에서 AWS credentials와 설정 로드
                    sh '''#!/bin/bash
                        # .env 파일에서 환경 변수 로드 (bash의 set -a 대신 export 사용)
                        while IFS='=' read -r key value; do
                            # 주석과 빈 줄 무시
                            if [[ ! $key =~ ^# && -n $key ]]; then
                                # 따옴표 제거
                                value="${value%\"}"
                                value="${value#\"}"
                                export "$key=$value"
                            fi
                        done < ${WORKSPACE}/.env
                        
                        # Target Group 결정
                        if [ "${DEPLOY_TARGET}" = "blue" ]; then
                            TG_NAME="${BLUE_TG}"
                        else
                            TG_NAME="${GREEN_TG}"
                        fi
                        
                        echo "🎯 Target Group: $TG_NAME"
                        echo "🌍 Region: ${AWS_REGION}"
                        
                        # Target Group ARN 조회
                        echo "🔍 Looking up Target Group ARN..."
                        TG_ARN=$(aws elbv2 describe-target-groups \
                            --names "$TG_NAME" \
                            --query 'TargetGroups[0].TargetGroupArn' \
                            --output text \
                            --region ${AWS_REGION})
                        
                        if [ -z "$TG_ARN" ] || [ "$TG_ARN" = "None" ]; then
                            echo "❌ Failed to find Target Group: $TG_NAME"
                            exit 1
                        fi
                        
                        echo "✅ Target Group ARN: $TG_ARN"
                        
                        # ALB Listener 규칙 수정
                        echo "🔧 Modifying ALB Listener..."
                        aws elbv2 modify-listener \
                            --listener-arn "${ALB_LISTENER_ARN}" \
                            --default-actions Type=forward,TargetGroupArn="$TG_ARN" \
                            --region ${AWS_REGION}
                        
                        if [ $? -eq 0 ]; then
                            echo "✅ Traffic switched to ${DEPLOY_TARGET} successfully"
                        else
                            echo "❌ Failed to switch traffic"
                            exit 1
                        fi
                    '''
                }
            }
        }

        stage('Cleanup Old Environment') {
            steps {
                script {
                    def oldEnvironment = env.DEPLOY_TARGET == 'blue' ? 'green' : 'blue'
                    def oldContainer = "loglens-app-${oldEnvironment}"

                    timeout(time: 2, unit: 'MINUTES') {
                        sh """#!/bin/bash
                            echo "🧹 Cleaning up old environment: ${oldContainer}"
                            
                            if [ \$(docker ps -aq -f name=${oldContainer}) ]; then
                                # Graceful shutdown (30초 대기)
                                docker stop -t 30 ${oldContainer} || true
                                docker rm ${oldContainer} || true
                                echo "✅ Old container removed: ${oldContainer}"
                            else
                                echo "ℹ️ No old container to clean up"
                            fi
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            echo """
                🎉 Deployment completed successfully!
                
                📊 Deployment Summary:
                - Service: ${params.SERVICE_NAME}
                - Target: ${env.DEPLOY_TARGET}
                - Port: ${env.DEPLOY_TARGET == 'blue' ? env.BLUE_PORT : env.GREEN_PORT}
                - Traffic Switch Mode: ${params.TRAFFIC_SWITCH_MODE}
            """
        }
        failure {
            echo "❌ Deployment failed!"
            script {
                // 실패 시 롤백 로직
                if (env.DEPLOY_TARGET) {
                    def containerName = "loglens-app-${env.DEPLOY_TARGET}"
                    sh """#!/bin/bash
                        echo "🔙 Rolling back deployment..."
                        docker stop ${containerName} || true
                        docker rm ${containerName} || true
                    """
                } else {
                    echo "⚠️ DEPLOY_TARGET not set, skipping rollback"
                }
            }
        }
        always {
            // .env 파일 제거 (보안)
            sh '''#!/bin/bash
                rm -f ${WORKSPACE}/.env
                rm -f infra/.env
                echo "🔒 Environment file cleaned up"
            '''
            cleanWs()
        }
    }
}
