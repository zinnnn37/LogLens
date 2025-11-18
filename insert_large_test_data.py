#!/usr/bin/env python3
"""
대규모 테스트 로그 데이터 생성 (1000개)
- ERROR: 5% (50개)
- WARN: 15% (150개)
- INFO: 80% (800개)
"""

import os
import sys
from datetime import datetime, timedelta
from opensearchpy import OpenSearch
from openai import OpenAI
from dotenv import load_dotenv
import random
import time

# 환경 변수 로드
load_dotenv()

OPENSEARCH_HOST = os.getenv("OPENSEARCH_HOST", "localhost")
OPENSEARCH_PORT = int(os.getenv("OPENSEARCH_PORT", "9200"))
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL")

# OpenSearch 클라이언트
os_client = OpenSearch(
    hosts=[{"host": OPENSEARCH_HOST, "port": OPENSEARCH_PORT}],
    http_auth=("admin", "admin"),
    use_ssl=False,
    verify_certs=False,
)

# OpenAI 클라이언트
openai_client = OpenAI(
    api_key=OPENAI_API_KEY,
    base_url=OPENAI_BASE_URL,
)


def generate_timestamp(minutes_ago: int) -> str:
    """N분 전의 timestamp 생성"""
    timestamp = datetime.utcnow() - timedelta(minutes=minutes_ago)
    return timestamp.isoformat() + "Z"


def generate_embedding(text: str) -> list:
    """텍스트를 벡터로 변환 (1536차원)"""
    max_retries = 3
    for attempt in range(max_retries):
        try:
            response = openai_client.embeddings.create(
                model="text-embedding-3-large",
                input=text,
                encoding_format="float",
                dimensions=1536  # 명시적으로 1536차원 지정
            )
            vector = response.data[0].embedding
            if vector and len(vector) == 1536:
                return vector
            else:
                print(f"  ⚠️  Embedding 형식 오류 (attempt {attempt+1}): len={len(vector) if vector else 0}")
        except Exception as e:
            print(f"  ⚠️  Embedding 생성 실패 (attempt {attempt+1}/{max_retries}): {e}")
            if attempt < max_retries - 1:
                time.sleep(1)  # 재시도 전 대기

    print(f"  ❌ Embedding 생성 최종 실패")
    return None


# 로그 템플릿 정의
ERROR_TEMPLATES = [
    "NullPointerException in {class_name}.{method}",
    "Database connection timeout in {class_name}",
    "Authentication failed for user request",
    "File not found: {file_path}",
    "OutOfMemoryError in {service}",
    "Invalid request parameter: {param}",
    "API rate limit exceeded",
    "Service {service} unavailable",
    "Payment transaction failed: {reason}",
    "Failed to parse JSON response",
]

WARN_TEMPLATES = [
    "Slow database query detected ({duration}ms)",
    "Cache miss for key: {key}",
    "Deprecated API endpoint used",
    "High memory usage: {usage}%",
    "Retry attempt {attempt} for {operation}",
    "SSL certificate expires in {days} days",
    "Unusual traffic pattern detected",
    "Queue size exceeding threshold",
    "Session timeout warning",
    "Disk usage above 80%",
]

INFO_TEMPLATES = [
    "User {user_id} logged in successfully",
    "Request processed in {duration}ms",
    "Cache hit for key: {key}",
    "Scheduled task {task} completed",
    "API call to {endpoint} successful",
    "File {filename} uploaded successfully",
    "Email sent to {recipient}",
    "Database backup completed",
    "Configuration reloaded",
    "Health check passed",
]

SERVICES = ["user-service", "payment-service", "auth-service", "product-service", "order-service"]
CLASSES = [
    "UserController", "PaymentService", "AuthController", "ProductRepository",
    "OrderProcessor", "CacheManager", "DatabaseConnection", "ApiGateway"
]
METHODS = [
    "createUser", "processPayment", "validateToken", "executeQuery",
    "handleRequest", "updateRecord", "deleteEntity", "fetchData"
]


