import { useState } from 'react';
import CodeBlock from '@/components/CodeBlock';
import FloatingChecklist from '@/components/FloatingChecklist';

type TabType = 'backend' | 'frontend';

const Docs = () => {
  const [activeTab, setActiveTab] = useState<TabType>('backend');

  return (
    <div className="-m-6 min-h-full overflow-y-auto px-6 pt-8 pb-8">
      <div className="mx-auto max-w-5xl">
        {/* 페이지 헤더 */}
        <div className="mb-8">
          <div className="mb-4 flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-blue-600 to-indigo-600 shadow-lg">
              <svg
                className="h-7 w-7 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
            </div>
            <div>
              <div className="flex items-baseline gap-3">
                <h1 className="text-3xl font-bold text-gray-900">LogLens</h1>
                <span className="text-sm font-medium text-gray-500">
                  v1.1.0
                </span>
              </div>
              <p className="mt-1 text-gray-600">
                TraceID 기반 함수 호출 추적 및 성능 측정 라이브러리
              </p>
            </div>
          </div>
        </div>

        {/* 탭 메뉴 */}
        <div className="mb-8">
          <div className="flex gap-2 border-b border-gray-200">
            <button
              onClick={() => setActiveTab('backend')}
              className={`px-6 py-3 font-semibold transition-all ${
                activeTab === 'backend'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Backend (Spring Boot)
            </button>
            <button
              onClick={() => setActiveTab('frontend')}
              className={`px-6 py-3 font-semibold transition-all ${
                activeTab === 'frontend'
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              Frontend (React)
            </button>
          </div>
        </div>

        {/* 콘텐츠 영역 */}
        <div className="prose max-w-none space-y-8">
          {activeTab === 'backend' ? <BackendDocs /> : <FrontendDocs />}
        </div>
      </div>
      <FloatingChecklist />
    </div>
  );
};

// 백엔드 문서 컴포넌트
const BackendDocs = () => {
  return (
    <>
      {/* 주요 기능 섹션 */}
      <section id="features">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">주요 기능</h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-2 text-xl font-semibold text-gray-800">
              1. 의존성 자동 수집
            </h3>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>
                애플리케이션 시작 시 컴포넌트 간 의존성 관계를 자동으로 분석
              </li>
              <li>Controller → Service → Repository 의존성 그래프 생성</li>
              <li>Collector 서버로 자동 전송</li>
            </ul>
          </div>

          <div>
            <h3 className="mb-2 text-xl font-semibold text-gray-800">
              2. 자동 메서드 로깅
            </h3>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>@Controller, @Service, @Repository 메서드 자동 로깅</li>
              <li>요청/응답, 실행 시간, 예외 자동 기록</li>
              <li>JSON 구조화된 로그 출력</li>
            </ul>
          </div>

          <div>
            <h3 className="mb-2 text-xl font-semibold text-gray-800">
              3. 민감 정보 보호
            </h3>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>@Sensitive: 비밀번호, 토큰 등 마스킹 (***)</li>
              <li>@ExcludeValue: 로그에서 완전히 제외 (&lt;excluded&gt;)</li>
            </ul>
          </div>

          <div>
            <h3 className="mb-2 text-xl font-semibold text-gray-800">
              4. 비동기 처리 지원
            </h3>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>AsyncExecutor를 통한 MDC(Trace ID) 자동 전파</li>
              <li>비동기 작업에서도 요청 추적 가능</li>
            </ul>
          </div>

          <div>
            <h3 className="mb-2 text-xl font-semibold text-gray-800">
              5. Trace ID 자동 추적
            </h3>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>HTTP 요청마다 고유 Trace ID 자동 생성</li>
              <li>모든 로그에 Trace ID 포함</li>
              <li>분산 환경에서 요청 추적 용이</li>
            </ul>
          </div>
        </div>
      </section>

      {/* 설치 방법 섹션 */}
      <section id="installation">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">설치 방법</h2>
        <div className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">Gradle</h3>
            <CodeBlock
              code={`dependencies {
    implementation 'com.loglens:loglens-spring-boot-starter:1.1.0'
}`}
              language="gradle"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">Maven</h3>
            <CodeBlock
              code={`<dependency>
    <groupId>a306</groupId>
    <artifactId>dependency-logger-starter</artifactId>
    <version>1.0.0</version>
</dependency>`}
              language="xml"
            />
          </div>
        </div>
      </section>

      {/* 기본 설정 섹션 */}
      <section id="configuration">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">기본 설정</h2>
        <div className="rounded-lg border bg-white p-6 shadow-sm">
          <h3 className="mb-3 text-lg font-semibold text-gray-800">
            application.yml
          </h3>
          <CodeBlock
            code={`spring:
  application:
    name: my-service  # 프로젝트 이름 (필수)

dependency:
  logger:
    collector:
      url: http://collector-server:8081  # Collector 서버 URL
    sender:
      enabled: true  # 의존성 전송 활성화 (default: true)

    # 선택적 설정
    trace:
      enabled: true  # Trace ID 필터 (default: true)
    method-execution:
      enabled: true  # 메서드 로깅 (default: true)
    exception-handler:
      enabled: true  # 예외 로깅 (default: true)`}
            language="yaml"
          />
        </div>
      </section>

      {/* 의존성 자동 수집 섹션 */}
      <section id="dependency-collection">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          의존성 자동 수집
        </h2>
        <div className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              작동 원리
            </h3>
            <p className="mb-2 text-gray-700">
              애플리케이션 시작 시 (ApplicationReadyEvent) 자동으로:
            </p>
            <ol className="ml-4 list-inside list-decimal space-y-2 text-gray-700">
              <li>
                <strong>컴포넌트 수집</strong>
                <ul className="mt-1 ml-6 list-inside list-disc">
                  <li>@RestController</li>
                  <li>@Service</li>
                  <li>@Repository (클래스 기반)</li>
                  <li>JPA Repository (인터페이스 기반)</li>
                </ul>
              </li>
              <li>
                <strong>의존성 분석</strong>
                <ul className="mt-1 ml-6 list-inside list-disc">
                  <li>생성자 파라미터 기반으로 의존성 추적</li>
                  <li>Component → Dependencies 관계 매핑</li>
                </ul>
              </li>
              <li>
                <strong>Collector 전송</strong>
                <ul className="mt-1 ml-6 list-inside list-disc">
                  <li>프로젝트 전체 의존성 그래프를 Collector 서버로 전송</li>
                </ul>
              </li>
            </ol>
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              코드 예시
            </h3>
            <CodeBlock
              code={`@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // ✅ 자동으로 의존성 수집
}

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;  // ✅ 자동으로 의존성 수집
}

public interface UserRepository extends JpaRepository<User, Long> {
    // ✅ 인터페이스 기반 Repository도 자동 인식
}`}
              language="java"
            />
          </div>

          <div className="rounded-r-lg border-l-4 border-amber-400 bg-amber-50 p-4">
            <h3 className="mb-2 text-lg font-semibold text-amber-900">
              ⚠️ 중요: @Component 대신 @Service 사용 권장
            </h3>
            <p className="mb-3 text-amber-800">
              비즈니스 로직이나 데이터 흐름을 추적하고 싶은 컴포넌트는
              @Component 대신 명확한 Stereotype을 사용하세요.
            </p>
            <CodeBlock
              code={`// ❌ 나쁜 예 - UNKNOWN 레이어로 표시됨
@Component
public class EmailSender {
    // 이메일 전송 로직
}

// ✅ 좋은 예 - SERVICE 레이어로 명확하게 표시됨
@Service
public class EmailSender {
    // 이메일 전송 로직
}`}
              language="java"
            />
          </div>
        </div>
      </section>

      {/* 메서드 로깅 섹션 */}
      <section id="method-logging">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          메서드 로깅
        </h2>
        <div className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              자동 로깅 대상
            </h3>
            <p className="mb-2 text-gray-700">
              다음 어노테이션이 붙은 클래스의 <strong>public 메서드</strong>는
              자동으로 로깅됩니다:
            </p>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>@RestController</li>
              <li>@Service</li>
              <li>@Repository</li>
              <li>@Component</li>
              <li>JPA Repository 인터페이스</li>
            </ul>
          </div>

          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              로깅 내용
            </h3>
            <ol className="ml-4 list-inside list-decimal space-y-2 text-gray-700">
              <li>
                <strong>REQUEST 로그</strong>
                <ul className="mt-1 ml-6 list-inside list-disc">
                  <li>HTTP 정보 (method, endpoint, queryString)</li>
                  <li>메서드명</li>
                  <li>파라미터 값</li>
                </ul>
              </li>
              <li>
                <strong>RESPONSE 로그</strong>
                <ul className="mt-1 ml-6 list-inside list-disc">
                  <li>실행 시간 (ms)</li>
                  <li>반환 값</li>
                  <li>HTTP 상태 코드</li>
                </ul>
              </li>
              <li>
                <strong>EXCEPTION 로그</strong>
                <ul className="mt-1 ml-6 list-inside list-disc">
                  <li>예외 타입 및 메시지</li>
                  <li>스택 트레이스 (상위 3줄)</li>
                  <li>HTTP 요청 정보</li>
                </ul>
              </li>
            </ol>
          </div>
        </div>
      </section>

      {/* 어노테이션 사용법 섹션 */}
      <section id="annotations">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          어노테이션 사용법
        </h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              1. @Sensitive - 민감 정보 마스킹
            </h3>
            <p className="mb-3 text-gray-700">
              비밀번호, 토큰 등 민감한 정보를 ****로 마스킹합니다.
            </p>
            <CodeBlock
              code={`@PostMapping("/login")
public ResponseEntity<TokenDto> login(@RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
}

public record LoginRequest(
    String email,
    @Sensitive String password  // 로그: "password": "****"
) {}`}
              language="java"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              2. @ExcludeValue - 로그에서 제외
            </h3>
            <p className="mb-3 text-gray-700">
              큰 데이터나 불필요한 정보를 로그에서 완전히 제외합니다.
            </p>
            <CodeBlock
              code={`@PostMapping("/files/upload")
public ResponseEntity<FileDto> uploadFile(
    @RequestParam String fileName,
    @ExcludeValue @RequestParam MultipartFile file  // 로그: "<excluded>"
) {
    return ResponseEntity.ok(fileService.upload(fileName, file));
}`}
              language="java"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              3. @LogMethodExecution - 수동 로깅
            </h3>
            <p className="mb-3 text-gray-700">
              @Component 클래스의 특정 메서드만 로깅하고 싶을 때 사용합니다.
            </p>
            <CodeBlock
              code={`@Component
public class DataProcessor {

    @LogMethodExecution(
        logParams = true,           // 파라미터 로깅 (default: true)
        logResponse = true,          // 응답 로깅 (default: true)
        logExecutionTime = true      // 실행 시간 로깅 (default: true)
    )
    public ProcessResult process(String data) {
        // 처리 로직
        return new ProcessResult();
    }
}`}
              language="java"
            />
          </div>
        </div>
      </section>

      {/* 비동기 처리 섹션 */}
      <section id="async">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          비동기 처리
        </h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-red-700">
              ❌ 문제: MDC(Trace ID) 전파 안됨
            </h3>
            <p className="mb-3 text-gray-700">
              일반적인 비동기 처리에서는 Trace ID가 전파되지 않습니다:
            </p>
            <CodeBlock
              code={`@Service
public class NotificationService {

    public void sendNotification(String message) {
        // ❌ 나쁜 예: Trace ID 손실
        CompletableFuture.runAsync(() -> {
            log.info("알림 전송: {}", message);  // trace_id: null
        });
    }
}`}
              language="java"
              variant="error"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-green-700">
              ✅ 해결: AsyncExecutor 사용
            </h3>
            <p className="mb-3 text-gray-700">
              AsyncExecutor를 사용하면{' '}
              <strong>MDC(Trace ID)가 자동으로 전파</strong>됩니다:
            </p>
            <CodeBlock
              code={`@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AsyncExecutor asyncExecutor;  // ✅ 주입

    public void sendNotification(String message) {
        // ✅ 좋은 예: Trace ID 자동 전파
        asyncExecutor.run(() -> {
            log.info("알림 전송: {}", message);  // trace_id 유지됨!
        });
    }
}`}
              language="java"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              AsyncExecutor 주요 메서드
            </h3>
            <div className="space-y-4">
              <div>
                <h4 className="mb-2 font-semibold text-gray-800">
                  반환값 없는 비동기 작업
                </h4>
                <CodeBlock
                  code={`// 즉시 실행
CompletableFuture<Void> future = asyncExecutor.run(() -> {
    log.info("비동기 작업 실행");
});

// 지연 실행
asyncExecutor.runAfterDelay(() -> {
    log.info("5초 후 실행");
}, Duration.ofSeconds(5));

// 병렬 실행
asyncExecutor.runAll(
    () -> log.info("작업 1"),
    () -> log.info("작업 2"),
    () -> log.info("작업 3")
);`}
                  language="java"
                />
              </div>

              <div>
                <h4 className="mb-2 font-semibold text-gray-800">
                  반환값 있는 비동기 작업
                </h4>
                <CodeBlock
                  code={`// 즉시 실행
CompletableFuture<String> future = asyncExecutor.supply(() -> {
    return "작업 결과";
});

String result = future.get();  // 결과 대기

// 병렬 실행 후 결과 수집
CompletableFuture<List<String>> results = asyncExecutor.supplyAll(
    () -> "결과1",
    () -> "결과2",
    () -> "결과3"
);`}
                  language="java"
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Trace ID 추적 섹션 */}
      <section id="trace-id">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          Trace ID 추적
        </h2>
        <div className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              자동 생성
            </h3>
            <p className="mb-3 text-gray-700">
              모든 HTTP 요청마다 고유한 Trace ID가 자동으로 생성됩니다:
            </p>
            <CodeBlock
              code={`GET /api/users/1
→ Trace ID: 550e8400-e29b-41d4-a716-446655440000 (자동 생성)`}
              language="bash"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              클라이언트가 제공한 Trace ID 사용
            </h3>
            <p className="mb-3 text-gray-700">
              클라이언트가 X-Trace-Id 헤더를 전송하면 해당 ID를 사용합니다:
            </p>
            <CodeBlock
              code={`curl -H "X-Trace-Id: my-custom-trace-id" \\
     http://localhost:8080/api/users/1`}
              language="bash"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              응답 헤더
            </h3>
            <p className="mb-3 text-gray-700">
              모든 응답에 Trace ID가 포함됩니다:
            </p>
            <CodeBlock
              code={`HTTP/1.1 200 OK
X-Trace-Id: 550e8400-e29b-41d4-a716-446655440000`}
              language="bash"
            />
          </div>
        </div>
      </section>

      {/* 설정 옵션 섹션 */}
      <section id="settings">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">설정 옵션</h2>
        <div className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
          <CodeBlock
            code={`spring:
  application:
    name: user-service

dependency:
  logger:
    # Collector 설정
    collector:
      url: http://localhost:8081       # Collector 서버 URL

    # 전송 설정
    sender:
      enabled: true                     # 의존성 전송 활성화 (default: true)

    # Trace ID 설정
    trace:
      enabled: true                     # Trace ID 필터 (default: true)

    # 메서드 로깅 설정
    method-execution:
      enabled: true                     # 메서드 로깅 (default: true)

    # 예외 로깅 설정
    exception-handler:
      enabled: true                     # @ExceptionHandler 로깅 (default: true)`}
            language="yaml"
          />

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              설정별 설명
            </h3>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200 overflow-hidden rounded-lg border">
                <thead className="bg-gradient-to-r from-slate-700 to-slate-600">
                  <tr>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-white">
                      설정
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-white">
                      기본값
                    </th>
                    <th className="px-4 py-3 text-left text-sm font-semibold text-white">
                      설명
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 bg-white">
                  <tr className="transition-colors hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-sm text-gray-700">
                      collector.url
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      http://localhost:8081
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      Collector 서버 주소
                    </td>
                  </tr>
                  <tr className="transition-colors hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-sm text-gray-700">
                      sender.enabled
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">true</td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      의존성 정보 전송 활성화
                    </td>
                  </tr>
                  <tr className="transition-colors hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-sm text-gray-700">
                      trace.enabled
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">true</td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      Trace ID 자동 생성/추적
                    </td>
                  </tr>
                  <tr className="transition-colors hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-sm text-gray-700">
                      method-execution.enabled
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">true</td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      메서드 자동 로깅
                    </td>
                  </tr>
                  <tr className="transition-colors hover:bg-gray-50">
                    <td className="px-4 py-3 font-mono text-sm text-gray-700">
                      exception-handler.enabled
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">true</td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      예외 핸들러 로깅
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      {/* 로그 출력 형식 섹션 */}
      <section id="log-format">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          로그 출력 형식
        </h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              REQUEST 로그
            </h3>
            <CodeBlock
              code={`{
  "@timestamp": "2025-10-28T12:34:56.789Z",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000",
  "level": "INFO",
  "package": "com.example.controller.UserController",
  "layer": "CONTROLLER",
  "message": "Request received: createUser",
  "execution_time_ms": null,
  "request": {
    "http": {
      "method": "POST",
      "endpoint": "/api/users",
      "queryString": null
    },
    "method": "createUser",
    "parameters": {
      "request": {
        "name": "홍길동",
        "email": "hong@example.com",
        "password": "****"
      }
    }
  }
}`}
              language="json"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              RESPONSE 로그
            </h3>
            <CodeBlock
              code={`{
  "@timestamp": "2025-10-28T12:34:56.891Z",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000",
  "level": "INFO",
  "package": "com.example.controller.UserController",
  "layer": "CONTROLLER",
  "message": "Response completed: createUser",
  "execution_time_ms": 102,
  "response": {
    "http": {
      "method": "POST",
      "endpoint": "/api/users",
      "statusCode": 200
    },
    "method": "createUser",
    "result": {
      "id": 1,
      "name": "홍길동",
      "email": "hong@example.com"
    }
  }
}`}
              language="json"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              EXCEPTION 로그
            </h3>
            <CodeBlock
              code={`{
  "@timestamp": "2025-10-28T12:34:56.999Z",
  "trace_id": "550e8400-e29b-41d4-a716-446655440000",
  "level": "ERROR",
  "package": "com.example.exception.GlobalExceptionHandler",
  "layer": "CONTROLLER",
  "message": "Validation failed: 2 error(s)",
  "exception": {
    "type": "MethodArgumentNotValidException",
    "message": "Validation failed: 2 error(s)",
    "validationErrors": {
      "email": "must be a well-formed email address",
      "password": "size must be between 8 and 20"
    },
    "http": {
      "method": "POST",
      "endpoint": "/api/users"
    }
  }
}`}
              language="json"
              variant="error"
            />
          </div>
        </div>
      </section>

      {/* FAQ 섹션 */}
      <section id="faq">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">FAQ</h2>
        <div className="space-y-5 rounded-lg border bg-white p-6 shadow-sm">
          <div className="border-b pb-4">
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              Q1. 로그가 너무 많이 출력됩니다.
            </h3>
            <p className="mb-3 text-gray-700">
              A. 특정 기능을 비활성화할 수 있습니다:
            </p>
            <CodeBlock
              code={`dependency:
  logger:
    method-execution:
      enabled: false  # 메서드 로깅 비활성화`}
              language="yaml"
            />
          </div>

          <div className="border-b pb-4">
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              Q2. 의존성 전송을 테스트 환경에서만 비활성화하고 싶습니다.
            </h3>
            <p className="mb-3 text-gray-700">
              A. 프로파일별 설정을 사용하세요:
            </p>
            <CodeBlock
              code={`# application-test.yml
dependency:
  logger:
    sender:
      enabled: false`}
              language="yaml"
            />
          </div>

          <div className="border-b pb-4">
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              Q3. @Component를 꼭 @Service로 바꿔야 하나요?
            </h3>
            <p className="text-gray-700">
              A. 아니요, 필수는 아닙니다. 하지만{' '}
              <strong>
                의존성 그래프에서 데이터 흐름을 추적하고 싶은 중요한 컴포넌트
              </strong>
              는 @Service, @Controller, @Repository 중 하나를 사용하는 것을
              권장합니다.
            </p>
          </div>

          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              Q4. JPA Repository가 자동으로 인식되지 않습니다.
            </h3>
            <p className="mb-3 text-gray-700">
              A. Spring Data JPA 의존성이 제대로 추가되었는지 확인하세요:
            </p>
            <CodeBlock
              code={`implementation 'org.springframework.boot:spring-boot-starter-data-jpa'`}
              language="gradle"
            />
          </div>
        </div>
      </section>

      {/* 문의하기 섹션 */}
      <section id="contact" className="pb-8">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">문의하기</h2>
        <div className="rounded-lg border border-blue-200 bg-gradient-to-br from-blue-50 to-indigo-50 p-6 shadow-sm">
          <p className="text-gray-700">
            문제가 발생하거나 개선 사항이 있으면 백엔드 팀에 문의해주세요.
          </p>
          <p className="mt-2 font-semibold text-gray-700">Happy Logging! 🎉</p>
        </div>
      </section>
    </>
  );
};

// 프론트엔드 문서 컴포넌트
const FrontendDocs = () => {
  return (
    <>
      {/* 설치 섹션 */}
      <section id="frontend-installation">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">설치</h2>
        <div className="rounded-lg border bg-white p-6 shadow-sm">
          <CodeBlock
            code={`npm install https://www.loglens.co.kr/static/libs/soo1-loglens-0.0.7.tgz`}
            language="bash"
          />
        </div>
      </section>

      {/* 시작하기 섹션 */}
      <section id="frontend-getting-started">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">시작하기</h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              1단계: 라이브러리 import
            </h3>
            <CodeBlock
              code={`import { initLogLens, loglens, withLogLens, useLogLens } from 'soo1-loglens';`}
              language="typescript"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              2단계: 초기화
            </h3>
            <p className="mb-3 text-gray-700">
              앱 시작 시{' '}
              <code className="rounded bg-gray-100 px-2 py-1">
                initLogLens()
              </code>
              를 호출하여 라이브러리를 초기화하세요.
            </p>
            <CodeBlock
              code={`// React 앱의 경우 (main.tsx 또는 App.tsx)
initLogLens({
  domain: "https://your-domain.com",  // 백엔드 도메인 (api/logs/frontend로 로그 전송)
  maxLogs: 1000,                       // 최대 로그 보관 개수
  autoFlushEnabled: true,              // 자동 로그 전송 여부
  autoFlushInterval: 30000,            // 30초마다 전송
  captureErrors: true                  // 에러 자동 수집
});`}
              language="typescript"
            />

            <div className="mt-4">
              <h4 className="mb-2 font-semibold text-gray-800">초기화 옵션</h4>
              <CodeBlock
                code={`{
  domain: string;             // 필수: 로그 수집 서버 도메인
  maxLogs?: number;           // 기본값: 1000
  autoFlushEnabled?: boolean; // 기본값: true
  autoFlushInterval?: number; // 기본값: 30000 (30초)
  captureErrors?: boolean;    // 기본값: true
}`}
                language="typescript"
              />
            </div>
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              3단계: 함수 래핑
            </h3>
            <p className="mb-3 text-gray-700">
              추적하고 싶은 기능의 시작이 되는 <strong>최상위 함수</strong>를{' '}
              <code className="rounded bg-gray-100 px-2 py-1">
                withLogLens()
              </code>
              로 감싸세요. 최상위 함수만 래핑하면 내부에서 호출되는 함수들에도
              자동으로 traceId가 전파됩니다.
            </p>
          </div>
        </div>
      </section>

      {/* 일반 함수 래핑 섹션 */}
      <section id="frontend-wrapping">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          일반 함수 래핑
        </h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              동기 함수
            </h3>
            <CodeBlock
              code={`const processData = withLogLens(
  function processData(data: any) {
    // 로직...
    return result;
  },
  { logger: 'DataService.processData' }
);`}
              language="typescript"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              비동기 함수 (자동 지원)
            </h3>
            <CodeBlock
              code={`const fetchUserData = withLogLens(
  async function fetchUserData(userId: string) {
    const response = await fetch(\`/api/users/\${userId}\`);
    return response.json();
  },
  { logger: 'API.fetchUserData' }
);`}
              language="typescript"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              화살표 함수
            </h3>
            <CodeBlock
              code={`const calculateTotal = withLogLens(
  (items: Item[]) => {
    return items.reduce((sum, item) => sum + item.price, 0);
  },
  { logger: 'Cart.calculateTotal' }
);`}
              language="typescript"
            />
          </div>
        </div>
      </section>

      {/* React Hook 사용 섹션 */}
      <section id="frontend-react-hook">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          React Hook 사용
        </h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <p className="text-gray-700">
            React 컴포넌트에서는{' '}
            <code className="rounded bg-gray-100 px-2 py-1">useLogLens</code>{' '}
            Hook을 사용하세요.
          </p>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              기본 사용법
            </h3>
            <CodeBlock
              code={`import { useLogLens } from 'soo1-loglens';

function UserProfile({ userId }: { userId: string }) {
  const fetchUser = useLogLens(
    async () => {
      const response = await fetch(\`/api/users/\${userId}\`);
      return response.json();
    },
    { logger: 'UserProfile.fetchUser' },
    [userId]  // 의존성 배열
  );

  return (
    <div>
      <button onClick={() => fetchUser()}>Load User</button>
    </div>
  );
}`}
              language="typescript"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              옵션 포함
            </h3>
            <CodeBlock
              code={`const updateUser = useLogLens(
  async (userData: UserData) => {
    const response = await fetch(\`/api/users/\${userId}\`, {
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
);`}
              language="typescript"
            />
          </div>

          <div className="rounded-r-lg border-l-4 border-blue-400 bg-blue-50 p-4">
            <h3 className="mb-2 text-lg font-semibold text-blue-900">
              💡 의존성 배열(deps) 이해하기
            </h3>
            <p className="mb-3 text-blue-800">
              <code className="rounded bg-blue-100 px-2 py-1">
                useLogLens()
              </code>
              의 세 번째 인자는 <strong>의존성 배열</strong>입니다. 이는 React의{' '}
              <code className="rounded bg-blue-100 px-2 py-1">useMemo()</code>,{' '}
              <code className="rounded bg-blue-100 px-2 py-1">
                useCallback()
              </code>
              과 동일한 원리로 동작합니다.
            </p>
            <p className="text-blue-800">
              함수 내부에서 <strong>외부 변수</strong>(props, state 등)를 참조할
              때 해당 변수가 변경되면 함수도 새로운 값을 반영하도록 재생성해야
              합니다.
            </p>
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              Stale Closure 문제
            </h3>
            <CodeBlock
              code={`function UserComponent({ userId }: { userId: string }) {
  // ❌ 잘못된 예: deps가 비어있음
  const fetchUser = useLogLens(
    async () => {
      // userId가 변경되어도 항상 처음 마운트될 때의 값만 사용됨
      console.log(userId);  // 예: 항상 "user-1"만 출력
      const response = await fetch(\`/api/users/\${userId}\`);
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
      const response = await fetch(\`/api/users/\${userId}\`);
      return response.json();
    },
    { logger: 'fetchUser' },
    [userId]  // ✅ userId가 변경되면 함수 재생성
  );
}`}
              language="typescript"
              variant="error"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              의존성 배열 작성 규칙
            </h3>
            <ol className="ml-4 list-inside list-decimal space-y-3 text-gray-700">
              <li>
                <strong>외부 변수를 사용하지 않는 경우</strong>
                <CodeBlock
                  code={`const staticFunction = useLogLens(
  () => {
    console.log('Hello');  // 외부 변수 참조 없음
  },
  { logger: 'static' },
  []  // 한 번만 생성
);`}
                  language="typescript"
                />
              </li>
              <li>
                <strong>props를 사용하는 경우</strong> → 해당 props를 deps에
                포함
                <CodeBlock
                  code={`const fetchProduct = useLogLens(
  async () => {
    const res = await fetch(\`/api/products/\${productId}\`);
    return res.json();
  },
  { logger: 'fetchProduct' },
  [productId]  // productId 변경 시 재생성
);`}
                  language="typescript"
                />
              </li>
              <li>
                <strong>여러 외부 변수를 사용하는 경우</strong> → 모두 deps에
                포함
                <CodeBlock
                  code={`const fetchFilteredData = useLogLens(
  async () => {
    const res = await fetch(\`/api/users/\${userId}/products/\${productId}?filter=\${filter}\`);
    return res.json();
  },
  { logger: 'fetchFilteredData' },
  [userId, productId, filter]  // 셋 중 하나라도 변경되면 재생성
);`}
                  language="typescript"
                />
              </li>
              <li>
                <strong>함수 파라미터만 사용하는 경우</strong> → 빈 배열
                <CodeBlock
                  code={`const deleteProduct = useLogLens(
  async (id: string) => {  // 인자로 받은 id만 사용
    await fetch(\`/api/products/\${id}\`, { method: 'DELETE' });
  },
  { logger: 'deleteProduct' },
  []  // 외부 변수 참조 없음
);`}
                  language="typescript"
                />
              </li>
            </ol>
          </div>
        </div>
      </section>

      {/* withLogLens 옵션 섹션 */}
      <section id="frontend-options">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          withLogLens 옵션
        </h2>
        <div className="rounded-lg border bg-white p-6 shadow-sm">
          <CodeBlock
            code={`{
  // ⚠️ 함수 이름 - logger에 넣어주는 것을 강력히 권장합니다
  // 난독화된 코드에서는 함수 이름 추출이 제대로 되지 않습니다.
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
}`}
            language="typescript"
          />
        </div>
      </section>

      {/* 주요 기능 섹션 */}
      <section id="frontend-features">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">주요 기능</h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              1. TraceID 자동 전파
            </h3>
            <p className="mb-3 text-gray-700">
              최상위 함수만 래핑하면 내부 호출 체인에 자동으로 traceId가
              전파됩니다.
            </p>
            <CodeBlock
              code={`// 내부 함수들 (래핑 불필요)
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
);`}
              language="typescript"
            />
          </div>

          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              2. 비동기 처리 자동 지원
            </h3>
            <p className="mb-3 text-gray-700">
              Promise, async/await를 사용하는 비동기 함수도 자동으로 추적됩니다.
            </p>
            <CodeBlock
              code={`const asyncOperation = withLogLens(
  async () => {
    await delay(1000);
    const result = await fetch('/api/data');
    return result.json();
  },
  { logger: 'Service.asyncOperation' }
);`}
              language="typescript"
            />
          </div>

          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              3. 성능 측정
            </h3>
            <p className="mb-3 text-gray-700">
              각 함수의 실행 시간이 자동으로 측정되어 콘솔에 표시됩니다.
            </p>
            <CodeBlock
              code={`[ INFO] → UserService.getUserData (  256ms)`}
              language="bash"
            />
          </div>

          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              4. 직접 로깅 메서드
            </h3>
            <p className="mb-3 text-gray-700">
              <code className="rounded bg-gray-100 px-2 py-1">
                withLogLens()
              </code>
              로 래핑하지 않고도 직접 로그를 남길 수 있습니다. (래핑하지 않은
              영역에서 로그를 남기는 경우 traceId로 추적되지 않습니다.)
            </p>
            <CodeBlock
              code={`import { loglens } from 'soo1-loglens';

function processOrder(orderId: string) {
  loglens.info('주문 처리 시작', { orderId });

  try {
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
}`}
              language="typescript"
            />

            <div className="mt-4">
              <h4 className="mb-2 font-semibold text-gray-800">메서드</h4>
              <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
                <li>
                  <code className="rounded bg-gray-100 px-2 py-1">
                    loglens.info(message, context?)
                  </code>{' '}
                  - 일반 정보 로깅
                </li>
                <li>
                  <code className="rounded bg-gray-100 px-2 py-1">
                    loglens.warn(message, context?)
                  </code>{' '}
                  - 경고 로깅
                </li>
                <li>
                  <code className="rounded bg-gray-100 px-2 py-1">
                    loglens.error(message, context?)
                  </code>{' '}
                  - 에러 로깅
                </li>
              </ul>
            </div>
          </div>

          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              5. 민감 데이터 마스킹
            </h3>
            <p className="mb-3 text-gray-700">
              환경변수를 통해 마스킹할 필드를 지정할 수 있습니다.
            </p>

            <h4 className="mb-2 font-semibold text-gray-800">
              환경변수 설정 (React)
            </h4>
            <CodeBlock
              code={`// .env 파일
VITE_LOGLENS_MASK_FIELDS=password,token,accessToken,refreshToken,secret,ssn,cardNumber`}
              language="bash"
            />

            <CodeBlock
              code={`// vite.config.ts 또는 앱 초기화 시
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
});`}
              language="typescript"
            />

            <div className="mt-4">
              <h4 className="mb-2 font-semibold text-gray-800">
                마스킹 기본값
              </h4>
              <CodeBlock
                code={`RegExp[] = [
  /^.*(password|passwd|pwd|pass|pw|secret).*$/i,
  /^.*(token|auth|authorization|bearer).*$/i,
  /^.*(api[_-]?key|access[_-]?key|secret[_-]?key).*$/i,
  /^.*(번호|number|no|ssn|security).*$/i,
  /^.*(card|cvv|cvc|pin).*$/i,
];`}
                language="typescript"
              />
            </div>
          </div>
        </div>
      </section>

      {/* 주의사항 섹션 */}
      <section id="frontend-warnings">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">주의사항</h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div className="rounded-r-lg border-l-4 border-amber-400 bg-amber-50 p-4">
            <h3 className="mb-2 text-lg font-semibold text-amber-900">
              ⚠️ 1. logger 옵션 사용 권장
            </h3>
            <p className="mb-3 text-amber-800">
              함수 이름을 추출하는 기능이 있지만, 빌드 후 난독화된 코드에서는
              제대로 동작하지 않습니다. 모든 함수에{' '}
              <code className="rounded bg-amber-100 px-2 py-1">logger</code>{' '}
              옵션을 명시적으로 지정하는 것을 강력히 권장합니다.
            </p>
            <CodeBlock
              code={`// 🥺 난독화 시 함수 이름이 'a' 또는 'anonymous'로 표시됨
const fetchData = withLogLens(async () => { ... });

// ✅ logger를 명시적으로 지정
const fetchData = withLogLens(
  async () => { ... },
  { logger: 'DataService.fetchData' }
);`}
              language="typescript"
            />
          </div>

          <div className="rounded-r-lg border-l-4 border-amber-400 bg-amber-50 p-4">
            <h3 className="mb-2 text-lg font-semibold text-amber-900">
              ⚠️ 2. 민감한 데이터 주의
            </h3>
            <p className="mb-3 text-amber-800">
              <code className="rounded bg-amber-100 px-2 py-1">
                includeArgs: true
              </code>{' '}
              또는{' '}
              <code className="rounded bg-amber-100 px-2 py-1">
                includeResult: true
              </code>{' '}
              사용 시 반드시 마스킹 설정을 확인하세요.
            </p>
            <CodeBlock
              code={`// 민감한 데이터가 포함된 함수
const processPayment = withLogLens(
  async (cardNumber: string, cvv: string) => {
    // ...
  },
  {
    logger: 'Payment.processPayment',
    includeArgs: true // ⚠️ 결제 관련 민감 데이터 마스킹 패턴에 있는지 확인
  }
);`}
              language="typescript"
            />
          </div>

          <div className="rounded-r-lg border-l-4 border-amber-400 bg-amber-50 p-4">
            <h3 className="mb-2 text-lg font-semibold text-amber-900">
              ⚠️ 3. 초기화 필수
            </h3>
            <p className="text-amber-800">
              앱 시작 시 반드시{' '}
              <code className="rounded bg-amber-100 px-2 py-1">
                initLogLens()
              </code>
              를 호출하세요. 초기화하지 않으면 로그가 수집되지 않습니다.
            </p>
          </div>
        </div>
      </section>

      {/* React 컴포넌트 전체 예제 섹션 */}
      <section id="frontend-examples">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          React 컴포넌트 전체 예제
        </h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              TodoList 컴포넌트
            </h3>
            <CodeBlock
              code={`import { useLogLens } from 'soo1-loglens';
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
      await fetch(\`/api/todos/\${id}\`, { method: 'DELETE' });
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
}`}
              language="typescript"
            />
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              서비스 레이어
            </h3>
            <CodeBlock
              code={`// services/userService.ts
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
}`}
              language="typescript"
            />
          </div>
        </div>
      </section>

      {/* API 레퍼런스 섹션 */}
      <section id="frontend-api">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          API 레퍼런스
        </h2>
        <div className="space-y-6 rounded-lg border bg-white p-6 shadow-sm">
          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              전역 함수
            </h3>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  initLogLens(config)
                </code>{' '}
                - 라이브러리 초기화
              </li>
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  withLogLens(fn, options?)
                </code>{' '}
                - 함수 래핑
              </li>
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  useLogLens(fn, options?, deps?)
                </code>{' '}
                - React Hook
              </li>
            </ul>
          </div>

          <div>
            <h3 className="mb-3 text-lg font-semibold text-gray-800">
              loglens 객체
            </h3>

            <h4 className="mt-4 mb-2 font-semibold text-gray-800">로그 조회</h4>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  loglens.getLogs()
                </code>{' '}
                - 모든 로그
              </li>
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  loglens.getLogsByTraceId(traceId)
                </code>{' '}
                - 특정 traceId 로그
              </li>
            </ul>

            <h4 className="mt-4 mb-2 font-semibold text-gray-800">로그 관리</h4>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  loglens.clear()
                </code>{' '}
                - 로그 초기화
              </li>
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  loglens.flush()
                </code>{' '}
                - 즉시 전송
              </li>
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  loglens.stopAutoFlush()
                </code>{' '}
                - 자동 전송 중지
              </li>
            </ul>

            <h4 className="mt-4 mb-2 font-semibold text-gray-800">상태 확인</h4>
            <ul className="ml-4 list-inside list-disc space-y-1 text-gray-700">
              <li>
                <code className="rounded bg-gray-100 px-2 py-1">
                  loglens.getStatus()
                </code>{' '}
                - 현재 상태 조회
              </li>
            </ul>
          </div>
        </div>
      </section>

      {/* 콘솔 출력 형식 섹션 */}
      <section id="frontend-console">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">
          콘솔 출력 형식
        </h2>
        <div className="rounded-lg border bg-white p-6 shadow-sm">
          <CodeBlock
            code={`[ INFO] → UserService.fetchUser (  256ms)
[ INFO] → API.fetchUserData (  145ms)
[ERROR] → Payment.processPayment (   89ms)`}
            language="bash"
          />
        </div>
      </section>

      {/* FAQ 섹션 */}
      <section id="frontend-faq">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">FAQ</h2>
        <div className="space-y-5 rounded-lg border bg-white p-6 shadow-sm">
          <div className="border-b pb-4">
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              Q1. 로그가 수집되지 않아요
            </h3>
            <p className="text-gray-700">
              A.{' '}
              <code className="rounded bg-gray-100 px-2 py-1">
                initLogLens()
              </code>
              를 앱 시작 시 호출했는지 확인하세요.
            </p>
          </div>

          <div>
            <h3 className="mb-2 text-lg font-semibold text-gray-800">
              Q2. 함수 이름이 'anonymous'나 이상한 문자로 나와요
            </h3>
            <p className="text-gray-700">
              A. <code className="rounded bg-gray-100 px-2 py-1">logger</code>{' '}
              옵션을 명시적으로 지정하세요. 현재 빌드 후 난독화된 코드에서는
              함수 이름 추출이 제대로 되지 않습니다.
            </p>
          </div>
        </div>
      </section>

      {/* 문의하기 섹션 */}
      <section id="frontend-contact" className="pb-8">
        <h2 className="mb-4 text-2xl font-semibold text-gray-800">문의하기</h2>
        <div className="rounded-lg border border-blue-200 bg-gradient-to-br from-blue-50 to-indigo-50 p-6 shadow-sm">
          <p className="text-gray-700">
            문제가 발생하거나 개선 사항이 있으면 개발 팀에 문의해주세요.
          </p>
          <p className="mt-2 font-semibold text-gray-700">Happy Logging! 🎉</p>
        </div>
      </section>
    </>
  );
};

export default Docs;
