#!/bin/bash

# ============================================
# 11월 인덱스 Reindex 스크립트 (KNN 벡터 마이그레이션)
# ============================================

set -e

OPENSEARCH_HOST="${OPENSEARCH_HOST:-http://localhost:9200}"

echo "🔍 OpenSearch 연결 확인: $OPENSEARCH_HOST"

# OpenSearch 헬스 체크
until curl -s "$OPENSEARCH_HOST/_cluster/health" > /dev/null; do
    echo "⏳ OpenSearch 대기 중..."
    sleep 5
done

echo "✅ OpenSearch 연결 성공"
echo ""

# 11월 인덱스 목록 조회
echo "🔍 11월 인덱스 검색 중..."
INDICES=$(curl -s "$OPENSEARCH_HOST/_cat/indices/*_2025_11?h=index" | grep -v "^\." | sort)

if [ -z "$INDICES" ]; then
    echo "❌ 11월 인덱스를 찾을 수 없습니다."
    exit 0
fi

# 인덱스 개수
INDEX_COUNT=$(echo "$INDICES" | wc -l)
echo "   찾은 인덱스: ${INDEX_COUNT}개"
echo ""

# 각 인덱스 정보 표시
echo "📊 인덱스 목록:"
for index in $INDICES; do
    STATS=$(curl -s "$OPENSEARCH_HOST/_cat/indices/$index?h=store.size,docs.count")
    echo "   - $index ($STATS)"
done
echo ""

# 사용자 확인
read -p "⚠️  위 인덱스들을 Reindex 하시겠습니까? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ 취소되었습니다."
    exit 0
fi
echo ""

# KNN 매핑 정의 (create_opensearch_index.sh와 동일)
MAPPING=$(cat <<'EOF'
{
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
      "log_id": {"type": "long"},
      "project_uuid": {
        "type": "text",
        "fields": {"keyword": {"type": "keyword"}}
      },
      "timestamp": {
        "type": "date",
        "format": "strict_date_optional_time||epoch_millis"
      },
      "service_name": {"type": "keyword"},
      "logger": {"type": "keyword"},
      "source_type": {"type": "keyword"},
      "layer": {"type": "keyword"},
      "log_level": {"type": "keyword"},
      "level": {"type": "keyword"},
      "message": {
        "type": "text",
        "fields": {"keyword": {"type": "keyword", "ignore_above": 256}}
      },
      "comment": {"type": "text"},
      "method_name": {"type": "keyword"},
      "class_name": {"type": "keyword"},
      "thread_name": {"type": "keyword"},
      "trace_id": {"type": "keyword"},
      "requester_ip": {"type": "ip"},
      "duration": {"type": "integer"},
      "stack_trace": {"type": "text"},
      "log_details": {
        "type": "object",
        "properties": {
          "exception_type": {"type": "keyword"},
          "execution_time": {"type": "integer"},
          "http_method": {"type": "keyword"},
          "request_uri": {"type": "keyword"},
          "request_headers": {"type": "object", "enabled": false},
          "request_body": {"type": "text"},
          "response_status": {"type": "integer"},
          "response_body": {"type": "text"},
          "class_name": {"type": "keyword"},
          "method_name": {"type": "keyword"},
          "additional_info": {"type": "object", "enabled": false}
        }
      },
      "ai_analysis": {
        "type": "object",
        "properties": {
          "summary": {"type": "text"},
          "error_cause": {"type": "text"},
          "solution": {"type": "text"},
          "tags": {"type": "keyword"},
          "analysis_type": {"type": "keyword"},
          "target_type": {"type": "keyword"},
          "analyzed_at": {"type": "date"}
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
      "indexed_at": {"type": "date"},
      "@timestamp": {"type": "date"}
    }
  }
}
EOF
)

# 각 인덱스 처리
CURRENT=0
SUCCEEDED=0
FAILED=0