def generate_log_message(level: str) -> str:
    """레벨에 맞는 로그 메시지 생성"""
    if level == "ERROR":
        template = random.choice(ERROR_TEMPLATES)
        return template.format(
            class_name=random.choice(CLASSES),
            method=random.choice(METHODS),
            file_path=f"/data/file_{random.randint(1, 100)}.txt",
            service=random.choice(SERVICES),
            param=f"param_{random.randint(1, 10)}",
            reason=random.choice(["insufficient funds", "invalid card", "network error"])
        )
    elif level == "WARN":
        template = random.choice(WARN_TEMPLATES)
        return template.format(
            duration=random.randint(500, 5000),
            key=f"cache_key_{random.randint(1, 1000)}",
            usage=random.randint(70, 95),
            attempt=random.randint(1, 5),
            operation=random.choice(["database query", "API call", "file upload"]),
            days=random.randint(1, 30),
            task=f"task_{random.randint(1, 20)}"
        )
    else:  # INFO
        template = random.choice(INFO_TEMPLATES)
        return template.format(
            user_id=f"user_{random.randint(1000, 9999)}",
            duration=random.randint(10, 500),
            key=f"cache_key_{random.randint(1, 1000)}",
            task=f"task_{random.randint(1, 20)}",
            endpoint=f"/api/v1/{random.choice(['users', 'products', 'orders'])}",
            filename=f"document_{random.randint(1, 100)}.pdf",
            recipient=f"user{random.randint(1, 100)}@example.com"
        )


