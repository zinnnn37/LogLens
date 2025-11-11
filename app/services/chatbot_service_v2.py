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
    def _classify_query_type(question: str) -> str:
        """
        질문 유형 분류

        Returns:
            'analysis' | 'simple' | 'greeting'
        """
        question_lower = question.lower()

        # 인사말
        greeting_keywords = ['안녕', 'hello', 'hi', '처음', '반가']
        if any(keyword in question_lower for keyword in greeting_keywords):
            return 'greeting'

        # 분석 질문
        analysis_keywords = [
            '에러', 'error', '분석', '통계', '가장', 'most', '느린', 'slow',
            '성능', 'performance', '원인', '해결', '추천', '권장',
            '트래픽', 'traffic', '비교', 'compare', '심각', 'critical'
        ]
        if any(keyword in question_lower for keyword in analysis_keywords):
            return 'analysis'

        # 기본은 simple
        return 'simple'

    @staticmethod
    def _validate_and_enhance_response(answer: str, query_type: str, question: str) -> str:
        """
        답변 검증 및 자동 확장

        Args:
            answer: Agent가 생성한 답변
            query_type: 질문 유형 ('analysis' | 'simple' | 'greeting')
            question: 원본 질문

        Returns:
            검증 및 확장된 답변
        """
        # 길이 체크
        answer_length = len(answer)

        # 최소 길이 요구사항
        min_length = 800 if query_type == 'analysis' else 300

        # 구조 체크
        has_headers = bool(re.search(r'^#{1,3}\s', answer, re.MULTILINE))
        has_code_block = '```' in answer
        has_table = '|' in answer and '---' in answer
        has_bold = '**' in answer

        # 분석 질문인데 너무 짧거나 구조가 없는 경우
        if query_type == 'analysis':
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
            # 질문 유형 분류
            query_type = self._classify_query_type(question)

            # Agent 실행 (비동기)
            result = await agent_executor.ainvoke(agent_input)

            # Agent 결과에서 답변 추출
            answer = result.get("output", "죄송합니다. 답변을 생성할 수 없습니다.")

            # 답변 검증 및 확장
            validated_answer = self._validate_and_enhance_response(answer, query_type, question)

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
