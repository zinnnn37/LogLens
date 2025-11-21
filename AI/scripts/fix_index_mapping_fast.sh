#!/bin/bash

# ============================================
# OpenSearch 인덱스 빠른 재생성 스크립트
# 경고: 인덱스 삭제 후 재생성하므로 데이터 손실 발생!
# ============================================

set -e

OPENSEARCH_HOST="${OPENSEARCH_HOST:-http://localhost:9200}"
INDEX_NAME="$1"

if [ -z "$INDEX_NAME" ]; then
    echo "사용법: $0 <index_name>"
    echo "예시: $0 a0b4a1a9_d2ae_3672_a0e1_3a4863922226_2025_11"
    exit 1
fi

echo "=========================================="
echo "⚠️  경고: 빠른 인덱스 재생성"
echo "=========================================="
echo "인덱스: $INDEX_NAME"
echo ""
echo "이 스크립트는 인덱스를 삭제하고 재생성합니다."
echo "❌ 모든 데이터가 영구적으로 삭제됩니다!"
echo "❌ Logstash가 다시 데이터를 전송해야 합니다!"
echo ""

# 인덱스 존재 확인
if curl -s -f "$OPENSEARCH_HOST/$INDEX_NAME" > /dev/null 2>&1; then
    DOC_COUNT=$(curl -s "$OPENSEARCH_HOST/$INDEX_NAME/_count" | jq -r '.count')
    echo "현재 문서 수: $DOC_COUNT"
    echo ""
else
    echo "❌ 인덱스 '$INDEX_NAME'가 존재하지 않습니다!"
    exit 1
fi

read -p "정말로 인덱스를 삭제하시겠습니까? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo "❌ 작업을 취소했습니다."
    exit 0
fi

echo ""
echo "1️⃣  logs-template 확인"
if ! curl -s -f "$OPENSEARCH_HOST/_index_template/logs-template" > /dev/null 2>&1; then
    echo "❌ logs-template이 없습니다!"
    echo "   먼저 create_opensearch_index.sh를 실행하세요."
    exit 1
fi
echo "✅ logs-template 존재 확인"
echo ""

echo "2️⃣  인덱스 삭제"
curl -s -X DELETE "$OPENSEARCH_HOST/$INDEX_NAME"
echo "✅ 인덱스 삭제 완료"
echo ""

echo "3️⃣  인덱스 재생성 (템플릿 자동 적용)"
# OpenSearch는 데이터가 들어올 때 자동으로 인덱스를 생성하지만,
# 수동으로 생성하여 템플릿 적용을 확인합니다.
curl -s -X PUT "$OPENSEARCH_HOST/$INDEX_NAME"
echo "✅ 인덱스 재생성 완료"
echo ""

echo "4️⃣  log_vector 필드 매핑 확인"
sleep 2  # 인덱스 생성 대기
LOG_VECTOR_TYPE=$(curl -s "$OPENSEARCH_HOST/$INDEX_NAME/_mapping" | jq -r ".\"$INDEX_NAME\".mappings.properties.log_vector.type" 2>/dev/null)

if [ "$LOG_VECTOR_TYPE" == "knn_vector" ]; then
    echo "✅ log_vector: knn_vector (정상)"
    DIMENSION=$(curl -s "$OPENSEARCH_HOST/$INDEX_NAME/_mapping" | jq -r ".\"$INDEX_NAME\".mappings.properties.log_vector.dimension" 2>/dev/null)
    echo "✅ dimension: $DIMENSION"
else
    echo "❌ log_vector 타입이 knn_vector가 아닙니다: $LOG_VECTOR_TYPE"
    echo "   템플릿 설정을 확인하세요."
    exit 1
fi
echo ""

echo "=========================================="
echo "재생성 완료!"
echo "=========================================="
echo ""
echo "📋 다음 단계:"
echo "   1. Logstash가 자동으로 데이터를 다시 전송할 때까지 대기"
echo "   2. BE 서비스에서 로그 데이터 확인"
echo "   3. AI 서비스에서 로그 분석 API 테스트"
echo ""
echo "⚠️  참고: 데이터가 다시 수집될 때까지 해당 프로젝트의 로그가 보이지 않습니다."
echo ""
