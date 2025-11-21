#!/bin/bash

# ============================================
# OpenSearch 인덱스 안전 마이그레이션 스크립트
# log_vector 필드를 knn_vector로 재매핑
# ============================================

set -e

OPENSEARCH_HOST="${OPENSEARCH_HOST:-http://localhost:9200}"
SOURCE_INDEX="$1"

if [ -z "$SOURCE_INDEX" ]; then
    echo "사용법: $0 <source_index_name>"
    echo "예시: $0 a0b4a1a9_d2ae_3672_a0e1_3a4863922226_2025_11"
    exit 1
fi

DEST_INDEX="${SOURCE_INDEX}_new"
BACKUP_INDEX="${SOURCE_INDEX}_backup_$(date +%Y%m%d_%H%M%S)"

echo "=========================================="
echo "OpenSearch 인덱스 안전 마이그레이션"
echo "=========================================="
echo "Source: $SOURCE_INDEX"
echo "Dest:   $DEST_INDEX"
echo "Backup: $BACKUP_INDEX"
echo ""

# 1. Source 인덱스 존재 확인
echo "1️⃣  Source 인덱스 확인"
if ! curl -s -f "$OPENSEARCH_HOST/$SOURCE_INDEX" > /dev/null 2>&1; then
    echo "❌ Source 인덱스 '$SOURCE_INDEX'가 존재하지 않습니다!"
    exit 1
fi
echo "✅ Source 인덱스 존재 확인"

# 문서 개수 확인
DOC_COUNT=$(curl -s "$OPENSEARCH_HOST/$SOURCE_INDEX/_count" | jq -r '.count')
echo "   문서 개수: $DOC_COUNT"
echo ""

# 2. logs-template 확인
echo "2️⃣  logs-template 확인"
if ! curl -s -f "$OPENSEARCH_HOST/_index_template/logs-template" > /dev/null 2>&1; then
    echo "❌ logs-template이 없습니다!"
    echo "   먼저 create_opensearch_index.sh를 실행하세요."
    exit 1
fi
echo "✅ logs-template 존재 확인"
echo ""

# 3. Dest 인덱스 생성 (템플릿 자동 적용됨)
echo "3️⃣  Dest 인덱스 생성 (템플릿 적용)"
if curl -s -f "$OPENSEARCH_HOST/$DEST_INDEX" > /dev/null 2>&1; then
    echo "⚠️  Dest 인덱스가 이미 존재합니다. 삭제하고 다시 생성합니다."
    curl -s -X DELETE "$OPENSEARCH_HOST/$DEST_INDEX"
    echo ""
fi

# 빈 인덱스 생성 (템플릿이 자동으로 매핑 적용)
curl -s -X PUT "$OPENSEARCH_HOST/$DEST_INDEX"
echo "✅ Dest 인덱스 생성 완료 (템플릿 자동 적용)"

# log_vector 필드 확인
LOG_VECTOR_TYPE=$(curl -s "$OPENSEARCH_HOST/$DEST_INDEX/_mapping" | jq -r ".\"$DEST_INDEX\".mappings.properties.log_vector.type" 2>/dev/null)
if [ "$LOG_VECTOR_TYPE" == "knn_vector" ]; then
    echo "✅ log_vector: knn_vector 확인됨"
else
    echo "❌ log_vector 타입이 knn_vector가 아닙니다: $LOG_VECTOR_TYPE"
    echo "   템플릿 설정을 확인하세요."
    exit 1
fi
echo ""

# 4. Reindex 실행
echo "4️⃣  Reindex 시작"
echo "   Source: $SOURCE_INDEX → Dest: $DEST_INDEX"
echo "   문서 수: $DOC_COUNT (예상 시간: ~$(($DOC_COUNT / 1000)) 초)"
echo ""

# Reindex 작업 실행
REINDEX_RESPONSE=$(curl -s -X POST "$OPENSEARCH_HOST/_reindex?wait_for_completion=false" \
-H 'Content-Type: application/json' \
-d '{
  "source": {
    "index": "'"$SOURCE_INDEX"'"
  },
  "dest": {
    "index": "'"$DEST_INDEX"'"
  }
}')

TASK_ID=$(echo "$REINDEX_RESPONSE" | jq -r '.task')

if [ "$TASK_ID" == "null" ] || [ -z "$TASK_ID" ]; then
    echo "❌ Reindex 작업 시작 실패"
    echo "$REINDEX_RESPONSE" | jq .
    exit 1
fi

echo "✅ Reindex 작업 시작됨"
echo "   Task ID: $TASK_ID"
echo ""

