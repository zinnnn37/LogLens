# LogLens

TraceID 기반 함수 호출 추적 및 성능 측정 라이브러리

## 설치

```bash
npm install https://www.loglens.co.kr/static/libs/soo1-loglens-0.0.9.tgz
```

## 시작하기

### 1단계: 라이브러리 임포트

```ts
import { initLogLens, loglens, withLogLens, useLogLens } from 'soo1-loglens';
```

### 2단계: 초기화

앱 시작 시 `initLogLens()`를 호출하여 라이브러리를 초기화하세요.

```ts
// React 앱의 경우 (main.tsx 또는 App.tsx)
initLogLens({
  domain: "https://your-domain.com",  // 백엔드 도메인 (api/logs/frontend로 로그 전송)
  maxLogs: 1000,                       // 최대 로그 보관 개수
  autoFlushEnabled: true,              // 자동 로그 전송 여부
  autoFlushInterval: 30000,            // 30초마다 전송
  captureErrors: true                  // 에러 자동 수집
});
```

#### 초기화 옵션

```ts
{
  domain: string;             // 필수: 로그 수집 서버 도메인
  maxLogs?: number;           // 기본값: 1000
  autoFlushEnabled?: boolean; // 기본값: true
  autoFlushInterval?: number; // 기본값: 30000 (30초)
  captureErrors?: boolean;    // 기본값: true
}
```

### 3단계: 함수 래핑

추적하고 싶은 **최상위 함수**를 `withLogLens()`로 감싸세요.
**최상위 함수만 래핑하면 내부에서 호출되는 함수들에도 자동으로 traceId가 전파됩니다.**

#### 일반 함수 래핑

```ts
// 동기 함수
const processData = withLogLens(
  function processData(data: any) {
    // 로직...
    return result;
  },
  { logger: 'DataService.processData' }
);

// 비동기 함수 (자동 지원)
const fetchUserData = withLogLens(
  async function fetchUserData(userId: string) {
    const response = await fetch(`/api/users/${userId}`);
    return response.json();
  },
  { logger: 'API.fetchUserData' }
);

// 화살표 함수
const calculateTotal = withLogLens(
  (items: Item[]) => {
    return items.reduce((sum, item) => sum + item.price, 0);
  },
  { logger: 'Cart.calculateTotal' }
);
```

#### React Hook 사용

React 컴포넌트에서는 `useLogLens` Hook을 사용하세요.

```tsx
import { useLogLens } from 'soo1-loglens';

function UserProfile({ userId }: { userId: string }) {
  // 기본 사용법
  const fetchUser = useLogLens(
    async () => {
      const response = await fetch(`/api/users/${userId}`);
      return response.json();
    },
    { logger: 'UserProfile.fetchUser' },
    [userId]  // 의존성 배열
  );

  // 옵션 포함
  const updateUser = useLogLens(
    async (userData: UserData) => {
      const response = await fetch(`/api/users/${userId}`, {
        method: 'PUT',
        body: JSON.stringify(userData),
      });
      return response.json();
    },
    {
      logger: 'UserProfile.updateUser',
      includeArgs: true,  // 인자 로깅
      includeResult: true, // 결과 로깅
      level: 'INFO'
    },
    [userId]
  );

  return (
    <div>
      <button onClick={() => fetchUser()}>Load User</button>
      <button onClick={() => updateUser({ name: 'John' })}>Update</button>
    </div>
  );
}
```

#### 의존성 배열(deps) 이해하기

`useLogLens`의 세 번째 인자는 **의존성 배열**입니다. 이는 React의 `useMemo`/`useCallback`과 동일한 원리로 동작합니다.

**왜 필요한가?**

함수 내부에서 **외부 변수**(props, state 등)를 참조할 때, 해당 변수가 변경되면 함수도 새로운 값을 반영하도록 재생성해야 합니다. 의존성 배열에 이러한 변수들을 명시하면, 변수가 변경될 때만 함수를 재생성하고, 그렇지 않으면 이전 함수를 재사용하여 성능을 최적화합니다.

**Stale Closure 문제**

```tsx
function UserComponent({ userId }: { userId: string }) {
  // ❌ 잘못된 예: deps가 비어있음
  const fetchUser = useLogLens(
    async () => {
      // userId가 변경되어도 항상 처음 마운트될 때의 값만 사용됨 (오래된 클로저)
      console.log(userId);  // 예: 항상 "user-1"만 출력
      const response = await fetch(`/api/users/${userId}`);
      return response.json();
    },
    { logger: 'fetchUser' },
    []  // ⚠️ 빈 배열 = 한 번만 생성되고 절대 재생성되지 않음
  );

  // ✅ 올바른 예: userId를 deps에 포함
  const fetchUserCorrect = useLogLens(
    async () => {
      // userId가 변경될 때마다 함수가 재생성되어 항상 최신 값을 사용
      console.log(userId);  // userId 변경에 따라 "user-1", "user-2", ... 출력
      const response = await fetch(`/api/users/${userId}`);
      return response.json();
    },
    { logger: 'fetchUser' },
    [userId]  // ✅ userId가 변경되면 함수 재생성
  );

  return <button onClick={fetchUserCorrect}>Load User</button>;
}
```

