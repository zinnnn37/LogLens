#!/bin/bash

# ============================================
# OpenSearch _v2 인덱스를 원본 이름으로 변경
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

# 모든 _v2 인덱스 찾기
V2_INDICES=$(curl -s "$OPENSEARCH_HOST/_cat/indices/*_v2?h=index")

if [ -z "$V2_INDICES" ]; then
    echo "ℹ️  _v2 인덱스가 없습니다."
    exit 0
fi

echo "🔍 발견된 _v2 인덱스:"
echo "$V2_INDICES"
echo ""

# 사용자 확인
echo "⚠️  위 인덱스들을 원본 이름으로 변경합니다."
echo "   원본 인덱스가 있다면 삭제됩니다!"
echo ""
read -p "계속하시겠습니까? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "❌ 작업 취소됨"
    exit 0
fi

echo ""

for V2_INDEX in $V2_INDICES; do
    # _v2 제거한 원본 이름
    ORIGINAL_INDEX="${V2_INDEX%_v2}"

    echo "📦 처리 중: $V2_INDEX → $ORIGINAL_INDEX"

    # 1. 원본 인덱스 삭제 (있다면)
    echo "  🗑️  원본 인덱스 삭제 시도..."
    DELETE_RESULT=$(curl -s -X DELETE "$OPENSEARCH_HOST/$ORIGINAL_INDEX" 2>&1)
    if echo "$DELETE_RESULT" | grep -q "acknowledged.*true"; then
        echo "     원본 인덱스 삭제됨"
    elif echo "$DELETE_RESULT" | grep -q "index_not_found"; then
        echo "     원본 인덱스 없음 (정상)"
    else
        echo "     삭제 응답: $DELETE_RESULT"
    fi

    # 2. 새 인덱스 생성 (템플릿 자동 적용)
    echo "  🔨 새 인덱스 생성..."
    CREATE_RESULT=$(curl -s -X PUT "$OPENSEARCH_HOST/$ORIGINAL_INDEX")
    if echo "$CREATE_RESULT" | grep -q "acknowledged.*true"; then
        echo "     ✅ 생성 성공"
    else
        echo "     ❌ 생성 실패: $CREATE_RESULT"
        echo "     건너뜀"
        echo ""
        continue
    fi

    # 3. 데이터 복사
    echo "  📋 데이터 복사 중..."
    REINDEX_RESULT=$(curl -s -X POST "$OPENSEARCH_HOST/_reindex?wait_for_completion=true" \
      -H 'Content-Type: application/json' -d "{
        \"source\": {\"index\": \"$V2_INDEX\"},
        \"dest\": {\"index\": \"$ORIGINAL_INDEX\"}
      }")

    # 복사된 문서 수 확인
    COPIED=$(echo "$REINDEX_RESULT" | jq -r '.total // 0')
    echo "     복사된 문서: $COPIED"

    # 4. 개수 확인
    ORIGINAL_COUNT=$(curl -s "$OPENSEARCH_HOST/$ORIGINAL_INDEX/_count" | jq -r '.count')
    V2_COUNT=$(curl -s "$OPENSEARCH_HOST/$V2_INDEX/_count" | jq -r '.count')

    echo "  📊 검증 중..."
    echo "     원본: $ORIGINAL_COUNT"
    echo "     _v2:  $V2_COUNT"

    if [ "$ORIGINAL_COUNT" == "$V2_COUNT" ] && [ "$ORIGINAL_COUNT" != "null" ]; then
        echo "  ✅ 검증 성공!"

        # 5. _v2 삭제
        echo "  🗑️  _v2 인덱스 삭제..."
        curl -s -X DELETE "$OPENSEARCH_HOST/$V2_INDEX" > /dev/null
        echo "  ✅ 완료!"
    else
        echo "  ❌ 검증 실패!"
        echo "     원본 인덱스는 유지되지만 _v2는 삭제하지 않았습니다."
        echo "     수동으로 확인 후 처리하세요."
    fi

    echo ""
done

echo "🎉 모든 인덱스 처리 완료!"
echo ""
echo "📋 최종 인덱스 목록:"
curl -s "$OPENSEARCH_HOST/_cat/indices/*_2025_*?v&h=index,docs.count,store.size"
