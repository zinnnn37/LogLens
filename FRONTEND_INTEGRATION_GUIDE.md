# V2 API 프론트엔드 통합 가이드

## 개요

V2 API는 RAG (Retrieval-Augmented Generation) 검증을 위한 `sources`와 `validation` 필드를 제공합니다.
프론트엔드에서 이 정보를 표시하여 AI 답변의 신뢰도와 근거를 사용자에게 보여줄 수 있습니다.

## 1. Chatbot V2 API

### 엔드포인트
```
POST /api/v2/chatbot/ask
```

### 응답 구조
```typescript
interface ChatResponse {
  answer: string;
  from_cache: boolean;
  related_logs: any[];
  answered_at: string;

  // V2 추가 필드
  sources: LogSource[] | null;
  validation: ValidationInfo | null;
}

interface LogSource {
  log_id: string;
  timestamp: string;
  level: string;  // "ERROR", "WARN", "INFO"
  message: string;
  service_name: string;
  relevance_score: number | null;  // 0.0 ~ 1.0
  class_name?: string;
  method_name?: string;
}

interface ValidationInfo {
  confidence: number;  // 0 ~ 100
  sample_count: number;
  sampling_strategy: string;  // "proportional_vector_knn", "random_filter", etc.
  coverage: string;
  data_quality: string;  // "high", "medium", "low"
  limitation: string;
  note: string | null;
}
```

### UI 구현 예시

#### 1) 신뢰도 배지
```tsx
function ConfidenceBadge({ validation }: { validation: ValidationInfo }) {
  const getColor = (confidence: number) => {
    if (confidence >= 80) return 'bg-green-100 text-green-800';
    if (confidence >= 60) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  };

  return (
    <span className={`px-2 py-1 rounded text-sm ${getColor(validation.confidence)}`}>
      신뢰도: {validation.confidence}%
    </span>
  );
}
```

#### 2) 출처 목록
```tsx
function SourcesList({ sources }: { sources: LogSource[] }) {
  return (
    <div className="mt-4 border rounded p-4">
      <h4 className="font-bold mb-2">📋 분석 출처 ({sources.length}개 로그)</h4>
      <ul className="space-y-2">
        {sources.map((source, index) => (
          <li key={index} className="flex items-start">
            <span className={`px-2 py-1 rounded text-xs mr-2 ${
              source.level === 'ERROR' ? 'bg-red-100' :
              source.level === 'WARN' ? 'bg-yellow-100' : 'bg-blue-100'
            }`}>
              {source.level}
            </span>
            <div className="flex-1">
              <p className="text-sm">{source.message.substring(0, 100)}...</p>
              <p className="text-xs text-gray-500">
                {source.service_name} • {source.timestamp}
                {source.relevance_score && ` • 관련성: ${(source.relevance_score * 100).toFixed(1)}%`}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
```

#### 3) 검증 정보 표시
```tsx
function ValidationDetails({ validation }: { validation: ValidationInfo }) {
  return (
    <div className="mt-2 text-sm text-gray-600 border-l-4 border-blue-500 pl-3">
      <p><strong>샘플 크기:</strong> {validation.sample_count}개</p>
      <p><strong>샘플링 전략:</strong> {validation.sampling_strategy}</p>
      <p><strong>커버리지:</strong> {validation.coverage}</p>
      <p><strong>데이터 품질:</strong> {validation.data_quality}</p>
      {validation.limitation && (
        <p className="text-yellow-600"><strong>제한사항:</strong> {validation.limitation}</p>
      )}
    </div>
  );
}
```

#### 4) 전체 컴포넌트 통합
```tsx
function ChatbotAnswer({ response }: { response: ChatResponse }) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      {/* AI 답변 */}
      <div className="prose" dangerouslySetInnerHTML={{ __html: response.answer }} />

      {/* V2 검증 정보 */}
      {response.validation && (
        <div className="mt-4">
          <ConfidenceBadge validation={response.validation} />
          <ValidationDetails validation={response.validation} />
        </div>
      )}

      {/* V2 출처 정보 */}
      {response.sources && response.sources.length > 0 && (
        <SourcesList sources={response.sources} />
      )}
    </div>
  );
}
```

## 2. Log Analysis V2 API

### 엔드포인트
```
GET /api/v1/logs/{log_id}/analysis?project_uuid={project_uuid}
```

### 응답 구조
```typescript
interface LogAnalysisResponse {
  log_id: number;
  analysis: LogAnalysisResult;
  from_cache: boolean;
  similar_log_id: number | null;
  similarity_score: number | null;

  // V2 추가 필드
  sources: LogSource[] | null;
  validation: ValidationInfo | null;
}
```

### UI 구현 예시

