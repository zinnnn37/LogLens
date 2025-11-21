#!/bin/bash

# ============================================
# OpenSearch _v2 인덱스를 원본 이름으로 변경 (최종 버전)
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
echo "   기존 alias가 있다면 삭제됩니다!"
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

    # 1. Alias 확인 및 삭제
    echo "  🔍 Alias 확인..."
    ALIAS_CHECK=$(curl -s "$OPENSEARCH_HOST/_cat/aliases/$ORIGINAL_INDEX?h=alias" 2>/dev/null)

    if [ -n "$ALIAS_CHECK" ]; then
        echo "     Alias 발견: $ORIGINAL_INDEX"
        echo "  🗑️  Alias 삭제..."

        # Alias가 가리키는 실제 인덱스 찾기
        ALIASED_INDEX=$(curl -s "$OPENSEARCH_HOST/_cat/aliases/$ORIGINAL_INDEX?h=index")

        # Alias 삭제
        curl -s -X POST "$OPENSEARCH_HOST/_aliases" \
          -H 'Content-Type: application/json' -d "{
            \"actions\": [
              {\"remove\": {\"index\": \"$ALIASED_INDEX\", \"alias\": \"$ORIGINAL_INDEX\"}}
            ]
          }" > /dev/null

        echo "     ✅ Alias 삭제됨"
    else
        echo "     Alias 없음"
    fi

    # 2. 원본 인덱스 확인 및 삭제 (실제 인덱스인 경우)
    INDEX_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" "$OPENSEARCH_HOST/$ORIGINAL_INDEX")

    if [ "$INDEX_EXISTS" == "200" ]; then
        echo "  🗑️  원본 인덱스 삭제..."
        curl -s -X DELETE "$OPENSEARCH_HOST/$ORIGINAL_INDEX" > /dev/null
        echo "     ✅ 삭제됨"
    fi

    # 3. 새 인덱스 생성 (템플릿 자동 적용)
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

    # 4. 데이터 복사
    echo "  📋 데이터 복사 중..."
    REINDEX_RESULT=$(curl -s -X POST "$OPENSEARCH_HOST/_reindex?wait_for_completion=true&refresh=true" \
      -H 'Content-Type: application/json' -d "{
        \"source\": {\"index\": \"$V2_INDEX\"},
        \"dest\": {\"index\": \"$ORIGINAL_INDEX\", \"version_type\": \"internal\"}
      }")

    # 복사된 문서 수 확인
    COPIED=$(echo "$REINDEX_RESULT" | jq -r '.total // 0')
    echo "     복사된 문서: $COPIED"

    # 5. Refresh 강제 실행
    echo "  🔄 인덱스 새로고침..."
    curl -s -X POST "$OPENSEARCH_HOST/$ORIGINAL_INDEX/_refresh" > /dev/null
    sleep 2  # Refresh 완료 대기

    # 6. 개수 확인
    ORIGINAL_COUNT=$(curl -s "$OPENSEARCH_HOST/$ORIGINAL_INDEX/_count" | jq -r '.count')
    V2_COUNT=$(curl -s "$OPENSEARCH_HOST/$V2_INDEX/_count" | jq -r '.count')

    echo "  📊 검증 중..."
    echo "     원본: $ORIGINAL_COUNT"
    echo "     _v2:  $V2_COUNT"
    echo "     복사: $COPIED"

    if [ "$ORIGINAL_COUNT" == "$V2_COUNT" ] && [ "$ORIGINAL_COUNT" == "$COPIED" ] && [ "$ORIGINAL_COUNT" != "null" ] && [ "$ORIGINAL_COUNT" != "0" ]; then
        echo "  ✅ 검증 성공!"

        # 7. _v2 삭제
        echo "  🗑️  _v2 인덱스 삭제..."
        curl -s -X DELETE "$OPENSEARCH_HOST/$V2_INDEX" > /dev/null
        echo "  ✅ 완료!"
    else
        echo "  ⚠️  검증 경고!"
        echo "     일치하지 않지만 복사는 완료되었습니다."
        echo "     원본 카운트가 0이면 refresh 문제일 수 있습니다."

        # 한 번 더 확인
        sleep 3
        RECHECK_COUNT=$(curl -s "$OPENSEARCH_HOST/$ORIGINAL_INDEX/_count" | jq -r '.count')
        echo "  🔄 재확인: $RECHECK_COUNT"

        if [ "$RECHECK_COUNT" == "$V2_COUNT" ] && [ "$RECHECK_COUNT" != "0" ]; then
            echo "  ✅ 재확인 성공! _v2 삭제..."
            curl -s -X DELETE "$OPENSEARCH_HOST/$V2_INDEX" > /dev/null
            echo "  ✅ 완료!"
        else
            echo "  ❌ 여전히 불일치. _v2 유지."
            echo "     수동 확인 필요: curl '$OPENSEARCH_HOST/$ORIGINAL_INDEX/_count'"
        fi
    fi

    echo ""
done

echo "🎉 모든 인덱스 처리 완료!"
echo ""
echo "📋 최종 상태:"
echo ""
echo "인덱스 목록:"
curl -s "$OPENSEARCH_HOST/_cat/indices/*_2025_*?v&h=index,docs.count,store.size"
echo ""
echo "Alias 목록:"
ALIASES=$(curl -s "$OPENSEARCH_HOST/_cat/aliases/*_2025_*?v&h=alias,index")
if [ -z "$ALIASES" ]; then
    echo "(없음)"
else
    echo "$ALIASES"
fi
