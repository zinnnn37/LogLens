import os
from datetime import datetime, timedelta
import random
import uuid
from opensearchpy import OpenSearch

# Sample data for realistic logs
SERVICES = ['user-service', 'auth-service', 'payment-service', 'order-service', 'product-service']
HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']
ENDPOINTS = [
    '/api/v1/users', '/api/v1/auth/login', '/api/v1/payments',
    '/api/v1/orders', '/api/v1/products', '/api/v1/cart',
    '/api/v1/users/{id}', '/api/v1/orders/{id}', '/api/v1/products/{id}'
]
LOG_LEVELS = ['INFO', 'WARN', 'ERROR', 'DEBUG']
SOURCE_TYPES = ['FE', 'BE', 'INFRA']
LAYERS = ['Controller', 'Service', 'Repository', 'Util', 'Config']
LOGGERS = ['com.example.user', 'com.example.auth', 'com.example.payment', 'com.example.order', 'com.example.product']
COMPONENTS = ['UserComponent', 'AuthComponent', 'PaymentComponent', 'OrderComponent', 'ProductComponent']
THREAD_NAMES = ['http-nio-8080-exec-1', 'http-nio-8080-exec-2', 'http-nio-8080-exec-3', 'scheduled-task-1', 'async-task-1']
EXCEPTION_TYPES = [
    'NullPointerException', 'IllegalArgumentException',
    'SQLException', 'TimeoutException', 'ValidationException'
]
MESSAGES = [
    'Request processed successfully',
    'User authentication successful',
    'Database query executed',
    'Payment transaction completed',
    'Order created successfully',
    'Product inventory updated',
    'Cache miss - fetching from database',
    'API rate limit check passed',
    'Invalid request parameter',
    'Database connection timeout',
    'Authentication failed',
    'Payment gateway error',
    'Null pointer exception in service layer'
]

def generate_random_ip():
    """Generate random IP address"""
    return f"192.168.{random.randint(1, 255)}.{random.randint(1, 255)}"

