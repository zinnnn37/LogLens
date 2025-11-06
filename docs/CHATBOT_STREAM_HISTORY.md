# Chatbot Stream & History 구현 가이드

## 📚 목차
1. [개요](#1-개요)
2. [Stream 구현 (Fetch API + ReadableStream)](#2-stream-구현)
3. [Chat History 구현](#3-chat-history-구현)
4. [Stream + History 통합](#4-stream--history-통합)
5. [실습 가이드](#5-실습-가이드)
6. [트러블슈팅](#6-트러블슈팅)

---

## 1. 개요

### 현재 상태 vs 개선 후

```
┌─────────────────────────────────────────────────────────┐
│              현재 (Non-Stream, No History)              │
└─────────────────────────────────────────────────────────┘

사용자: "최근 에러 알려줘"
   ↓
[Loading... 25초 대기] ⏳
   ↓
챗봇: "NPE 3건, DB 타임아웃 2건 발생했습니다"

사용자: "그 중 가장 심각한 건?"
   ↓
챗봇: "무엇을 말씀하시는지 모르겠습니다" ❌

문제점:
- 긴 대기 시간 (20-30초)
- 이전 대화 맥락 이해 불가
- 연속 대화 불가능


┌─────────────────────────────────────────────────────────┐
│        개선 후 (Stream + History)                        │
└─────────────────────────────────────────────────────────┘

사용자: "최근 에러 알려줘"
   ↓
챗봇: "N" (0.5초)
챗봇: "PE 3건" (1초)
챗봇: ", DB 타임아웃 2건 발생" (1.5초)
챗봇: "했습니다" (2초) ✅ 실시간 타이핑

사용자: "그 중 가장 심각한 건?"
   ↓
챗봇: "앞서 말씀드린 DB 타임아웃이 가장 심각합니다..." ✅ 맥락 이해

장점:
- 즉각적인 피드백 (0.5초 이내)
- 이전 대화 기억
- 자연스러운 연속 대화
```

---

## 2. Stream 구현

### 2.1. 백엔드 구현 (FastAPI)

#### Step 1: 필요한 import 추가

**파일**: `app/api/v1/chatbot.py`

```python
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
import json
from app.services.chatbot_service import chatbot_service
from app.models.chat import ChatRequest, ChatResponse
```

#### Step 2: Stream 엔드포인트 추가

```python
@router.post("/chatbot/ask/stream")
async def ask_chatbot_stream(request: ChatRequest):
    """
    Stream 방식으로 답변 생성

    SSE (Server-Sent Events) 형식으로 실시간 답변 전송:
    - 각 청크를 "data: {content}\n\n" 형식으로 전송
    - 완료 시 "data: [DONE]\n\n" 전송
    - 에러 시 "data: {\"error\": \"...\"}\n\n" 전송

    Args:
        request: ChatRequest (question, project_id, filters, time_range)

    Returns:
        StreamingResponse (text/event-stream)
    """
    async def generate():
        try:
            # 1. 질문 임베딩 생성
            question_vector = await embedding_service.embed_query(request.question)

            # 2. 캐시 체크 (2-stage validation)
            cache_candidates = await similarity_service.find_similar_questions(
                question_vector=question_vector,
                k=settings.CACHE_CANDIDATE_SIZE,
                project_id=request.project_id
            )

            # 캐시된 답변이 있으면 한번에 전송
            for candidate in cache_candidates:
                if candidate["score"] >= chatbot_service.threshold:
                    if chatbot_service._is_cache_valid(candidate):
                        if chatbot_service._metadata_matches(
                            candidate.get("metadata", {}),
                            request.filters,
                            request.time_range,
                            request.project_id
                        ):
                            # 캐시 히트 - 전체 답변 전송
                            cached_answer = candidate["answer"]
                            yield f"data: {cached_answer}\n\n"
                            yield "data: [DONE]\n\n"
                            return

            # 3. 캐시 미스 - 관련 로그 검색
            relevant_logs_data = await similarity_service.find_similar_logs(
                log_vector=question_vector,
                k=chatbot_service.max_context,
                filters=request.filters,
                project_id=request.project_id,
            )

            # 4. 컨텍스트 준비
            context_logs = chatbot_service._format_context_logs(relevant_logs_data)

            # 5. LLM 스트리밍 생성
            full_answer = ""
            async for chunk in chatbot_chain.astream({
                "context_logs": context_logs,
                "question": request.question,
            }):
                content = chunk.content
                full_answer += content

                # SSE 형식으로 청크 전송
                yield f"data: {content}\n\n"

            # 6. 완료 신호
            yield "data: [DONE]\n\n"

            # 7. QA 캐싱 (백그라운드에서 비동기 실행)
            related_log_ids = [log["log_id"] for log in relevant_logs_data]
            ttl = chatbot_service._calculate_ttl(request.question, request.time_range)

            # asyncio.create_task로 백그라운드 실행 (스트림 종료 후)
            import asyncio
            asyncio.create_task(chatbot_service._cache_qa_pair(
                question=request.question,
                question_vector=question_vector,
                answer=full_answer,
                related_log_ids=related_log_ids,
                metadata={
                    "project_id": request.project_id,
                    "filters": request.filters,
                    "time_range": request.time_range,
                },
                ttl=ttl
            ))

        except Exception as e:
            # 에러 전송
            error_data = json.dumps({"error": str(e)})
            yield f"data: {error_data}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # Nginx 버퍼링 비활성화
        }
    )
```

#### 핵심 포인트

**SSE 형식**:
```
data: Hello
data:  world
data: !

data: [DONE]

```

각 메시지는 `data: {content}\n\n`으로 구성:
- `data: `: 프리픽스
- `{content}`: 실제 내용
- `\n\n`: 메시지 구분자 (2개의 줄바꿈)

---

### 2.2. 프론트엔드 구현 (React)

#### ChatBot 컴포넌트

**파일**: `src/components/ChatBot.jsx`

```jsx
import { useState } from 'react';
import './ChatBot.css';

function ChatBot() {
    const [messages, setMessages] = useState([]);
    const [currentAnswer, setCurrentAnswer] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);
    const [input, setInput] = useState('');
    const [error, setError] = useState(null);

    /**
     * Stream 방식으로 질문 전송 및 응답 수신
     */
    const askQuestionStream = async (question) => {
        // 사용자 메시지 추가
        const userMessage = {
            type: 'user',
            content: question,
            timestamp: new Date()
        };
        setMessages(prev => [...prev, userMessage]);

        // 초기화
        setCurrentAnswer('');
        setIsStreaming(true);
        setError(null);

        try {
            // Fetch API로 POST 요청
            const response = await fetch('/api/v1/chatbot/ask/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    question: question,
                    project_id: 'proj-123', // 실제 프로젝트 ID 사용
                    filters: null,
                    time_range: null
                })
            });

            // HTTP 에러 체크
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // ReadableStream 읽기
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');

            let buffer = ''; // 불완전한 청크를 위한 버퍼
            let fullAnswer = ''; // 전체 답변 누적

            while (true) {
                const { done, value } = await reader.read();

                if (done) {
                    console.log('Stream 완료');
                    break;
                }

                // Uint8Array를 문자열로 디코딩
                const chunk = decoder.decode(value, { stream: true });
                buffer += chunk;

                // 줄바꿈 기준으로 분리
                const lines = buffer.split('\n');

                // 마지막 라인은 불완전할 수 있으므로 버퍼에 보관
                buffer = lines.pop() || '';

                // 각 라인 처리
                for (const line of lines) {
                    // SSE 형식: "data: {content}"
                    if (line.startsWith('data: ')) {
                        const data = line.slice(6); // 'data: ' 제거

                        // 종료 신호 체크
                        if (data === '[DONE]') {
                            console.log('LLM 생성 완료');
                            continue;
                        }

                        // 에러 체크
                        try {
                            const parsed = JSON.parse(data);
                            if (parsed.error) {
                                throw new Error(parsed.error);
                            }
                        } catch (e) {
                            // JSON이 아니면 일반 텍스트로 처리
                        }

                        // 답변 누적 및 화면 업데이트
                        fullAnswer += data;
                        setCurrentAnswer(fullAnswer);
                    }
                }
            }

            // 봇 메시지 추가 (스트리밍 완료 후)
            const botMessage = {
                type: 'bot',
                content: fullAnswer,
                timestamp: new Date(),
                fromCache: false // Stream은 캐시 아님
            };
            setMessages(prev => [...prev, botMessage]);

        } catch (err) {
            console.error('Stream 에러:', err);
            setError(err.message);

            // 에러 메시지 표시
            const errorMessage = {
                type: 'bot',
                content: `오류가 발생했습니다: ${err.message}`,
                timestamp: new Date(),
                isError: true
            };
            setMessages(prev => [...prev, errorMessage]);

        } finally {
            setIsStreaming(false);
            setCurrentAnswer('');
            setInput('');
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (input.trim() && !isStreaming) {
            askQuestionStream(input.trim());
        }
    };

    return (
        <div className="chatbot-container">
            <div className="chatbot-header">
                <h2>로그 분석 챗봇</h2>
            </div>

            <div className="messages-container">
                {/* 이전 메시지들 */}
                {messages.map((msg, idx) => (
                    <div key={idx} className={`message ${msg.type} ${msg.isError ? 'error' : ''}`}>
                        <div className="message-content">
                            {msg.content}
                        </div>
                        <div className="message-timestamp">
                            {msg.timestamp.toLocaleTimeString('ko-KR')}
                        </div>
                    </div>
                ))}

                {/* 현재 스트리밍 중인 답변 */}
                {isStreaming && currentAnswer && (
                    <div className="message bot streaming">
                        <div className="message-content">
                            {currentAnswer}
                            <span className="typing-cursor">▊</span>
                        </div>
                    </div>
                )}

                {/* 에러 표시 */}
                {error && (
                    <div className="error-banner">
                        ⚠️ {error}
                    </div>
                )}
            </div>

            <form onSubmit={handleSubmit} className="input-form">
                <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    placeholder="로그에 대해 질문하세요..."
                    disabled={isStreaming}
                    className="chat-input"
                />
                <button
                    type="submit"
                    disabled={isStreaming || !input.trim()}
                    className="send-button"
                >
                    {isStreaming ? '답변 중...' : '전송'}
                </button>
            </form>
        </div>
    );
}

export default ChatBot;
```

#### CSS 스타일

**파일**: `src/components/ChatBot.css`

```css
.chatbot-container {
    display: flex;
    flex-direction: column;
    height: 600px;
    max-width: 800px;
    margin: 0 auto;
    border: 1px solid #ddd;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.chatbot-header {
    background: #4CAF50;
    color: white;
    padding: 1rem;
    text-align: center;
}

.messages-container {
    flex: 1;
    overflow-y: auto;
    padding: 1rem;
    background: #f9f9f9;
}

.message {
    margin-bottom: 1rem;
    padding: 0.75rem;
    border-radius: 8px;
    max-width: 70%;
    word-wrap: break-word;
}

.message.user {
    background: #2196F3;
    color: white;
    margin-left: auto;
    text-align: right;
}

.message.bot {
    background: white;
    border: 1px solid #ddd;
}

.message.error {
    background: #ffebee;
    border: 1px solid #f44336;
    color: #d32f2f;
}

.message.streaming {
    border: 2px solid #4CAF50;
}

.message-content {
    white-space: pre-wrap;
    font-size: 0.95rem;
    line-height: 1.5;
}

.message-timestamp {
    font-size: 0.75rem;
    color: #888;
    margin-top: 0.5rem;
}

/* 타이핑 커서 애니메이션 */
@keyframes blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
}

.typing-cursor {
    animation: blink 1s infinite;
    font-weight: bold;
    color: #4CAF50;
    margin-left: 2px;
}

.input-form {
    display: flex;
    gap: 0.5rem;
    padding: 1rem;
    background: white;
    border-top: 1px solid #ddd;
}

.chat-input {
    flex: 1;
    padding: 0.75rem;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 1rem;
}

.chat-input:disabled {
    background: #f0f0f0;
    cursor: not-allowed;
}

.send-button {
    padding: 0.75rem 1.5rem;
    background: #4CAF50;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: bold;
}

.send-button:hover:not(:disabled) {
    background: #45a049;
}

.send-button:disabled {
    background: #ccc;
    cursor: not-allowed;
}

.error-banner {
    background: #ffebee;
    border: 1px solid #f44336;
    color: #d32f2f;
    padding: 0.75rem;
    border-radius: 4px;
    margin-bottom: 1rem;
}
```

---

## 3. Chat History 구현

### 3.1. 개념

**Chat History**는 이전 대화 내용을 LLM에 전달하여 문맥을 이해할 수 있게 합니다.

```
대화 흐름:

[Turn 1]
User: "최근 에러 알려줘"
Bot: "NPE 3건, DB 타임아웃 2건 발생했습니다"

[Turn 2]
User: "그 중 가장 심각한 건?"
                 ↑
            대명사 "그"가 무엇을 가리키는지?
            → Turn 1의 맥락이 필요!

History 없이:
Bot: "무엇을 말씀하시는지 모르겠습니다" ❌

History 있으면:
LLM Input:
  [History]
  - User: "최근 에러 알려줘"
  - Bot: "NPE 3건, DB 타임아웃 2건 발생했습니다"

  [Current]
  - User: "그 중 가장 심각한 건?"

Bot: "앞서 말씀드린 DB 타임아웃이 가장 심각합니다" ✅
```

### 3.2. 백엔드 구현

#### Step 1: 모델 수정

**파일**: `app/models/chat.py`

```python
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

class ChatMessage(BaseModel):
    """단일 채팅 메시지"""
    role: str = Field(..., description="'user' 또는 'assistant'")
    content: str = Field(..., description="메시지 내용")

class ChatRequest(BaseModel):
    """Chatbot question request with history support"""

    question: str = Field(..., description="User's question about logs")
    project_id: str = Field(..., description="Project ID for multi-tenancy isolation")

    # History 추가
    chat_history: Optional[List[ChatMessage]] = Field(
        default=None,
        description="Previous conversation history"
    )

    filters: Optional[Dict[str, Any]] = Field(None, description="Optional filters for log search")
    time_range: Optional[Dict[str, str]] = Field(
        None, description="Time range filter (start, end)"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "question": "그 중 가장 심각한 건?",
                "project_id": "proj-123",
                "chat_history": [
                    {"role": "user", "content": "최근 에러 알려줘"},
                    {"role": "assistant", "content": "NPE 3건, DB 타임아웃 2건 발생했습니다"}
                ],
                "filters": None,
                "time_range": None,
            }
        }
```

#### Step 2: 체인 수정 (LangChain)

**파일**: `app/chains/chatbot_chain.py`

```python
"""
LangChain chain for chatbot QA with history support
"""

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.messages import HumanMessage, AIMessage
from app.core.config import settings


# Initialize LLM
llm = ChatOpenAI(
    model=settings.LLM_MODEL,
    temperature=0.7,
    api_key=settings.OPENAI_API_KEY,
    base_url=settings.OPENAI_BASE_URL,
)

# Prompt template with history support
chatbot_prompt = ChatPromptTemplate.from_messages([
    (
        "system",
        """You are a helpful log analysis assistant. Answer questions about application logs based on the provided context.

Guidelines:
- Use the context logs to provide accurate, specific answers
- Consider the conversation history when answering
- If the context doesn't contain relevant information, say so clearly
- Provide actionable insights when possible
- Use clear, concise language
- Include relevant log details (timestamps, error counts, patterns)
- Answer in Korean if the question is in Korean, English if in English"""
    ),

    # 대화 기록 추가 (동적)
    MessagesPlaceholder(variable_name="chat_history", optional=True),

    (
        "human",
        """Context - Recent Logs:
{context_logs}

Question: {question}

Answer:"""
    ),
])

# Create the chain
chatbot_chain = chatbot_prompt | llm
```

**핵심**: `MessagesPlaceholder`
- `variable_name="chat_history"`: 이 이름으로 히스토리를 받음
- `optional=True`: 히스토리가 없어도 동작 (첫 대화)

#### Step 3: 서비스 수정

**파일**: `app/services/chatbot_service.py`

기존 `ask()` 메서드 수정:

```python
from langchain_core.messages import HumanMessage, AIMessage

async def ask(
    self,
    question: str,
    project_id: str,
    chat_history: Optional[List[Dict[str, str]]] = None,
    filters: Optional[Dict[str, Any]] = None,
    time_range: Optional[Dict[str, str]] = None,
) -> ChatResponse:
    """
    Answer a question with chat history support

    Args:
        question: User's question
        project_id: Project ID
        chat_history: Previous conversation (list of {role, content})
        filters: Optional filters
        time_range: Optional time range
    """
    # ... (기존 캐시/벡터 검색 로직) ...

    # 컨텍스트 준비
    context_logs = self._format_context_logs(relevant_logs_data)

    # Chat history를 LangChain 메시지로 변환
    history_messages = []
    if chat_history:
        for msg in chat_history[-10:]:  # 최근 10개만 (토큰 절약)
            if msg["role"] == "user":
                history_messages.append(HumanMessage(content=msg["content"]))
            elif msg["role"] == "assistant":
                history_messages.append(AIMessage(content=msg["content"]))

    # LLM 호출 (히스토리 포함)
    response = await chatbot_chain.ainvoke({
        "context_logs": context_logs,
        "question": question,
        "chat_history": history_messages,  # 히스토리 전달
    })

    answer = response.content

    # ... (캐싱 로직) ...

    return ChatResponse(...)
```

**중요**: 히스토리 개수 제한
- `chat_history[-10:]`: 최근 10개만 사용
- 이유: 토큰 절약 + 너무 오래된 대화는 관련성 낮음

---

### 3.3. 프론트엔드 수정

**파일**: `src/components/ChatBot.jsx`

```jsx
function ChatBot() {
    const [messages, setMessages] = useState([]);
    const [currentAnswer, setCurrentAnswer] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);
    const [input, setInput] = useState('');

    /**
     * 메시지 배열을 chat_history 형식으로 변환
     */
    const buildChatHistory = () => {
        return messages.map(msg => ({
            role: msg.type === 'user' ? 'user' : 'assistant',
            content: msg.content
        }));
    };

    const askQuestionStream = async (question) => {
        // 사용자 메시지 추가
        const userMessage = {
            type: 'user',
            content: question,
            timestamp: new Date()
        };
        setMessages(prev => [...prev, userMessage]);

        setCurrentAnswer('');
        setIsStreaming(true);

        try {
            // History 포함하여 요청
            const chatHistory = buildChatHistory();

            const response = await fetch('/api/v1/chatbot/ask/stream', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    question: question,
                    project_id: 'proj-123',
                    chat_history: chatHistory,  // 히스토리 전달
                    filters: null,
                    time_range: null
                })
            });

            // ... (기존 스트리밍 로직 동일) ...

        } catch (err) {
            // ...
        }
    };

    // ... (나머지 코드 동일) ...
}
```

**핵심**:
- `buildChatHistory()`: 기존 메시지를 API 형식으로 변환
- 매 요청마다 전체 히스토리를 서버에 전달

---

### 3.4. 메모리 관리

#### 문제: 대화가 길어지면?

```
대화 100턴 × 평균 500토큰 = 50,000 토큰!
→ 비용 폭발 💸
```

#### 해결: 슬라이딩 윈도우

**백엔드** (이미 구현됨):
```python
# 최근 10개만 사용
history_messages = []
if chat_history:
    for msg in chat_history[-10:]:  # 최근 10개
        # ...
```

**프론트엔드** (추가 최적화):
```jsx
const buildChatHistory = () => {
    // 최근 20개 메시지만 (10턴)
    const recentMessages = messages.slice(-20);

    return recentMessages.map(msg => ({
        role: msg.type === 'user' ? 'user' : 'assistant',
        content: msg.content
    }));
};
```

#### 추가 최적화: 요약 기반 히스토리

매우 긴 대화의 경우:

```python
# 오래된 대화는 요약하여 저장
if len(chat_history) > 20:
    # 처음 10개는 요약
    old_messages = chat_history[:10]
    summary = await summarize_conversation(old_messages)

    # 요약 + 최근 10개
    condensed_history = [
        {"role": "system", "content": f"Previous context: {summary}"}
    ] + chat_history[-10:]
else:
    condensed_history = chat_history
```

---

## 4. Stream + History 통합

### 4.1. 전체 아키텍처

```
┌──────────────────────────────────────────────────────────┐
│                Frontend (React)                          │
└──────────────────────────────────────────────────────────┘
                         │
                         │ POST /chatbot/ask/stream
                         │ Body: {
                         │   question,
                         │   project_id,
                         │   chat_history: [...]  ← 이전 대화
                         │ }
                         ▼
┌──────────────────────────────────────────────────────────┐
│           FastAPI: ask_chatbot_stream()                  │
│  1. 캐시 체크 (question + history 기반)                   │
│  2. 벡터 검색 (관련 로그 찾기)                            │
│  3. LLM 호출 (히스토리 포함)                              │
└──────────────────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│         LangChain: chatbot_chain.astream()               │
│  ┌────────────────────────────────────────┐              │
│  │ System: "You are a helpful assistant"  │              │
│  ├────────────────────────────────────────┤              │
│  │ History:                               │              │
│  │  - User: "최근 에러 알려줘"              │              │
│  │  - AI: "NPE 3건, DB 2건"                │              │
│  ├────────────────────────────────────────┤              │
│  │ Context: [로그 5개]                     │              │
│  ├────────────────────────────────────────┤              │
│  │ Human: "가장 심각한 건?"                │              │
│  └────────────────────────────────────────┘              │
└──────────────────────────────────────────────────────────┘
                         │
                         │ Stream chunks
                         ▼
┌──────────────────────────────────────────────────────────┐
│         SSE Stream Response                              │
│  data: 앞서                                              │
│  data:  말씀드린                                          │
│  data:  DB 타임아웃이                                     │
│  data:  가장 심각합니다                                   │
│  data: [DONE]                                            │
└──────────────────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│      Frontend: Real-time UI Update                      │
│  ┌──────────────────────────────────────┐                │
│  │ User: 최근 에러 알려줘                │                │
│  │ Bot: NPE 3건, DB 2건                 │                │
│  │                                      │                │
│  │ User: 가장 심각한 건?                │                │
│  │ Bot: 앞서 말씀드린 DB 타임아웃...▊   │  ← 타이핑 중    │
│  └──────────────────────────────────────┘                │
└──────────────────────────────────────────────────────────┘
```

### 4.2. 통합 코드

#### 백엔드 (최종)

**파일**: `app/api/v1/chatbot.py`

```python
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from langchain_core.messages import HumanMessage, AIMessage
import json

from app.services.chatbot_service import chatbot_service
from app.services.embedding_service import embedding_service
from app.services.similarity_service import similarity_service
from app.chains.chatbot_chain import chatbot_chain
from app.models.chat import ChatRequest
from app.core.config import settings

router = APIRouter()

@router.post("/chatbot/ask/stream")
async def ask_chatbot_stream(request: ChatRequest):
    """
    Stream + History 통합 엔드포인트
    """
    async def generate():
        try:
            # 1. 임베딩 생성
            question_vector = await embedding_service.embed_query(request.question)

            # 2. 캐시 체크 (생략 가능, 히스토리 때문에 캐시 히트율 낮음)
            # ...

            # 3. 관련 로그 검색
            relevant_logs_data = await similarity_service.find_similar_logs(
                log_vector=question_vector,
                k=chatbot_service.max_context,
                filters=request.filters,
                project_id=request.project_id,
            )

            # 4. 컨텍스트 준비
            context_logs = chatbot_service._format_context_logs(relevant_logs_data)

            # 5. Chat history 변환
            history_messages = []
            if request.chat_history:
                for msg in request.chat_history[-10:]:  # 최근 10개
                    if msg.role == "user":
                        history_messages.append(HumanMessage(content=msg.content))
                    elif msg.role == "assistant":
                        history_messages.append(AIMessage(content=msg.content))

            # 6. LLM 스트리밍 (히스토리 포함)
            async for chunk in chatbot_chain.astream({
                "context_logs": context_logs,
                "question": request.question,
                "chat_history": history_messages,  # 히스토리
            }):
                content = chunk.content
                yield f"data: {content}\n\n"

            # 7. 완료
            yield "data: [DONE]\n\n"

        except Exception as e:
            error_data = json.dumps({"error": str(e)})
            yield f"data: {error_data}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        }
    )
```

#### 프론트엔드 (최종)

**파일**: `src/components/ChatBot.jsx`

```jsx
import { useState, useRef, useEffect } from 'react';
import './ChatBot.css';

function ChatBot() {
    const [messages, setMessages] = useState([]);
    const [currentAnswer, setCurrentAnswer] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);
    const [input, setInput] = useState('');
    const messagesEndRef = useRef(null);

    // 자동 스크롤
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, currentAnswer]);

    const buildChatHistory = () => {
        // 최근 20개 메시지만 (10턴)
        const recentMessages = messages.slice(-20);
        return recentMessages.map(msg => ({
            role: msg.type === 'user' ? 'user' : 'assistant',
            content: msg.content
        }));
    };

    const askQuestionStream = async (question) => {
        const userMessage = {
            type: 'user',
            content: question,
            timestamp: new Date()
        };
        setMessages(prev => [...prev, userMessage]);

        setCurrentAnswer('');
        setIsStreaming(true);

        try {
            const chatHistory = buildChatHistory();

            const response = await fetch('/api/v1/chatbot/ask/stream', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    question,
                    project_id: 'proj-123',
                    chat_history: chatHistory,
                    filters: null,
                    time_range: null
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';
            let fullAnswer = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value, { stream: true });
                buffer += chunk;
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';

                for (const line of lines) {
                    if (line.startsWith('data: ')) {
                        const data = line.slice(6);

                        if (data === '[DONE]') continue;

                        try {
                            const parsed = JSON.parse(data);
                            if (parsed.error) throw new Error(parsed.error);
                        } catch {}

                        fullAnswer += data;
                        setCurrentAnswer(fullAnswer);
                    }
                }
            }

            const botMessage = {
                type: 'bot',
                content: fullAnswer,
                timestamp: new Date()
            };
            setMessages(prev => [...prev, botMessage]);

        } catch (err) {
            console.error('Error:', err);
            const errorMessage = {
                type: 'bot',
                content: `오류: ${err.message}`,
                timestamp: new Date(),
                isError: true
            };
            setMessages(prev => [...prev, errorMessage]);
        } finally {
            setIsStreaming(false);
            setCurrentAnswer('');
            setInput('');
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (input.trim() && !isStreaming) {
            askQuestionStream(input.trim());
        }
    };

    return (
        <div className="chatbot-container">
            <div className="chatbot-header">
                <h2>🤖 로그 분석 챗봇</h2>
                <p>이전 대화를 기억하며 질문에 답변합니다</p>
            </div>

            <div className="messages-container">
                {messages.map((msg, idx) => (
                    <div key={idx} className={`message ${msg.type} ${msg.isError ? 'error' : ''}`}>
                        <div className="message-avatar">
                            {msg.type === 'user' ? '👤' : '🤖'}
                        </div>
                        <div className="message-bubble">
                            <div className="message-content">{msg.content}</div>
                            <div className="message-timestamp">
                                {msg.timestamp.toLocaleTimeString('ko-KR')}
                            </div>
                        </div>
                    </div>
                ))}

                {isStreaming && currentAnswer && (
                    <div className="message bot streaming">
                        <div className="message-avatar">🤖</div>
                        <div className="message-bubble">
                            <div className="message-content">
                                {currentAnswer}
                                <span className="typing-cursor">▊</span>
                            </div>
                        </div>
                    </div>
                )}

                <div ref={messagesEndRef} />
            </div>

            <form onSubmit={handleSubmit} className="input-form">
                <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    placeholder="로그에 대해 질문하세요..."
                    disabled={isStreaming}
                    className="chat-input"
                />
                <button
                    type="submit"
                    disabled={isStreaming || !input.trim()}
                    className="send-button"
                >
                    {isStreaming ? '⏳' : '📤'}
                </button>
            </form>
        </div>
    );
}

export default ChatBot;
```

---

## 5. 실습 가이드

### 5.1. 단계별 구현

#### Phase 1: Stream만 먼저 구현

**목표**: 기본 스트리밍 동작 확인

1. **백엔드**: `app/api/v1/chatbot.py`에 `/chatbot/ask/stream` 추가
2. **프론트엔드**: 간단한 테스트 페이지 작성
3. **테스트**: "최근 에러 알려줘" 입력 → 실시간 타이핑 확인

**테스트 코드**:
```bash
# cURL로 테스트
curl -X POST http://localhost:8000/api/v1/chatbot/ask/stream \
  -H "Content-Type: application/json" \
  -d '{
    "question": "최근 에러 알려줘",
    "project_id": "proj-123"
  }'

# 출력 예시:
# data: 최근
# data:  24시간
# data:  동안
# ...
# data: [DONE]
```

#### Phase 2: History 추가

**목표**: 이전 대화 기억하는지 확인

1. **모델 수정**: `ChatRequest`에 `chat_history` 추가
2. **체인 수정**: `MessagesPlaceholder` 추가
3. **프론트엔드**: 히스토리 전달 로직 추가
4. **테스트**: 연속 대화 시나리오

**테스트 시나리오**:
```
Turn 1:
  User: "최근 NPE 에러 알려줘"
  Bot: "UserService에서 3건 발생했습니다"

Turn 2 (히스토리 포함):
  User: "그거 언제 발생한 거야?"
  Bot: "앞서 말씀드린 NPE는 오늘 오전 10시에 발생했습니다" ✅
```

#### Phase 3: 최적화

1. **메모리 제한**: 최근 10개 턴만 유지
2. **UI 개선**: 아바타, 타임스탬프, 자동 스크롤
3. **에러 처리**: 타임아웃, 재시도 로직

---

### 5.2. 테스트 체크리스트

#### Stream 테스트

- [ ] 첫 청크가 1초 이내에 도착하는가?
- [ ] 타이핑 효과가 자연스러운가?
- [ ] 긴 답변(500자 이상)도 스트리밍되는가?
- [ ] [DONE] 신호 수신 후 스트림이 종료되는가?
- [ ] 에러 발생 시 에러 메시지가 표시되는가?

#### History 테스트

- [ ] 이전 대화 맥락을 이해하는가?
  - "그거", "그 중", "앞에서" 등 대명사 이해
- [ ] 10턴 이상 대화 시 메모리 제한이 동작하는가?
- [ ] 새 세션 시작 시 히스토리가 초기화되는가?
- [ ] 히스토리가 없는 첫 질문도 정상 동작하는가?

#### 통합 테스트

- [ ] Stream + History 동시 사용 시 정상 동작하는가?
- [ ] 여러 사용자가 동시에 사용해도 히스토리가 섞이지 않는가?
- [ ] 브라우저 새로고침 후 히스토리가 유지되는가? (로컬 스토리지 사용 시)

---

## 6. 트러블슈팅

### 6.1. Stream 관련

#### 문제: 첫 청크가 늦게 도착함

**증상**: 5-10초 후에야 첫 청크가 옴

**원인**:
- 벡터 검색이 느림
- OpenSearch 인덱스 최적화 필요

**해결**:
```python
# 벡터 검색 타임아웃 설정
relevant_logs = await asyncio.wait_for(
    similarity_service.find_similar_logs(...),
    timeout=3.0  # 3초 타임아웃
)
```

또는 즉시 응답:
```python
# 청크 하나를 먼저 보내기
yield "data: 답변을 생성하고 있습니다...\n\n"

# 그 후 검색
relevant_logs = await search_logs(...)
```

#### 문제: Nginx에서 버퍼링됨

**증상**: 답변이 다 생성된 후 한번에 옴

**원인**: Nginx가 SSE를 버퍼링

**해결**:
```nginx
# nginx.conf
location /api/v1/chatbot/ask/stream {
    proxy_pass http://backend;
    proxy_buffering off;  # 버퍼링 비활성화
    proxy_cache off;
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    chunked_transfer_encoding off;
}
```

FastAPI 헤더:
```python
return StreamingResponse(
    generate(),
    headers={
        "X-Accel-Buffering": "no",  # Nginx 버퍼링 강제 비활성화
    }
)
```

#### 문제: 청크가 깨져서 옴

**증상**: "안녕하" + "세요" → "안녕하ì„¸ìš"" (인코딩 깨짐)

**원인**: UTF-8 멀티바이트 문자가 청크 경계에서 분리됨

**해결**:
```javascript
// decoder에 stream: true 옵션
const decoder = new TextDecoder('utf-8');

while (true) {
    const { done, value } = await reader.read();

    // stream: true로 불완전한 문자 보존
    const chunk = decoder.decode(value, { stream: true });
    // ...
}
```

---

### 6.2. History 관련

#### 문제: 히스토리가 너무 길어짐

**증상**: 10턴 이상 대화 시 응답 느려짐, 비용 증가

**해결**:
```python
# 슬라이딩 윈도우 (최근 10개만)
history_messages = []
if chat_history:
    for msg in chat_history[-10:]:  # 최근 10개
        # ...
```

또는 토큰 기반:
```python
import tiktoken

def trim_history_by_tokens(chat_history, max_tokens=2000):
    """히스토리를 토큰 제한 내로 자르기"""
    enc = tiktoken.encoding_for_model("gpt-4o-mini")

    total_tokens = 0
    trimmed = []

    # 최신 메시지부터 역순으로
    for msg in reversed(chat_history):
        msg_tokens = len(enc.encode(msg["content"]))

        if total_tokens + msg_tokens > max_tokens:
            break

        trimmed.insert(0, msg)
        total_tokens += msg_tokens

    return trimmed
```

#### 문제: 맥락을 잘못 이해함

**증상**: "그거" → 전혀 다른 것을 가리킴

**원인**:
- 히스토리가 너무 오래됨
- 여러 주제가 섞여있음

**해결**:
```python
# 프롬프트 개선
chatbot_prompt = ChatPromptTemplate.from_messages([
    ("system", """...

    Important:
    - Pay special attention to pronouns (그거, 그것, 이거) in the current question
    - Refer to the most recent relevant context in the chat history
    - If unsure about what a pronoun refers to, ask for clarification
    """),
    # ...
])
```

#### 문제: 세션 관리

**증상**: 페이지 새로고침 시 히스토리 사라짐

**해결**: 로컬 스토리지 사용

```jsx
// 메시지 저장
useEffect(() => {
    localStorage.setItem('chatHistory', JSON.stringify(messages));
}, [messages]);

// 메시지 복원
useEffect(() => {
    const saved = localStorage.getItem('chatHistory');
    if (saved) {
        setMessages(JSON.parse(saved));
    }
}, []);

// 초기화 버튼
const clearHistory = () => {
    setMessages([]);
    localStorage.removeItem('chatHistory');
};
```

---

### 6.3. 성능 최적화

#### 병렬 처리

캐시 체크와 벡터 검색을 병렬로:

```python
import asyncio

# 순차 (느림)
cached = await check_cache(question)
logs = await search_logs(question)

# 병렬 (빠름)
cached, logs = await asyncio.gather(
    check_cache(question),
    search_logs(question)
)
```

#### 조기 종료

캐시 히트 시 즉시 반환:

```python
async def generate():
    # 캐시 체크 (빠름)
    cached = await check_cache(...)
    if cached:
        yield f"data: {cached['answer']}\n\n"
        yield "data: [DONE]\n\n"
        return  # 조기 종료, 벡터 검색 안 함!

    # 캐시 미스 시에만 검색
    logs = await search_logs(...)
```

---

## 7. 추가 기능 (선택)

### 7.1. 타이핑 속도 조절

느린 타이핑 효과:

```python
async def generate():
    async for chunk in chatbot_chain.astream({...}):
        yield f"data: {chunk.content}\n\n"

        # 인위적인 딜레이 (선택)
        await asyncio.sleep(0.05)  # 50ms 딜레이
```

### 7.2. 중단 기능

사용자가 답변 생성을 취소:

```jsx
const abortControllerRef = useRef(null);

const askQuestionStream = async (question) => {
    // AbortController 생성
    abortControllerRef.current = new AbortController();

    const response = await fetch('/api/v1/chatbot/ask/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({...}),
        signal: abortControllerRef.current.signal  // 중단 신호
    });

    // ...
};

const stopGeneration = () => {
    if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        setIsStreaming(false);
    }
};

// UI
{isStreaming && (
    <button onClick={stopGeneration}>중단</button>
)}
```

### 7.3. 음성 입력

```jsx
const startVoiceInput = () => {
    const recognition = new webkitSpeechRecognition();
    recognition.lang = 'ko-KR';

    recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setInput(transcript);
    };

    recognition.start();
};
```

---

## 8. 요약

### Stream 구현 핵심

```python
# 백엔드
async def generate():
    async for chunk in chatbot_chain.astream({...}):
        yield f"data: {chunk.content}\n\n"
    yield "data: [DONE]\n\n"

return StreamingResponse(generate(), media_type="text/event-stream")
```

```jsx
// 프론트엔드
const reader = response.body.getReader();
const decoder = new TextDecoder();

while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = decoder.decode(value, { stream: true });
    // SSE 파싱 후 UI 업데이트
}
```

### History 구현 핵심

```python
# 백엔드: MessagesPlaceholder
from langchain_core.prompts import MessagesPlaceholder

chatbot_prompt = ChatPromptTemplate.from_messages([
    ("system", "..."),
    MessagesPlaceholder(variable_name="chat_history", optional=True),
    ("human", "{question}")
])

# 히스토리 변환
history_messages = [
    HumanMessage(content=msg["content"]) if msg["role"] == "user"
    else AIMessage(content=msg["content"])
    for msg in chat_history[-10:]  # 최근 10개
]
```

```jsx
// 프론트엔드: 히스토리 구축
const buildChatHistory = () => {
    return messages.slice(-20).map(msg => ({
        role: msg.type === 'user' ? 'user' : 'assistant',
        content: msg.content
    }));
};
```

### 체크리스트

- [x] Stream 엔드포인트 구현
- [x] SSE 형식 준수
- [x] 프론트엔드 ReadableStream 처리
- [x] MessagesPlaceholder 추가
- [x] 히스토리 제한 (10턴)
- [x] 에러 처리
- [x] UI/UX 개선 (타이핑 커서, 자동 스크롤)

완료! 🎉
