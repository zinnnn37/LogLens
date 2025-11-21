FROM openjdk:21-jdk-slim

# 타임존 설정
ENV TZ=Asia/Seoul
RUN apt-get update && \
    apt-get install -y tzdata curl procps && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 애플리케이션 사용자 생성
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# JAR 파일 복사
COPY --chown=spring:spring build/libs/*.jar app.jar


# GC 로깅 및 JVM 모니터링 옵션 추가
# - GC 로그를 /app/logs/gc.log에 기록
# - Full GC, 메모리 사용률, 힙 상태, pause 시간 모두 기록
# - String Deduplication 활성화로 메모리 중복 최소화
ENV JAVA_OPTS="\
  -Xms512m \
  -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -Xlog:gc*,gc+heap=debug:file=/app/logs/gc.log:time,uptime,level,tags \
  -XX:+PrintCommandLineFlags \
"

# 로그 디렉토리 생성
RUN mkdir -p /app/logs && chown -R spring:spring /app/logs

# 포트 노출
EXPOSE 8080

# 사용자 전환
USER spring

# 헬스체크
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health-check || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
