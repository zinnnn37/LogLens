// ci-build-job.groovy
pipeline {
    agent any

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'master', description: '빌드할 브랜치')
        string(name: 'SERVICE_NAME', defaultValue: 'loglens', description: '서비스 이름')
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-21-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${PATH}"
        GIT_REPO = 'https://gitlab.com/your-org/loglens.git'
    }

    stages {
        stage('Checkout') {
            steps {
                echo "📦 Checking out ${params.BRANCH_NAME} branch"
                git branch: "${params.BRANCH_NAME}",
                        url: "${GIT_REPO}",
                        credentialsId: 'gitlab-credentials'
            }
        }

        stage('Build') {
            steps {
                echo "🔨 Building Spring Boot application with JDK 21"
                sh '''
                    chmod +x ./gradlew
                    ./gradlew clean build -x test --no-daemon
                '''
            }
        }

        stage('Test') {
            steps {
                echo "🧪 Running tests"
                sh './gradlew test --no-daemon'
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo "📤 Archiving build artifacts"
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            script {
                def testResults = findFiles(glob: 'build/test-results/test/*.xml')
                if (testResults.length == 0) {
                    echo "⚠️ 테스트 결과 파일이 없습니다."
                }
            }
            junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
            cleanWs()
        }
    }
}
