import CodeBlock from '@/components/CodeBlock';
import FloatingChecklist from '@/components/FloatingChecklist';

const Docs = () => {
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
                <h1 className="text-3xl font-bold text-gray-900">
                  Dependency Logger Starter
                </h1>
                <span className="text-sm font-medium text-gray-500">
                  v1.1.0
                </span>
              </div>
              <p className="mt-1 text-gray-600">
                Spring Boot 애플리케이션의 의존성 추적, 메서드 로깅을 자동화하는
                라이브러리입니다.
              </p>
            </div>
          </div>
        </div>

        {/* 콘텐츠 영역 */}
        <div className="prose max-w-none space-y-8">
          {/* 주요 기능 섹션 */}
          <section id="features">
            <h2 className="mb-4 text-2xl font-semibold text-gray-800">
              주요 기능
            </h2>
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
                  <li>
                    @ExcludeValue: 로그에서 완전히 제외 (&lt;excluded&gt;)
                  </li>
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
            <h2 className="mb-4 text-2xl font-semibold text-gray-800">
              설치 방법
            </h2>
            <div className="space-y-4 rounded-lg border bg-white p-6 shadow-sm">
              <div>
                <h3 className="mb-3 text-lg font-semibold text-gray-800">
                  Gradle
                </h3>
                <CodeBlock
                  code={`dependencies {
    implementation 'com.loglens:loglens-spring-boot-starter:1.1.0'
}`}
                  language="gradle"
                />
              </div>

              <div>
                <h3 className="mb-3 text-lg font-semibold text-gray-800">
                  Maven
                </h3>
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
            <h2 className="mb-4 text-2xl font-semibold text-gray-800">
              기본 설정
            </h2>
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
                      <li>
                        프로젝트 전체 의존성 그래프를 Collector 서버로 전송
                      </li>
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
                  다음 어노테이션이 붙은 클래스의 <strong>public 메서드</strong>
                  는 자동으로 로깅됩니다:
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
            <h2 className="mb-4 text-2xl font-semibold text-gray-800">
              설정 옵션
            </h2>
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
                        <td className="px-4 py-3 text-sm text-gray-700">
                          true
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-700">
                          의존성 정보 전송 활성화
                        </td>
                      </tr>
                      <tr className="transition-colors hover:bg-gray-50">
                        <td className="px-4 py-3 font-mono text-sm text-gray-700">
                          trace.enabled
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-700">
                          true
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-700">
                          Trace ID 자동 생성/추적
                        </td>
                      </tr>
                      <tr className="transition-colors hover:bg-gray-50">
                        <td className="px-4 py-3 font-mono text-sm text-gray-700">
                          method-execution.enabled
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-700">
                          true
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-700">
                          메서드 자동 로깅
                        </td>
                      </tr>
                      <tr className="transition-colors hover:bg-gray-50">
                        <td className="px-4 py-3 font-mono text-sm text-gray-700">
                          exception-handler.enabled
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-700">
                          true
                        </td>
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
                    의존성 그래프에서 데이터 흐름을 추적하고 싶은 중요한
                    컴포넌트
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
            <h2 className="mb-4 text-2xl font-semibold text-gray-800">
              문의하기
            </h2>
            <div className="rounded-lg border border-blue-200 bg-gradient-to-br from-blue-50 to-indigo-50 p-6 shadow-sm">
              <p className="text-gray-700">
                문제가 발생하거나 개선 사항이 있으면 백엔드 팀에 문의해주세요.
              </p>
              <p className="mt-2 font-semibold text-gray-700">
                Happy Logging! 🎉
              </p>
            </div>
          </section>
        </div>
      </div>
      <FloatingChecklist />
    </div>
  );
};

export default Docs;
