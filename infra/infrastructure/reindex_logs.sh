#!/bin/bash

# ============================================
# OpenSearch 로그 인덱스 리인덱싱 스크립트
# ============================================
# 목적: 기존 인덱스를 새로운 매핑으로 재인덱싱
# 사용법: ./reindex_logs.sh <project_uuid> <year_month>
# 예: ./reindex_logs.sh 9911573f-8a1d-3b96-98b4-5a0def93513b 2025_11

set -e

OPENSEARCH_HOST="${OPENSEARCH_HOST:-http://localhost:9200}"
OPENSEARCH_USER="${OPENSEARCH_USER:-admin}"
OPENSEARCH_PASS="${OPENSEARCH_PASS:-admin}"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 사용법 출력
usage() {
    echo "사용법: $0 [project_uuid] [year_month]"
    echo ""
    echo "파라미터:"
    echo "  project_uuid  : 프로젝트 UUID (선택, 기본값: 모든 인덱스)"
    echo "  year_month    : YYYY_MM 형식 (선택, 기본값: 현재 월)"
    echo ""
    echo "예제:"
    echo "  $0                                                    # 현재 월의 모든 인덱스"
    echo "  $0 9911573f-8a1d-3b96-98b4-5a0def93513b             # 특정 프로젝트의 현재 월"
    echo "  $0 9911573f-8a1d-3b96-98b4-5a0def93513b 2025_11     # 특정 프로젝트의 특정 월"
    exit 1
}

# 인자 파싱
PROJECT_UUID="${1:-}"
YEAR_MONTH="${2:-$(date +'%Y_%m')}"

# PROJECT_UUID를 언더스코어로 변환
if [ -n "$PROJECT_UUID" ]; then
    SAFE_UUID=$(echo "$PROJECT_UUID" | tr '-' '_')
    SOURCE_INDEX="${SAFE_UUID}_${YEAR_MONTH}"
else
    SOURCE_INDEX="*_${YEAR_MONTH}"
fi