def generate_log_entry(index: int, base_time: datetime, project_uuid: str):
    """Generate a single log entry"""
    # Randomize timestamp slightly
    timestamp = base_time - timedelta(minutes=random.randint(0, 1440))  # Within last 24 hours
    indexed_at = base_time

    # Random log level with realistic distribution
    level_weights = {'INFO': 0.6, 'WARN': 0.2, 'ERROR': 0.15, 'DEBUG': 0.05}
    level = random.choices(list(level_weights.keys()), weights=list(level_weights.values()))[0]

    # Service and endpoint
    service_name = random.choice(SERVICES)
    http_method = random.choice(HTTP_METHODS)
    endpoint = random.choice(ENDPOINTS)

    # Additional metadata
    logger = random.choice(LOGGERS)
    source_type = random.choice(SOURCE_TYPES)
    layer = random.choice(LAYERS)
    component_name = random.choice(COMPONENTS)
    thread_name = random.choice(THREAD_NAMES)
    class_name = f"com.example.{service_name.replace('-', '.')}.{layer}"
    method_name = f"handle{http_method.capitalize()}Request"

    # Execution time and status based on level
    if level == 'ERROR':
        http_status = random.choice([500, 503, 504, 400, 401, 403, 404])
        execution_time = random.randint(2000, 10000)  # Slower for errors
        message = random.choice([m for m in MESSAGES if 'error' in m.lower() or 'failed' in m.lower() or 'exception' in m.lower()])
        exception_type = random.choice(EXCEPTION_TYPES)
        stack_trace = f"at com.example.{service_name.replace('-', '.')}.Controller.handleRequest(Controller.java:{random.randint(50, 200)})\n" + \
                     f"at com.example.framework.DispatcherServlet.doDispatch(DispatcherServlet.java:{random.randint(800, 1000)})"
    elif level == 'WARN':
        http_status = random.choice([200, 201, 400, 404])
        execution_time = random.randint(500, 3000)
        message = random.choice([m for m in MESSAGES if 'timeout' in m.lower() or 'invalid' in m.lower() or 'miss' in m.lower()])
        exception_type = None
        stack_trace = None
    else:  # INFO or DEBUG
        http_status = random.choice([200, 201, 204])
        execution_time = random.randint(50, 1000)
        message = random.choice([m for m in MESSAGES if 'success' in m.lower() or 'completed' in m.lower() or 'executed' in m.lower()])
        exception_type = None
        stack_trace = None

    # Generate trace_id (sometimes shared for related logs)
    if random.random() < 0.3:  # 30% chance to reuse recent trace_id
        trace_id = f"trace-{random.randint(max(0, index-10), max(1, index))}"
    else:
        trace_id = f"trace-{index}"

    log_entry = {
        'log_id': index + 1,  # long 타입: 1부터 시작하는 숫자
        'project_uuid': project_uuid,
        'trace_id': trace_id,
        'timestamp': timestamp.isoformat(),
        'level': level,
        'log_level': level,  # duplicate for compatibility
        'message': message,
        'service_name': service_name,
        'logger': logger,
        'source_type': source_type,
        'layer': layer,
        'method_name': method_name,
        'class_name': class_name,
        'thread_name': thread_name,
        'component_name': component_name,
        'requester_ip': generate_random_ip(),  # EC2: requester_ip (ip type)
        'duration': execution_time,
        'indexed_at': indexed_at.isoformat(),
        '@timestamp': timestamp.isoformat(),
        'log_details': {
            'http_method': http_method,
            'response_status': http_status,  # EC2: response_status
            'request_uri': endpoint,  # EC2: request_uri
            'execution_time': execution_time,
            'class_name': class_name,
            'method_name': method_name
        }
    }

    # Add exception/stacktrace details for ERROR logs
    if exception_type:
        log_entry['log_details']['exception_type'] = exception_type
        log_entry['log_details']['stacktrace'] = stack_trace
        log_entry['stacktrace'] = stack_trace  # Top-level stacktrace
        log_entry['comment'] = f"Error occurred in {class_name}.{method_name}: {exception_type}"
    else:
        log_entry['comment'] = f"Normal operation in {class_name}.{method_name}"

    return log_entry

def main():
    """Generate and insert 100 test logs"""
    print("Connecting to OpenSearch...")

    # Connect to OpenSearch
    client = OpenSearch(
        hosts=[{'host': 'localhost', 'port': 9200}],
        http_compress=True,
        use_ssl=False,
        verify_certs=False,
        ssl_assert_hostname=False,
        ssl_show_warn=False
    )

    print("Generating 100 test logs...")
    base_time = datetime.now()
    project_uuid = '9f8c4c75-a936-3ab6-92a5-d1309cd9f87e'

    # Create index name based on current month
    index_name = f"{project_uuid.replace('-', '_')}_{base_time.strftime('%Y_%m')}"

    logs = []
    for i in range(100):
        log = generate_log_entry(i, base_time, project_uuid)
        logs.append(log)
        if (i + 1) % 20 == 0:
            print(f"  Generated {i + 1}/100 logs...")

    print(f"\nInserting logs into index: {index_name}")

    success_count = 0
    for i, log in enumerate(logs):
        try:
            client.index(
                index=index_name,
                body=log,
                refresh=True
            )
            success_count += 1
            if (i + 1) % 20 == 0:
                print(f"  Inserted {i + 1}/100 logs...")
        except Exception as e:
            print(f"  Error inserting log {i + 1}: {e}")

    print(f"\n✓ Successfully inserted {success_count}/100 logs into {index_name}")

    # Verify insertion
    print("\nVerifying insertion...")
    count_result = client.count(index=index_name)
    print(f"Total documents in index: {count_result['count']}")

    print(f"\nTo view logs, run:")
    print(f"curl -X GET 'http://localhost:9200/{index_name}/_search?size=10&pretty' 2>/dev/null")

if __name__ == '__main__':
    main()
