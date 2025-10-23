// deploy-job.groovy - Part 1
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
        AWS_REGION = 'ap-northeast-2'
        ALB_LISTENER_ARN = 'your-alb-listener-arn'
        BLUE_TG = 'loglens-blue-tg'
        GREEN_TG = 'loglens-green-tg'
    }

    stages {
        stage('Start Data Services') {
            steps {
                echo "🐠 Starting MySQL & Redis with credentials"
                withCredentials([
                        string(credentialsId: 'MYSQL_ROOT_PASSWORD', variable: 'MYSQL_ROOT_PASSWORD'),
                        string(credentialsId: 'MYSQL_DATABASE', variable: 'MYSQL_DATABASE'),
                        string(credentialsId: 'MYSQL_USER', variable: 'MYSQL_USER'),
                        string(credentialsId: 'MYSQL_PASSWORD', variable: 'MYSQL_PASSWORD'),
                        string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PASSWORD')
                ]) {
                    sh '''
                        cd infra/
                        export MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
                        export MYSQL_DATABASE=${MYSQL_DATABASE}
                        export MYSQL_USER=${MYSQL_USER}
                        export MYSQL_PASSWORD=${MYSQL_PASSWORD}
                        export REDIS_PASSWORD=${REDIS_PASSWORD}
                        
                        docker compose -f docker-compose-data.yml up -d
                        echo "✅ Data services started"
                    '''
                }
            }
        }

        stage('Determine Target Environment') {
            steps {
                script {
                    sh '''
                        BLUE_RUNNING=$(docker ps -q -f name=loglens-app-blue -f status=running)
                        GREEN_RUNNING=$(docker ps -q -f name=loglens-app-green -f status=running)
                        
                        if [ ! -z "$BLUE_RUNNING" ] && [ -z "$GREEN_RUNNING" ]; then
                            echo "DEPLOY_TARGET=green" > deploy-target.env
                            echo "🔵 Blue active → Deploying to Green"
                        elif [ ! -z "$GREEN_RUNNING" ] && [ -z "$BLUE_RUNNING" ]; then
                            echo "DEPLOY_TARGET=blue" > deploy-target.env
                            echo "🟢 Green active → Deploying to Blue"
                        else
                            echo "DEPLOY_TARGET=blue" > deploy-target.env
                            echo "⚪ Initial deployment → Blue"
                        fi
                    '''
                    def props = readProperties file: 'deploy-target.env'
                    env.DEPLOY_TARGET = props.DEPLOY_TARGET
                }
            }
        }

        stage('Deploy New Version') {
            steps {
                withCredentials([
                        string(credentialsId: 'MYSQL_USER', variable: 'MYSQL_USER'),
                        string(credentialsId: 'MYSQL_PASSWORD', variable: 'MYSQL_PASSWORD'),
                        string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PASSWORD')
                ]) {
                    script {
                        def containerName = "loglens-app-${env.DEPLOY_TARGET}"
                        def port = env.DEPLOY_TARGET == 'blue' ? env.BLUE_PORT : env.GREEN_PORT

                        sh """
                            if [ \$(docker ps -aq -f name=${containerName}) ]; then
                                docker stop ${containerName} || true
                                docker rm ${containerName} || true
                            fi
                            
                            docker run -d --name ${containerName} --network loglens-network \
                                -p ${port}:8080 -e SPRING_PROFILES_ACTIVE=prod \
                                -e SPRING_DATASOURCE_URL=jdbc:mysql://loglens-mysql:3306/loglens \
                                -e SPRING_DATASOURCE_USERNAME=${MYSQL_USER} \
                                -e SPRING_DATASOURCE_PASSWORD=${MYSQL_PASSWORD} \
                                -e SPRING_REDIS_HOST=loglens-redis -e SPRING_REDIS_PORT=6379 \
                                -e SPRING_REDIS_PASSWORD=${REDIS_PASSWORD} \
                                --restart unless-stopped ${IMAGE_NAME}
                            
                            echo "✅ ${containerName} deployed on port ${port}"
                        """
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def port = env.DEPLOY_TARGET == 'blue' ? env.BLUE_PORT : env.GREEN_PORT
                    timeout(time: 5, unit: 'MINUTES') {
                        sh """
                            for i in {1..30}; do
                                if curl -sf http://localhost:${port}/actuator/health; then
                                    echo "✅ Health check passed"
                                    exit 0
                                fi
                                echo "Waiting... (\$i/30)"
                                sleep 10
                            done
                            echo "❌ Health check failed"
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
                        input message: 'Traffic을 전환하시겠습니까?', ok: '전환'
                    }

                    withAWS(credentials: 'aws-credentials', region: env.AWS_REGION) {
                        sh """
                            TG=\$([ "${env.DEPLOY_TARGET}" = "blue" ] && echo "${BLUE_TG}" || echo "${GREEN_TG}")
                            TG_ARN=\$(aws elbv2 describe-target-groups --names \$TG \
                                --query 'TargetGroups[0].TargetGroupArn' --output text)
                            
                            aws elbv2 modify-listener --listener-arn ${ALB_LISTENER_ARN} \
                                --default-actions Type=forward,TargetGroupArn=\$TG_ARN
                            
                            echo "✅ Traffic switched to ${env.DEPLOY_TARGET}"
                        """
                    }
                }
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
            sh 'rm -f infra/dev/.env || true'
        }
    }
}