# 5. Reindex 진행 상황 모니터링
echo "5️⃣  Reindex 진행 상황 모니터링"
while true; do
    TASK_STATUS=$(curl -s "$OPENSEARCH_HOST/_tasks/$TASK_ID")
    COMPLETED=$(echo "$TASK_STATUS" | jq -r '.completed')

    if [ "$COMPLETED" == "true" ]; then
        echo "✅ Reindex 완료!"

        # 결과 확인
        CREATED=$(echo "$TASK_STATUS" | jq -r '.response.created')
        UPDATED=$(echo "$TASK_STATUS" | jq -r '.response.updated')
        FAILURES=$(echo "$TASK_STATUS" | jq -r '.response.failures | length')

        echo "   생성: $CREATED"
        echo "   업데이트: $UPDATED"
        echo "   실패: $FAILURES"

        if [ "$FAILURES" != "0" ] && [ "$FAILURES" != "null" ]; then
            echo "⚠️  일부 문서 마이그레이션 실패:"
            echo "$TASK_STATUS" | jq '.response.failures'
        fi
        break
    fi

    # 진행률 표시
    TOTAL=$(echo "$TASK_STATUS" | jq -r '.task.status.total // 0')
    CREATED_SO_FAR=$(echo "$TASK_STATUS" | jq -r '.task.status.created // 0')
    if [ "$TOTAL" -gt 0 ]; then
        PROGRESS=$((CREATED_SO_FAR * 100 / TOTAL))
        echo "   진행률: $PROGRESS% ($CREATED_SO_FAR / $TOTAL)"
    else
        echo "   준비 중..."
    fi

    sleep 5
done
echo ""

# 6. 문서 개수 검증
echo "6️⃣  마이그레이션 검증"
DEST_DOC_COUNT=$(curl -s "$OPENSEARCH_HOST/$DEST_INDEX/_count" | jq -r '.count')
echo "   Source 문서 수: $DOC_COUNT"
echo "   Dest 문서 수:   $DEST_DOC_COUNT"

if [ "$DOC_COUNT" -eq "$DEST_DOC_COUNT" ]; then
    echo "✅ 문서 개수 일치"
else
    echo "⚠️  문서 개수 불일치! 확인이 필요합니다."
fi
echo ""

# 7. Source 인덱스 백업 및 삭제, Dest 인덱스 rename
echo "7️⃣  인덱스 교체 (주의: 서비스 중단 가능)"
echo ""
read -p "계속 진행하시겠습니까? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "❌ 인덱스 교체를 취소했습니다."
    echo "   $DEST_INDEX는 유지됩니다."
    echo "   수동으로 확인 후 다음 명령어로 교체하세요:"
    echo ""
    echo "   # Source를 백업으로 rename"
    echo "   curl -X POST '$OPENSEARCH_HOST/_reindex' -H 'Content-Type: application/json' -d '{"source":{"index":"$SOURCE_INDEX"},"dest":{"index":"$BACKUP_INDEX"}}'"
    echo ""
    echo "   # Source 삭제"
    echo "   curl -X DELETE '$OPENSEARCH_HOST/$SOURCE_INDEX'"
    echo ""
    echo "   # Dest를 Source로 rename"
    echo "   curl -X POST '$OPENSEARCH_HOST/_reindex' -H 'Content-Type: application/json' -d '{"source":{"index":"$DEST_INDEX"},"dest":{"index":"$SOURCE_INDEX"}}'"
    echo ""
    echo "   # Dest 삭제"
    echo "   curl -X DELETE '$OPENSEARCH_HOST/$DEST_INDEX'"
    echo ""
    exit 0
fi

echo "🔄 인덱스 교체 시작..."

# Source를 Backup으로 복사
echo "   백업 생성 중: $SOURCE_INDEX → $BACKUP_INDEX"
curl -s -X POST "$OPENSEARCH_HOST/_reindex?wait_for_completion=true" \
-H 'Content-Type: application/json' \
-d '{
  "source": {"index": "'"$SOURCE_INDEX"'"},
  "dest": {"index": "'"$BACKUP_INDEX"'"}
}' > /dev/null
echo "   ✅ 백업 완료"

# Source 삭제
echo "   원본 삭제 중: $SOURCE_INDEX"
curl -s -X DELETE "$OPENSEARCH_HOST/$SOURCE_INDEX"
echo "   ✅ 원본 삭제 완료"

# Dest를 Source로 rename (재색인)
echo "   새 인덱스 이름 변경 중: $DEST_INDEX → $SOURCE_INDEX"
curl -s -X POST "$OPENSEARCH_HOST/_reindex?wait_for_completion=true" \
-H 'Content-Type: application/json' \
-d '{
  "source": {"index": "'"$DEST_INDEX"'"},
  "dest": {"index": "'"$SOURCE_INDEX"'"}
}' > /dev/null
echo "   ✅ 이름 변경 완료"

# Dest 삭제
echo "   임시 인덱스 삭제 중: $DEST_INDEX"
curl -s -X DELETE "$OPENSEARCH_HOST/$DEST_INDEX"
echo "   ✅ 임시 인덱스 삭제 완료"

echo ""
echo "=========================================="
echo "마이그레이션 완료!"
echo "=========================================="
echo ""
echo "📋 결과:"
echo "   - 새 인덱스: $SOURCE_INDEX (log_vector는 knn_vector)"
echo "   - 백업: $BACKUP_INDEX"
echo "   - 문서 수: $DEST_DOC_COUNT"
echo ""
echo "📌 다음 단계:"
echo "   1. AI 서비스에서 로그 분석 API 테스트"
echo "   2. 정상 작동 확인 후 백업 인덱스 삭제:"
echo "      curl -X DELETE '$OPENSEARCH_HOST/$BACKUP_INDEX'"
echo ""
