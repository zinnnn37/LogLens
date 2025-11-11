"""
Chatbot Service V2 - ReAct Agent 기반

LLM이 자율적으로 도구를 선택하여 로그 분석 수행
"""

import re
from typing import List, Optional, Dict, Any
from app.agents.chatbot_agent import create_log_analysis_agent
from app.models.chat import ChatResponse, ChatMessage
from langchain_core.messages import HumanMessage, AIMessage


class ChatbotServiceV2:
    """Agent 기반 챗봇 서비스"""

    @staticmethod
    def _is_off_topic(question: str) -> bool:
        """
        로그 분석과 무관한 질문 감지

        Args:
            question: 사용자 질문

        Returns:
            True if off-topic (로그 무관), False if log-related (로그 관련)
        """
        question_lower = question.lower()

        # 로그 관련 키워드 (한글 + 영어)
        log_keywords = [
            # 한글
            '에러', '오류', '로그', '성능', 'api', '서비스', '통계', '분석',
            '트래픽', '모니터링', '응답', '시간', '검색', '조회', '느린', '빠른',
            '장애', '실패', '성공', '요청', '배포', '서버', '헬스', '상태',
            '버그', '예외', '익셉션', '스택', '트레이스',
            # 영어
            'error', 'log', 'performance', 'api', 'service', 'statistics',
            'analysis', 'traffic', 'monitoring', 'response', 'time', 'search',
            'slow', 'fast', 'failure', 'deploy', 'server', 'health', 'status',
            'bug', 'exception', 'stack', 'trace'
        ]

        # 키워드가 하나라도 있으면 로그 관련 질문
        if any(keyword in question_lower for keyword in log_keywords):
            return False  # Not off-topic

        # 인사말 키워드
        greeting_keywords = ['안녕', 'hello', 'hi', '반가', 'hey']
        if any(greet in question_lower for greet in greeting_keywords):
            return True  # Off-topic (greeting)

        # 너무 짧은 질문 (3글자 미만)
        if len(question.strip()) < 3:
            return True  # Off-topic (too vague)

        # 키워드가 없으면 로그 무관으로 간주
        return True  # Off-topic (no log keywords)

    @staticmethod
    def _classify_query_type(question: str) -> str:
        """
        질문 유형 분류 (7가지)

        Returns:
            'error_analysis' | 'performance_analysis' | 'monitoring' | 'search' |
            'comparison' | 'deployment' | 'simple'
        """
        question_lower = question.lower()

        # 1. 배포 영향 분석
        deployment_keywords = ['배포', 'deploy', '릴리스', 'release', '배포 이후', '배포 전후']
        if any(keyword in question_lower for keyword in deployment_keywords):
            return 'deployment'

        # 2. 비교 분석
        comparison_keywords = ['비교', 'compare', 'vs', '대비', '차이', '어제', '오늘', '지난', '전후']
        if any(keyword in question_lower for keyword in comparison_keywords):
            return 'comparison'

        # 3. 에러 분석 (가장 구체적)
        error_keywords = [
            '에러', 'error', '오류', '장애', 'failure', '실패', 'exception',
            '버그', 'bug', '심각', 'critical', 'fatal', '원인', '해결'
        ]
        if any(keyword in question_lower for keyword in error_keywords):
            return 'error_analysis'

        # 4. 성능 분석
        performance_keywords = [
            '느린', 'slow', '빠른', 'fast', '성능', 'performance', '응답', 'response',
            '지연', 'latency', 'timeout', '병목', 'bottleneck'
        ]
        if any(keyword in question_lower for keyword in performance_keywords):
            return 'performance_analysis'

        # 5. 모니터링/통계
        monitoring_keywords = [
            '모니터링', 'monitor', '통계', 'statistics', '추세', 'trend',
            '증가', 'increase', '감소', 'decrease', '헬스', 'health',
            '상태', 'status', '서비스별', '가장 많은', 'most', '트래픽', 'traffic'
        ]
        if any(keyword in question_lower for keyword in monitoring_keywords):
            return 'monitoring'

        # 6. 로그 검색
        search_keywords = [
            '검색', 'search', '찾', 'find', '조회', 'lookup', '보여줘', 'show',
            '있', 'exist', '로그', 'log'
        ]
        if any(keyword in question_lower for keyword in search_keywords):
            return 'search'

        # 7. 기본 (simple)
        return 'simple'

    @staticmethod
    def _validate_and_enhance_response(answer: str, query_type: str, question: str) -> str:
        """
        답변 검증 및 자동 확장 (7가지 질문 유형별)

        Args:
            answer: Agent가 생성한 답변
            query_type: 질문 유형 (error_analysis/performance_analysis/monitoring/search/comparison/deployment/simple)
            question: 원본 질문

        Returns:
            검증 및 확장된 답변
        """
        answer_length = len(answer)

        # 유형별 최소 길이 요구사항 (완화됨 - 파싱 성공이 우선)
        min_lengths = {
            'error_analysis': 800,        # 1000 → 800 (완화)
            'performance_analysis': 700,   # 900 → 700 (완화)
            'monitoring': 600,             # 800 → 600 (완화)
            'comparison': 600,             # 800 → 600 (완화)
            'deployment': 700,             # 900 → 700 (완화)
            'search': 300,                 # 400 → 300 (완화)
            'simple': 200                  # 300 → 200 (완화)
        }
        min_length = min_lengths.get(query_type, 200)

        # 구조 체크
        has_headers = bool(re.search(r'^#{1,3}\s', answer, re.MULTILINE))
        has_code_block = '```' in answer
        has_table = '|' in answer and '---' in answer
        has_bold = '**' in answer

        # 분석 유형 질문 검증 (error_analysis, performance_analysis, monitoring, comparison, deployment)
        analysis_types = ['error_analysis', 'performance_analysis', 'monitoring', 'comparison', 'deployment']
        if query_type in analysis_types:
            issues = []

            if answer_length < min_length:
                issues.append(f"답변이 너무 짧습니다 (현재 {answer_length}자, 권장 {min_length}자 이상)")

            if not has_headers:
                issues.append("마크다운 헤더(##)가 없습니다")

            if not (has_code_block or has_table or has_bold):
                issues.append("기술적 디테일(코드블록/표/굵은글씨)이 부족합니다")

            # 문제가 있으면 힌트 추가
            if issues:
                hint = "\n\n---\n💡 **더 상세한 분석을 원하시면:**\n"
                hint += "- 특정 시간대를 지정해주세요 (예: 최근 24시간)\n"
                hint += "- 서비스 이름을 명시해주세요\n"
                hint += "- 구체적인 에러 타입이나 API 경로를 알려주세요"

                # 디버그 로그 (개발 환경에서만)
                print(f"⚠️ 답변 품질 체크: {', '.join(issues)}")

                # 답변에 힌트 추가
                answer += hint

        # simple 질문인데 너무 짧은 경우
        elif query_type == 'simple' and answer_length < 200:
            # 기본 가이드 추가
            answer += "\n\n궁금하신 점이 있으시면 편하게 질문해주세요! 😊"

        return answer

    @staticmethod
    def _validate_factual_accuracy(answer: str, question: str) -> List[str]:
        """
        답변의 사실 정확성 검증 (환각 방지)

        Args:
            answer: Agent 생성 답변
            question: 원본 질문

        Returns:
            경고 메시지 리스트
        """
        warnings = []

        # 1. 숫자 추출 (**로 강조된 숫자들**)
        numbers_in_answer = re.findall(r'\*\*(\d+(?:,\d+)?)\*\*', answer)

        # 2. "최근"이라는 단어가 있는지 확인
        if '최근' in question and '시간' not in question and '일' not in question:
            # "최근" 키워드만 있고 구체적 시간 없음 → 24시간이어야 함
            if '24' not in answer and '24시간' not in answer:
                warnings.append("'최근' 질문은 24시간 데이터를 사용해야 합니다")

        # 3. log_id 인용 확인 (에러 분석인 경우)
        if any(keyword in question for keyword in ['에러', 'error', '심각', 'critical']):
            if 'log_id' not in answer and '(log_id:' not in answer:
                warnings.append("에러 분석 시 log_id를 인용해야 추적 가능합니다")

        return warnings

    @staticmethod
    def _validate_required_sections(answer: str, query_type: str) -> List[str]:
        """
        질문 유형별 필수 섹션 검증

        Args:
            answer: Agent 생성 답변
            query_type: 질문 유형

        Returns:
            누락된 섹션 리스트
        """
        missing = []

        # 유형별 필수 섹션
        required_sections_map = {
            'error_analysis': ['##', '🔴', '✅'],  # 제목, 주요 발견, 권장 조치
            'performance_analysis': ['##', '⏱️', '✅'],  # 제목, 성능 지표, 권장 조치
            'monitoring': ['##', '📊'],  # 제목, 통계
            'comparison': ['##', '|'],  # 제목, 비교 표
            'deployment': ['##', '✅'],  # 제목, 권장 조치
        }

        required_sections = required_sections_map.get(query_type, [])
        for section in required_sections:
            if section not in answer:
                missing.append(section)

        return missing

    async def ask(
        self,
        question: str,
        project_uuid: str,
        chat_history: Optional[List[ChatMessage]] = None,
    ) -> ChatResponse:
        """
        ReAct Agent를 사용하여 질문에 답변

        Agent가 자율적으로:
        1. 질문 분석
        2. 필요한 도구 선택 (검색, 통계, 상세 조회 등)
        3. 도구 실행
        4. 결과 종합하여 답변 생성

        Args:
            question: 사용자 질문
            project_uuid: 프로젝트 UUID (언더스코어 형식)
            chat_history: 대화 기록 (선택)

        Returns:
            ChatResponse (answer, related_logs 등)
        """
        # ReAct Agent 생성 (project_uuid 바인딩)
        agent_executor = create_log_analysis_agent(project_uuid)

        # 대화 기록을 LangChain 메시지 형식으로 변환
        langchain_history = []
        if chat_history:
            for msg in chat_history:
                if msg.role == "user":
                    langchain_history.append(HumanMessage(content=msg.content))
                elif msg.role == "assistant":
                    langchain_history.append(AIMessage(content=msg.content))

        # 대화 히스토리를 문자열로 포맷팅 (프롬프트에 포함)
        history_text = ""
        if langchain_history:
            history_text = "\n\n## 이전 대화:\n"
            for msg in langchain_history:
                role = "User" if isinstance(msg, HumanMessage) else "Assistant"
                history_text += f"{role}: {msg.content}\n"

        # Agent 실행 입력 구성
        agent_input = {
            "input": question,
            "chat_history": history_text  # 프롬프트 변수로 전달
        }

        try:
            # 🚫 로그 무관 질문 사전 필터링 (Agent 호출 전)
            if self._is_off_topic(question):
                print(f"🚫 Off-topic question detected, skipping agent: {question[:50]}...")
                # Agent 호출 없이 즉시 범위 설명 반환
                return ChatResponse(
                    answer="""죄송합니다. 저는 로그 분석 전문 AI 어시스턴트입니다.

다음과 같은 질문에만 답변할 수 있습니다:

**📊 에러 분석:**
- "최근 에러 로그 보여줘"
- "가장 심각한 에러는?"
- "NullPointerException 분석해줘"

**⚡ 성능 분석:**
- "응답 시간이 느린 API는?"
- "성능 병목 지점 찾아줘"
- "트래픽 패턴 분석해줘"

**🔍 로그 검색:**
- "user-service 로그 찾아줘"
- "최근 24시간 로그 조회"
- "특정 시간대 로그 검색"

**📈 시스템 모니터링:**
- "서비스 헬스 체크"
- "에러율 추이 분석"
- "시간대별 트래픽"

무엇을 도와드릴까요? 😊""",
                    from_cache=False,
                    related_logs=[]
                )

            # 질문 유형 분류 (로그 관련 질문인 경우)
            query_type = self._classify_query_type(question)

            # Agent 실행 (비동기)
            result = await agent_executor.ainvoke(agent_input)

            # Agent 결과에서 답변 추출
            answer = result.get("output", "죄송합니다. 답변을 생성할 수 없습니다.")

            # 답변 검증 및 확장
            validated_answer = self._validate_and_enhance_response(answer, query_type, question)

            # 추가 검증: 사실 정확성 + 필수 섹션
            factual_warnings = self._validate_factual_accuracy(validated_answer, question)
            missing_sections = self._validate_required_sections(validated_answer, query_type)

            # 검증 결과 로깅 (디버그용)
            if factual_warnings:
                print(f"⚠️ 사실 정확성 경고: {factual_warnings}")
            if missing_sections:
                print(f"⚠️ 누락 섹션: {missing_sections}")

            # ChatResponse 형식으로 반환
            # Agent는 자체적으로 로그를 검색하므로 related_logs는 빈 리스트
            return ChatResponse(
                answer=validated_answer,
                from_cache=False,  # V2는 캐싱 미지원
                related_logs=[]  # Agent가 내부적으로 로그 처리
            )

        except Exception as e:
            print(f"❌ Agent 실행 중 오류: {e}")
            # 에러 발생 시 사용자 친화적 메시지 반환
            return ChatResponse(
                answer=f"죄송합니다. 질문 처리 중 오류가 발생했습니다: {str(e)}",
                from_cache=False,
                related_logs=[]
            )


# Global service instance
chatbot_service_v2 = ChatbotServiceV2()
