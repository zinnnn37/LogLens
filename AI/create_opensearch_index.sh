#!/bin/bash

# EC2-compatible OpenSearch Index Template Creation Script
# This script creates an index template that matches the EC2 production environment

OPENSEARCH_HOST="${OPENSEARCH_HOST:-localhost}"
OPENSEARCH_PORT="${OPENSEARCH_PORT:-9200}"
OPENSEARCH_URL="http://${OPENSEARCH_HOST}:${OPENSEARCH_PORT}"

echo "Creating OpenSearch index template for logs..."
echo "Target: ${OPENSEARCH_URL}"

# Create index template
curl -X PUT "${OPENSEARCH_URL}/_index_template/logs_template" \
  -H 'Content-Type: application/json' \
  -d '{
  "index_patterns": ["*_20*_*"],
  "priority": 100,
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "5s",
      "index": {
        "knn": true,
        "knn.algo_param.ef_search": 512
      }
    },
    "mappings": {
      "properties": {
        "log_id": {
          "type": "keyword"
        },
        "trace_id": {
          "type": "keyword"
        },
        "timestamp": {
          "type": "date",
          "format": "strict_date_optional_time||epoch_millis"
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
        "service_name": {
          "type": "keyword"
        },
        "requester_ip": {
          "type": "ip"
        },
        "duration": {
          "type": "integer"
        },
        "log_details": {
          "type": "object",
          "properties": {
            "http_method": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type": "keyword",
                  "ignore_above": 256
                }
              }
            },
            "http_status": {
              "type": "integer"
            },
            "endpoint": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type": "keyword",
                  "ignore_above": 256
                }
              }
            },
            "execution_time": {
              "type": "integer"
            },
            "exception_type": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type": "keyword",
                  "ignore_above": 256
                }
              }
            },
            "stack_trace": {
              "type": "text"
            }
          }
        },
        "embedding": {
          "type": "knn_vector",
          "dimension": 1536,
          "method": {
            "name": "hnsw",
            "space_type": "cosinesimil",
            "engine": "nmslib",
            "parameters": {
              "ef_construction": 512,
              "m": 16
            }
          }
        }
      }
    }
  }
}'

echo ""
echo "Index template created successfully!"
echo ""
echo "To verify the template, run:"
echo "curl -X GET '${OPENSEARCH_URL}/_index_template/logs_template' | jq"
