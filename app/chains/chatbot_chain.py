"""
LangChain chain for chatbot QA with history support
"""

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from app.core.config import settings


# Initialize LLM
llm = ChatOpenAI(
    model=settings.LLM_MODEL,
    temperature=0.7,
    api_key=settings.OPENAI_API_KEY,
    base_url=settings.OPENAI_BASE_URL,
)

# Prompt template for RAG-based chatbot with history support
chatbot_prompt = ChatPromptTemplate.from_messages(
    [
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
- Answer in Korean if the question is in Korean, English if in English""",
        ),
        # Chat history placeholder (이전 대화 기록)
        MessagesPlaceholder(variable_name="chat_history", optional=True),
        (
            "human",
            """Context - Recent Logs:
{context_logs}

Question: {question}

Answer:""",
        ),
    ]
)

# Create the chain
chatbot_chain = chatbot_prompt | llm