**의존성 배열 작성 규칙**

```tsx
function MyComponent({ productId, userId }: Props) {
  const [filter, setFilter] = useState('all');

  // 1. 외부 변수를 사용하지 않는 경우 → 빈 배열
  const staticFunction = useLogLens(
    () => {
      console.log('Hello');  // 외부 변수 참조 없음
    },
    { logger: 'static' },
    []  // 한 번만 생성
  );

  // 2. props를 사용하는 경우 → 해당 props를 deps에 포함
  const fetchProduct = useLogLens(
    async () => {
      const res = await fetch(`/api/products/${productId}`);
      return res.json();
    },
    { logger: 'fetchProduct' },
    [productId]  // productId 변경 시 재생성
  );

  // 3. 여러 외부 변수를 사용하는 경우 → 모두 deps에 포함
  const fetchFilteredData = useLogLens(
    async () => {
      const res = await fetch(`/api/users/${userId}/products/${productId}?filter=${filter}`);
      return res.json();
    },
    { logger: 'fetchFilteredData' },
    [userId, productId, filter]  // 셋 중 하나라도 변경되면 재생성
  );

  // 4. 함수 파라미터만 사용하는 경우 → 빈 배열
  const deleteProduct = useLogLens(
    async (id: string) => {  // 인자로 받은 id만 사용
      await fetch(`/api/products/${id}`, { method: 'DELETE' });
    },
    { logger: 'deleteProduct' },
    []  // 외부 변수 참조 없음
  );

  return <div>{/* ... */}</div>;
}
```

**일반적인 규칙**

- 함수 내부에서 **사용하는 모든 외부 변수**를 deps에 넣으세요
- **함수 파라미터**는 deps에 넣지 않아도 됩니다 (매번 호출 시 새로 받으므로)
- **외부 변수를 전혀 사용하지 않으면** `[]` (빈 배열)을 사용하세요
- ESLint의 `exhaustive-deps` 규칙을 활성화하면 자동으로 경고해줍니다

#### withLogLens 옵션

```ts
{
  // ⚠️ 함수 이름 - logger에 넣어주는 것을 강력히 권장합니다
  // 난독화된 코드에서는 함수 이름 추출이 제대로 되지 않습니다.
  // 추후 자동 추출 개선 가능성 있음
  logger?: string;  // 예: 'Service.functionName', 'Component.handleClick'

  // 함수 이름 (logger가 없을 때 사용됨)
  name?: string;

  // 로그 레벨
  level?: 'INFO' | 'WARN' | 'ERROR';  // 기본값: 'INFO'

  // 함수 인자 로깅 여부
  includeArgs?: boolean;  // 기본값: false

  // 함수 결과 로깅 여부
  includeResult?: boolean;  // 기본값: false

  // 추가 컨텍스트 데이터
  context?: Record<string, any>;

  // 로그 비활성화 (traceId만 전파)
  skipLog?: boolean;  // 기본값: false
}
```

## 주요 기능

### 1. TraceID 자동 전파

**최상위 함수만 래핑하면 내부 호출 체인에 자동으로 traceId가 전파됩니다.**

```ts
// 내부 함수들 (래핑 불필요)
async function fetchFromDB(id: string) {
  // DB 조회...
}

async function processResult(data: any) {
  // 데이터 가공...
}

// 최상위 함수만 래핑
const getUserData = withLogLens(
  async function getUserData(userId: string) {
    const rawData = await fetchFromDB(userId);     // traceId 자동 전파
    const processed = await processResult(rawData); // traceId 자동 전파
    return processed;
  },
  { logger: 'UserService.getUserData' }
);
```

### 2. 비동기 처리 자동 지원

Promise, async/await를 사용하는 비동기 함수도 자동으로 추적됩니다.

```ts
const asyncOperation = withLogLens(
  async () => {
    await delay(1000);
    const result = await fetch('/api/data');
    return result.json();
  },
  { logger: 'Service.asyncOperation' }
);
```

### 3. 성능 측정

각 함수의 실행 시간이 자동으로 측정되어 콘솔에 표시됩니다.

```
[ INFO] → UserService.getUserData (  256ms)
```

### 4. 직접 로깅 메서드

`withLogLens`로 래핑하지 않고도 직접 로그를 남길 수 있습니다.

