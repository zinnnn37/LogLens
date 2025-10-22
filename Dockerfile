FROM openjdk:21-jdk-slim

# 타임존 설정
ENV TZ=Asia/Seoul
RUN apt-get update && \
    apt-get install -y tzdata curl && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 애플리케이션 사용자 생성
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# JAR 파일 복사
COPY --chown=spring:spring build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 사용자 전환
USER spring

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/health-check || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