```tsx
function LogAnalysis({ analysis }: { analysis: LogAnalysisResponse }) {
  return (
    <div>
      {/* 분석 결과 */}
      <div className="mb-4">
        <h3>요약: {analysis.analysis.summary}</h3>
        <p>원인: {analysis.analysis.error_cause}</p>
        <p>해결방안: {analysis.analysis.solution}</p>
      </div>

      {/* V2 검증 정보 */}
      {analysis.validation && (
        <div className="bg-gray-50 p-4 rounded">
          <div className="flex items-center justify-between mb-2">
            <span className="font-bold">분석 신뢰도</span>
            <ConfidenceBadge validation={analysis.validation} />
          </div>
          <p className="text-sm text-gray-600">
            {analysis.validation.sample_count}개 로그 기반 분석
            ({analysis.validation.sampling_strategy})
          </p>
        </div>
      )}

      {/* V2 관련 로그 */}
      {analysis.sources && analysis.sources.length > 1 && (
        <div className="mt-4">
          <h4 className="font-bold">관련 로그 ({analysis.sources.length}개)</h4>
          <SourcesList sources={analysis.sources} />
        </div>
      )}
    </div>
  );
}
```

## 3. Document Generation V2 API

### 응답 구조
```typescript
interface AiHtmlDocumentResponse {
  html_content: string;
  metadata: AiDocumentMetadata;
  validation_status: AiValidationStatus;
}

interface AiDocumentMetadata {
  word_count: number;
  estimated_reading_time: string;
  sections_generated: string[];
  generation_time: number;
  health_score: number;
  critical_issues: number;

  // V2 추가 필드
  analysis_metadata: AnalysisMetadata | null;
}

interface AnalysisMetadata {
  generated_at: string;
  data_range: string;
  total_logs_analyzed: number;
  error_logs: number;
  warn_logs: number;
  info_logs: number;
  sample_strategy: Record<string, string>;
  limitations: string[];
}
```

### UI 구현 예시

```tsx
function DocumentMetadata({ metadata }: { metadata: AiDocumentMetadata }) {
  return (
    <div className="bg-gray-50 p-4 rounded mb-4">
      <h4 className="font-bold mb-2">📊 문서 메타데이터</h4>
      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <p>단어 수: {metadata.word_count?.toLocaleString()}</p>
          <p>예상 읽기 시간: {metadata.estimated_reading_time}</p>
          <p>생성 시간: {metadata.generation_time?.toFixed(2)}초</p>
        </div>
        <div>
          <p>건강 점수: {metadata.health_score}/100</p>
          <p>중요 이슈: {metadata.critical_issues}개</p>
        </div>
      </div>

      {/* V2 분석 메타데이터 */}
      {metadata.analysis_metadata && (
        <div className="mt-4 border-t pt-4">
          <h5 className="font-bold mb-2">🔍 분석 데이터 출처</h5>
          <p className="text-sm">기간: {metadata.analysis_metadata.data_range}</p>
          <p className="text-sm">분석 로그: 총 {metadata.analysis_metadata.total_logs_analyzed}개</p>
          <div className="flex gap-4 text-sm">
            <span className="text-red-600">ERROR: {metadata.analysis_metadata.error_logs}</span>
            <span className="text-yellow-600">WARN: {metadata.analysis_metadata.warn_logs}</span>
            <span className="text-blue-600">INFO: {metadata.analysis_metadata.info_logs}</span>
          </div>
          {metadata.analysis_metadata.limitations && (
            <div className="mt-2">
              <p className="text-xs text-gray-600 font-bold">제한사항:</p>
              <ul className="text-xs text-gray-600 list-disc list-inside">
                {metadata.analysis_metadata.limitations.map((limit, i) => (
                  <li key={i}>{limit}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
```

## 4. 신뢰도 기준

- **80% 이상**: 높은 신뢰도 (녹색) - 충분한 데이터 기반
- **60-79%**: 중간 신뢰도 (노란색) - 제한적 데이터
- **60% 미만**: 낮은 신뢰도 (빨간색) - 불충분한 데이터 또는 단일 로그

## 5. 샘플링 전략 설명

| 전략 | 설명 | 신뢰도 |
|------|------|--------|
| `proportional_vector_knn` | ERROR 로그 Vector 검색 (유사도 기반) | 높음 |
| `trace_id_filter` | Trace ID 기반 관련 로그 수집 | 높음 |
| `random_filter` | WARN/INFO 랜덤 샘플링 | 중간 |
| `single_log` | 단일 로그 분석 | 낮음 |
| `aggregation` | 집계 쿼리 (실제 로그 없음) | 낮음 |

## 6. 구현 팁

1. **null 체크**: `sources`와 `validation`은 optional이므로 항상 null 체크
2. **점진적 표시**: 기본 답변을 먼저 보여주고, 검증 정보는 접을 수 있도록 구현
3. **아이콘 사용**: 신뢰도 레벨별로 시각적 아이콘 (✅, ⚠️, ❌) 추가
4. **툴팁**: 샘플링 전략이나 데이터 품질에 마우스 오버 시 상세 설명 표시
5. **로딩 상태**: V2 API는 추가 계산이 필요하므로 응답 시간이 길 수 있음

## 7. 예시 코드 (React + TypeScript)

전체 통합 예시는 `examples/frontend-integration/` 디렉토리 참고
