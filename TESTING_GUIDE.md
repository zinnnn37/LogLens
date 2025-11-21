# HTML 문서 생성 기능 테스트 가이드

## 🚀 Phase 1: 수동 테스트

### 1. 서버 실행

```bash
cd /mnt/c/SSAFY/third_project/AI

# 의존성 설치 (최초 1회)
pip install jinja2==3.1.3

# 서버 실행
uvicorn app.main:app --reload
```

### 2. API 문서 확인

브라우저에서 접속: `http://localhost:8000/docs`

**확인사항:**
- ✅ "Analysis Documents V2" 섹션이 보이는지 확인
- ✅ 2개의 새로운 엔드포인트 확인:
  - `POST /api/v2-langgraph/analysis/projects/html-document`
  - `POST /api/v2-langgraph/analysis/errors/html-document`

### 3. 프로젝트 분석 HTML 생성 테스트

1. `/docs`에서 `POST /api/v2-langgraph/analysis/projects/html-document` 선택
2. "Try it out" 클릭
3. Request body에 다음 샘플 데이터 입력:

```json
{
  "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
  "document_type": "PROJECT_ANALYSIS",
  "format": "HTML",
  "data": {
    "projectInfo": {
      "name": "LogLens",
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "description": "실시간 로그 분석 플랫폼"
    },
    "timeRange": {
      "startTime": "2024-01-01T00:00:00",
      "endTime": "2024-01-31T23:59:59"
    },
    "metrics": {
      "totalLogs": 15000,
      "errorCount": 250,
      "warnCount": 1200,
      "infoCount": 13550,
      "avgResponseTime": 125.5
    },
    "topErrors": [
      {
        "logId": 1,
        "message": "NullPointerException in UserService.getUser()",
        "timestamp": "2024-01-15T10:30:00",
        "componentName": "UserService",
        "logLevel": "ERROR",
        "traceId": "abc123"
      },
      {
        "logId": 2,
        "message": "Database connection timeout",
        "timestamp": "2024-01-16T14:20:00",
        "componentName": "DatabasePool",
        "logLevel": "ERROR",
        "traceId": "def456"
      }
    ]
  },
  "options": {
    "includeCharts": true,
    "darkMode": false,
    "includeComponents": true,
    "includeAlerts": true
  },
  "style_preferences": {
    "css_framework": "tailwind",
    "chart_library": "chartjs",
    "color_scheme": "blue"
  }
}
```

4. "Execute" 클릭
5. **응답 확인:**
   - Status: 200 OK
   - Response body에 `html_content`, `metadata`, `validation_status` 포함
   - `metadata.health_score` 값 확인 (0-100)
   - `validation_status.is_valid_html` = true

6. **HTML 저장 및 확인:**
   - 응답의 `html_content` 값을 복사
   - `project_analysis_test.html` 파일로 저장
   - 브라우저에서 열어서 렌더링 확인

**확인사항:**
- ✅ 프로젝트명 "LogLens" 표시
- ✅ 메트릭 통계 (총 로그 15,000개, 에러 250개 등)
- ✅ 건강 점수 표시
- ✅ Chart.js 차트 (도넛 차트)
- ✅ 최근 에러 로그 목록
- ✅ 권장사항 섹션

### 4. 에러 분석 HTML 생성 테스트

1. `/docs`에서 `POST /api/v2-langgraph/analysis/errors/html-document` 선택
2. "Try it out" 클릭
3. Request body에 다음 샘플 데이터 입력:

```json
{
  "log_id": 12345,
  "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
  "document_type": "ERROR_ANALYSIS",
  "format": "HTML",
  "data": {
    "errorLog": {
      "logId": 12345,
      "level": "ERROR",
      "message": "NullPointerException in UserService.getUser()",
      "stackTrace": "java.lang.NullPointerException: Cannot invoke method getEmail() on null object\n\tat com.example.service.UserService.getUser(UserService.java:45)\n\tat com.example.controller.UserController.getUserInfo(UserController.java:28)\n\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)",
      "timestamp": "2024-01-15T10:35:00.123Z",
      "componentName": "UserService",
      "traceId": "abc123-def456-ghi789"
    },
    "existingAnalysis": {
      "summary": "UserService의 getUser() 메서드에서 NullPointerException이 발생했습니다.",
      "errorCause": "user_id=12345에 해당하는 User 객체가 데이터베이스에서 조회되지 않아 null이 반환되었으며, 이를 검증 없이 사용하려다 예외가 발생했습니다. 근본 원인은 사용자 존재 여부에 대한 사전 검증 로직이 누락되었기 때문입니다.",
      "solution": "[즉시] UserService.getUser() 호출 전 null 체크를 추가하거나 Optional<User> 반환 타입으로 변경하세요.\n[단기] 존재하지 않는 user_id 요청 시 명시적인 UserNotFoundException을 발생시키도록 리팩토링하세요.\n[장기] API 레이어에서 user_id 유효성 검증 로직을 추가하세요.",
      "tags": ["SEVERITY_HIGH", "NullPointerException", "UserService", "DataIntegrity"]
    },
    "relatedLogs": [
      {
        "logId": 12346,
        "message": "User not found in database: userId=12345",
        "timestamp": "2024-01-15T10:34:59.998Z",
        "componentName": "UserRepository",
        "logLevel": "WARN"
      },
      {
        "logId": 12347,
        "message": "API request /users/12345 received",
        "timestamp": "2024-01-15T10:34:59.850Z",
        "componentName": "UserController",
        "logLevel": "INFO"
      }
    ]
  },
  "options": {
    "includeRelatedLogs": true,
    "includeImpactAnalysis": true,
    "includeSimilarErrors": false,
    "includeCodeExamples": false,
    "maxRelatedLogs": 10
  },
  "style_preferences": {
    "css_framework": "tailwind",
    "chart_library": "chartjs",
    "color_scheme": "red"
  }
}
```

