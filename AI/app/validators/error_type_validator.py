"""
ErrorTypeValidator: 사용자 오류 vs 시스템 오류 올바른 분류 검증

LLM이 프롬프트 지시를 따르지 않거나 잘못 분류한 경우를 감지하고 재생성 요청
"""

import re
from typing import Dict, Any, List
from app.validators.base_validator import BaseValidator, ValidationResult
from app.models.analysis import LogAnalysisResult


class ErrorTypeValidator(BaseValidator):
    """
    사용자 오류 vs 시스템 오류 분류 검증 및 부적절한 solution 제안 감지

    검증 로직:
    1. 로그 데이터를 기반으로 실제 에러 타입 판단 (USER_ERROR vs SYSTEM_ERROR)
    2. USER_ERROR인데 백엔드 수정을 제안하는 경우 → 재생성 요청
    3. SYSTEM_ERROR인데 사용자 조치만 제안하는 경우 → 재생성 요청
    """

    @property
    def name(self) -> str:
        """Validator name"""
        return "ErrorTypeValidator"

    # 사용자 오류 패턴
    USER_ERROR_PATTERNS = {
        'exception_types': ['BusinessException'],
        'error_codes_regex': [
            r'^A\d+-\d+$',  # A409-1, A403-1 등
            r'^G4\d+(-\d+)?$',  # G400, G403, G404-1, G409 등
        ],
        'error_codes': ['A409-1', 'G400', 'G400-1', 'G403', 'G404-1', 'G409'],
        'http_status': [400, 403, 404, 409],
        'message_keywords': [
            '이미 사용 중인', '중복된', '존재하지 않습니다',
            '유효하지 않은', '잘못된 형식', '권한이 없습니다',
            '필수 값', '입력값', '형식 오류', '허용되지 않은',
            'duplicate', 'invalid', 'not found',  # 사용자 입력 관련
            'forbidden', 'unauthorized'
        ]
    }

    # 시스템 오류 패턴
    SYSTEM_ERROR_PATTERNS = {
        'exception_types': [
            'NullPointerException', 'SQLException', 'IOException',
            'TimeoutException', 'OutOfMemoryError', 'StackOverflowError',
            'DatabaseException', 'ConnectionException'
        ],
        'error_codes_regex': [
            r'^G5\d+(-\d+)?$',  # G500, G500-1, G503 등
        ],
        'error_codes': ['G500', 'G500-1', 'G502', 'G503', 'G504'],
        'http_status': [500, 502, 503, 504],
        'message_keywords': [
            'unexpected', 'timeout', 'failed to connect',
            'internal error', 'connection refused', 'out of memory',
            '예상치 못한', '연결 실패', '타임아웃', '메모리 부족'
        ]
    }

    # 백엔드 수정 제안 키워드
    BACKEND_FIX_KEYWORDS = [
        '메서드 수정', '로직 수정', '코드 수정', 'DB 수정',
        '인덱스 추가', '쿼리 최적화', '예외 처리 추가',
        'null 체크 추가', '리팩토링', '클래스 수정',
        '검증 로직 수정', '중복 검증 수정', '테이블 수정',
        'modify method', 'fix logic', 'refactor', 'add null check'
    ]

    async def validate(
        self,
        analysis: LogAnalysisResult,
        log_data: Dict[str, Any]
    ) -> ValidationResult:
        """
        에러 타입 분류가 올바른지 검증

        Args:
            analysis: LLM이 생성한 분석 결과
            log_data: 원본 로그 데이터

        Returns:
            ValidationResult (passed, score, suggestions)
        """
        # 1. 로그 데이터로 실제 에러 타입 판단
        actual_error_type = self._classify_error(log_data)

        # 2. LLM이 생성한 tags에서 에러 타입 추출
        llm_error_type = self._extract_error_type_from_tags(analysis.tags)

        # 3. USER_ERROR인데 백엔드 수정 제안하는지 확인
        if actual_error_type == 'USER_ERROR':
            # 3-1. tags에 USER_ERROR가 포함되었는지 확인
            if llm_error_type != 'USER_ERROR':
                return ValidationResult(
                    validator_name="ErrorTypeValidator",
                    passed=False,
                    score=0.0,
                    suggestions=[
                        f"This is a USER_ERROR but tags do not include 'USER_ERROR'",
                        f"Detected patterns: {self._get_detected_patterns(log_data, 'USER_ERROR')}",
                        "Add 'USER_ERROR' to tags and use SEVERITY_LOW or SEVERITY_MEDIUM"
                    ]
                )

            # 3-2. solution에 백엔드 수정 제안이 있는지 확인
            if self._suggests_backend_fix(analysis.solution):
                return ValidationResult(
                    validator_name="ErrorTypeValidator",
                    passed=False,
                    score=0.0,
                    suggestions=[
                        "This is a USER_ERROR - do NOT suggest backend code fixes",
                        "The system is working correctly - this is a user input error",
                        "Remove backend fix suggestions like: 메서드 수정, 로직 수정, DB 수정",
                        "Focus on:",
                        "  - User actions (다른 이메일 주소 사용, 올바른 값 입력)",
                        "  - Optional frontend improvements (실시간 검증, 더 명확한 에러 메시지)",
                        "  - Optional UX improvements (플로우 개선)"
                    ]
                )

            # 3-3. error_cause가 사용자 관점인지 확인
            if analysis.error_cause and self._is_backend_focused_cause(analysis.error_cause):
                return ValidationResult(
                    validator_name="ErrorTypeValidator",
                    passed=False,
                    score=0.5,  # 부분 감점
                    suggestions=[
                        "error_cause should explain what the USER did wrong, not what's wrong with the backend",
                        "❌ Bad: '이메일 검증 로직에서 예외 발생'",
                        "✅ Good: '사용자가 이미 등록된 이메일을 입력하여 중복 검증 실패'"
                    ]
                )

        # 4. SYSTEM_ERROR인데 사용자 조치만 제안하는지 확인
        if actual_error_type == 'SYSTEM_ERROR':
            # 4-1. tags에 SYSTEM_ERROR가 포함되었는지 확인
            if llm_error_type != 'SYSTEM_ERROR':
                return ValidationResult(
                    validator_name="ErrorTypeValidator",
                    passed=False,
                    score=0.0,
                    suggestions=[
                        f"This is a SYSTEM_ERROR but tags do not include 'SYSTEM_ERROR'",
                        f"Detected patterns: {self._get_detected_patterns(log_data, 'SYSTEM_ERROR')}",
                        "Add 'SYSTEM_ERROR' to tags"
                    ]
                )

            # 4-2. solution에 백엔드 수정 제안이 있는지 확인
            if not self._suggests_backend_fix(analysis.solution):
                return ValidationResult(
                    validator_name="ErrorTypeValidator",
                    passed=False,
                    score=0.0,
                    suggestions=[
                        "This is a SYSTEM_ERROR - suggest backend fixes",
                        "Add immediate actions, short-term improvements, and long-term strategies",
                        "Include code examples and specific fix suggestions"
                    ]
                )

        # 5. 모든 검증 통과
        return ValidationResult(
            validator_name="ErrorTypeValidator",
            passed=True,
            score=1.0,
            suggestions=[]
        )

    def _classify_error(self, log_data: Dict[str, Any]) -> str:
        """
        로그 데이터를 기반으로 에러 타입 분류

        판단 우선순위:
        1. BusinessException 체크 (최우선)
        2. 에러 코드 정규식 매칭
        3. HTTP 상태 코드
        4. 메시지 키워드
        5. 예외 타입

        Returns:
            'USER_ERROR' or 'SYSTEM_ERROR'
        """
        message = log_data.get('message', '')
        exception_type = log_data.get('exceptionType', log_data.get('exception_type', ''))
        level = log_data.get('level', '')
        error_code = self._extract_error_code(message)

        # 1. BusinessException 체크 (최우선 - 99% USER_ERROR)
        if any(pattern in exception_type for pattern in self.USER_ERROR_PATTERNS['exception_types']):
            return 'USER_ERROR'

        # 2. 에러 코드 정규식 매칭
        if error_code:
            # USER_ERROR 패턴
            for pattern in self.USER_ERROR_PATTERNS['error_codes_regex']:
                if re.match(pattern, error_code):
                    return 'USER_ERROR'

            # SYSTEM_ERROR 패턴
            for pattern in self.SYSTEM_ERROR_PATTERNS['error_codes_regex']:
                if re.match(pattern, error_code):
                    return 'SYSTEM_ERROR'

            # 직접 매칭
            if error_code in self.USER_ERROR_PATTERNS['error_codes']:
                return 'USER_ERROR'
            if error_code in self.SYSTEM_ERROR_PATTERNS['error_codes']:
                return 'SYSTEM_ERROR'

        # 3. HTTP 상태 코드 (로그에 포함되어 있으면)
        http_status = log_data.get('httpStatus', log_data.get('http_status'))
        if http_status:
            if http_status in self.USER_ERROR_PATTERNS['http_status']:
                return 'USER_ERROR'
            if http_status in self.SYSTEM_ERROR_PATTERNS['http_status']:
                return 'SYSTEM_ERROR'

        # 4. 메시지 키워드 (우선순위: SYSTEM > USER, 시스템 오류가 더 치명적)
        message_lower = message.lower()

        # SYSTEM_ERROR 키워드 먼저 체크 (우선순위 높음)
        if any(keyword in message_lower for keyword in self.SYSTEM_ERROR_PATTERNS['message_keywords']):
            return 'SYSTEM_ERROR'

        # USER_ERROR 키워드
        if any(keyword in message for keyword in self.USER_ERROR_PATTERNS['message_keywords']):
            return 'USER_ERROR'

        # 5. 예외 타입
        if any(pattern in exception_type for pattern in self.SYSTEM_ERROR_PATTERNS['exception_types']):
            return 'SYSTEM_ERROR'

        # 6. Stack Trace 존재 여부
        stack_trace = log_data.get('stackTrace', log_data.get('stack_trace', ''))
        if stack_trace and len(stack_trace) > 50:  # 실제 stack trace가 있으면
            return 'SYSTEM_ERROR'

        # 기본값: ERROR 레벨이면 SYSTEM_ERROR, 아니면 USER_ERROR
        return 'SYSTEM_ERROR' if level == 'ERROR' else 'USER_ERROR'

    def _extract_error_code(self, message: str) -> str:
        """메시지에서 에러 코드 추출 (예: A409-1, G500)"""
        # 패턴: A409-1, G400, G500-1 등
        match = re.search(r'([AG]\d+(?:-\d+)?)', message)
        return match.group(1) if match else ''

    def _extract_error_type_from_tags(self, tags: List[str]) -> str:
        """tags에서 USER_ERROR 또는 SYSTEM_ERROR 추출"""
        if 'USER_ERROR' in tags:
            return 'USER_ERROR'
        if 'SYSTEM_ERROR' in tags:
            return 'SYSTEM_ERROR'
        return 'UNKNOWN'

    def _suggests_backend_fix(self, solution: str) -> bool:
        """solution에 백엔드 수정 제안이 포함되어 있는지 확인"""
        if not solution:
            return False

        solution_lower = solution.lower()
        return any(keyword in solution for keyword in self.BACKEND_FIX_KEYWORDS)

    def _is_backend_focused_cause(self, error_cause: str) -> bool:
        """error_cause가 백엔드 관점인지 확인 (사용자 관점이어야 함)"""
        backend_keywords = [
            '로직에서 예외', '메서드에서 발생', '검증 로직에서',
            '코드에서 오류', '시스템에서 예외'
        ]
        return any(keyword in error_cause for keyword in backend_keywords)

    def _get_detected_patterns(self, log_data: Dict[str, Any], error_type: str) -> str:
        """감지된 패턴 목록 반환 (디버깅용)"""
        detected = []
        message = log_data.get('message', '')
        exception_type = log_data.get('exceptionType', log_data.get('exception_type', ''))

        patterns = (
            self.USER_ERROR_PATTERNS if error_type == 'USER_ERROR'
            else self.SYSTEM_ERROR_PATTERNS
        )

        # BusinessException
        if any(p in exception_type for p in patterns.get('exception_types', [])):
            detected.append(f"exception_type={exception_type}")

        # 에러 코드
        error_code = self._extract_error_code(message)
        if error_code and error_code in patterns.get('error_codes', []):
            detected.append(f"error_code={error_code}")

        # 키워드
        matched_keywords = [
            kw for kw in patterns.get('message_keywords', [])
            if kw in message
        ]
        if matched_keywords:
            detected.append(f"keywords={', '.join(matched_keywords[:3])}")

        return '; '.join(detected) if detected else 'general pattern matching'
