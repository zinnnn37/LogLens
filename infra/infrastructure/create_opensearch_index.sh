#!/bin/bash

# ============================================
# OpenSearch 로그 인덱스 생성 스크립트 (안정 버전)
# ============================================

set -e

OPENSEARCH_HOST="${OPENSEARCH_HOST:-http://localhost:9200}"
INDEX_TEMPLATE_NAME="logs-template"
INDEX_PATTERN="*_20*_*"  # Matches {anything}_{YYYY}_{MM} format

echo "🔍 OpenSearch 연결 확인: $OPENSEARCH_HOST"

# OpenSearch 헬스 체크
until curl -s "$OPENSEARCH_HOST/_cluster/health" > /dev/null; do
    echo "⏳ OpenSearch 대기 중..."
    sleep 5
done

echo "✅ OpenSearch 연결 성공"

# OpenSearch 버전 확인
echo "🔍 OpenSearch 버전 확인..."
OPENSEARCH_VERSION=$(curl -s "$OPENSEARCH_HOST/" | jq -r '.version.number' 2>/dev/null || echo "unknown")
echo "   버전: $OPENSEARCH_VERSION"

# 기존 템플릿 삭제 (선택사항)
echo "🗑️ 기존 인덱스 템플릿 삭제 (존재하는 경우)..."
curl -s -X DELETE "$OPENSEARCH_HOST/_index_template/$INDEX_TEMPLATE_NAME" 2>/dev/null || true
echo ""

# 인덱스 템플릿 생성 (KNN 벡터 제외 버전)
echo "📝 인덱스 템플릿 생성 중..."

curl -X PUT "$OPENSEARCH_HOST/_index_template/$INDEX_TEMPLATE_NAME" \
-H 'Content-Type: application/json' \
-d '{
  "index_patterns": ["'"$INDEX_PATTERN"'"],
  "template": {
    "settings": {
      "index.knn": true,
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "refresh_interval": "5s",
      "index": {
        "max_result_window": 10000
      }
    },
    "mappings": {
      "properties": {
        "log_id": {
          "type": "long"
        },
        "project_uuid": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword"
            }
          }
        },
        "timestamp": {
          "type": "date",
          "format": "strict_date_optional_time||epoch_millis||yyyy-MM-dd HH:mm:ss.SSS"
        },
        "service_name": {
          "type": "keyword"
        },
        "logger": {
          "type": "keyword"
        },
        "source_type": {
          "type": "keyword"
        },
        "layer": {
          "type": "keyword"
        },
        "log_level": {
          "type": "keyword"
        },
        "level": {
          "type": "keyword"
        },
        "message": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "comment": {
          "type": "text"
        },
        "method_name": {
          "type": "keyword"
        },
        "class_name": {
          "type": "keyword"
        },
        "thread_name": {
          "type": "keyword"
        },
        "trace_id": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "requester_ip": {
          "type": "ip"
        },
        "duration": {
          "type": "integer"
        },
        "stack_trace": {
          "type": "text"
        },
        "log_details": {
          "type": "object",
          "properties": {
            "exception_type": {
              "type": "keyword"
            },
            "execution_time": {
              "type": "integer"
            },
            "http_method": {
              "type": "keyword"
            },
            "request_uri": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type": "keyword",
                  "ignore_above": 256
                }
              }
            },
            "request_headers": {
              "type": "object",
              "enabled": false
            },
            "request_body": {
              "type": "object",
              "enabled": false
            },
            "response_status": {
              "type": "integer"
            },
            "response_body": {
              "type": "object",
              "enabled": false
            },
            "class_name": {
              "type": "keyword"
            },
            "method_name": {
              "type": "keyword"
            },
            "stacktrace": {
              "type": "text"
            },
            "additional_info": {
              "type": "object",
              "enabled": false
            }
          }
        },
        "ai_analysis": {
          "type": "object",
          "properties": {
            "summary": {
              "type": "text"
            },
            "error_cause": {
              "type": "text"
            },
            "solution": {
              "type": "text"
            },
            "tags": {
              "type": "keyword"
            },
            "analysis_type": {
              "type": "keyword"
            },
            "target_type": {
              "type": "keyword"
            },
            "analyzed_at": {
              "type": "date"
            }
          }
        },
        "log_vector": {
          "type": "knn_vector",
          "dimension": 1536,
          "method": {
            "name": "hnsw",
            "space_type": "innerproduct",
            "engine": "faiss"
          }
        },
        "indexed_at": {
          "type": "date",
          "format": "strict_date_optional_time||epoch_millis||yyyy-MM-dd HH:mm:ss.SSS"
        },
        "@timestamp": {
          "type": "date",
          "format": "strict_date_optional_time||epoch_millis||yyyy-MM-dd HH:mm:ss.SSS"
        }
      }
    }
  },
  "priority": 200,
  "version": 3
}'

echo ""
RESULT=$?

if [ $RESULT -eq 0 ]; then
    echo "✅ 인덱스 템플릿 생성 완료"
else
    echo "❌ 인덱스 템플릿 생성 실패"
    exit 1
fi

# 인덱스 템플릿 확인
echo ""
echo "🔍 생성된 인덱스 템플릿 확인:"
curl -s "$OPENSEARCH_HOST/_index_template/$INDEX_TEMPLATE_NAME" | jq '.index_templates[0].index_template | {name, index_patterns, priority}' 2>/dev/null || echo "jq 없음 - 전체 출력 생략"

echo ""
echo "🎉 OpenSearch 인덱스 설정 완료!"
