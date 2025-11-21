"""
EC2 평면 구조 형식의 샘플 데이터 삽입
사용자가 제공한 document 형식을 모방
"""
import requests
from datetime import datetime, timedelta
import random

# OpenSearch 설정
OPENSEARCH_URL = "http://localhost:9200"
PROJECT_UUID = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e"
INDEX_NAME = f"{PROJECT_UUID.replace('-', '_')}_2025_11"

# EC2 형식의 샘플 데이터 (평면 구조 + execution_time 필드)
sample_logs = [
    {
        "log_id": 1001,
        "project_uuid": PROJECT_UUID,
        "timestamp": (datetime.utcnow() - timedelta(hours=1)).isoformat() + "Z",
        "service_name": "Loglens",
        "level": "INFO",
        "source_type": "BE",
        "class_name": "CorsFilter",
        "method_name": "doFilter",
        "logger": "com.example.demo.global.filter.CorsFilter",
        "thread_name": "http-nio-8081-exec-2",
        "requester_ip": "0:0:0:0:0:0:0:1",
        "layer": "Other",
        "execution_time": 9,  # 평면 구조의 execution_time
        "message": "Response completed: doFilter",
        "comment": "thread: http-nio-8081-exec-2, app: demo, pid: 10768"
    },
    {
        "log_id": 1002,
        "project_uuid": PROJECT_UUID,
        "timestamp": (datetime.utcnow() - timedelta(hours=2)).isoformat() + "Z",
        "service_name": "UserService",
        "level": "INFO",
        "source_type": "BE",
        "class_name": "UserController",
        "method_name": "createUser",
        "logger": "com.example.demo.user.UserController",
        "thread_name": "http-nio-8081-exec-5",
        "requester_ip": "192.168.1.100",
        "layer": "Controller",
        "execution_time": 145,
        "message": "User created successfully",
        "comment": "thread: http-nio-8081-exec-5, app: demo, pid: 10768"
    },
    {
        "log_id": 1003,
        "project_uuid": PROJECT_UUID,
        "timestamp": (datetime.utcnow() - timedelta(hours=3)).isoformat() + "Z",
        "service_name": "PaymentService",
        "level": "INFO",
        "source_type": "BE",
        "class_name": "PaymentController",
        "method_name": "processPayment",
        "logger": "com.example.demo.payment.PaymentController",
        "thread_name": "http-nio-8081-exec-10",
        "requester_ip": "192.168.1.101",
        "layer": "Controller",
        "execution_time": 3250,  # 느린 API
        "message": "Payment processed",
        "comment": "thread: http-nio-8081-exec-10, app: demo, pid: 10768"
    },
    {
        "log_id": 1004,
        "project_uuid": PROJECT_UUID,
        "timestamp": (datetime.utcnow() - timedelta(hours=4)).isoformat() + "Z",
        "service_name": "OrderService",
        "level": "INFO",
        "source_type": "BE",
        "class_name": "OrderController",
        "method_name": "createOrder",
        "logger": "com.example.demo.order.OrderController",
        "thread_name": "http-nio-8081-exec-3",
        "requester_ip": "192.168.1.102",
        "layer": "Controller",
        "execution_time": 2100,  # 느린 API
        "message": "Order created",
        "comment": "thread: http-nio-8081-exec-3, app: demo, pid: 10768"
    },
    {
        "log_id": 1005,
        "project_uuid": PROJECT_UUID,
        "timestamp": (datetime.utcnow() - timedelta(hours=5)).isoformat() + "Z",
        "service_name": "UserService",
        "level": "INFO",
        "source_type": "BE",
        "class_name": "UserController",
        "method_name": "getUser",
        "logger": "com.example.demo.user.UserController",
        "thread_name": "http-nio-8081-exec-7",
        "requester_ip": "192.168.1.103",
        "layer": "Controller",
        "execution_time": 25,
        "message": "User retrieved",
        "comment": "thread: http-nio-8081-exec-7, app: demo, pid: 10768"
    },
]

def insert_documents():
    """샘플 데이터 삽입"""
    print(f"🔄 EC2 평면 구조 샘플 데이터 삽입 시작...\n")
    print(f"인덱스: {INDEX_NAME}")
    print(f"문서 수: {len(sample_logs)}\n")

    for log in sample_logs:
        doc_id = log["log_id"]
        url = f"{OPENSEARCH_URL}/{INDEX_NAME}/_doc/{doc_id}"

        response = requests.put(url, json=log, headers={"Content-Type": "application/json"})

        if response.status_code in [200, 201]:
            print(f"✅ log_id {doc_id} 삽입 성공: {log['class_name']}.{log['method_name']} ({log['execution_time']}ms)")
        else:
            print(f"❌ log_id {doc_id} 삽입 실패: {response.text}")

    # 인덱스 리프레시 (즉시 검색 가능하도록)
    refresh_url = f"{OPENSEARCH_URL}/{INDEX_NAME}/_refresh"
    requests.post(refresh_url)

    print(f"\n✅ 총 {len(sample_logs)}건 삽입 완료 (인덱스 리프레시됨)")
    print("\n📊 예상 결과:")
    print("1. get_slowest_apis: PaymentController.processPayment (3250ms) 가장 느림")
    print("2. 두 번째: OrderController.createOrder (2100ms)")
    print("3. 세 번째: UserController.createUser (145ms)")

if __name__ == "__main__":
    insert_documents()