```ts
import { loglens } from 'soo1-loglens';

function processOrder(orderId: string) {
  loglens.info('주문 처리 시작', { orderId });

  try {
    // 주문 처리 로직...
    const result = validateOrder(orderId);

    if (result.warnings.length > 0) {
      loglens.warn('주문 검증 경고', {
        orderId,
        warnings: result.warnings
      });
    }

    loglens.info('주문 처리 완료', { orderId, result });
    return result;

  } catch (error) {
    loglens.error('주문 처리 실패', {
      orderId,
      error: error.message
    });
    throw error;
  }
}
```

**메서드**

- `loglens.info(message, context?)` - 일반 정보 로깅
- `loglens.warn(message, context?)` - 경고 로깅
- `loglens.error(message, context?)` - 에러 로깅

**파라미터**

- `message`: 로그 메시지 (string)
- `context`: 추가 컨텍스트 데이터 (선택사항, object)

**활용 시나리오**

```tsx
// React 컴포넌트에서
function UserDashboard() {
  useEffect(() => {
    loglens.info('대시보드 마운트됨', {
      timestamp: new Date().toISOString()
    });

    return () => {
      loglens.info('대시보드 언마운트됨');
    };
  }, []);

  const handleDelete = async (id: string) => {
    loglens.warn('사용자 삭제 시도', { userId: id });

    try {
      await deleteUser(id);
      loglens.info('사용자 삭제 성공', { userId: id });
    } catch (error) {
      loglens.error('사용자 삭제 실패', {
        userId: id,
        error: error.message
      });
    }
  };

  return <div>{/* ... */}</div>;
}
```

```ts
// 조건부 로깅
function fetchUserData(userId: string) {
  const cacheKey = `user:${userId}`;
  const cached = cache.get(cacheKey);

  if (cached) {
    loglens.info('캐시에서 데이터 반환', { userId, cacheKey });
    return cached;
  }

  loglens.warn('캐시 미스 - DB 조회', { userId });
  return fetchFromDB(userId);
}
```

### 5. 민감 데이터 마스킹

환경변수를 통해 마스킹할 필드를 지정할 수 있습니다.

#### 환경변수 설정 (React)

```env
# .env 파일
VITE_LOGLENS_MASK_FIELDS=password,token,accessToken,refreshToken,secret,ssn,cardNumber
```

```ts
// vite.config.ts 또는 앱 초기화 시
initLogLens({
  domain: "https://your-domain.com",
  maskFields: import.meta.env.VITE_LOGLENS_MASK_FIELDS?.split(',') || [
    'password',
    'token',
    'accessToken',
    'refreshToken',
    'secret',
    'ssn',
    'cardNumber'
  ]
});
```

#### 마스킹 기본값
```ts
RegExp[] = [
    /^.*(password|passwd|pwd|pass|pw|secret).*$/i,
    /^.*(token|auth|authorization|bearer).*$/i,
    /^.*(api[_-]?key|access[_-]?key|secret[_-]?key).*$/i,
    /^.*(번호|number|no|ssn|security).*$/i,
    /^.*(card|cvv|cvc|pin).*$/i,
  ];
```

#### 마스킹 예시

```ts
const login = withLogLens(
  async (email: string, password: string) => {
    return await api.post('/auth/login', { email, password });
  },
  {
    logger: 'Auth.login',
    includeArgs: true  // password는 자동 마스킹됨
  }
);

// 로그 출력: { email: 'user@example.com', password: '***' }
```

## 중요 주의사항

### ⚠️ 1. logger 옵션 필수 권장

**함수 이름을 추출하는 기능이 있지만, 난독화된 코드에서는 제대로 동작하지 않습니다.**
모든 함수에 `logger` 옵션을 명시적으로 지정하는 것을 강력히 권장합니다.

```ts
// ❌ 나쁜 예 - 난독화 시 함수 이름이 'a' 또는 'anonymous'로 표시됨
const fetchData = withLogLens(async () => { ... });

// ✅ 좋은 예 - logger를 명시적으로 지정
const fetchData = withLogLens(
  async () => { ... },
  { logger: 'DataService.fetchData' }
);
```

**참고:** 함수 이름 자동 추출 기능은 추후 개선될 가능성이 있습니다.

### ⚠️ 2. React Hook에서 logger 필수

```tsx
// ⚠️ 함수 이름 확인 어려움
const handleSubmit = useLogLens(
  async (data) => { ... },
  {},  // logger 없음
  [deps]
);

// ✅ 정확한 함수 이름 추출 가능
const handleSubmit = useLogLens(
  async (data) => { ... },
  { logger: 'MyComponent.handleSubmit' },
  [deps]
);
```

### ⚠️ 3. 민감한 데이터 주의

`includeArgs: true` 또는 `includeResult: true` 사용 시 반드시 마스킹 설정을 확인하세요.

