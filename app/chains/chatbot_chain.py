"""
LangChain chain for chatbot QA
"""

from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from app.core.config import settings


# Initialize LLM
llm = ChatOpenAI(
    model=settings.LLM_MODEL,
    temperature=0.7,
    api_key=settings.OPENAI_API_KEY,
    base_url=settings.OPENAI_BASE_URL,
)

# Prompt template for RAG-based chatbot
chatbot_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """You are a helpful log analysis assistant. Answer questions about application logs based on the provided context.

Guidelines:
- Use the context logs to provide accurate, specific answers
- If the context doesn't contain relevant information, say so clearly
- Provide actionable insights when possible
- Use clear, concise language
- Include relevant log details (timestamps, error counts, patterns)
- Answer in Korean if the question is in Korean, English if in English""",
        ),
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
