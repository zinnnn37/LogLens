"""
LangChain chain for log analysis
"""

from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from langchain.output_parsers import PydanticOutputParser
from app.core.config import settings
from app.models.analysis import LogAnalysisResult


# Initialize LLM
llm = ChatOpenAI(
    model=settings.LLM_MODEL,
    temperature=0.3,
    openai_api_key=settings.OPENAI_API_KEY,
)

# Output parser
output_parser = PydanticOutputParser(pydantic_object=LogAnalysisResult)

# Prompt template
log_analysis_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """You are an expert log analysis AI. Analyze the given application log and provide:
1. A brief summary of what happened
2. The root cause of the error (if applicable)
3. A suggested solution or next steps
4. Relevant tags for categorization

Be concise and actionable. Focus on helping developers quickly understand and resolve issues.

{format_instructions}""",
        ),
        (
            "human",
            """Analyze this log:

Service: {service_name}
Level: {level}
Message: {message}
Method: {method_name}
Class: {class_name}
Timestamp: {timestamp}
Duration: {duration}ms
Stack Trace: {stack_trace}
Additional Context: {additional_context}""",
        ),
    ]
)

# Create the chain
log_analysis_chain = (
    log_analysis_prompt.partial(format_instructions=output_parser.get_format_instructions())
    | llm
    | output_parser
)