for index in $INDICES; do
    CURRENT=$((CURRENT + 1))
    echo "=========================================="
    echo "📝 인덱스 $CURRENT/$INDEX_COUNT: $index"
    echo "=========================================="

    NEW_INDEX="${index}_v2"

    # 1. 새 인덱스 이미 존재하는지 확인
    if curl -s -o /dev/null -w "%{http_code}" "$OPENSEARCH_HOST/$NEW_INDEX" | grep -q "200"; then
        echo "⚠️  $NEW_INDEX 이미 존재합니다. 건너뜁니다."
        echo ""
        continue
    fi

    # 2. 새 인덱스 생성
    echo "   🔧 새 인덱스 생성 중..."
    CREATE_RESULT=$(curl -s -X PUT "$OPENSEARCH_HOST/$NEW_INDEX" \
        -H 'Content-Type: application/json' \
        -d "$MAPPING")

    if echo "$CREATE_RESULT" | grep -q '"acknowledged":true'; then
        echo "   ✅ 새 인덱스 생성 완료"
    else
        echo "   ❌ 새 인덱스 생성 실패: $CREATE_RESULT"
        FAILED=$((FAILED + 1))
        echo ""
        continue
    fi

    # 3. 원본 문서 개수 확인
    OLD_COUNT=$(curl -s "$OPENSEARCH_HOST/$index/_count" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
    echo "   📊 원본 문서 개수: $OLD_COUNT"

    # 4. Reindex 실행
    echo "   🔄 데이터 복사 중 (시간이 걸릴 수 있습니다)..."
    REINDEX_RESULT=$(curl -s -X POST "$OPENSEARCH_HOST/_reindex?wait_for_completion=true" \
        -H 'Content-Type: application/json' \
        -d "{
            \"source\": {\"index\": \"$index\"},
            \"dest\": {\"index\": \"$NEW_INDEX\"},
            \"conflicts\": \"proceed\"
        }")

    # Reindex 결과 디버깅 출력
    echo "   🔍 Reindex 결과: $REINDEX_RESULT"

    # Reindex 결과 확인 (created 필드 체크)
    CREATED=$(echo "$REINDEX_RESULT" | grep -o '"created":[0-9]*' | grep -o '[0-9]*')

    if [ ! -z "$CREATED" ] && [ "$CREATED" -gt 0 ]; then
        echo "   ✅ Reindex 완료 (생성: $CREATED)"
    else
        echo "   ❌ Reindex 실패 - 생성된 문서 없음"
        echo "   상세: $REINDEX_RESULT"
        FAILED=$((FAILED + 1))
        # 실패한 인덱스 삭제
        curl -s -X DELETE "$OPENSEARCH_HOST/$NEW_INDEX" > /dev/null
        echo ""
        continue
    fi

    # 5. 인덱스 Refresh (검색 가능하게 만들기)
    echo "   🔄 인덱스 Refresh 중..."
    curl -s -X POST "$OPENSEARCH_HOST/$NEW_INDEX/_refresh" > /dev/null

    # 6. 새 인덱스 문서 개수 확인
    NEW_COUNT=$(curl -s "$OPENSEARCH_HOST/$NEW_INDEX/_count" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
    echo "   📊 새 인덱스 문서 개수: $NEW_COUNT"

    if [ "$OLD_COUNT" != "$NEW_COUNT" ]; then
        echo "   ❌ 문서 개수 불일치! ($OLD_COUNT vs $NEW_COUNT)"
        FAILED=$((FAILED + 1))
        echo ""
        continue
    fi

    echo "   ✅ 문서 개수 검증 완료"

    # 7. Replica 활성화
    echo "   🔧 Replica 활성화 중..."
    curl -s -X PUT "$OPENSEARCH_HOST/$NEW_INDEX/_settings" \
        -H 'Content-Type: application/json' \
        -d '{"index": {"number_of_replicas": 0}}' > /dev/null

    # 8. Alias 전환 (무중단 전환)
    echo "   🔀 Alias 전환 중..."
    ALIAS_RESULT=$(curl -s -X POST "$OPENSEARCH_HOST/_aliases" \
        -H 'Content-Type: application/json' \
        -d "{
            \"actions\": [
                {\"add\": {\"index\": \"$NEW_INDEX\", \"alias\": \"$index\"}},
                {\"remove_index\": {\"index\": \"$index\"}}
            ]
        }")

    if echo "$ALIAS_RESULT" | grep -q '"acknowledged":true'; then
        echo "   ✅ Alias 전환 및 기존 인덱스 삭제 완료"
        SUCCEEDED=$((SUCCEEDED + 1))
    else
        echo "   ❌ Alias 전환 실패: $ALIAS_RESULT"
        FAILED=$((FAILED + 1))
    fi

    echo ""
done

# 최종 결과
echo "=========================================="
echo "🎉 마이그레이션 완료!"
echo "=========================================="
echo "   총 인덱스: $INDEX_COUNT"
echo "   성공: $SUCCEEDED"
echo "   실패: $FAILED"
echo ""

if [ $FAILED -eq 0 ]; then
    echo "✅ 모든 인덱스가 성공적으로 마이그레이션되었습니다!"
    echo ""
    echo "📋 다음 단계:"
    echo "   1. KNN 벡터 검색 테스트"
    echo "   2. AI 분석 API 테스트"
    echo "   3. 유사도 캐싱 동작 확인"
    exit 0
else
    echo "⚠️  일부 인덱스 마이그레이션이 실패했습니다."
    echo "   실패한 인덱스를 수동으로 확인해주세요."
    exit 1
fi
