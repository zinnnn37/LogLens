# LogLens

TraceID 기반 함수 호출 추적 및 성능 측정 라이브러리

## 설치

```bash
npm install soo1-loglens
```

## 시작하기

### 1단계: 초기화

앱 시작 시 `LogCollector.init()`을 호출해주세요

```ts
// 기본값
{
  maxLogs: 1000,     // 최대 로그 갯수
  autoFlush: {
    enabled: false,  // 자동 로그 전송 여부
    interval: 60000, // 1초마다 전송
    endpoint: '',    // 로그 분석 API 엔드포인트
  },
};
```

### 2단계: 함수 래핑

추적하고 싶은 함수를 `withLogLens()`로 감싸세요.
다양한 옵션을 추가할 수 있습니다.

```ts
options = {
  // 함수의 이름. 로그에 표시될 함수명을 지정
  // 기본값: fn.name (함수 자체의 name 속성)
  // ⚠️ 익명 함수의 경우 'function'으로 표시됨
  name?: string;

  // 로그의 중요도 레벨
  // INFO: 일반 정보, WARN: 경고, ERROR: 에러
  // 기본값: INFO
  level?: LogLevel;

  // 로그에 표시될 로거 이름 (함수가 어디서 호출되었는지 식별용)
  // 예: 'API.fetchUser', 'Service.processData'
  // 기본값: fn.name
  // ⚠️ 익명 함수의 경우 'function'으로 표시됨
  logger?: string;

  // 함수에 전달된 인자를 로그에 포함할지 여부
  // true로 설정하면 함수의 파라미터 값이 로그에 기록됨
  // 기본값: false
  // ⚠️ 비밀번호 같은 민감한 데이터 주의
  includeArgs?: boolean;

  // 함수의 반환값을 로그에 포함할지 여부
  // true로 설정하면 함수가 반환한 결과가 로그에 기록됨
  // 기본값: false
  // ⚠️ 민감한 데이터 주의
  includeResult?: boolean;

  // 로그에 추가로 포함할 커스텀 데이터
  // 예: { userId: '123', service: 'payment' }
  // 로그 분석 시 필터링이나 그룹핑에 활용
  context?: Record<string, any>;

  // true로 설정하면 로그 분석을 하지 않음
  // 기본값: false
  skipLog?: boolean;
};
```

### 3단계: 실행

함수를 호출하면 자동으로 로그가 수집되고 콘솔에 출력됩니다.

## 주요 기능

**TraceID 기반 추적**  
중첩된 함수 호출을 하나의 TraceID로 연결하여 전체 실행 흐름을 추적합니다.

**성능 측정**  
각 함수의 실행 시간을 밀리초 단위로 자동 측정합니다.

**시각적 콘솔 출력**  
들여쓰기와 색상으로 함수 호출 구조를 명확하게 표시합니다.

**로그 수집 및 관리**  
JSON 형태로 구조화된 로그를 메모리에 저장하고 조회, 전송, 초기화할 수 있습니다.

**에러 자동 수집**  
함수 실행 중 발생한 에러를 자동으로 로깅합니다.

## 중요 주의사항

### ⚠️ 1. name 혹은 logger 옵션 지정

옵션 없이 사용하면 익명 함수의 이름이 모두 `anonymous` 혹은 `function`으로 표시됩니다.  
원활한 디버깅을 위해 `{ name: '함수이름', logger: '분류.함수이름' }` 옵션을 전달하세요.

```ts
const function1 = withLogLens(
  async (param: Param) => {
    const data = await fetchById(id);
    return { data };
  },
  {
    name: 'function1',
    logger: 'Service.function1'
  },
);
```

### ⚠️ 2. LogCollector.init() 호출

초기화하지 않으면 콘솔에 로그가 출력되지 않습니다.  
앱 시작 시 반드시 `LogCollector.init()`를 호출하세요.
기본값은 위를 참고해주세요.

### ⚠️ 3. 메모리 관리

`maxLogs` 설정 없이 사용하면 로그가 무한정 쌓입니다.  
`maxLogs`를 설정하거나 주기적으로 `LogCollector.clear()`를 호출하세요.

### ⚠️ 4. 민감한 데이터 주의

`includeArgs: true` 또는 `includeResult: true` 사용 시 함수의 인자와 결과가 로그에 기록됩니다.  
비밀번호, 토큰 등 민감한 데이터가 포함된 함수에는 사용하지 마세요.

### ⚠️ 5. 성능 오버헤드

모든 함수를 래핑하면 성능에 영향을 줄 수 있습니다.  
중요한 함수만 선택적으로 래핑하세요.

## 옵션

**withLogLens 옵션**
- `logger`: 로거 이름 (필수 권장)
- `level`: 로그 레벨 (`INFO`, `WARN`, `ERROR`)
- `includeArgs`: 함수 인자 로깅 여부
- `includeResult`: 함수 결과 로깅 여부
- `skipLog`: 로그 없이 TraceID만 관리
- `context`: 추가 컨텍스트 정보

**LogCollector.init 설정**
- `maxLogs`: 최대 로그 보관 개수
- `enableConsole`: 콘솔 출력 여부
- `autoFlush`: 자동 로그 전송 설정
  - `enabled`: 자동 전송 활성화
  - `interval`: 전송 주기 (밀리초)
  - `endpoint`: 전송할 API 엔드포인트

## API

**LogCollector 메서드**
- `init(config)`: 초기화
- `getLogs()`: 모든 로그 조회
- `getLogsByTraceId(traceId)`: 특정 TraceID 로그만 조회
- `clear()`: 로그 초기화
- `flush()`: 로그 전송
- `getStatus()`: 현재 상태 조회
- `stopAutoFlush()`: 자동 전송 중지

## 출력 형식

```
[ INFO] → FunctionName (  156ms)      # 최상위 함수
[ INFO] |→ NestedFunction (   52ms)   # 중첩된 함수
[ERROR] → FailedFunction (   12ms)    # 에러 발생
```

**표기 의미**
- `→`: 최상위 호출
- `|→`: 중첩된 호출 (깊이만큼 `|` 추가)
- 괄호 안 숫자: 실행 시간 (밀리초)
- 색상: INFO(검정), WARN(노랑), ERROR(빨강)

## 문제 해결

**모든 로그가 'anonymous' || 'function'으로 나와요**  
→ 무기명 함수의 경우 `function.name`으로 함수 이름을 가져올 수 없습니다
  `withLogLens(fn, { name: '이름', logger: '분류.이름' })` 옵션을 추가해주세요

**메모리가 계속 증가해요**  
→ `maxLogs` 설정 또는 주기적으로 `LogCollector.clear()`를 호출해주세요

**중첩 호출이 추적되지 않아요**  
→ 모든 함수를 `withLogLens`로 래핑했는지 확인해주세요
