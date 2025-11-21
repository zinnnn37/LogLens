#!/bin/bash

# EC2 실제 템플릿 (requester_ip 포함)

OPENSEARCH_HOST="${OPENSEARCH_HOST:-localhost}"
OPENSEARCH_PORT="${OPENSEARCH_PORT:-9200}"
OPENSEARCH_URL="http://${OPENSEARCH_HOST}:${OPENSEARCH_PORT}"

echo "Creating EC2 template with requester_ip..."
echo "Target: ${OPENSEARCH_URL}"

curl -X PUT "${OPENSEARCH_URL}/_index_template/logs_template" \
  -H 'Content-Type: application/json' \
  -d '{
  "index_patterns": ["*_20*_*"],
  "priority": 200,
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
        "stacktrace": {
          "type": "text"
        },
        "component_name": {
          "type": "keyword"
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
              "type": "keyword"
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
  }
}'

echo ""
echo "✓ Template created with requester_ip field!"
