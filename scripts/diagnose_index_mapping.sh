#!/bin/bash

# ============================================
# OpenSearch 인덱스 매핑 진단 스크립트
# ============================================

set -e

OPENSEARCH_HOST="${OPENSEARCH_HOST:-http://localhost:9200}"

echo "=========================================="
echo "OpenSearch 인덱스 매핑 진단"
echo "=========================================="
echo ""

# 1. OpenSearch 버전 및 KNN 플러그인 확인
echo "1️⃣  OpenSearch 버전 확인"
echo "---"
curl -s "$OPENSEARCH_HOST/" | jq -r '.version.number' 2>/dev/null || echo "jq not installed"
echo ""

echo "2️⃣  KNN 플러그인 설치 확인"
echo "---"
curl -s "$OPENSEARCH_HOST/_cat/plugins?v" | grep -i knn || echo "❌ KNN 플러그인이 설치되지 않았습니다!"
echo ""

# 2. 템플릿 확인
echo "3️⃣  logs-template 존재 확인"
echo "---"
TEMPLATE_EXISTS=$(curl -s "$OPENSEARCH_HOST/_index_template/logs-template" | jq -r '.index_templates | length' 2>/dev/null || echo "0")
if [ "$TEMPLATE_EXISTS" -gt 0 ]; then
    echo "✅ logs-template 존재함"
    echo "   패턴:"
    curl -s "$OPENSEARCH_HOST/_index_template/logs-template" | jq -r '.index_templates[0].index_template.index_patterns[]' 2>/dev/null
    echo "   우선순위:"
    curl -s "$OPENSEARCH_HOST/_index_template/logs-template" | jq -r '.index_templates[0].index_template.priority' 2>/dev/null
else
    echo "❌ logs-template이 없습니다!"
fi
echo ""

# 3. 실제 인덱스 목록 확인
echo "4️⃣  현재 생성된 로그 인덱스 목록"
echo "---"
curl -s "$OPENSEARCH_HOST/_cat/indices/*_2025_*?v&s=index" || echo "인덱스 없음"
echo ""

# 4. 에러 발생 인덱스들의 매핑 확인
echo "5️⃣  에러 발생 인덱스 매핑 상세 확인"
echo "---"

# 에러가 발생했던 인덱스들
ERROR_INDICES=(
    "a0b4a1a9_d2ae_3672_a0e1_3a4863922226_2025_11"
    "9f8c4c75_a936_3ab6_92a5_d1309cd9f87e_2025_11"
)

for index in "${ERROR_INDICES[@]}"; do
    echo ""
    echo "📋 인덱스: $index"

    # 인덱스 존재 확인
    if curl -s -f "$OPENSEARCH_HOST/$index" > /dev/null 2>&1; then
        echo "   ✅ 인덱스 존재함"

        # log_vector 필드 타입 확인
        echo "   log_vector 필드 타입:"
        LOG_VECTOR_TYPE=$(curl -s "$OPENSEARCH_HOST/$index/_mapping" | jq -r ".\"$index\".mappings.properties.log_vector.type" 2>/dev/null || echo "null")

        if [ "$LOG_VECTOR_TYPE" == "knn_vector" ]; then
            echo "   ✅ log_vector: knn_vector (정상)"
            # dimension 확인
            DIMENSION=$(curl -s "$OPENSEARCH_HOST/$index/_mapping" | jq -r ".\"$index\".mappings.properties.log_vector.dimension" 2>/dev/null)
            echo "   ✅ dimension: $DIMENSION"
        elif [ "$LOG_VECTOR_TYPE" == "null" ]; then
            echo "   ❌ log_vector 필드가 존재하지 않습니다!"
        else
            echo "   ❌ log_vector 타입이 knn_vector가 아닙니다: $LOG_VECTOR_TYPE"
        fi

        # 인덱스 설정에서 index.knn 확인
        echo "   index.knn 설정:"
        INDEX_KNN=$(curl -s "$OPENSEARCH_HOST/$index/_settings" | jq -r ".\"$index\".settings.index.knn" 2>/dev/null || echo "null")
        if [ "$INDEX_KNN" == "true" ]; then
            echo "   ✅ index.knn: true"
        else
            echo "   ❌ index.knn: $INDEX_KNN (false 또는 없음)"
        fi
    else
        echo "   ❌ 인덱스가 존재하지 않습니다"
    fi
    echo "   ---"
done

echo ""
echo "=========================================="
echo "진단 완료"
echo "=========================================="
echo ""
echo "📋 다음 단계:"
echo "   1. 위 결과를 확인하세요"
echo "   2. log_vector가 knn_vector가 아닌 인덱스가 있으면 재생성이 필요합니다"
echo "   3. 해결 방법:"
echo "      - 옵션 A: 인덱스 삭제 후 재생성 (빠름, 데이터 손실)"
echo "      - 옵션 B: Reindex로 마이그레이션 (안전, 데이터 보존)"
echo "      - 옵션 C: 코드 수정으로 임시 우회 (근본 해결 아님)"
echo ""