```ts
// 민감한 데이터가 포함된 함수
const processPayment = withLogLens(
  async (cardNumber: string, cvv: string) => {
    // ...
  },
  {
    logger: 'Payment.processPayment',
    includeArgs: true // ⚠️ 결제 관련 민감 데이터 마스킹 패턴에 있는지 확인
  }
);
```

### ⚠️ 4. 초기화 필수

앱 시작 시 반드시 `initLogLens()`를 호출하세요. 초기화하지 않으면 로그가 수집되지 않습니다.

### ⚠️ 5. 메모리 관리

`maxLogs` 설정을 통해 메모리 사용량을 제한하세요.

## 실전 예제

### React 컴포넌트 전체 예제

```tsx
import { useLogLens } from 'soo1-loglens';
import { useState } from 'react';

function TodoList() {
  const [todos, setTodos] = useState([]);

  // 데이터 조회
  const fetchTodos = useLogLens(
    async () => {
      const response = await fetch('/api/todos');
      const data = await response.json();
      setTodos(data);
      return data;
    },
    { logger: 'TodoList.fetchTodos' },
    []
  );

  // 새 할일 추가
  const addTodo = useLogLens(
    async (title: string) => {
      const response = await fetch('/api/todos', {
        method: 'POST',
        body: JSON.stringify({ title }),
      });
      const newTodo = await response.json();
      setTodos([...todos, newTodo]);
      return newTodo;
    },
    {
      logger: 'TodoList.addTodo',
      includeArgs: true,
      includeResult: true
    },
    [todos]
  );

  // 할일 삭제
  const deleteTodo = useLogLens(
    async (id: string) => {
      await fetch(`/api/todos/${id}`, { method: 'DELETE' });
      setTodos(todos.filter(t => t.id !== id));
    },
    { logger: 'TodoList.deleteTodo' },
    [todos]
  );

  return (
    <div>
      <button onClick={fetchTodos}>Load Todos</button>
      {/* ... */}
    </div>
  );
}
```

### 서비스 레이어 예제

```ts
// services/userService.ts
import { withLogLens } from 'soo1-loglens';

class UserService {
  // 최상위 메서드만 래핑
  fetchUser = withLogLens(
    async (userId: string) => {
      const user = await this.getUserFromDB(userId);
      const profile = await this.enrichUserProfile(user);
      return profile;
    },
    { logger: 'UserService.fetchUser' }
  );

  updateUser = withLogLens(
    async (userId: string, data: UserData) => {
      await this.validateUserData(data);
      const updated = await this.saveUserToDB(userId, data);
      return updated;
    },
    {
      logger: 'UserService.updateUser',
      includeArgs: true  // data에 민감정보 없는지 확인!
    }
  );

  // 내부 메서드들 (래핑 불필요 - traceId 자동 전파)
  private async getUserFromDB(userId: string) { /* ... */ }
  private async enrichUserProfile(user: User) { /* ... */ }
  private async validateUserData(data: UserData) { /* ... */ }
  private async saveUserToDB(userId: string, data: UserData) { /* ... */ }
}
```

## API 레퍼런스

### 전역 함수

- `initLogLens(config)` - 라이브러리 초기화
- `withLogLens(fn, options?)` - 함수 래핑
- `useLogLens(fn, options?, deps?)` - React Hook

### loglens 객체

```ts
// 로그 조회
loglens.getLogs()                    // 모든 로그
loglens.getLogsByTraceId(traceId)    // 특정 traceId 로그

// 로그 관리
loglens.clear()                       // 로그 초기화
loglens.flush()                       // 즉시 전송
loglens.stopAutoFlush()               // 자동 전송 중지

// 상태 확인
loglens.getStatus()                   // 현재 상태 조회
```

## 백엔드 연동

Spring Boot 예제:

```java
@RestController
@RequestMapping("/api/logs")
public class LogController {

    @PostMapping("/frontend")
    public ResponseEntity<Void> collectFrontendLogs(
        @RequestBody List<LogEntry> logs
    ) {
        // LogLens Gradle 라이브러리가 자동 처리
        // 또는 커스텀 로직 구현
        return ResponseEntity.ok().build();
    }
}
```

## 콘솔 출력 형식

```
[ INFO] → UserService.fetchUser (  256ms)
[ INFO] → API.fetchUserData (  145ms)
[ERROR] → Payment.processPayment (   89ms)
```

- `→`: 함수 시작
- `(XXXms)`: 실행 시간
- `[ INFO]`, `[WARN]`, `[ERROR]`: 로그 레벨

## 문제 해결

**Q. 로그가 수집되지 않아요**
A. `initLogLens()`를 앱 시작 시 호출했는지 확인하세요.

**Q. 함수 이름이 'anonymous'나 이상한 문자로 나와요**
A. `logger` 옵션을 명시적으로 지정하세요. 난독화된 코드에서는 함수 이름 추출이 제대로 되지 않습니다.

## 라이선스

MIT