4. "Execute" 클릭
5. **응답 확인:**
   - Status: 200 OK
   - `metadata.severity` = "HIGH"
   - `metadata.root_cause` 포함
   - `validation_status.is_valid_html` = true

6. **HTML 저장 및 확인:**
   - 응답의 `html_content` 복사
   - `error_analysis_test.html` 파일로 저장
   - 브라우저에서 열어서 렌더링 확인

**확인사항:**
- ✅ 로그 ID "12345" 표시
- ✅ 에러 레벨 배지 "ERROR" (빨간색)
- ✅ AI 분석 결과 (요약, RCA, 해결방안)
- ✅ 스택 트레이스 표시
- ✅ 관련 로그 2개 표시
- ✅ 영향 분석 섹션
- ✅ 조치 권장사항

### 5. 다크모드 테스트

프로젝트 분석 요청에서 `options.darkMode`를 `true`로 변경하고 다시 테스트:

```json
{
  ...
  "options": {
    "darkMode": true,
    "includeCharts": true
  }
}
```

**확인사항:**
- ✅ 배경색이 어두운 색상 (#1a1a1a)
- ✅ 텍스트가 밝은 색상 (#e0e0e0)

---

## 🧪 Phase 2: pytest 자동화 테스트

### 1. pytest 설치

```bash
pip install pytest pytest-asyncio httpx
```

### 2. 개별 테스트 파일 실행

```bash
# 서비스 단위 테스트
pytest app/tests/test_html_document_service.py -v

# API 엔드포인트 테스트
pytest app/tests/test_analysis_api.py -v

# 템플릿 렌더링 테스트
pytest app/tests/test_templates.py -v
```

### 3. 전체 테스트 실행

```bash
# 모든 테스트 실행
pytest app/tests/ -v

# 커버리지와 함께 실행
pytest app/tests/ -v --cov=app/services/html_document_service --cov=app/api/v2_langgraph/analysis --cov-report=html
```

### 4. 예상 결과

```
test_html_document_service.py::test_generate_project_analysis_html PASSED
test_html_document_service.py::test_generate_error_analysis_html PASSED
test_html_document_service.py::test_metadata_generation_project PASSED
test_html_document_service.py::test_health_score_calculation PASSED
...

test_analysis_api.py::test_generate_project_analysis_html_success PASSED
test_analysis_api.py::test_invalid_document_type PASSED
...

test_templates.py::test_template_renders PASSED
test_templates.py::test_required_html_tags PASSED
...

======================== XX passed in X.XXs ========================
```

---

## 📊 테스트 체크리스트

### ✅ 서비스 레이어
- [x] 프로젝트 분석 HTML 생성
- [x] 에러 분석 HTML 생성
- [x] 건강 점수 계산
- [x] 메타데이터 생성
- [x] HTML 검증
- [x] 다크모드 옵션
- [x] 차트 옵션
- [x] 재생성 피드백

### ✅ API 엔드포인트
- [x] 프로젝트 분석 엔드포인트 (200)
- [x] 에러 분석 엔드포인트 (200)
- [x] 잘못된 document_type (400)
- [x] 필수 필드 누락 (422)
- [x] 응답 스키마 검증

### ✅ 템플릿
- [x] 프로젝트 분석 템플릿 렌더링
- [x] 에러 분석 템플릿 렌더링
- [x] 필수 HTML 태그
- [x] Tailwind CSS 로딩
- [x] Chart.js 로딩 (옵션)
- [x] 데이터 바인딩
- [x] 다크모드 스타일
- [x] 엣지 케이스 (빈 데이터, None 값)

---

## 🐛 트러블슈팅

### 문제: jinja2 모듈을 찾을 수 없음

```bash
pip install jinja2==3.1.3
```

### 문제: 템플릿 파일을 찾을 수 없음

템플릿 파일 위치 확인:
```bash
ls -la app/templates/analysis/
# project_analysis.html
# error_analysis.html
```

### 문제: 테스트 실패

1. 서버가 실행 중이면 종료 (API 테스트는 TestClient 사용)
2. Python 경로 확인
3. 의존성 재설치

---

## 📝 다음 단계

1. ✅ 모든 테스트 통과 확인
2. ✅ 생성된 HTML 파일 브라우저 렌더링 확인
3. ✅ BE 프로젝트에서 실제 API 호출 테스트
4. ✅ PDF 변환 테스트 (BE에서 수행)
5. ✅ 프로덕션 배포 전 성능 테스트
