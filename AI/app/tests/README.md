# Tests

HTML 문서 생성 기능에 대한 종합 테스트 스위트입니다.

## 📁 파일 구조

```
app/tests/
├── __init__.py                         # 테스트 패키지 초기화
├── conftest.py                         # pytest 설정 및 공유 픽스처
├── README.md                           # 이 파일
├── test_html_document_service.py       # 서비스 레이어 단위 테스트
├── test_analysis_api.py                # API 엔드포인트 통합 테스트
└── test_templates.py                   # Jinja2 템플릿 렌더링 테스트
```

## 🧪 테스트 범위

### 1. test_html_document_service.py (22개 테스트)

**서비스 레이어 단위 테스트:**
- HTML 문서 생성 (프로젝트/에러 분석)
- 메타데이터 생성 및 검증
- 건강 점수 계산 알고리즘
- HTML 검증 로직
- 옵션 처리 (다크모드, 차트)
- 재생성 피드백
- 유틸리티 함수 (포맷팅, 계산)
- 엣지 케이스 처리

### 2. test_analysis_api.py (15개 테스트)

**API 엔드포인트 통합 테스트:**
- 프로젝트 분석 엔드포인트
- 에러 분석 엔드포인트
- 요청/응답 검증
- 에러 처리 (400, 422)
- 스키마 검증
- 옵션별 동작 확인

### 3. test_templates.py (24개 테스트)

**템플릿 렌더링 테스트:**
- 템플릿 존재 확인
- HTML 구조 검증
- 데이터 바인딩
- CSS/JS 라이브러리 로딩
- 스타일 옵션 (다크모드, 색상 테마)
- 조건부 렌더링
- 엣지 케이스 (빈 데이터, None 값)

## 🚀 실행 방법

### 전체 테스트 실행

```bash
pytest app/tests/ -v
```

### 개별 파일 실행

```bash
# 서비스 테스트만
pytest app/tests/test_html_document_service.py -v

# API 테스트만
pytest app/tests/test_analysis_api.py -v

# 템플릿 테스트만
pytest app/tests/test_templates.py -v
```

### 특정 테스트 함수만 실행

```bash
pytest app/tests/test_html_document_service.py::test_generate_project_analysis_html -v
```

### 커버리지 측정

```bash
pytest app/tests/ --cov=app/services/html_document_service --cov=app/api/v2_langgraph/analysis --cov-report=html
```

커버리지 리포트는 `htmlcov/index.html`에서 확인 가능합니다.

### 특정 마커로 실행

```bash
# asyncio 테스트만
pytest app/tests/ -v -m asyncio

# 느린 테스트 제외
pytest app/tests/ -v -m "not slow"
```

## 📊 테스트 커버리지 목표

- 서비스 레이어: 85% 이상
- API 엔드포인트: 90% 이상
- 템플릿 렌더링: 80% 이상

## 🔧 픽스처 (Fixtures)

### 공통 픽스처 (conftest.py)
- `test_data_dir`: 테스트 데이터 디렉토리 경로

### 서비스 테스트 픽스처
- `html_service`: HtmlDocumentService 인스턴스
- `project_analysis_request`: 프로젝트 분석 요청 샘플
- `error_analysis_request`: 에러 분석 요청 샘플

### API 테스트 픽스처
- `client`: FastAPI TestClient
- `project_analysis_payload`: API 요청 페이로드
- `error_analysis_payload`: 에러 분석 페이로드

### 템플릿 테스트 픽스처
- `jinja_env`: Jinja2 Environment
- `project_template_context`: 프로젝트 템플릿 컨텍스트
- `error_template_context`: 에러 템플릿 컨텍스트

## 🐛 테스트 작성 가이드

### 1. 명명 규칙
- 테스트 파일: `test_*.py`
- 테스트 클래스: `Test*`
- 테스트 함수: `test_*`

### 2. 테스트 구조 (AAA 패턴)
```python
def test_example():
    # Arrange (준비)
    service = HtmlDocumentService()
    request = create_sample_request()

    # Act (실행)
    result = await service.generate_html_document(request)

    # Assert (검증)
    assert result.html_content is not None
```

### 3. 비동기 테스트
```python
@pytest.mark.asyncio
async def test_async_function():
    result = await some_async_function()
    assert result is not None
```

### 4. 예외 테스트
```python
def test_exception():
    with pytest.raises(ValueError):
        invalid_operation()
```

## 📝 CI/CD 통합

GitHub Actions 예시:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v2

    - name: Set up Python
      uses: actions/setup-python@v2
      with:
        python-version: '3.11'

    - name: Install dependencies
      run: |
        pip install -r requirements.txt
        pip install pytest pytest-asyncio pytest-cov httpx

    - name: Run tests
      run: |
        pytest app/tests/ -v --cov --cov-report=xml

    - name: Upload coverage
      uses: codecov/codecov-action@v2
```

## 🔍 디버깅

### 로그 출력
```bash
pytest app/tests/ -v -s  # -s 옵션으로 print 출력 보기
```

### 실패한 테스트만 재실행
```bash
pytest app/tests/ --lf  # last failed
```

### PDB 디버거 사용
```bash
pytest app/tests/ --pdb  # 실패 시 pdb 진입
```

### 특정 테스트만 상세 출력
```bash
pytest app/tests/test_html_document_service.py::test_health_score_calculation -vv
```

## ✅ 테스트 체크리스트

새로운 기능 추가 시:

- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] 엣지 케이스 테스트
- [ ] 에러 처리 테스트
- [ ] 문서화 (docstring)
- [ ] 커버리지 80% 이상 유지

## 📚 참고 자료

- [pytest 공식 문서](https://docs.pytest.org/)
- [pytest-asyncio](https://pytest-asyncio.readthedocs.io/)
- [FastAPI Testing](https://fastapi.tiangolo.com/tutorial/testing/)
- [Jinja2 Testing](https://jinja.palletsprojects.com/en/3.1.x/api/#jinja2.Environment)