DEST_INDEX_SUFFIX="_reindexed"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}OpenSearch 로그 리인덱싱 시작${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${GREEN}설정:${NC}"
echo -e "  OpenSearch Host: ${OPENSEARCH_HOST}"
echo -e "  소스 인덱스: ${SOURCE_INDEX}"
echo -e "  대상 인덱스 접미사: ${DEST_INDEX_SUFFIX}"
echo ""

# OpenSearch 연결 확인
echo -e "${YELLOW}📡 OpenSearch 연결 확인...${NC}"
if ! curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" "${OPENSEARCH_HOST}/_cluster/health" > /dev/null; then
    echo -e "${RED}❌ OpenSearch 연결 실패${NC}"
    exit 1
fi
echo -e "${GREEN}✅ OpenSearch 연결 성공${NC}"
echo ""

# 인덱스 목록 조회
echo -e "${YELLOW}🔍 리인덱싱할 인덱스 조회...${NC}"
INDICES=$(curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
    "${OPENSEARCH_HOST}/_cat/indices/${SOURCE_INDEX}?h=index" | grep -v "^\." || true)

if [ -z "$INDICES" ]; then
    echo -e "${RED}❌ 매칭되는 인덱스가 없습니다: ${SOURCE_INDEX}${NC}"
    exit 1
fi

echo -e "${GREEN}발견된 인덱스:${NC}"
echo "$INDICES" | while read -r idx; do
    DOC_COUNT=$(curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
        "${OPENSEARCH_HOST}/${idx}/_count" | jq -r '.count')
    echo -e "  - ${idx} (${DOC_COUNT}건)"
done
echo ""

# 사용자 확인
read -p "위 인덱스들을 리인덱싱하시겠습니까? (y/N): " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
    echo -e "${YELLOW}리인덱싱 취소${NC}"
    exit 0
fi
echo ""

# 각 인덱스에 대해 리인덱싱 수행
echo "$INDICES" | while read -r SOURCE; do
    if [ -z "$SOURCE" ]; then
        continue
    fi

    DEST="${SOURCE}${DEST_INDEX_SUFFIX}"

    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}리인덱싱: ${SOURCE} → ${DEST}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    # 대상 인덱스가 이미 존재하면 삭제
    if curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
        -X HEAD "${OPENSEARCH_HOST}/${DEST}" > /dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  대상 인덱스가 이미 존재합니다. 삭제합니다...${NC}"
        curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
            -X DELETE "${OPENSEARCH_HOST}/${DEST}" > /dev/null
        echo -e "${GREEN}✅ 기존 인덱스 삭제 완료${NC}"
    fi

    # 리인덱싱 실행
    echo -e "${YELLOW}🔄 리인덱싱 중...${NC}"
    REINDEX_RESULT=$(curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
        -X POST "${OPENSEARCH_HOST}/_reindex?wait_for_completion=false" \
        -H 'Content-Type: application/json' \
        -d '{
          "source": {
            "index": "'"${SOURCE}"'"
          },
          "dest": {
            "index": "'"${DEST}"'"
          }
        }')

    TASK_ID=$(echo "$REINDEX_RESULT" | jq -r '.task')

    if [ "$TASK_ID" == "null" ] || [ -z "$TASK_ID" ]; then
        echo -e "${RED}❌ 리인덱싱 시작 실패${NC}"
        echo "$REINDEX_RESULT" | jq .
        continue
    fi

    echo -e "${GREEN}✅ 리인덱싱 작업 시작 (Task ID: ${TASK_ID})${NC}"

    # 진행 상황 모니터링
    echo -e "${YELLOW}📊 진행 상황 모니터링...${NC}"
    while true; do
        TASK_STATUS=$(curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
            "${OPENSEARCH_HOST}/_tasks/${TASK_ID}")

        COMPLETED=$(echo "$TASK_STATUS" | jq -r '.completed')

        if [ "$COMPLETED" == "true" ]; then
            echo -e "${GREEN}✅ 리인덱싱 완료${NC}"

            # 결과 확인
            TOTAL=$(echo "$TASK_STATUS" | jq -r '.task.status.total')
            CREATED=$(echo "$TASK_STATUS" | jq -r '.task.status.created')
            UPDATED=$(echo "$TASK_STATUS" | jq -r '.task.status.updated')

            echo -e "  총 문서 수: ${TOTAL}"
            echo -e "  생성: ${CREATED}"
            echo -e "  업데이트: ${UPDATED}"
            break
        fi

        # 진행률 표시
        TOTAL=$(echo "$TASK_STATUS" | jq -r '.task.status.total // 0')
        CREATED=$(echo "$TASK_STATUS" | jq -r '.task.status.created // 0')

        if [ "$TOTAL" -gt 0 ]; then
            PROGRESS=$((CREATED * 100 / TOTAL))
            echo -ne "  진행률: ${PROGRESS}% (${CREATED}/${TOTAL})\r"
        fi

        sleep 2
    done

    # 문서 수 비교
    echo -e "${YELLOW}🔍 문서 수 검증...${NC}"
    SOURCE_COUNT=$(curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
        "${OPENSEARCH_HOST}/${SOURCE}/_count" | jq -r '.count')
    DEST_COUNT=$(curl -s -u "${OPENSEARCH_USER}:${OPENSEARCH_PASS}" \
        "${OPENSEARCH_HOST}/${DEST}/_count" | jq -r '.count')

    echo -e "  소스 인덱스 (${SOURCE}): ${SOURCE_COUNT}건"
    echo -e "  대상 인덱스 (${DEST}): ${DEST_COUNT}건"

    if [ "$SOURCE_COUNT" -eq "$DEST_COUNT" ]; then
        echo -e "${GREEN}✅ 문서 수 일치${NC}"
    else
        echo -e "${RED}⚠️  문서 수 불일치 (차이: $((SOURCE_COUNT - DEST_COUNT))건)${NC}"
    fi

    echo ""
done

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎉 모든 리인덱싱 완료!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}다음 단계:${NC}"
echo -e "  1. 리인덱싱된 데이터 확인"
echo -e "  2. Alias 전환 (선택)"
echo -e "     예: curl -X POST '${OPENSEARCH_HOST}/_aliases' -d '{...}'"
echo -e "  3. 기존 인덱스 삭제 (확인 후)"
echo -e "     예: curl -X DELETE '${OPENSEARCH_HOST}/${SOURCE_INDEX}'"
echo ""
