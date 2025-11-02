"""
LangChain chain for log summarization (Map phase in Map-Reduce)
"""

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.core.config import settings


# Initialize LLM with lower temperature for consistent summaries
llm = ChatOpenAI(
    model=settings.LLM_MODEL,
    temperature=0.1,  # Low temperature for consistency
    api_key=settings.OPENAI_API_KEY,
    base_url=settings.OPENAI_BASE_URL,
)

# Prompt template for summarizing a chunk of logs
log_summarization_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """You are a log summarization expert. Your task is to summarize a chunk of application logs into 2-3 concise sentences.

Focus on:
- Key events that occurred
- Any errors or warnings
- Critical patterns or anomalies

Be extremely concise. Extract only the most important information.""",
        ),
        (
            "human",
            """Summarize this chunk of logs (Chunk {chunk_number}/{total_chunks}):

{logs}

Provide a 2-3 sentence summary focusing on the most critical information.""",
        ),
    ]
)

# Create the summarization chain
log_summarization_chain = log_summarization_prompt | llm
