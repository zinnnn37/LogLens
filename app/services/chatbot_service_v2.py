"""
Chatbot Service V2 - ReAct Agent 기반

LLM이 자율적으로 도구를 선택하여 로그 분석 수행
"""

from typing import List, Optional, Dict, Any
from app.agents.chatbot_agent import create_log_analysis_agent
from app.models.chat import ChatResponse, ChatMessage
from langchain_core.messages import HumanMessage, AIMessage


class ChatbotServiceV2:
    """Agent 기반 챗봇 서비스"""

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
            # Agent 실행 (비동기)
            result = await agent_executor.ainvoke(agent_input)

            # Agent 결과에서 답변 추출
            answer = result.get("output", "죄송합니다. 답변을 생성할 수 없습니다.")

            # ChatResponse 형식으로 반환
            # Agent는 자체적으로 로그를 검색하므로 related_logs는 빈 리스트
            return ChatResponse(
                answer=answer,
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
