#!/usr/bin/env python3
"""
테스트 로그 데이터를 OpenSearch에 삽입하는 스크립트
KNN 벡터 필드 포함
"""

import os
import sys
from datetime import datetime
from opensearchpy import OpenSearch
from openai import OpenAI

# 환경 변수 로드
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

# 현재 시간 기준으로 테스트 로그 생성
def generate_recent_timestamp(hours_ago: int) -> str:
    """현재 UTC 시간에서 N시간 전의 timestamp 생성"""
    from datetime import datetime, timedelta
    timestamp = datetime.utcnow() - timedelta(hours=hours_ago)
    return timestamp.isoformat() + "Z"

# 테스트 로그 데이터 (다양한 서비스 및 시간대)
SAMPLE_LOGS = [
    # ERROR 로그들 (최근 24시간)
    {
        "method_name": "UserController.createUser",
        "level": "ERROR",
        "message": "NullPointerException: User object is null at line 45",
        "timestamp": generate_recent_timestamp(0.5),  # 30분 전
        "service_name": "user-service",
        "source_type": "BE",
        "log_id": 101,
    },
    {
        "method_name": "PaymentService.processPayment",
        "level": "ERROR",
        "message": "DatabaseTimeout: Connection pool exhausted after 30s",
        "timestamp": generate_recent_timestamp(3),  # 3시간 전
        "service_name": "payment-service",
        "source_type": "BE",
        "log_id": 102,
    },
    {
        "method_name": "AuthController.validateToken",
        "level": "ERROR",
        "message": "InvalidTokenException: JWT signature verification failed",
        "timestamp": generate_recent_timestamp(12),  # 12시간 전
        "service_name": "auth-service",
        "source_type": "BE",
        "log_id": 103,
    },
    # WARN 로그들
    {
        "method_name": "DatabaseConnection.executeQuery",
        "level": "WARN",
        "message": "Slow query detected: execution time 3.5s",
        "timestamp": generate_recent_timestamp(6),  # 6시간 전
        "service_name": "user-service",
        "source_type": "BE",
        "log_id": 104,
    },
    {
        "method_name": "CacheManager.get",
        "level": "WARN",
        "message": "Cache miss for key: user:12345",
        "timestamp": generate_recent_timestamp(2),  # 2시간 전
        "service_name": "cache-service",
        "source_type": "BE",
        "log_id": 105,
    },
    # INFO 로그들
    {
        "method_name": "Workflow.processUserWorkflow",
        "level": "INFO",
        "message": "Workflow.processUserWorkflow completed successfully",
        "timestamp": generate_recent_timestamp(1),  # 1시간 전
        "service_name": "Loglens",
        "source_type": "FE",
        "log_id": 106,
    },
    {
        "method_name": "ApiController.healthCheck",
        "level": "INFO",
        "message": "Health check passed: all services operational",
        "timestamp": generate_recent_timestamp(0.25),  # 15분 전
        "service_name": "gateway",
        "source_type": "BE",
        "log_id": 107,
    },
    # 7일 전 로그 (시간 범위 테스트용)
    {
        "method_name": "BatchJob.processDailyReports",
        "level": "ERROR",
        "message": "OutOfMemoryError: Java heap space exceeded",
        "timestamp": generate_recent_timestamp(168),  # 7일 전
        "service_name": "batch-service",
        "source_type": "BE",
        "log_id": 108,
    },
]

PROJECT_UUID = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e"
INDEX_NAME = "9f8c4c75_a936_3ab6_92a5_d1309cd9f87e_2025_11"


def generate_embedding(text: str) -> list:
    """OpenAI API로 텍스트 embedding 생성 (1536 차원으로 축소)"""
    try:
        print(f"   🔄 Embedding 생성 중... (텍스트 길이: {len(text)})")
        response = openai_client.embeddings.create(
            model="text-embedding-3-large",
            input=text,
            dimensions=1536,  # 1536 차원으로 축소
        )
        embedding = response.data[0].embedding
        print(f"   ✅ Embedding 생성 완료 (dim: {len(embedding)})")
        return embedding
    except Exception as e:
        print(f"❌ Embedding 생성 실패: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


def insert_logs():
    """로그 데이터를 OpenSearch에 삽입"""
    print(f"📝 {len(SAMPLE_LOGS)}개의 로그 삽입 시작...")
    print(f"   인덱스: {INDEX_NAME}")
    print(f"   프로젝트: {PROJECT_UUID}")
    print(f"   현재 UTC 시간: {datetime.utcnow().isoformat()}Z")
    print()

    for i, log in enumerate(SAMPLE_LOGS, 1):
        print(f"[{i}/{len(SAMPLE_LOGS)}] {log['method_name'][:40]}... ({log['level']})")

        # Embedding 생성
        embedding_text = f"{log['method_name']} {log['message']}"
        log_vector = generate_embedding(embedding_text)

        # 문서 생성
        doc = {
            "log_id": log["log_id"],
            "project_uuid": PROJECT_UUID,
            "service_name": log["service_name"],
            "log_level": log["level"],
            "level": log["level"],
            "source_type": log["source_type"],
            "method_name": log["method_name"],
            "class_name": log["method_name"].split(".")[0] if "." in log["method_name"] else "",
            "message": log["message"],
            "timestamp": log["timestamp"],
            "log_vector": log_vector,
            "indexed_at": datetime.utcnow().isoformat() + "Z",
        }

        # OpenSearch에 삽입
        try:
            os_client.index(
                index=INDEX_NAME,
                body=doc,
                id=log["log_id"],
            )
            print(f"   ✅ 삽입 완료 (log_id: {log['log_id']})")
        except Exception as e:
            print(f"   ❌ 삽입 실패: {e}")
            continue

    # Refresh
    print()
    print("🔄 인덱스 새로고침...")
    os_client.indices.refresh(index=INDEX_NAME)

    # 확인
    print("✅ 삽입 완료!")
    print()
    print(f"📊 인덱스 정보:")
    stats = os_client.count(index=INDEX_NAME)
    print(f"   총 문서 수: {stats['count']}")

    # 샘플 조회
    print()
    print("📋 샘플 로그 조회:")
    result = os_client.search(
        index=INDEX_NAME,
        body={"query": {"match_all": {}}, "size": 1}
    )
    if result["hits"]["hits"]:
        hit = result["hits"]["hits"][0]["_source"]
        print(f"   log_id: {hit['log_id']}")
        print(f"   message: {hit['message'][:50]}...")
        print(f"   log_vector: [{hit['log_vector'][0]:.4f}, ..., {hit['log_vector'][-1]:.4f}] (dim: {len(hit['log_vector'])})")


if __name__ == "__main__":
    print("🚀 OpenSearch 테스트 로그 삽입 스크립트")
    print()

    # 환경 변수 확인
    if not OPENAI_API_KEY:
        print("❌ OPENAI_API_KEY 환경 변수가 설정되지 않았습니다")
        sys.exit(1)

    # OpenSearch 연결 확인
    try:
        info = os_client.info()
        print(f"✅ OpenSearch 연결 성공: {info['version']['number']}")
        print()
    except Exception as e:
        print(f"❌ OpenSearch 연결 실패: {e}")
        sys.exit(1)

    # 삽입 실행
    insert_logs()
    print()
    print("🎉 완료!")