def insert_large_test_data():
    """대규모 테스트 데이터 삽입"""

    print("=" * 80)
    print("🚀 대규모 테스트 로그 삽입 스크립트")
    print("=" * 80)
    print()

    # OpenSearch 연결 확인
    try:
        info = os_client.info()
        print(f"✅ OpenSearch 연결 성공: {info['version']['number']}")
    except Exception as e:
        print(f"❌ OpenSearch 연결 실패: {e}")
        return

    # 프로젝트 UUID
    project_uuid = "test-large-data-project"
    index_name = f"{project_uuid.replace('-', '_')}_2025_11"

    # 로그 분포 설정
    total_logs = 200  # 200개로 축소 (빠른 테스트)
    error_count = 10   # 5%
    warn_count = 30   # 15%
    info_count = 160   # 80%

    print(f"📝 {total_logs}개의 로그 삽입 시작...")
    print(f"   인덱스: {index_name}")
    print(f"   분포: ERROR={error_count} (5%), WARN={warn_count} (15%), INFO={info_count} (80%)")
    print(f"   현재 UTC 시간: {datetime.utcnow().isoformat()}Z")
    print()

    # 로그 레벨 리스트 생성
    levels = (
        ["ERROR"] * error_count +
        ["WARN"] * warn_count +
        ["INFO"] * info_count
    )
    random.shuffle(levels)  # 섞어서 시간순으로 다양하게

    # 배치 삽입 준비
    batch_size = 50
    batch_docs = []
    inserted_count = 0
    start_time = time.time()

    print(f"🔄 벡터 생성 중... (배치 크기: {batch_size})")
    print()

    for i, level in enumerate(levels, 1):
        # 로그 데이터 생성
        log_id = 1000 + i
        message = generate_log_message(level)
        service = random.choice(SERVICES)
        class_name = random.choice(CLASSES)
        method_name = random.choice(METHODS)

        # 시간은 최근 24시간 내 랜덤
        minutes_ago = random.randint(0, 24 * 60)
        timestamp = generate_timestamp(minutes_ago)

        # 벡터 생성 텍스트
        vector_text = f"{level} {service} {class_name}.{method_name} {message}"

        # Embedding 생성 (배치마다 한 번씩 진행 상황 출력)
        if i % batch_size == 1:
            print(f"  [{i}/{total_logs}] {level} 로그 벡터화 중...")

        log_vector = generate_embedding(vector_text)

        if log_vector is None:
            print(f"  ⚠️ 로그 {log_id} 벡터 생성 실패 - 건너뜀")
            continue

        # 로그 문서
        doc = {
            "log_id": log_id,
            "project_uuid": project_uuid,
            "timestamp": timestamp,
            "level": level,
            "log_level": level,
            "message": message,
            "service_name": service,
            "logger": f"com.example.{service}",
            "source_type": "APPLICATION",
            "layer": "Service",
            "method_name": method_name,
            "class_name": f"com.example.{service}.{class_name}",
            "thread_name": f"http-nio-8080-exec-{random.randint(1, 10)}",
            "trace_id": f"trace-{random.randint(1000, 9999)}",
            "requester_ip": f"192.168.1.{random.randint(1, 254)}",
            "duration": random.randint(10, 1000),
            "indexed_at": datetime.utcnow().isoformat() + "Z",
            "@timestamp": timestamp,
            "log_vector": log_vector,
            "log_details": {
                "http_method": random.choice(["GET", "POST", "PUT", "DELETE"]),
                "response_status": 200 if level == "INFO" else (400 if level == "WARN" else 500),
                "request_uri": f"/api/v1/{random.choice(['users', 'products', 'orders'])}",
                "execution_time": random.randint(10, 1000),
                "class_name": f"com.example.{service}.{class_name}",
                "method_name": method_name
            }
        }

        batch_docs.append(doc)

        # 배치 삽입
        if len(batch_docs) >= batch_size:
            try:
                for doc in batch_docs:
                    os_client.index(index=index_name, body=doc, refresh=False)
                inserted_count += len(batch_docs)
                elapsed = time.time() - start_time
                rate = inserted_count / elapsed if elapsed > 0 else 0
                print(f"  ✅ {inserted_count}/{total_logs} 삽입 완료 ({rate:.1f} docs/sec)")
                batch_docs = []
            except Exception as e:
                print(f"  ❌ 배치 삽입 실패: {e}")
                batch_docs = []

    # 남은 문서 삽입
    if batch_docs:
        try:
            for doc in batch_docs:
                os_client.index(index=index_name, body=doc, refresh=False)
            inserted_count += len(batch_docs)
        except Exception as e:
            print(f"  ❌ 마지막 배치 삽입 실패: {e}")

    # 인덱스 새로고침
    print()
    print("🔄 인덱스 새로고침 중...")
    os_client.indices.refresh(index=index_name)

    elapsed = time.time() - start_time

    print()
    print("=" * 80)
    print("✅ 삽입 완료!")
    print("=" * 80)
    print(f"📊 통계:")
    print(f"   총 삽입 문서: {inserted_count}개")
    print(f"   소요 시간: {elapsed:.1f}초")
    print(f"   평균 속도: {inserted_count/elapsed:.1f} docs/sec")
    print()
    print(f"📋 인덱스 정보:")

    # 인덱스 통계
    try:
        count_result = os_client.count(index=index_name)
        print(f"   인덱스: {index_name}")
        print(f"   총 문서 수: {count_result['count']}개")

        # 레벨별 집계
        agg_result = os_client.search(
            index=index_name,
            body={
                "size": 0,
                "aggs": {
                    "by_level": {
                        "terms": {"field": "level", "size": 10}
                    }
                }
            }
        )

        print(f"   레벨별 분포:")
        for bucket in agg_result["aggregations"]["by_level"]["buckets"]:
            level = bucket["key"]
            count = bucket["doc_count"]
            percentage = count / count_result['count'] * 100
            print(f"     - {level}: {count}개 ({percentage:.1f}%)")
    except Exception as e:
        print(f"   ⚠️ 통계 조회 실패: {e}")

    print()
    print(f"✅ 프로젝트 UUID: {project_uuid}")
    print(f"✅ 인덱스: {index_name}")
    print()


if __name__ == "__main__":
    insert_large_test_data()
