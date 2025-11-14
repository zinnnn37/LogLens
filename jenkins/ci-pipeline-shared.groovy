// ci-build-job.groovy
pipeline {
    agent any

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'be/develop', description: '빌드할 브랜치')
        string(name: 'SERVICE_NAME', defaultValue: 'loglens', description: '서비스 이름')
    }

    environment {
        JAVA_HOME = '/opt/java/openjdk'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        GIT_REPO = 'https://lab.ssafy.com/s13-final/S13P31A306.git'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "📦 Checking out ${params.BRANCH_NAME} branch"
                git branch: "${params.BRANCH_NAME}",
                        url: "${GIT_REPO}",
                        credentialsId: 'gitlab_username_with_pw'
            }
        }

        stage('Build') {
            steps {
                echo "🔨 Building Spring Boot application with JDK 21"
                sh '''
                    echo "Java Version: $(java -version 2>&1 | head -n 1)"
                    echo "JAVA_HOME: $JAVA_HOME"
                    chmod +x ./gradlew
                    ./gradlew clean build -x test --no-daemon
                '''
            }
        }

//        stage('Test') {
//            steps {
//                echo "🧪 Running tests"
//                sh '''
//                    ./gradlew test --no-daemon
//                '''
//            }
//            post {
//                always {
//                    script {
//                        def testResults = findFiles(glob: 'build/test-results/test/*.xml')
//                        if (testResults.length > 0) {
//                            junit 'build/test-results/test/*.xml'
//                        } else {
//                            echo "⚠️ No test results found"
//                        }
//                    }
//                }
//            }
//        }

        stage('Archive Artifacts') {
            steps {
                echo "📤 Archiving build artifacts"
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo "🎉 CI Build completed successfully!"
        }
        failure {
            echo "❌ CI Build failed!"
        }
        always {
            cleanWs()
        }
    }
}
