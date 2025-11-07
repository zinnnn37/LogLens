#!/bin/bash

# ============================================
# v2 인덱스 이름을 원본 이름으로 변경하는 스크립트
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

# v2 인덱스 목록 조회
echo "🔍 v2 인덱스 검색 중..."
V2_INDICES=$(curl -s "$OPENSEARCH_HOST/_cat/indices/*_2025_11_v2?h=index" | grep -v "^\." | sort)

if [ -z "$V2_INDICES" ]; then
    echo "❌ v2 인덱스를 찾을 수 없습니다."
    exit 0
fi

# 인덱스 개수
INDEX_COUNT=$(echo "$V2_INDICES" | wc -l)
echo "   찾은 v2 인덱스: ${INDEX_COUNT}개"
echo ""

# 각 인덱스 정보 표시
echo "📊 인덱스 목록:"
for index in $V2_INDICES; do
    STATS=$(curl -s "$OPENSEARCH_HOST/_cat/indices/$index?h=store.size,docs.count")
    ORIGINAL_NAME="${index%_v2}"
    echo "   - $index → $ORIGINAL_NAME ($STATS)"
done
echo ""

# 사용자 확인
read -p "⚠️  위 v2 인덱스들의 이름을 변경하시겠습니까? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ 취소되었습니다."
    exit 0
fi
echo ""

# 각 인덱스 처리
CURRENT=0
SUCCEEDED=0
FAILED=0

for v2_index in $V2_INDICES; do
    CURRENT=$((CURRENT + 1))
    ORIGINAL_INDEX="${v2_index%_v2}"

    echo "=========================================="
    echo "📝 인덱스 $CURRENT/$INDEX_COUNT: $v2_index"
    echo "=========================================="
    echo "   목표: $v2_index → $ORIGINAL_INDEX"
    echo ""

    # 1. 원본 이름 인덱스가 이미 존재하는지 확인
    if curl -s -o /dev/null -w "%{http_code}" "$OPENSEARCH_HOST/$ORIGINAL_INDEX" | grep -q "200"; then
        echo "   ⚠️  $ORIGINAL_INDEX 이미 존재합니다."
        echo "   원본 인덱스를 먼저 삭제해야 합니다."
        read -p "   $ORIGINAL_INDEX 를 삭제하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            curl -s -X DELETE "$OPENSEARCH_HOST/$ORIGINAL_INDEX" > /dev/null
            echo "   ✅ 기존 인덱스 삭제 완료"
        else
            echo "   ⚠️  건너뜁니다."
            echo ""
            continue
        fi
    fi

    # 2. Alias 제거 (있다면)
    echo "   🔗 Alias 제거 중..."
    curl -s -X POST "$OPENSEARCH_HOST/_aliases" \
        -H 'Content-Type: application/json' \
        -d "{
            \"actions\": [
                {\"remove\": {\"index\": \"$v2_index\", \"alias\": \"$ORIGINAL_INDEX\"}}
            ]
        }" > /dev/null 2>&1 || true
    echo "   ✅ Alias 제거 완료"

    # 3. 문서 개수 확인
    V2_COUNT=$(curl -s "$OPENSEARCH_HOST/$v2_index/_count" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
    echo "   📊 v2 인덱스 문서 개수: $V2_COUNT"

    # 4. v2 → 원본 이름으로 Reindex
    echo "   🔄 인덱스 이름 변경 중 (Reindex)..."
    REINDEX_RESULT=$(curl -s -X POST "$OPENSEARCH_HOST/_reindex?wait_for_completion=true" \
        -H 'Content-Type: application/json' \
        -d "{
            \"source\": {\"index\": \"$v2_index\"},
            \"dest\": {\"index\": \"$ORIGINAL_INDEX\"}
        }")

    # Reindex 결과 확인
    CREATED=$(echo "$REINDEX_RESULT" | grep -o '"created":[0-9]*' | grep -o '[0-9]*')

    if [ ! -z "$CREATED" ] && [ "$CREATED" -gt 0 ]; then
        echo "   ✅ Reindex 완료 (생성: $CREATED)"
    else
        echo "   ❌ Reindex 실패"
        echo "   상세: $REINDEX_RESULT"
        FAILED=$((FAILED + 1))
        echo ""
        continue
    fi

    # 5. Refresh
    echo "   🔄 인덱스 Refresh 중..."
    curl -s -X POST "$OPENSEARCH_HOST/$ORIGINAL_INDEX/_refresh" > /dev/null

    # 6. 문서 개수 검증
    NEW_COUNT=$(curl -s "$OPENSEARCH_HOST/$ORIGINAL_INDEX/_count" | grep -o '"count":[0-9]*' | grep -o '[0-9]*')
    echo "   📊 새 인덱스 문서 개수: $NEW_COUNT"

    if [ "$V2_COUNT" != "$NEW_COUNT" ]; then
        echo "   ❌ 문서 개수 불일치! ($V2_COUNT vs $NEW_COUNT)"
        FAILED=$((FAILED + 1))
        echo ""
        continue
    fi

    echo "   ✅ 문서 개수 검증 완료"

    # 7. v2 인덱스 삭제
    echo "   🗑️  v2 인덱스 삭제 중..."
    DELETE_RESULT=$(curl -s -X DELETE "$OPENSEARCH_HOST/$v2_index")

    if echo "$DELETE_RESULT" | grep -q '"acknowledged":true'; then
        echo "   ✅ v2 인덱스 삭제 완료"
        SUCCEEDED=$((SUCCEEDED + 1))
    else
        echo "   ❌ v2 인덱스 삭제 실패: $DELETE_RESULT"
        FAILED=$((FAILED + 1))
    fi

    echo ""
done

# 최종 결과
echo "=========================================="
echo "🎉 이름 변경 완료!"
echo "=========================================="
echo "   총 인덱스: $INDEX_COUNT"
echo "   성공: $SUCCEEDED"
echo "   실패: $FAILED"
echo ""

if [ $FAILED -eq 0 ]; then
    echo "✅ 모든 인덱스 이름이 성공적으로 변경되었습니다!"
    echo ""
    echo "📋 최종 인덱스 목록:"
    curl -s "$OPENSEARCH_HOST/_cat/indices/*_2025_11?v&h=index,docs.count,store.size"
    exit 0
else
    echo "⚠️  일부 인덱스 이름 변경이 실패했습니다."
    echo "   실패한 인덱스를 수동으로 확인해주세요."
    exit 1
fi
