"""
LangChain chain for log analysis
"""

from typing import Dict, Any
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import PydanticOutputParser
from langchain_core.runnables import RunnableLambda
from app.core.config import settings
from app.models.analysis import LogAnalysisResult


# Initialize LLM
llm = ChatOpenAI(
    model=settings.LLM_MODEL,
    temperature=0.3,
    api_key=settings.OPENAI_API_KEY,
    base_url=settings.OPENAI_BASE_URL,
)

# Output parser
output_parser = PydanticOutputParser(pydantic_object=LogAnalysisResult)


def format_log_content(inputs: Dict[str, Any]) -> Dict[str, Any]:
    """
    Format log content based on input type (single log vs trace-based analysis)

    Args:
        inputs: Dictionary containing log data

    Returns:
        Dictionary with formatted log_content field added
    """
    if "logs_context" in inputs:
        # Trace-based analysis
        log_content = f"""Trace Analysis for Trace ID: {inputs.get('trace_id', 'N/A')}
Total Logs: {inputs.get('total_logs', 0)}
Center Log ID: {inputs.get('center_log_id', '')}
Service: {inputs.get('service_name', 'Unknown')}

=== All Logs in Trace (±3 seconds) ===
{inputs.get('logs_context', '')}

Analyze the entire trace flow above and provide insights on:
1. What happened across all these logs
2. Root cause of any errors
3. How to fix the issue"""
    else:
        # Single log analysis
        log_content = f"""Service: {inputs.get('service_name', 'Unknown')}
Level: {inputs.get('level', 'Unknown')}
Message: {inputs.get('message', '')}
Method: {inputs.get('method_name', 'N/A')}
Class: {inputs.get('class_name', 'N/A')}
Timestamp: {inputs.get('timestamp', '')}
Duration: {inputs.get('duration', 'N/A')}ms
Stack Trace: {inputs.get('stack_trace', 'N/A')}
Additional Context: {inputs.get('additional_context', {})}"""

    # Return inputs with log_content added
    return {**inputs, "log_content": log_content}


# Prompt template (supports both single log and trace context)
log_analysis_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """You are an expert log analysis AI. Analyze the given application log(s) and provide:
1. A brief summary of what happened
2. The root cause of the error (if applicable)
3. A suggested solution or next steps
4. Relevant tags for categorization

When analyzing multiple logs (trace context), consider the entire flow to identify:
- The sequence of events leading to the error
- Which component or service caused the issue
- The propagation of errors across services

Be concise and actionable. Focus on helping developers quickly understand and resolve issues.

{format_instructions}""",
        ),
        (
            "human",
            """Analyze this log:

{log_content}""",
        ),
    ]
)

# Create the chain with RunnableLambda for dynamic log formatting
log_analysis_chain = (
    RunnableLambda(format_log_content)
    | log_analysis_prompt.partial(format_instructions=output_parser.get_format_instructions())
    | llm
    | output_parser
)
