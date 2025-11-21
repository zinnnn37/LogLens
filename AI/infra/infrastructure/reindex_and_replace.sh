#!/bin/bash

# ============================================
# OpenSearch 리인덱싱 및 원본 교체 스크립트
# ============================================
# 목적: 기존 인덱스를 새 매핑으로 리인덱싱 후 원본 교체
# 사용법: ./reindex_and_replace.sh <project_uuid> <year_month>

set -e

OPENSEARCH_HOST="${OPENSEARCH_HOST:-http://localhost:9200}"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 사용법 출력
usage() {
    echo "사용법: $0 <project_uuid> <year_month>"
    echo ""
    echo "파라미터:"
    echo "  project_uuid  : 프로젝트 UUID (하이픈 포함, 예: 9911573f-8a1d-3b96-98b4-5a0def93513b)"
    echo "  year_month    : YYYY_MM 형식 (예: 2025_11)"
    echo ""
    echo "예제:"
    echo "  $0 9911573f-8a1d-3b96-98b4-5a0def93513b 2025_11"
    exit 1
}

# 인자 확인
if [ $# -ne 2 ]; then
    usage
fi

PROJECT_UUID="$1"
YEAR_MONTH="$2"

# UUID 변환
SAFE_UUID=$(echo "$PROJECT_UUID" | tr '-' '_')
SOURCE_INDEX="${SAFE_UUID}_${YEAR_MONTH}"
TEMP_INDEX="${SOURCE_INDEX}_v2"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}OpenSearch 리인덱싱 및 교체 시작${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "${GREEN}설정:${NC}"
echo -e "  OpenSearch Host: ${OPENSEARCH_HOST}"
echo -e "  원본 인덱스: ${SOURCE_INDEX}"
echo -e "  임시 인덱스: ${TEMP_INDEX}"
echo ""

# OpenSearch 연결 확인
echo -e "${YELLOW}📡 OpenSearch 연결 확인...${NC}"
HEALTH_CHECK=$(curl -m 10 -s "${OPENSEARCH_HOST}/_cluster/health" 2>&1)
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ OpenSearch 연결 실패 (timeout 또는 연결 불가)${NC}"
    echo "$HEALTH_CHECK"
    exit 1
fi
echo -e "${GREEN}✅ OpenSearch 연결 성공${NC}"
echo ""

# 원본 인덱스 존재 확인
echo -e "${YELLOW}🔍 원본 인덱스 확인...${NC}"
INDEX_CHECK=$(curl -m 10 -s -X HEAD -w "%{http_code}" "${OPENSEARCH_HOST}/${SOURCE_INDEX}" 2>&1)
HTTP_CODE="${INDEX_CHECK: -3}"

if [ "$HTTP_CODE" != "200" ]; then
    echo -e "${RED}❌ 원본 인덱스가 존재하지 않습니다: ${SOURCE_INDEX}${NC}"
    echo -e "${RED}   HTTP 상태 코드: ${HTTP_CODE}${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 원본 인덱스 존재 확인${NC}"

# 문서 수 조회
echo -e "${YELLOW}   문서 수 조회 중...${NC}"
SOURCE_COUNT=$(curl -m 10 -s "${OPENSEARCH_HOST}/${SOURCE_INDEX}/_count" | jq -r '.count')
if [ -z "$SOURCE_COUNT" ] || [ "$SOURCE_COUNT" == "null" ]; then
    echo -e "${RED}❌ 문서 수 조회 실패${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 원본 인덱스: ${SOURCE_COUNT}건${NC}"
echo ""

# 임시 인덱스가 이미 있으면 삭제
if curl -s -X HEAD "${OPENSEARCH_HOST}/${TEMP_INDEX}" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  임시 인덱스가 이미 존재합니다. 삭제합니다...${NC}"
    curl -s -X DELETE "${OPENSEARCH_HOST}/${TEMP_INDEX}" > /dev/null
    echo -e "${GREEN}✅ 기존 임시 인덱스 삭제 완료${NC}"
fi
echo ""

# 1단계: 리인덱싱
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}1단계: 리인덱싱 (${SOURCE_INDEX} → ${TEMP_INDEX})${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "${YELLOW}🔄 리인덱싱 시작 (${SOURCE_COUNT}건 처리 예상)...${NC}"
echo -e "${YELLOW}   이 작업은 수 분 소요될 수 있습니다. 기다려주세요...${NC}"

REINDEX_START=$(date +%s)
REINDEX_RESULT=$(curl -m 1800 -s -X POST "${OPENSEARCH_HOST}/_reindex?wait_for_completion=true&timeout=30m" \
  -H 'Content-Type: application/json' \
  -d "{
    \"source\": {
      \"index\": \"${SOURCE_INDEX}\"
    },
    \"dest\": {
      \"index\": \"${TEMP_INDEX}\"
    }
  }" 2>&1)
REINDEX_END=$(date +%s)
REINDEX_DURATION=$((REINDEX_END - REINDEX_START))

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 리인덱싱 요청 실패 (timeout 또는 연결 에러)${NC}"
    echo "$REINDEX_RESULT"
    exit 1
fi

# 리인덱싱 결과 확인
TOTAL=$(echo "$REINDEX_RESULT" | jq -r '.total')
CREATED=$(echo "$REINDEX_RESULT" | jq -r '.created')
FAILURES=$(echo "$REINDEX_RESULT" | jq -r '.failures | length')

if [ "$FAILURES" != "0" ] && [ "$FAILURES" != "null" ]; then
    echo -e "${RED}❌ 리인덱싱 실패 (failures: ${FAILURES})${NC}"
    echo "$REINDEX_RESULT" | jq '.failures' 2>/dev/null || echo "$REINDEX_RESULT"
    exit 1
fi

echo -e "${GREEN}✅ 리인덱싱 완료 (소요 시간: ${REINDEX_DURATION}초)${NC}"
echo -e "  총 문서 수: ${TOTAL}"
echo -e "  생성: ${CREATED}"
echo -e "  속도: $((SOURCE_COUNT / REINDEX_DURATION))건/초"
echo ""

# 2단계: 검증
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}2단계: 새 인덱스 검증${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "${YELLOW}🔍 문서 수 비교...${NC}"
TEMP_COUNT=$(curl -s "${OPENSEARCH_HOST}/${TEMP_INDEX}/_count" | jq -r '.count')

echo -e "  원본 인덱스: ${SOURCE_COUNT}건"
echo -e "  새 인덱스: ${TEMP_COUNT}건"

if [ "$SOURCE_COUNT" -ne "$TEMP_COUNT" ]; then
    echo -e "${RED}⚠️  경고: 문서 수 불일치 (차이: $((SOURCE_COUNT - TEMP_COUNT))건)${NC}"
    read -p "계속 진행하시겠습니까? (y/N): " CONTINUE
    if [ "$CONTINUE" != "y" ] && [ "$CONTINUE" != "Y" ]; then
        echo -e "${YELLOW}작업 취소${NC}"
        exit 0
    fi
else
    echo -e "${GREEN}✅ 문서 수 일치${NC}"
fi
echo ""

# log_details 필드 확인
echo -e "${YELLOW}🔍 log_details 파싱 확인...${NC}"
SAMPLE=$(curl -s -X POST "${OPENSEARCH_HOST}/${TEMP_INDEX}/_search" \
  -H 'Content-Type: application/json' \
  -d '{
    "size": 1,
    "query": {
      "bool": {
        "must": [
          {"exists": {"field": "log_details.http_method"}},
          {"exists": {"field": "log_details.request_uri"}}
        ]
      }
    },
    "_source": ["log_details.http_method", "log_details.request_uri", "log_details.execution_time"]
  }')

HITS=$(echo "$SAMPLE" | jq -r '.hits.total.value')

if [ "$HITS" -gt 0 ]; then
    echo -e "${GREEN}✅ log_details 파싱 성공 (${HITS}건 확인)${NC}"
    echo "$SAMPLE" | jq '.hits.hits[0]._source' 2>/dev/null || true
else
    echo -e "${YELLOW}⚠️  log_details.http_method/request_uri 필드가 없습니다${NC}"
    echo -e "${YELLOW}   (평면 구조 필드로 대체 가능)${NC}"
fi
echo ""

# 3단계: 사용자 확인
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}3단계: 원본 교체 준비${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "${RED}⚠️  경고: 다음 작업을 수행합니다${NC}"
echo -e "  1. 원본 인덱스 삭제: ${SOURCE_INDEX}"
echo -e "  2. 새 인덱스에 Alias 생성: ${TEMP_INDEX} → ${SOURCE_INDEX}"
echo ""
echo -e "${YELLOW}이 작업은 되돌릴 수 없습니다!${NC}"
read -p "계속 진행하시겠습니까? (yes 입력 필요): " FINAL_CONFIRM

if [ "$FINAL_CONFIRM" != "yes" ]; then
    echo -e "${YELLOW}작업 취소됨${NC}"
    echo -e "${GREEN}임시 인덱스는 유지됩니다: ${TEMP_INDEX}${NC}"
    exit 0
fi
echo ""

# 4단계: 원본 삭제
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}4단계: 원본 인덱스 삭제${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "${YELLOW}🗑️  원본 인덱스 삭제 중...${NC}"
DELETE_RESULT=$(curl -s -X DELETE "${OPENSEARCH_HOST}/${SOURCE_INDEX}")

if echo "$DELETE_RESULT" | jq -e '.acknowledged == true' > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 원본 인덱스 삭제 완료${NC}"
else
    echo -e "${RED}❌ 원본 인덱스 삭제 실패${NC}"
    echo "$DELETE_RESULT"
    exit 1
fi
echo ""

# 5단계: Alias 생성
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}5단계: Alias 생성${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

echo -e "${YELLOW}🔗 Alias 생성 중 (${TEMP_INDEX} → ${SOURCE_INDEX})...${NC}"
ALIAS_RESULT=$(curl -s -X POST "${OPENSEARCH_HOST}/_aliases" \
  -H 'Content-Type: application/json' \
  -d "{
    \"actions\": [
      {
        \"add\": {
          \"index\": \"${TEMP_INDEX}\",
          \"alias\": \"${SOURCE_INDEX}\"
        }
      }
    ]
  }")

if echo "$ALIAS_RESULT" | jq -e '.acknowledged == true' > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Alias 생성 완료${NC}"
else
    echo -e "${RED}❌ Alias 생성 실패${NC}"
    echo "$ALIAS_RESULT"
    exit 1
fi
echo ""

# 최종 확인
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎉 모든 작업 완료!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}현재 상태:${NC}"
echo -e "  실제 인덱스: ${TEMP_INDEX} (${TEMP_COUNT}건)"
echo -e "  Alias: ${SOURCE_INDEX} → ${TEMP_INDEX}"
echo ""
echo -e "${YELLOW}다음 단계:${NC}"
echo -e "  1. AI chatbot 테스트"
echo -e "     curl -X POST 'http://localhost:8000/api/v2/chatbot/ask/stream' \\"
echo -e "       -d '{\"question\":\"응답시간이 가장 느린 API는?\",\"project_uuid\":\"${PROJECT_UUID}\"}'"
echo ""
echo -e "  2. 확인 완료 후 임시 인덱스 이름 정리 (선택)"
echo -e "     (현재는 ${SOURCE_INDEX} alias를 통해 정상 접근 가능)"
echo ""
